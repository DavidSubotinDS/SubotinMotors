# S8 payment-service extraction

Status (2026-09-24): implemented in the working tree on
`feature/david.subotin_payment-service`, based on merged S7 commit
`42cde105c2c3f744f6a60bfb1984f842c3383cd2`. Local and remote CI evidence is
recorded separately in [the S8 handoff](payment-service-pr.md). This document
describes implemented behavior unless a limitation is explicitly identified.

## Ownership after S8

`services/payment-service` is the sole runtime owner of Stripe configuration,
Checkout creation, provider session/payment-intent mappings, signed webhook
verification and receipts, provider reconciliation, expiry requests and the
payment-result outbox. It also holds copied legacy payment/account audit rows.
Its independent artifact, Flyway history, MySQL credential and container do not
include a backend domain/JPA library.

The backend keeps business state: carts, stock and holds, store orders, listings
and deposit reservations. Its `tb_checkout_attempt` remains a compatibility and
business-result projection during coexistence. It calls the payment service with
a stable attempt UUID and consumes normalized terminal results through RabbitMQ.
It has no Stripe SDK, secret, webhook controller or independent provider login.
Legacy auction checkout and connected-account onboarding stay retired. Existing
admin audit reads remain available from legacy tables during coexistence; no new
legacy payment mutation is accepted.

Identity remains the only browser session owner. It exchanges the backend's
fixed credential for a short-lived `payment-service` assertion with exactly
`create-store-payment`, `create-deposit-payment`, `payment-lookup` and
`payment-expire`. The payment service validates RS256, issuer, audience,
`tokenUse`, subject, time claims, JWT ID and the complete fixed scope set.
Browser payment reads receive user assertions and enforce owner-or-admin access.

The gateway keeps the single public origin. Exact `POST /webhooks/stripe` and
`/api/payments/**` go to payment-service; other webhook methods/suffixes do not.
The gateway streams the raw body and `Stripe-Signature` without parsing or
retrying the mutation. Existing session cookies, CSRF contract, upload handling
and frontend token refresh behavior are unchanged.

## Owned data

Payment Flyway V1 creates:

| Table | Purpose |
| --- | --- |
| `payment_attempt` | Idempotent provider intent, namespaced business reference, actor, amount/currency, provider mapping and reconciliation state |
| `payment_webhook_receipt` | Unique verified provider event receipt, payload hash and processing state; raw payload is not stored |
| `payment_outbox` | Durable normalized terminal events with aggregate version and correlation/causation IDs |
| `payment_provider_account_audit` | Copied historical connected-account audit; retired for new writes |
| `payment_legacy_audit` | Copied historical auction-payment audit; retired for new writes |
| `payment_copy_checkpoint` | Target-side copy/parity marker and source fingerprint |

Backend V24 adds the idempotent `tb_payment_result_inbox`; V25 restores only its
runtime DML grants; V26 adds the source-side cutover marker. Applied V1-V23 and
payment identifiers are never edited. There is no cross-schema foreign key,
join or runtime database grant. Service references use scalar attempt, business
and actor IDs. Backend-local business FKs remain in its schema.

`PaymentCopy` is an offline, one-time copy utility. It refuses to run without an
explicit writer-freeze acknowledgement, a declared UTC source and distinct
source/target JDBC URLs. The target owner must already have run payment V1 and
must be empty. It copies all store/deposit attempts, provider IDs, legacy account
and auction audit rows, and both old webhook receipt sources. It checks counts,
amount/provider-ID parity, a deterministic fingerprint, source stability and
both cutover markers before writers reopen.

## REST and event contracts

Private routes require a valid service assertion:

- `GET /internal/v1/capabilities`
- `POST /internal/v1/payments` with `Idempotency-Key` equal to the durable attempt UUID
- `GET /internal/v1/payments/{paymentId}`
- `GET /internal/v1/payment-attempts/{attemptId}`
- `GET /internal/v1/provider-sessions/{providerSessionId}`
- `POST /internal/v1/payments/{paymentId}/expire`

The create request includes a server-derived buyer ID, immutable amount/currency,
source service, business type/ID/version and return-route enum. Payment checks
the source/purpose/scope combination. Reusing an attempt ID with a different
canonical request is `409`. A provider timeout becomes durable
`RECONCILE_REQUIRED`; callers do not replay an unsafe request with a new key.
Clients use bounded connect/read timeouts and a bounded concurrent-call permit.

