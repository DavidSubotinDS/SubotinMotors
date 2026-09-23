# S6 owner handoff — locally accepted

Branch: `feature/david.subotin_notification-service`.
Base: merged S5 `cddde6da41d32d3d37fae9a8eaa71cc4027cca73`, exact-commit
Backend/Frontend success verified in run 35673771885.
No staging, commit, push, PR or merge has been performed by the agent.

Implemented: independent notification owner/build/container/schema, gateway
assertion routing, backend transactional ending-soon outbox, RabbitMQ confirms,
transactional inbox/business dedupe, delayed retries/DLQ/redrive, encrypted reset
delivery, guarded inbox copy/archive cutover, private native/Compose harnesses.
See [the runbook](notification-service.md) for ownership and rollback boundaries.

## Evidence so far (2026-09-22)

- Backend: 106 tests and JAR passed including the eligibility-lock refinement
  and independent scheduler regression.
- Identity: 40 tests and JAR passed, including encrypted outbox transaction rollback.
- Notification: 14 tests and JAR passed, including import parity, assertion misuse,
  expiry/tamper/duplicate delivery and versioned JSON Schema fixtures.
- Gateway: 24 tests and JAR passed, including notification audience/CSRF forwarding.
- Native H2 browser: updated suite passed 20/20 in `target/s6/native-final.log`;
  all five listener ports were subsequently verified free.
- Frontend: 29 unit tests passed; native frontend production build passed.
- Production images: all six built and started non-root; private owner ports and
  absence of test controls in all four production JARs were verified.
- Docker Engine: 28.3.3 verified during this run.
- Real RabbitMQ/MySQL delivery, event/business dedupe, producer permissions,
  poison quarantine, repaired replay and delayed retry passed in disposable
  project `autostrada-test-83c46e039dd09026a601bee607eb3fa8`.
- Final project `autostrada-test-ecfb47bf10838c69f0591c0014f1f53c`
  passed notification copy/read-state parity, V21 cutover, real RabbitMQ/MySQL
  delivery/retry/DLQ/redrive, broker outage recovery, schema grants/constraints,
  preserved-volume restart, three-schema backup/restore parity, database outage
  recovery and all 20 browser scenarios. It verified cleanup of owned containers,
  volumes and networks.
- Final native H2 browser rerun passed 20/20 and discarded its in-memory data.
- Remote S6 CI is unverified until the owner commits/pushes and the required
  Backend and Frontend checks run on the exact proposed revision.
- Project `autostrada-test-4b80e808d0fcb6b6677f747c91dd8fb7` passed production
  broker-outage recovery, public cookie/CSRF/CORS checks, MySQL integrity/locking,
  preserved-volume restart, three-schema logical restore/parity and DB outage
  recovery. Its browser run passed 10/20 and exposed stale gateway DNS after
  owner-container replacement; see the correction below.
- Real SMTP and Stripe sandbox delivery: not verified; tests use suppression or
  private mailbox sinks and simulated payment providers.

The working tree is locally accepted for owner review and commit. Private
diagnostics are ignored under `target/s6` and `compose-results`; do not stage
them or secret environment files. Merge only after exact-revision remote Backend
and Frontend checks succeed.

## Failures found and corrected during acceptance

1. Project `autostrada-test-057cbf13b90ab605145414efa1367eaf` found default-exchange
   retry publishing denied by RabbitMQ. Corrected with a dedicated topic exchange
   and queue-name topic permissions; subsequent real retry/DLQ checks passed.
2. Project `autostrada-test-bbf3b8b1dbfd81340c695e977c5bccb1` found the cutover
   test assumed an empty inbox. It now compares complete source counts/read state
   while preserving the existing seeded row and two added fixtures.
3. Project `autostrada-test-83c46e039dd09026a601bee607eb3fa8` found that disabling
   auction scanning also disabled the outbox scheduler. Scheduling infrastructure
   is now independent; only the scanner bean obeys the scanner switch. A focused
   scheduler test and the real broker-outage regression cover this correction.
