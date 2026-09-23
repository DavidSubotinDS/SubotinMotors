# S6 notification ownership and operations

S6 is implemented on `feature/david.subotin_notification-service`; acceptance
results and remaining gates are tracked in [the handoff](notification-service-pr.md).
S5 is merged at `cddde6da41d32d3d37fae9a8eaa71cc4027cca73`; Backend and Frontend
passed on that exact commit in GitHub Actions run 35673771885. Older S4b/S5
working-tree status paragraphs in historical handoffs are not current status.

## Ownership and contracts

`services/notification-service` owns `tb_notification`, `tb_message_inbox` and
`tb_delivery` in its own schema. It has no JPA/domain dependency on another
service and no access to identity, auction or follow tables. Backend retains
follow eligibility, the ending-soon scheduler, and `tb_notification_outbox`.
Identity retains credentials, sessions, reset token hashes/consumption and
`tb_delivery_outbox`. SMTP credentials and sending move to notification.

The gateway sends `/api/user/notifications` and descendants, and legacy POST
`/user/notifications/{id}/read` and `/user/notifications/read-all`, to notification.
The SPA page remains frontend-owned. List/read/read-all DTO keys remain; nested
auction data is a public snapshot, with a backend public image URL instead of
embedded image bytes and a nullable seller display name. No private profile
data is published. Snapshots do not promise live auction state; auction detail
remains the authoritative view. Notification IDs and read timestamps survive import.

