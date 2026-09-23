# S7 checkout reliability

Status (2026-09-23): implemented on
`feature/david.subotin_checkout-reliability`, based on merged `master`
`39e750e521f60f44645d18e9179fcedc9df9db0c`. This stage changes the existing
backend only; it does not create payment-service or move payment ownership.
Remote CI, merge status and live Stripe sandbox behavior remain unverified until
the owner pushes the branch and completes those gates.

## Actual ownership

The backend remains the owner of store orders, stock, listing deposits, Stripe
Checkout creation, verified webhook handling and payment reconciliation. The
gateway still owns the single public origin and forwards the exact raw webhook
body and `Stripe-Signature`. Identity-service remains the only login/session
owner. Notification-service and RabbitMQ are unchanged by S7.

S7 adds three backend-owned tables in Flyway V22:

| Table or column | Purpose |
| --- | --- |
| `tb_checkout_attempt` | Durable store/deposit intent, browser request key, canonical request hash, private email snapshot, aggregate/provider mapping, reconciliation and expiry state |
| `tb_stock_hold` | Explicit per-attempt inventory reservation with `ACTIVE`, `CONSUMED` or `RELEASED` state |
| `tb_checkout_webhook_inbox` | Durable verified Stripe event receipt before business dispatch, including unmatched early events |
| `tb_store_order.checkout_attempt_id` | Unique optional link for new orders; legacy rows remain valid |
| `tb_listing_deposit.checkout_attempt_id` | Unique optional link for new deposits; legacy rows remain valid |

V23 restores only `SELECT, INSERT, UPDATE, DELETE` on the three new tables to
the backend runtime account after V19's identity cutover removed schema-wide
runtime grants. Applied V1-V21 migrations are unchanged. Existing orders,
deposits, provider IDs and business rows are not rewritten.

## Checkout state and transaction boundary

`CheckoutPreparationService` first derives the actor from the trusted identity
assertion and obtains the current actor's private checkout profile. In one local
transaction it locks the selected parts or listing, validates availability,
persists the attempt and order/deposit, creates explicit holds, updates stock or
reservation state, and clears the captured cart. The Stripe call starts only
after that transaction commits.

The attempt UUID is also the Stripe idempotency key. A repeated browser request
with the same `Idempotency-Key` returns the same attempt and provider URL. Reuse
for a different cart or listing is rejected. A missing header remains compatible:
the server creates a UUID, while the React clients now generate and retain a UUID
for each explicit checkout action.

Provider timeouts and lost responses produce `RECONCILE_REQUIRED`; they do not
release stock or create a second local order. The scheduled reconciler retries
the persisted intent with the same provider key and bounded backoff. Abandoned
unresolved attempts expire after 24 hours and release their exact hold once.

Terminal business states are monotonic. `checkout.session.completed` is paid
only when Stripe reports `payment_status=paid`. A verified unpaid completion
stays pending. A late paid outcome attempts to reclaim a previously released
stock/listing reservation. If that is unsafe, the explicit
`PAID_STOCK_CONFLICT` or `PAID_RESERVATION_CONFLICT` state is retained; a later
expiry or another paid event cannot downgrade it or change stock again.

## Routes and browser contract

Existing public routes and successful behavior are retained:

- `POST /api/store/checkout`
- `POST /api/user/listings/{listingId}/deposit`
- legacy form checkout/deposit actions and redirects
- `GET /api/store/checkout/success?session_id=...`
- `GET /api/user/listing-deposits/success?session_id=...`
- `POST /webhooks/stripe`

Both checkout mutations accept an optional `Idempotency-Key` header and return
the additive DTO:

```json
{
  "checkoutUrl": "https://checkout.stripe.com/...",
  "attemptId": "uuid",
  "status": "CHECKOUT_CREATED",
  "retryable": false
}
```

When provider resolution is ambiguous, `checkoutUrl` is null, `status` is
`RECONCILE_REQUIRED`, and `retryable` is true. React shows a pending message and
waits for another explicit user action. The CSRF client may refresh its token,
but it never automatically replays an unsafe checkout request. Successful old
clients can continue reading `checkoutUrl`.

## Webhook durability and security

The gateway/raw-body/signature contract is unchanged. Only a successfully
verified Stripe event reaches `CheckoutWebhookInboxService`. The event is
persisted before store, deposit or legacy payment dispatch. An event that arrives
before its provider session is linked remains `PENDING` and is retried. Provider
event IDs are unique; business transitions and hold state also protect against
distinct events that describe the same or conflicting outcome.

The actor is never accepted in a checkout body or query parameter. Store and
deposit reads remain current-user scoped, USER/ADMIN restrictions are unchanged,
and success pages remain read-only. The attempt's email snapshot is used only
for provider creation and is not returned by an API or written to application
logs. Passwords, sessions, CSRF/reset tokens, assertions and provider secrets
must remain absent from checkout logs.

## Operations and observability

The backend exports:

- `checkout.pending.attempts`
- `checkout.pending.oldest.age.seconds`
- `checkout.stock.holds`
- `checkout.reconciliation.failures`
- `checkout.reconciliation.attempts{result=success|failure}`

Scheduler defaults are `checkout.reconciliation.poll-ms=5000` and
`checkout.webhook-inbox.poll-ms=2000`. Liveness does not depend on Stripe.
Readiness continues to describe the backend's required local dependencies; an
ambiguous provider call becomes durable pending work rather than failing startup.

## Rollout

1. Back up the three service databases and record the running image/commit.
2. Stop new store/deposit checkout creation. Keep the signed webhook route
   available if possible and inventory existing pending provider sessions.
3. Deploy the S7 backend migration owner so V22 and V23 complete. Confirm all 23
   backend Flyway migrations, new constraints/indexes and runtime grants.
4. Deploy backend, frontend and gateway from the same revision. Verify readiness,
   login/session/CSRF, one store checkout and one listing deposit through the
   public gateway origin.
5. Reopen checkout creation and monitor pending age, active holds, reconciliation
   failures and webhook inbox backlog.

No data copy is required in S7. Pre-S7 rows keep a null attempt link and continue
to correlate existing signed webhooks by provider session ID. Do not invent a
new attempt for an already-created provider session.

## Recovery and rollback

Before any S7 checkout is created, the previous image can be restored after
confirming V22's additive schema is tolerated; do not reverse applied migrations.
After new writes, stop new checkout creation and keep durable webhook ingress
running. Reconcile every `PROVIDER_PENDING`, `RECONCILE_REQUIRED` and pending
inbox row with Stripe, and retain the attempt/hold tables as the audit source.

Do not start the previous backend while unresolved S7 attempts or active holds
exist. The old image cannot safely resume an idempotent attempt, understand an
explicit hold, or undo a provider session. Forward repair is preferred. If an
application rollback is unavoidable, first reach terminal provider truth for
every S7 attempt, ensure every hold is consumed or released exactly once, and
export the attempt/provider mapping. Never delete V22 data, restore old stock
snapshots, replay a checkout with a new key, or run two webhook owners.

## Remaining coupling and next stage

Stripe SDK/configuration, provider identifiers, webhook receipt dispatch,
reconciliation and legacy payment audit remain in `back/`. Store stock/orders
and listing reservations also remain there, intentionally. S8 is the next
documented stage: extract `services/payment-service` with its own schema,
container, provider adapter and webhook ownership. S7 does not implement S8.
