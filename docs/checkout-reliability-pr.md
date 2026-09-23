# S7 owner handoff: durable checkout reliability

Status (2026-09-23): working-tree implementation on
`feature/david.subotin_checkout-reliability`, based on merged `master`
`39e750e521f60f44645d18e9179fcedc9df9db0c`. No files were staged, committed,
pushed or merged by the assistant. See [the S7 runbook](checkout-reliability.md)
for the behavior, rollout and rollback contract.

## Implemented scope

- Added durable idempotent attempts, request hashes, explicit stock holds and a
  verified webhook inbox through additive V22/V23 migrations.
- Moved provider calls outside the local order/deposit preparation transaction.
- Added same-key provider recovery, scheduled reconciliation and deterministic
  crash seams that exist only as an inert production interface/test override.
- Preserved existing routes and successful checkout DTO behavior while adding
  attempt/status/retry fields and explicit pending UI behavior.
- Preserved S4a CSRF/session rotation and no mutation replay; React attaches a
  stable per-action `Idempotency-Key` alongside the CSRF token.
- Added monotonic paid/conflict handling, early-webhook retry, confirmed expiry,
  and safe late-payment stock/reservation reclamation.
- Updated the disposable H2 fixture and production MySQL/Compose bootstrap; no
  developer database or permanent local deployment was used.

## Local evidence

Evidence is recorded from this branch and must not be represented as remote CI.

- Focused backend reliability tests: passed, including a real two-thread
  last-item race and the store attempt/crash/webhook cases.
- Full backend verification: 115 tests passed with zero failures, errors or
  skips; the production JAR was built successfully.
- Production JAR inspection found no E2E controls, test migrations,
  `application-e2e` configuration or Spring test libraries.
- Frontend: 7 test files / 30 tests passed; production Vite build passed.
- Native gateway/disposable H2 browser: 20/20 Playwright scenarios passed.
- Docker Engine 28.3.3 was available.
- Disposable production-image MySQL/RabbitMQ integration passed under project
  `autostrada-test-824df5e35b2e0f9fb94e2df161d5a1d9`: clean and upgrade
  migrations through V23, S5/S6 copy parity, runtime grants, constraints,
  locking, notification delivery/dedupe/redrive, broker and database outage
  recovery, preserved restart data and migration checksums, logical
  backup/restore, and 20/20 Playwright scenarios through the Compose gateway.
  The harness verified removal of its containers, volumes and network.

Earlier failed attempts were diagnostic. They exposed bootstrap validation
while V18/V20 copy phases were deliberately frozen, missing runtime grants for
the new V22 tables, a stale 21-migration harness assertion, native H2 Flyway
location/reset-fixture mismatches, and accidental inclusion of an H2-only test
migration in the MySQL launcher. The implementation now disables JPA schema
validation only during guarded copy phases, adds V23 grants, expects all 23
production migrations, separates browser-only H2 identity-boundary setup, and
pins production Compose to `classpath:db/migration`. Every failed disposable
project was removed before the final passing run.

## Unverified external gates

- GitHub Backend and Frontend checks for the eventual exact commit.
- Real Stripe sandbox success, cancel, deposit and delayed-payment behavior.
- SMTP delivery; notification delivery remains the already-merged S6 behavior.
- Production deployment; only isolated local container acceptance is in scope.

## Suggested pull request

Title: `feat: make checkout creation durable and idempotent`

Body:

```markdown
## Problem

Store and listing-deposit checkout previously called Stripe inside the local
business transaction. A timeout or process failure could leave ambiguous stock,
orders and provider sessions, while a browser retry could create duplicate work.

## Result

- persist checkout attempts, canonical request hashes and explicit stock holds
  before contacting Stripe
- use the attempt UUID as the Stripe idempotency key and reconcile ambiguous
  provider outcomes with bounded retries
- durably store verified webhooks, including callbacks that arrive before the
  local provider-session link exists
- keep paid/conflict states monotonic and release or reclaim reservations once
- send a stable browser `Idempotency-Key` and show pending state without unsafe
  automatic replay
- add V22/V23 MySQL migrations, metrics, crash/concurrency tests and S7 runbooks

Existing gateway routes, raw signed Stripe webhook forwarding, session/CSRF
contracts, USER/ADMIN ownership checks and legacy redirects remain compatible.

## Validation

- Backend: 115 tests and production package
- Frontend: 30 tests and production build
- Native gateway/H2 browser: 20/20 scenarios
- Disposable MySQL/RabbitMQ Compose integration and browser: 20/20 scenarios
- Production images, migrations through V23, grants, locking, backup/restore,
  outage recovery and resource cleanup

Real Stripe sandbox and remote GitHub checks remain owner-run gates.
```

The next documented stage is S8, payment-service extraction. Do not combine it
with this pull request.
