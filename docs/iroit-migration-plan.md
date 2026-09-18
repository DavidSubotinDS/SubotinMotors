# IROIT incremental migration and implementation backlog

Current implementation status (2026-09-14): S1 merged into master as `35007b5`
with successful merged-master Backend/Frontend CI. S2 code and local evidence
are on `feature/david.subotin_api-gateway`; see [S2 handoff](api-gateway-pr.md)
for actual verification and remaining gates. No S2 commit/push/PR/merge is claimed.
S3+ remains proposed. The historical design-stage wording below is retained as
the stage contract, not a claim that every listed stage has shipped.

Status: proposed, documented on 2026-09-13/14. Start with the verified
[baseline](iroit-baseline.md), [architecture](iroit-architecture.md),
[ownership inventory](iroit-service-ownership.md), [contracts](iroit-api-events.md)
and [security design](iroit-security.md). No stage below is implemented by this
documentation change. Preserve normal React workflows at every release.

## Gates applying to every implementation stage

Each stage is complete only when its stage-specific criteria **and all applicable
common gates** have evidence in the PR. A skeleton process with a health endpoint
does not count as an extracted business microservice.

| Gate | Evidence required |
| --- | --- |
| Functionality | Existing affected React/legacy flows and DTO/redirect contracts pass; new pending/error states have matching frontend support. No accidental revival of retired payment flows |
| Independent build | Each new executable builds from its own directory and wrapper, without compiling `back/` or sibling services; artifacts contain no shared entity/domain jar |
| Independent startup | Each service starts with its own DB and configured dependencies; tests use stubs where appropriate. Optional peer outages do not prevent process startup. Health distinguishes liveness from readiness; readiness does not require unrelated peers |
| Data | Owner-only credentials; Flyway validates clean install and an upgrade/copy; MySQL integration test covers migrations, constraints, locking and ID preservation. H2 unit/integration tests remain useful but do not replace this |
| Tests | Relevant unit, owner/role security, HTTP contract and business integration tests; browser E2E for affected critical flows. Add messaging duplicate/retry/reorder and provider ambiguity tests when applicable |
| Containers | Dockerfile, `.dockerignore`, non-root runtime where practical, health check, explicit config, no secrets in images, documented ports; Compose service/database dependencies and volumes updated with each extraction |
| CI | Existing `Backend` and `Frontend` checks remain required. Add every new build/test/container/contract/E2E check; failed service checks must block merge via required aggregate or individually required checks. Owner adjusts protection deliberately |
| Observability | Health/readiness, structured logs with service/request/trace IDs and useful failure metrics; HTTP trace propagation when clients appear, event correlation when RabbitMQ appears. No credentials/private payload logging |
| Recovery | Backup/import manifest for data moves; tested owner-specific restore or forward repair; last healthy image/config references; no route rollback to stale data after new writes |
| Documentation | Update these decisions, service README/runbook, route owner table, environment examples and demo steps; record verified results and remaining limitations |

For a stage that adds no executable, verify the existing runnable topology and
affected builds/containers/CI; mark new-process criteria not applicable rather
than inventing a service. Do not create infrastructure without a flow that uses
it. Fast checks should run on every PR; container/MySQL/broker integration jobs
must run whenever their contracts or implementations change. Avoid path filters
that leave required checks pending or omit shared-contract consumers. A stable
required `Services` aggregate can cover new jobs if the owner enables it before
the first service merges; keep existing `Backend`/`Frontend` names.

## S0 - Review architecture and identify baseline

This task supplies documentation and verified CI/tag evidence. Owner reviews,
creates the annotated baseline tag, commits and merges the focused docs PR.
Acceptance: Mermaid diagrams/source links and full inventory reviewed; unknown
rubric requirements remain explicit; only docs changed. Runtime build/test and
container changes are not applicable. No baseline tag, commit or PR was created
by the assistant.

## S1 - Browser E2E baseline

Implementation evidence and current limitations are recorded in the
[browser E2E runbook](browser-e2e.md). The baseline now lives on the S1 feature
branch; this statement does not claim it is merged or that remote CI/manual
Stripe smoke has run. The following acceptance criteria remain the stage contract.

Add a small Playwright suite against the current backend/frontend before routing
or security changes. Use isolated deterministic data and controlled clocks for
auction deadlines; no dependency on a developer's file H2 database.