Identity remains the sole session owner. Gateway validates the browser cookie
and CSRF through private session exchange, requesting audience
`notification-service`, then strips browser credentials before forwarding.
Notification validates RS256 signature, issuer, audience, token use, expiry and
USER role; reads/mutations derive the recipient from the assertion subject.
ADMIN alone does not acquire USER permissions. Backend obtains a service token
with only `notification-count` to call `GET /internal/v1/users/{id}/unread-count`.
The backend client derives the ID from trusted authentication, uses a bounded
connection/read timeout and concurrency limit, and does not retry mutations.
Peer failure produces an unavailable response, not an invented zero count.
Gateway routing, session exchange and health probes bound cached DNS addresses
to five seconds; routed/session-exchange pooled connections also have a bounded
lifetime. This permits owner container replacement without retaining a recycled
Docker IP. Automatic transport retry is disabled, so unsafe requests are never
replayed as a recovery strategy. See the [Reactor Netty resolver documentation](https://projectreactor.io/docs/netty/release/reference/http-client.html).

## Messaging and sensitive delivery

`marketplace.auction-ending-soon.v1` uses `autostrada.events` and durable
`notification.business.v1`. The envelope has eventId, eventType, schemaVersion,
occurredAt, producer, aggregateType, aggregateId, aggregateVersion,
correlationId, causationId and payload; optional traceparent is propagated.
Version 1 accepts only the documented ending-soon producer/type and public
snapshot fields. Payload contains auctionId, recipientId, notificationType,
message, dedupeKey and auctionSnapshot. Business dedupe is
`recipient:auction:ENDING_SOON`, independently of event-ID inbox dedupe.

Versioned [JSON Schema](../services/notification-service/src/main/resources/contracts/notification-message-v1.schema.json)
and [OpenAPI](../services/notification-service/src/main/resources/contracts/notification.openapi.yaml)
live with the owner. Schema fixtures are checked in its required build.
Unknown additive envelope/payload fields are tolerated; the embedded public
profile/auction snapshot is deliberately allowlisted to prevent accidentally
exposing private fields. This privacy restriction takes precedence over the
general additive-field proposal in the architecture document. The proposed
plaintext reset command fields are inside authenticated ciphertext on the wire.

The local business transaction writes the outbox. Relays mark publication only
after a mandatory routed publish is confirmed. Consumer writes and inbox marker
commit in one database transaction before acknowledgement. Redelivery is
expected. Duplicate delivery never creates a second notification or resets read
state. There is one scheduled consumer/SMTP worker per owner in this stage.

`identity.password-reset-delivery.v1` uses restricted `autostrada.commands` and
`notification.delivery.v1`. Outer payload contains deliveryId, expiresAt and
encryptedPayload. AES-256-GCM encrypts the recipient, fixed template identifier,
reset URL, delivery ID and expiry; ID and expiry also authenticate the ciphertext
as associated data. Only identity and notification receive the delivery key.
Broker users have distinct least-privilege credentials; backend cannot publish
reset commands. Identity's reset transaction and encrypted outbox insert commit
together. Published/expired producer ciphertext is erased. Consumer ciphertext
is erased on delivery, suppression or expiry. Expired commands are not sent or
redriven. Queue/DLQ retention for reset ciphertext is bounded to 30 minutes;
application expiry is checked again immediately before delivery.
Do not rotate the delivery key while unexpired ciphertext remains in either
outbox, delivery storage or broker queues. Drain it or allow it to expire under
the old key first, then update both owners together. The current cipher envelope
does not implement overlapping encryption-key versions.

`APP_MAIL_MODE=log` suppresses delivery and records no link, address or token.
Only the private test launcher has a mailbox sink. SMTP delivery is at least
once: a crash after SMTP acceptance but before the database marker can send a
duplicate. No claim of exactly-once external mail is made.
SMTP failures retry locally after 5/30/120 seconds, then retain a FAILED delivery
record until its ciphertext expires. Broker redrive does not clear a committed
delivery's dedupe marker or force another mail send. After repairing SMTP, the
user can request a fresh reset; never reset inbox dedupe to resend an old token.

Transient consumer failure goes through durable retry queues with 5/30/120-second
delays; invalid contracts or exhausted retries go to the corresponding `.dlq`.
Retry queues use quorum at-least-once dead lettering. Operator `Replay` redrives
at most 100 messages, preserves event IDs and destination dedupe, and rejects
expired sensitive delivery. Repair the contract/consumer first, pause competing
redrives, record the reason and before/after queue counts in a private operations
log, and use only the separate operator credential. Never paste payloads in logs.

Retry/redrive publishing uses `autostrada.notification-routing` with explicit
topic permissions for only the permitted queue names. It does not grant access
to the default exchange. RabbitMQ checks exchange permissions separately from
routing-key permissions: see [official access-control documentation](https://www.rabbitmq.com/docs/access-control).
Publisher confirms and consumer acknowledgements cover different boundaries,
as described in [RabbitMQ's reliability documentation](https://www.rabbitmq.com/docs/confirms).

Micrometer records consumer errors, queue depths, delivery lag and backend
outbox age/publish failures. Producer and consumer logs correlate event IDs and
correlation IDs. Full metrics export, dashboards and distributed trace storage
remain S12 work; health is the only exposed management endpoint by default.
The broker is a single durable local node, not a highly available cluster.

## Build and isolated validation

From the repository root in PowerShell:

```powershell
Push-Location services/notification-service
.\mvnw.cmd --batch-mode --no-transfer-progress clean verify
Pop-Location
node front/e2e/run.mjs
node front/e2e/compose.mjs
```

The native harness uses disposable H2 and a private simulated message pump;
it is not RabbitMQ or MySQL evidence. Compose builds production images, creates
unique disposable schemas/volumes and exercises real RabbitMQ/MySQL before the
browser suite. `--integration-only` skips browsers. Historical S4b
`--upgrade-from` is rejected; the S6 harness uses staged copy/parity instead.
Logs, SQL backups and browser artifacts stay ignored under `compose-results`.
Do not run test SQL against a developer database. Harness cleanup removes only
its generated project's resources on success or failure.

## Controlled rollout

Do not start S6 directly on an unprepared S5 schema: backend V21 intentionally
refuses to proceed without copy parity. Applied migrations are unchanged.
For an entirely fresh installation, first follow the staged S5 bootstrap in
[the identity runbook](identity-service.md): create the legacy baseline through
V18 in the new schema, copy identity data and verify its marker, then proceed
through V19/V20 and the S6 steps below. The disposable Compose harness executes
this sequence. A single unrestricted first-start command is intentionally not
a substitute for copy/cutover.

1. Inventory versions and preserve a consistent backup of all owner schemas,
   broker state and private configuration. Verify restore on disposable data.
   Stop public writes, the backend scheduler, relays and consumers; stop old
   backend/identity writers. Keep this maintenance window through parity checks.
2. Provision notification schema credentials with no cross-schema grants,
   distinct broker credentials and an AES key shared only by identity and
   notification. Preserve the existing identity signing keys and account data.
3. Apply backend V20 only (`SPRING_FLYWAY_TARGET=20`), identity V2 and notification
   V1 with all application writers/relays disabled or stopped. Run notification
   once with broker/delivery disabled to initialize its schema, then stop it.
4. Run `lithan.autostrada.notification.NotificationCopy` from the notification
   artifact using Spring Boot's `PropertiesLauncher` and `-Dloader.main`.
   Supply private `CUTOVER_SOURCE_URL/USER/PASSWORD` (maintenance access to backend)
   and `CUTOVER_TARGET_URL/USER/PASSWORD` (notification schema),
   `CUTOVER_WRITE_FREEZE=I_HAVE_STOPPED_ALL_WRITERS`, and
   `CUTOVER_SOURCE_TIMEZONE=UTC`. Acknowledge only after actually stopping writers
   and verifying historical timestamp timezone. Never put passwords in arguments.
   Example launcher, after injecting those variables privately:

   ```powershell
   java '-Dloader.main=lithan.autostrada.notification.NotificationCopy' -cp services/notification-service/target/notification-service-0.0.1-SNAPSHOT.jar org.springframework.boot.loader.launch.PropertiesLauncher
   ```

5. Copy verifies exact IDs, recipient/auction references, text, timestamps/read
   state and generated snapshots, rejects orphans/divergent reruns, and seeds
   producer dedupe. The marker becomes PARITY_VERIFIED only after comparisons.
   Apply V21 with the migration account and `DB_RUNTIME_USERNAME` configured.
   It archives the old inbox, removes its obsolete car FK and revokes runtime
   archive access. New owner IDs are scalar: no cross-service SQL FK is claimed.
   Destination positive-ID checks and unique recipient/auction/type and dedupe
   constraints remain. Backend still validates eligibility against local auctions.
6. Start broker and the new services/gateway; retain one inbox writer and one
   session owner. Verify authenticated list/read/count, CSRF, follow delivery,
   recovery and a broker outage/recovery before reopening traffic. Keep archives
   and backups until the maintenance retention decision is made explicitly.

## Rollback and recovery

Before new S6 writes, keep ingress stopped and restore the coordinated S5
database/application snapshot and old routes, including original grants and
notification FK. Do not simply rename a table around Flyway history.

After new inbox, reset or delivery writes, prefer repair and roll forward.
A rollback requires a frozen, reconciled export of new notification IDs,
read-state, business/event dedupe, pending outboxes and delivery receipts, plus
the latest identity credentials, roles and reset-consumption state. Import and
verify those deltas in a disposable rehearsal before routing traffic back.
Never restore stale passwords/roles/reset state or resend already delivered
reset commands. Preserve broker positions and dedupe together with owner data.
Restoring the application image alone is not a safe rollback.

S7, durable checkout preparation inside the existing backend, is next. Payment,
commerce and marketplace extraction, distributed checkout, CD and full
observability are not implemented by S6.