An expiry command returns `202 EXPIRY_REQUESTED`. It does not release business
inventory or publish `payment.expired.v1` merely because the provider request
returned. A verified callback or retrieved provider state supplies terminal
truth. Reconciliation also recovers lost create responses, unmatched early
webhooks and old Checkout sessions whose callback was missed.

The signed webhook accepts completed/async-success, async/payment-intent failure
and expiry outcomes. Provider event IDs are unique, early events remain unmatched
until a session link exists, terminal success cannot be downgraded, and only a
paid completion succeeds. Terminal transitions write one of these outbox events
in the same local transaction:

- `payment.succeeded.v1`
- `payment.failed.v1`
- `payment.expired.v1`

The durable Rabbit publisher uses confirms and mandatory routing. The backend
consumer stores the event ID before applying an order/deposit transition and
uses three delayed retry queues followed by a DLQ. Duplicate events have no
second stock, reservation or payment effect. Payment never changes stock,
listing, bid or order tables.

## Configuration and operation

The provider is disabled by default. Ordinary tests and disabled startup require
no Stripe key. Provider credentials exist only in the payment container. Database
and broker passwords remain runtime environment values and must not appear in
images, Git, application logs or retained CI output. No password, cookie/session
ID, CSRF/reset token, assertion, raw signed body or payment secret is logged.

Private health endpoints separate process liveness from database readiness.
Startup does not require the backend, gateway or RabbitMQ; outbox publication
recovers after broker availability returns. Aggregate metrics expose pending
reconciliation count/age, unmatched receipts and pending outbox count/age without
actor/provider labels. Standard HTTP metrics provide rate, error and latency.
Distributed trace export and dashboards remain the documented S12 work.

## Controlled rollout

1. Back up backend, identity, notification and payment schemas plus Rabbit state.
   Record commit, image IDs, UTC source setting and every Flyway checksum.
2. Disable new checkout/deposit creation. Inventory all nonterminal attempts and
   keep one signed webhook owner. Drain or record callbacks around the freeze.
3. Deploy backend V24-V26 and payment V1 with separate credentials. Do not give
   either owner access to the other schema.
4. Stop backend/payment writers and run `PaymentCopy` with the guarded freeze
   variables. Require count, amount, provider-ID, receipt and marker parity.
5. Switch the exact gateway webhook route and backend REST client together.
   Remove Stripe secrets from backend. Start Rabbit topology/consumers, then
   payment reconciliation before accepting new checkout creation.
6. Verify readiness, anonymous session/CSRF, USER/ADMIN rules, store checkout,
   deposit checkout, signed callback, success reads, retry and DLQ behavior
   through the actual gateway origin. Reopen creation and monitor the metrics.

For the permanent local CD environment, after merging S8 and before deployment,
run once from the repository root in Windows PowerShell:

```powershell
.\deploy\local\Update-LocalEnvironmentS8.ps1 -Path C:\AutostradaDeploy\autostrada.env
```

The deploy script backs up every present owner schema, stops writers, migrates,
runs the guarded copy only when the parity marker is absent, and then starts the
new revision. It never resets a normal volume.

## Recovery

Before any new payment-service write or callback, route/config rollback is safe
after verifying the old owner contains the complete source state. Once payment
has created a provider session or accepted a callback, prefer forward repair.
Keep payment ingress and its schema, stop new creation, pause business consumers
if necessary, and reconcile every attempt/receipt/outbox row with the provider.

Reversal after new writes requires a controlled freeze, a verified export of all
new attempts, receipts, provider state and result offsets, and exactly one active
webhook owner. Never start the old backend with stale payment data, restore old
stock/reservation snapshots, reuse an attempt under a new key, discard a paid
result, or deliver callbacks to both owners. Schema migrations are additive and
remain applied.

## Remaining coupling and next stage

The backend still owns commerce and marketplace tables plus the local checkout
attempt projection. It temporarily contains both business result handlers and
consumes both result queues. Legacy audit entities remain for read compatibility,
but their mutation service is retired. These are deliberate coexistence seams.

The next documented stage is S9: extract `commerce-service`, followed by its
reactive checkout review. S9 will move parts/cart/orders and the commerce result
consumer; it is not implemented by S8.