- Cover registration/login/session reuse/logout; USER vs ADMIN and cross-user
  denial; auction create/edit/bid/deadline; fixed-price listing/test ride;
  profile address; cart/order happy path; signed payment-result fixture,
  processing/cancel/expired display; notifications read/unread; admin order
  items and legacy redirect/query compatibility. Prioritize these flows rather
  than duplicating every component test in the browser.
- Keep deterministic CI provider simulation separate from an explicit manual
  Stripe sandbox smoke (success + cancel + deposit). Browser success navigation
  must not mark paid. CI must not require live secrets or pretend a mocked
  provider is a live Checkout test.
- Acceptance: local one-command isolated startup and E2E run; backend/frontend
  existing checks pass; CI uploads traces/screenshots on failure and tears down
  test processes. No new service/container is required yet; use current local
  processes until S3 provides Compose, then run the same suite there.
- Recovery: remove/disable only failing new test orchestration, retain test data
  isolation and existing CI checks; no application database migration.

## S2 - Gateway routing to the monolith

Implemented: independent Boot 3.5.15 / Cloud 2025.0.3 WebFlux gateway, explicit
API/webhook/legacy/SPA routes, private header boundary, CORS, proxy budgets,
correlation, health and transport tests; existing Frontend E2E traverses gateway.
Backend CI now gates gateway tests and image smoke. Backend selected-resource
redirects are repaired. [Gateway runbook](../gateway/README.md) records native
startup, container addressing and recovery. See the handoff for unverified gates.

Create `gateway/` with a Java 17 / existing Boot-compatible Spring Cloud Gateway
Server WebFlux dependency set, verified against official compatibility guidance
in that implementation task. This is routing only; backend still owns sessions.

- Route existing APIs, signed webhook and compatibility actions to `back/`;
  configure frontend to use gateway, preserve cookies/query/status/redirects,
  test multipart size behavior and byte-exact webhook forwarding. Distinguish
  React navigation from legacy forms; do not introduce redirect loops.
- Acceptance: independent gateway Maven build/start with backend stub; gateway
  Dockerfile and documented container-to-backend address; actual backend +
  frontend E2E through gateway; authentication/401/403/CSRF baseline behavior
  unchanged. New gateway CI/container test plus `Backend` and `Frontend` pass.
- Add bounded proxy timeouts, request IDs, route latency/error logs and health.
  Only public gateway ports are intended for final deployment; development
  backend access can remain loopback-only for diagnosis.
- Recovery: restore frontend API base and public reverse-proxy config to the
  original backend path; verify webhook/return/reset URLs together. No data move.

## S3 - Reproducible container baseline

Containerize backend/frontend and add Compose for gateway, React assets, backend
and MySQL. This validates actual MySQL migrations before distributing data.
Serve SPA assets and API through one public origin. Keep native development
available. Use test/sandbox configuration, persistent named DB volume and
explicit health/readiness; bootstrap demo data only deliberately.

- Acceptance: from clean checkout, build images and start topology with example
  non-secret config; fresh and preserved-volume restart both work; MySQL Flyway
  V1-V18 verifies; E2E runs through gateway; CI builds images and exercises
  Compose/MySQL integration alongside existing tests. `docker compose config`
  validates and the smoke job always tears down its isolated volumes.
- Health checks and correlated gateway/backend logs diagnose startup failures.
  Document backup/restore and service credentials; DB is not publicly exposed.
- Recovery: revert image/config changes and restore test backup if needed;
  never destroy a developer volume to make a migration pass. RabbitMQ is not
  added until S6 has its first consumer.

## S4 - Session/CSRF and identity boundary preparation

Use two focused PRs if needed: browser CSRF/session hardening, then identity
reference adapters. Both precede identity data cutover.

- Implement [security Phase B](iroit-security.md): client token handling,
  server CSRF validation and login session rotation with regression coverage.
- Introduce immutable principal IDs and domain profile clients; remove backend
  non-identity dependencies on user/password/role repositories and user JPA
  relationships. Supply the clients in-process initially. Split `AdminService`
  account and marketplace operations and mappers where needed.
- Acceptance: all auth/upload/checkout E2E passes; negative CSRF/session fixation
  and ownership tests; architecture/source check shows only identity code owns
  credential/profile repositories. Fresh and upgraded MySQL migrations, existing
  independent builds/Compose startup, CI and correlation logs pass. There is no
  new executable yet. Record identity display snapshots and scalar-ID parity.
