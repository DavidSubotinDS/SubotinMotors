# S8 payment-service owner handoff

Branch: `feature/david.subotin_payment-service`, based on merged S7 commit
`42cde105c2c3f744f6a60bfb1984f842c3383cd2`. The assistant did not stage,
commit, push, create a PR, merge or tag. The owner must review the working tree
and replace this branch's local evidence with remote required-check evidence
after pushing.

## Scope

- Independent payment-service build, production/test containers, V1 schema,
  disabled and Stripe adapters, signed ingress, reconciliation, health and
  operational metrics.
- Fixed identity service grant and strict assertion validation; gateway payment
  API/session bridge and exact raw webhook routing.
- Backend bounded idempotent REST client, normalized Rabbit result inbox,
  business transition handlers and retired legacy provider mutations.
- Guarded copy/cutover with attempts, amounts, provider identifiers, webhook
  receipts and legacy audit parity; additive backend V24-V26.
- Rabbit publisher confirms, consumer dedupe, delayed retries/DLQs, disposable
  MySQL/Compose/browser integration and local-CD migration support.

## Local evidence

Evidence below records completed local runs on 2026-09-25:

| Gate | Result |
| --- | --- |
| Payment service | Passed: 7 tests and production JAR built |
| Backend | Passed: 114 tests and production JAR built |
| Identity service | Passed: 40 tests and production JAR built |
| Notification service | Passed: 14 tests and production JAR built |
| Gateway | Passed: 25 tests and production JAR built |
| Frontend | Passed: 30 tests and production build |
| Native H2 browser | Passed: 20 scenarios |
| Node/PowerShell/Rabbit boot syntax | Passed |
| Production images and startup | Passed: services ran non-root with private service ports and readiness checks |
| Isolated MySQL copy/cutover | Passed: S8 attempt count, amount and provider-ID parity; source and target markers `PARITY_VERIFIED` |
| MySQL integrity and recovery | Passed: schema grants, constraints, restart persistence, four-schema backup/restore and database outage/recovery |
| RabbitMQ | Passed: delivery, dedupe, restricted producer grants, delayed retry/DLQ, repaired redrive and broker outage recovery |
| Compose browser through gateway/MySQL/RabbitMQ | Passed: all 20 scenarios, including four payment scenarios; owned resources removed |
| Production backend artifact | Passed: no E2E controls, legacy Stripe adapters/controller or Stripe SDK entries found |
| Real Stripe sandbox | Not verified; must remain reported separately from simulated provider evidence |
| Remote Backend / Frontend checks | Unverified until the owner pushes |

Private generated logs and Compose results belong under ignored `target/` and
`compose-results/`; do not commit them. The permanent `autostrada-local` project
and normal developer database were not used for S8 validation.

## Owner Git commands

Run only after reviewing `git diff`, this handoff's final evidence and ignored
artifacts. These commands intentionally stage only S8 paths:

```powershell
Set-Location C:\Projects\SubotinMotors
git status --short
git diff --check
git add -- `
  .env.compose.example `
  .github/workflows/ci.yml `
  README.md `
  back `
  compose.yaml `
  deploy/local `
  docs `
  front/README.md `
  front/e2e `
  gateway `
  infra/rabbitmq/boot.py `
  services/identity-service `
  services/payment-service
git diff --cached --stat
git diff --cached --check
git commit -m "feat: extract payment processing into an independent service" `
  -m "Move provider checkout, signed webhook ingestion, reconciliation, payment audit data, and normalized result publication into a separately built payment service with its own schema and container. Add guarded data copy/cutover, trusted service assertions, RabbitMQ result handling, gateway routing, CI coverage, and local deployment support while preserving existing checkout and authorization contracts."
git push -u origin feature/david.subotin_payment-service
```

Suggested PR title:

`feat: extract payment processing into an independent service`

Suggested PR body:

```markdown
S8 moves Stripe Checkout creation, signed webhook receipt, reconciliation and
payment audit ownership into an independently built payment service with its own
schema, credential, migration and container. The backend keeps stock/order and
listing-reservation invariants, calls payment with a fixed service grant, and
consumes idempotent terminal results through RabbitMQ.

The change adds a guarded frozen-writer copy with attempt, amount, provider-ID,
receipt and audit parity; exact gateway raw-webhook routing; bounded clients;
provider reconciliation; retry/DLQ topology; CI/container/MySQL/browser gates;
and local-CD cutover/recovery support. Existing session/CSRF, USER/ADMIN,
ownership, multipart and no-mutation-replay behavior remains intact.

Validation:
- See `docs/payment-service-pr.md` for exact local results and limitations.
- Real Stripe sandbox remains a separate owner verification gate.
- Required remote `Backend` and `Frontend` checks must pass on this PR and its
  exact merged commit.

Next stage: S9 commerce-service extraction and reactive checkout review; not
included here.
```

After merge, fetch and verify the exact `origin/master` SHA and both required
checks before creating the S9 branch. Then update the permanent deployment env
once with `Update-LocalEnvironmentS8.ps1` before allowing CD to deploy S8.