4. Project `autostrada-test-4b80e808d0fcb6b6677f747c91dd8fb7` exposed stale
   gateway routing after Docker reassigned owner IPs. The first browser session
   and CSRF requests returned 401 from a wrong owner; later tests passed after
   the cache expired. Gateway route/exchange/health DNS caches and pooled
   connection lifetimes are now bounded, with transport retry disabled. The
   harness verifies the CSRF route after replacement before starting browsers.
5. Project `autostrada-test-28708d6c66dedb7d6a85ad3e27fd3b2a` passed the production
   integration gates and 18/20 browser scenarios. The private Compose backend
   launcher inherited the unit-test relay-disable setting and localhost peer
   defaults. It now explicitly enables its real broker relay and supplies the
   generated Compose peer settings. Production configuration already passed
   the broker-outage test; this correction is confined to the test launcher.

Each failed project retained private diagnostics and verified removal of its
owned containers, volumes and networks. No normal database was reset.

## Owner commands (after acceptance and review)

Run from the repository root. Inspect the selected changes before committing.
These commands have not been run by the agent. Do not stage private test results
or normal environment files.

```powershell
$s6Files = @(
  'back/src/test/java/e2e/ComposeE2eApplication.java',
  'gateway/src/main/java/lithan/autostrada/gateway/UpstreamTransport.java',
  'gateway/src/main/java/lithan/autostrada/gateway/UpstreamHealth.java',
  'back/src/main/java/lithan/autostrada/auctions/config/AuctionSchedulingConfig.java',
  'back/src/test/java/lithan/autostrada/auctions/SchedulingIsolationTests.java',
  '.env.compose.example',
  '.github/workflows/ci.yml',
  'back/pom.xml',
  'back/README.md',
  'back/scripts/mysql-init-users.sh',
  'back/scripts/test-mysql-init.sh',
  'back/src/main/java/db/migration/V21__notification_cutover.java',
  'back/src/main/java/lithan/autostrada/auctions/config/SecurityConfig.java',
  'back/src/main/java/lithan/autostrada/auctions/controller/api/ApiExceptionHandler.java',
  'back/src/main/java/lithan/autostrada/auctions/controller/api/ApiModelMapper.java',
  'back/src/main/java/lithan/autostrada/auctions/controller/api/UserWorkspaceApiController.java',
  'back/src/main/java/lithan/autostrada/auctions/controller/AuctionUserController.java',
  'back/src/main/java/lithan/autostrada/auctions/controller/NavigationModelAdvice.java',
  'back/src/main/java/lithan/autostrada/auctions/entity/AuctionNotification.java',
  'back/src/main/java/lithan/autostrada/auctions/notification/EndingSoonOutbox.java',
  'back/src/main/java/lithan/autostrada/auctions/notification/NotificationClient.java',
  'back/src/main/java/lithan/autostrada/auctions/notification/NotificationImageController.java',
  'back/src/main/java/lithan/autostrada/auctions/notification/NotificationUnavailableException.java',
  'back/src/main/java/lithan/autostrada/auctions/notification/OutboxRelay.java',
  'back/src/main/java/lithan/autostrada/auctions/repository/AuctionNotificationRepository.java',
  'back/src/main/java/lithan/autostrada/auctions/repository/CarRepository.java',
  'back/src/main/java/lithan/autostrada/auctions/service/AuctionFollowService.java',
  'back/src/main/java/lithan/autostrada/auctions/service/AuctionNotificationService.java',
  'back/src/main/resources/application.properties',
  'back/src/main/resources/db/migration/V20__notification_outbox.sql',
  'back/src/test/java/e2e/E2eApplication.java',
  'back/src/test/java/fixtures/notification/entity/AuctionNotification.java',
  'back/src/test/java/fixtures/notification/repository/AuctionNotificationRepository.java',
  'back/src/test/java/lithan/autostrada/auctions/AccountAuctionFeatureIntegrationTests.java',
  'back/src/test/java/lithan/autostrada/auctions/BusinessIdentityFixtures.java',
  'back/src/test/resources/application.properties',
  'compose.yaml',
  'docs/browser-e2e.md',
  'docs/docker-compose.md',
  'docs/iroit-api-events.md',
  'docs/iroit-architecture.md',
  'docs/iroit-baseline.md',
  'docs/iroit-migration-plan.md',
  'docs/iroit-requirements-status.md',
  'docs/iroit-security.md',
  'docs/iroit-service-ownership.md',
  'docs/notification-service-pr.md',
  'docs/notification-service.md',
  'front/e2e/assert-stopped.mjs',
  'front/e2e/compose-cleanup.mjs',
  'front/e2e/compose.mjs',
  'front/e2e/fixtures.js',
  'front/e2e/local-message-pump.mjs',
  'front/e2e/run.mjs',
  'front/e2e/specs/marketplace.spec.js',
  'front/README.md',
  'gateway/README.md',
  'gateway/src/main/java/lithan/autostrada/gateway/GatewayRoutes.java',
  'gateway/src/main/java/lithan/autostrada/gateway/IdentityBridge.java',
  'gateway/src/main/resources/application.properties',
  'gateway/src/test/java/lithan/autostrada/gateway/GatewayTransportTests.java',
  'README.md',
  'services/identity-service/.dockerignore',
  'services/identity-service/pom.xml',
  'services/identity-service/src/main/java/lithan/autostrada/identity/controller/InternalIdentityController.java',
  'services/identity-service/src/main/java/lithan/autostrada/identity/IdentityApplication.java',
  'services/identity-service/src/main/java/lithan/autostrada/identity/service/DeliveryCipher.java',
  'services/identity-service/src/main/java/lithan/autostrada/identity/service/DeliveryRelay.java',
  'services/identity-service/src/main/java/lithan/autostrada/identity/service/EmailService.java',
  'services/identity-service/src/main/java/lithan/autostrada/identity/service/LoggingEmailService.java',
  'services/identity-service/src/main/java/lithan/autostrada/identity/service/PasswordResetService.java',
  'services/identity-service/src/main/java/lithan/autostrada/identity/service/QueuedEmailService.java',
  'services/identity-service/src/main/java/lithan/autostrada/identity/service/SmtpEmailService.java',
  'services/identity-service/src/main/resources/application.properties',
  'services/identity-service/src/main/resources/db/migration/V2__delivery_outbox.sql',
  'services/identity-service/src/test/java/e2e/E2eApplication.java',
  'services/identity-service/src/test/java/lithan/autostrada/identity/DeliveryOutboxTests.java',
  'services/identity-service/src/test/java/lithan/autostrada/identity/IdentityTestBase.java',
  'services/notification-service',
  'infra/rabbitmq'
)
git diff --check
git diff --stat
git add -- $s6Files
git diff --cached --stat
git diff --cached --check
git commit -m "Extract notification service with durable RabbitMQ delivery"
git push -u origin feature/david.subotin_notification-service
```

Suggested PR title: **Extract notification service with durable RabbitMQ delivery**.

Suggested body:

```markdown
Move inbox/read-state and reset-mail delivery into an independent notification service. Keep marketplace eligibility in the backend and credential/reset authority in identity, using transactional outboxes, RabbitMQ confirms, inbox dedupe and bounded retry/DLQ handling. Gateway session/CSRF exchange issues owner-specific assertions.

Preserve notification IDs/read state through guarded copy/parity and archive cutover. See docs/notification-service.md for coordinated rollout and rollback after new writes.

Validation: backend 106, identity 40, notification 14, gateway 24 and frontend 29 tests passed; native H2 and Compose/MySQL/RabbitMQ browser suites passed 20/20 each. Production images built and started non-root. Copy/cutover parity, permissions, retry/DLQ/redrive, broker/database outage recovery and three-schema backup/restore passed in disposable resources. Production JARs contained no test controls. Remote CI must still pass Backend and Frontend on the exact proposed revision. Real SMTP and Stripe sandbox delivery were not verified.

Next documented stage: S7 durable checkout preparation; no later extraction or CD is included.
```