- Recovery: adapters can revert while legacy identity remains the only writer.
  Coordinate client/server CSRF deployment rollback; do not leave enforcement
  disabled as a permanent compatibility solution.

## S5 - Extract identity-service

Move identity's five tables and business endpoints to
`services/identity-service/`; retain legacy account/profile IDs and hashes.
Implement private session exchange, service-token grants and service assertion
validation described in the security design. Identity-only keys and credentials
are injected. Move auth/profile/admin-user routes together, with one-time
re-login at the controlled cutover. The remaining backend uses assertions and
profile APIs rather than a second account database. Email delivery temporarily
remains identity-local until S6.

- Acceptance: independent identity artifact/container/schema/startup and migration
  tests; all registration/profile/reset/session/role flows through gateway;
  direct-service wrong issuer/audience/token-use and cross-user tests;
  old credentials cannot authenticate remaining backend separately. Other
  backend domains run against identity APIs with scalar references. CI includes
  identity and relevant end-to-end contracts. Check cookie/CORS behavior on the
  actual single public origin. Log auth failures without secrets; trace exchange.
- Recovery: before new identity writes, return to legacy auth and re-login.
  After new account/profile/password changes, freeze and reconcile into a
  compatible legacy schema or repair forward; never re-enable stale passwords
  or lost roles. Preserve reset consumption and session revocations.

## S6 - RabbitMQ and notification-service

First introduce outbox/inbox conventions with a real ending-soon publisher in
the remaining backend, RabbitMQ Compose service, retry/DLQ policy and tests.
Then extract inbox/delivery as `services/notification-service/`. Scheduler and
follow eligibility remain with marketplace code in `back/`; no notification
service access to car/follow/user tables. Import inbox snapshots/read state and
dedupe keys. Switch notification routes and replace workspace unread-count
repository access with a client. Move mail delivery from identity to the
restricted command queue only when the consumer and expiry handling are ready.

- Acceptance: independent notification build/container/schema/startup; real
  RabbitMQ integration for outbox recovery, duplicate messages, poison payload,
  delayed retry and DLQ replay; current follow/ending-soon/read/read-all browser
  behavior; reset-link mail still arrives in local sink or configured SMTP.
  Test expiry/payload redaction. CI builds producer and consumer and validates
  schemas. Broker-offline state leaves durable work rather than losing it.
- Observe inbox delivery time, consumer errors, retry/DLQ depth and outbox age;
  trace one auction follow to an inbox item. Liveness survives broker outage;
  inbox reads still work if its own DB is available.
- Recovery: pause publishers/consumers and capture positions. Before new inbox
  writes, restore old routing; after writes, preserve read-state/delivery/dedupe
  deltas before reverting. Never run both old direct notification creation and
  event creation without a shared business dedupe key in the destination owner.

## S7 - Durable checkout preparation inside the existing backend

Before network extraction, separate provider adapters from commerce and deposit
transactions, introduce attempt IDs/request hashes/local durable intent and
stock/reservation state, and add pending/retry frontend handling. Provider calls
run after the local transaction; background reconciliation recovers interrupted
attempts. Keep the existing routes, successful checkout response and retired
auction-flow redirects. Do not add a new business service here.

- Acceptance: deterministic crash points before/after provider creation/local
  commit, concurrent last-item checkout, duplicate UI requests, unpaid completed
  session, distinct duplicate provider outcomes, late success and confirmed
  expiry tested. Success pages remain reads. Store/deposit E2E, backend/frontend
  CI, MySQL migration and Compose startup pass. Repeat manual sandbox smoke.
- Observe pending-attempt age, stock-hold count and reconciliation failures;
  sensitive payloads remain excluded. This establishes the state machine and
  compatibility contract before adding a network failure boundary.
- Recovery: stop new checkout creation and reconcile provider attempts before
  reverting schema/code. Keep receiving signed events durably. Restoring the
  previous application image alone cannot undo a created Stripe session.

## S8 - Extract payment-service

Move provider integration, payment attempts/legacy audit and webhook receipts
to `services/payment-service/`. Import Stripe identifiers from **all** payment
sources, including store orders and listing deposits, with source namespace
mapping. Replace their callers with idempotent payment REST clients. The legacy
commerce/marketplace modules consume normalized payment results via RabbitMQ.
Implement reconciliation and expiry commands before cutting over callbacks.

- Acceptance: payment builds/starts independently with its DB and a provider
  stub or disabled sandbox; no Stripe key required for ordinary tests. Its
  Dockerfile, MySQL migrations, provider contract/security tests and CI exist.
  Import count/amount/provider-ID parity; raw-body webhook forwarding; lost
  response, unmatched early webhook, duplicate/reorder/retry/DLQ tests; store and
  deposit E2E and signed sandbox smoke pass. Payment has zero stock/listing/bid
  repository access. Legacy payment audit DTOs still render in admin.
- Cutover: freeze creation, inventory all pending sessions, durably capture or
  rely on provider retry during brief downtime, import receipts/mappings, switch
  the single webhook route to payment, disable old handlers/provider secrets,
  then reopen creation. Replay/reconcile callbacks received around the boundary.
- Observe signed receipt/reconciliation failures, attempt age, webhook latency,
  outbox lag and one trace across checkout -> provider receipt -> order result.
- Recovery: roll forward is preferred once payment accepts events/creates
  sessions. Keep durable ingress and pause business consumers while repairing.
  Reversal requires reconciled attempt/receipt exports and one active handler;
  never send the same webhook to two independent legacy/new owners.

## S9 - Extract commerce-service and reactive checkout review

Move parts, cart, orders/items and part comments to
`services/commerce-service/`; migrate shipping/item snapshots and holds. Other
domains use commerce DTOs, not its tables. Route catalog/store/admin-store and
part-comment endpoints; replace mixed summary/workspace clients. Add the
checkout-review interaction specified in the contracts document and use it in
the React cart UI. Identity and payment already exist, so this is a real
business-service interaction.

- Acceptance: independent commerce build/start/container/schema; SQL migrations
  and concurrent stock invariants; order/part-comment DTO and owner tests;
  duplicate event and hold-release checks; full store/admin E2E. CI verifies
  commerce, payment/identity consumer contracts and container integration.
  The gateway and backend contain no commerce-table access after cutover.
- Reactive acceptance: actual concurrent WebClient requests to identity/payment,
  MVC async response, timeout/cancellation and bounded-load tests; no request
  `.block()` or blocking provider/JPA on event-loop threads; trace demonstrates
  the two remote spans. Review never reserves stock or bypasses checkout checks.
- Observe stock conflicts, pending orders, executor/pool saturation and dependency
  latency. Cart browsing remains useful when checkout review reports unavailable.
- Recovery: use the common freeze/import protocol, including reservation and
  event-inbox state; do not restore old stock quantities while new paid orders
  exist. Before cutover writes, route reversal is safe after parity checks.

## S10 - Extract marketplace-service

Move remaining auctions/bids/vehicle listings/images, both test-drive models,
follows, auction comments and deposit business reservations to
`services/marketplace-service/`. Move the scheduler once, with its publication
dedupe history. Scalar identity references and payment clients already exist.

- Acceptance: independent artifact/container/schema/startup; Flyway image/ID/
  timestamp parity; auction deadline, concurrent bid/moderation, ownership,
  test-drive status, gallery, deposit exclusivity and late-payment tests; all
  marketplace browser flows pass. No marketplace foreign table reads, no
  duplicate schedulers. CI covers service, RabbitMQ contracts, MySQL, E2E and
  build. Existing admin bid approval semantics remain marketplace-local.
- Observe auction scan timing, rejected closed bids, reservation age and deposit
  reconciliation; trace one deposit result and one ending-soon notification.
- Recovery: freeze relevant mutations and scheduler, preserve newer bids,
  reservations/payment-result offsets and gallery blobs; reconcile before
  reverse migration or repair forward. A source tag cannot restore this data.

## S11 - Retire the legacy backend and finish route composition

Move the limited public-summary/workspace read composition and legacy redirect
catalogue to the gateway; domain actions remain in services. Identity owns the
admin user dashboard. Remove remaining fallback routes only when every used
path has an explicit owner and compatibility test. Disable `back/` deployment;
remove its code/tables in a separate reviewed cleanup after the recovery window.

- Acceptance: full React E2E and legacy alias checks pass with backend container
  absent; five independently built business artifacts plus gateway, each with
  its own schema credential; clean and preserved-volume Compose starts; all
  service/contract/container CI checks are required and passing. Check no shared
  table grants, shared domain jars or legacy service dependencies remain.
- Verify composed endpoint latency/503 behavior and uploaded images. Preserve
  metrics/traces across composition; no business transaction in the gateway.
- Recovery: restore compatible gateway image/route configuration to the same
  current service owners. Re-enabling stale monolith writes is not rollback.

## S12 - Complete observable operation

Consolidate the health/log/metric/trace work already shipped with each service.
Add a small optional Compose observability profile: Prometheus, Grafana and a
trace backend (proposed Tempo), using compatible Micrometer/OpenTelemetry
instrumentation. Structured stdout/Compose logs are sufficient initially for
logs; add a log backend only for a concrete rubric/demo need.

- Acceptance: scrape every service privately; dashboards show request rate/error/
  latency, connection/worker pressure, DB health, outbox age, queue/DLQ depth,
  pending payment/stock holds and notification lag. Show a correlation from
  gateway through business REST and asynchronous payment/notification events.
- Demonstrate an unavailable dependency and a poisoned event, actionable alert
  and recovery runbook. No public actuator/admin endpoints or secret payloads.
  CI validates provisioning/config and a telemetry smoke; all independent
  builds/startups/tests still pass with telemetry exporters unavailable.
- Recovery: disable optional telemetry profile/export without blocking business
  startup; retain health and structured local logs. Do not reset business data.

## S13 - Deployment/CD and course acceptance

Choose the actual host/public HTTPS origin/registry and secret injection once
the original deployment rubric and available host are known. Proposed minimal
delivery: CI builds and publishes immutable SHA-tagged images to GHCR after
successful master checks; an owner-triggered protected deployment workflow
pulls those exact images on one Compose host, backs up data, runs compatible
migrations, verifies readiness and executes a public-origin smoke. If the course
requires automatic continuous deployment rather than manually triggered
delivery, adjust the trigger and approval policy explicitly.

- Acceptance: reproducible deployment from immutable artifacts; no secrets in
  Git/images/logs; external HTTPS, cookie/reset/Stripe URL verification, sandbox
  success/deposit/cancel smoke and one REST + event + reactive demonstration.
  Verify every service's independent image/startup and all required CI results
  for the deployed SHA. Document host setup, restore and rollback commands.
- Test failed health rollout recovery using prior compatible images. Database
  migrations use expand/contract; incompatible data changes require backup and
  forward repair, not automatic blind image rollback. Observe the deployment
  failure in the dashboards/runbook.
- Final rubric matrix cites original requirement sections and links to artifacts,
  CI, screenshots/traces and demo steps. Until that rubric is supplied/reviewed,
  record coverage against this brief and explicitly leave course compliance
  unverified. Dependency remediation remains a separately tracked task.

## Focused follow-up branches

Create each branch from freshly fetched `master` after dependencies merge, not
from this architecture branch. The ordering below is intentional: prove current
flows, create routing/operational foundations, stabilize identity and messaging,
separate provider risk, then move commerce and marketplace data.

| Stage | Branch | Focus / dependency |
| --- | --- | --- |
| S1 | `feature/david.subotin_browser-e2e-baseline` | Next: isolated browser regression suite and CI evidence |
| S2 | `feature/david.subotin_api-gateway` | Pass-through gateway, Dockerfile and route/session compatibility; after S1 |
| S3 | `feature/david.subotin_docker-compose-baseline` | Backend/frontend/MySQL Compose and integration CI |
| S4a | `feature/david.subotin_session-csrf-foundation` | Coordinated CSRF/client/session rotation with E2E |
| S4b | `feature/david.subotin_identity-boundary` | Scalar user references and identity clients in backend |
| S5 | `feature/david.subotin_identity-service` | Identity data/routes, session exchange and internal trust |
| S6a | `feature/david.subotin_rabbitmq-outbox` | Broker, real ending-soon publisher, outbox/retry contracts |
| S6b | `feature/david.subotin_notification-service` | Inbox/delivery extraction, dedupe/import, reset delivery |
| S7 | `feature/david.subotin_checkout-reliability` | Durable local checkout attempts, stock holds and pending UI |
| S8 | `feature/david.subotin_payment-service` | Provider records, webhook owner, normalized results/recovery |
| S9a | `feature/david.subotin_commerce-service` | Parts/cart/order/comment extraction |
| S9b | `feature/david.subotin_reactive-checkout-review` | Commerce WebClient business fan-out and UI/test evidence |
| S10 | `feature/david.subotin_marketplace-service` | Marketplace data/routes/scheduler extraction |
| S11 | `feature/david.subotin_retire-legacy-backend` | Complete route composition and remove runtime fallback |
| S12 | `feature/david.subotin_observability` | Consolidated dashboards/traces/alerts and failure demo |
| S13a | `feature/david.subotin_container-cd` | Host-specific artifact publishing/deployment/recovery |
| S13b | `feature/david.subotin_iroit-course-evidence` | Requirement citations and final reproducible demonstration |
| Separate | `feature/david.subotin_frontend-dependency-remediation` | Re-audit and remediate reported npm vulnerabilities; outside architecture/extraction PRs |

S6a can publish to a durable staging queue with a test consumer; S6b connects the
real notification consumer before switching user inbox writes. Do not discard
queued events. S9 is complete only when both commerce extraction and reactive
interaction evidence pass. Further splitting a large stage into smaller coherent
PRs is allowed; keep its acceptance/recovery gates intact and document adapters.

The actual course specification should be located and its requirements matrix
updated as soon as available, not deferred until S13b. That last task assembles
final evidence; it is not the first requirements review.

## Owner commands for this documentation change

The requested branch already exists and is checked out. Review these six files;
stage them explicitly to avoid including unrelated future work. No commit/push/
PR/merge has been performed. `git diff --check` alone does not inspect untracked
files, so stage only after review and check the staged diff too.

```powershell
Set-Location C:\Projects\SubotinMotors
git status --short --branch
git add -- docs/iroit-baseline.md docs/iroit-architecture.md docs/iroit-service-ownership.md docs/iroit-api-events.md docs/iroit-security.md docs/iroit-migration-plan.md
git diff --cached --check
git diff --cached --stat
git diff --cached
git commit -m "docs: define IROIT microservices architecture and migration plan"
git push -u origin feature/david.subotin_microservices-architecture
```

Suggested PR title: **docs: define IROIT microservices architecture and migration plan**

Suggested PR body:

> Define the target five-service architecture and gateway for the IROIT project,
> grounded in the current React/Spring application. Document source ownership,
> private databases, API/event contracts, session/CSRF migration, reliable
> payment/order/stock coordination and staged extraction with acceptance and
> recovery gates. Record the verified CI baseline and owner-run tag commands.
>
> This change adds documentation only. Original course-rubric requirements that
> could not be verified are explicitly separated from proposed design choices.
>
> Validation: source inventory/ownership coverage, relative Markdown links,
> fenced JSON examples and whitespace checked; diagram syntax reviewed. No
> application tests or builds rerun for this documentation-only change. Baseline
> Backend and Frontend CI success is linked for the exact merged commit.

GitHub CLI currently needs authentication. If using it, run `gh auth login`
yourself first. The following creates a PR body with actual newlines in a
temporary file; alternatively paste the text above into GitHub's PR form.

```powershell
$architecturePrBody = @'
Define the target five-service architecture and gateway for the IROIT project, grounded in the current React/Spring application. Document source ownership, private databases, API/event contracts, session/CSRF migration, reliable payment/order/stock coordination and staged extraction with acceptance and recovery gates. Record the verified CI baseline and owner-run tag commands.

This change adds documentation only. Original course-rubric requirements that could not be verified are explicitly separated from proposed design choices.

Validation: source inventory/ownership coverage, relative Markdown links, fenced JSON examples and whitespace checked; diagram syntax reviewed. No application tests or builds rerun for this documentation-only change. Baseline Backend and Frontend CI success is linked for the exact merged commit.
'@
$architecturePrBodyPath = Join-Path ([System.IO.Path]::GetTempPath()) 'subotinmotors-iroit-architecture-pr.md'
Set-Content -LiteralPath $architecturePrBodyPath -Value $architecturePrBody -Encoding utf8
gh pr create --repo DavidSubotinDS/SubotinMotors --base master --head feature/david.subotin_microservices-architecture --title "docs: define IROIT microservices architecture and migration plan" --body-file $architecturePrBodyPath
```

After this PR is merged, and only with a clean/preserved working tree, start S1:

```powershell
git switch master
git fetch origin
git pull --ff-only origin master
git switch -c feature/david.subotin_browser-e2e-baseline
```

Do not reset or overwrite unrelated local work if these commands report a dirty
tree or diverged branch. Keep `master` as the default branch.
