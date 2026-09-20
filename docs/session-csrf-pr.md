# S4a evidence and owner handoff

Branch: `feature/david.subotin_session-csrf-foundation`, based on freshly fetched
`8d131049a8f8ed4b96f7d664eb57e3c52e934dc3`. Default branch remains `master`.
No staging, commits, pushes, PRs, merges or tags were performed. The original
clean `feature/david.subotin_session-csrf-hardening` branch and other branches,
ignored developer data and normal Docker resources were preserved.

## Prerequisite evidence

On 2026-09-20 the public GitHub API confirmed
[S3 PR #20](https://github.com/DavidSubotinDS/SubotinMotors/pull/20) merged to master
at 10:15:21 UTC with the exact SHA above. That merged commit passed
[Backend](https://github.com/DavidSubotinDS/SubotinMotors/actions/runs/35504550026/job/106062035971)
and [Frontend](https://github.com/DavidSubotinDS/SubotinMotors/actions/runs/35504550026/job/106062036117).
Only then was the requested branch created. GitHub CLI was unauthenticated;
public REST supplied verification. Required protection settings remain
owner-reported; check results/default branch were independently verified.
Local and remote tag lists were empty; no baseline tag is assumed.

All six shared architecture files, root/backend/frontend READMEs, gateway and
Compose/browser runbooks/handoffs, CI, security/auth/API/legacy controllers,
frontend client/call sites, route configuration and harnesses were inspected.
No applicable AGENTS.md was found in repository/ancestors; `.agents` was empty.
The [complete request inventory](session-csrf.md#inspected-unsafe-request-inventory)
records the S3 contract and coordinated S4a changes.

The first sandboxed Docker probe was denied access to its config/engine pipe.
A bounded host probe independently succeeded with Engine **28.3.3**. Docker then
ran the full S4a container/MySQL/browser suite. The recorded September 20 inference
manager startup incident remains historical; this task does not diagnose its root
cause, claim a factory reset completed, or assume preexisting images/volumes.
No reset, prune, WSL unregister or unrelated machine changes were performed.

## Implemented changes

- Backend `SecurityConfig`, `AuthApiController`, `SessionApiController`: anonymous
  non-cacheable `{token}` endpoint, expected token in HttpSession, default masked
  token handling, unsafe API/form enforcement, exact POST-only Stripe exception,
  distinguishable CSRF JSON 403, explicit native CORS header support and API/form
  fixation protection. API login uses framework strategies and explicit context
  persistence. `/api/session`, JSESSIONID, roles and ownership remain compatible.
- `RegisterController`: successful protected profile registration POST now
  invalidates its temporary session. Thank-you GET is read-only, closing the
  direct-backend navigation logout route while preserving redirects.
- Frontend `client.js` and `Navbar.jsx`: memory-only token acquisition, credentials,
  unsafe header injection, preserved FormData boundaries, serialized mutations,
  auth refresh, stale/expired-session feedback and queued-action cancellation.
  No automatic mutation retry, including checkout/deposit/network ambiguity.
  Logout failures are visible and its pending button is disabled.
- Gateway: explicit `csrf` route before API routing, backend ownership unchanged.
  Header/cookie forwarding test supplements existing precise CORS, transport,
  raw webhook, multipart, forwarding sanitization and health tests.
- Tests/harness: actual-token security matrix, existing API test clients updated,
  frontend lifecycle/no-replay tests, six additional browser scenarios, CSRF-aware
  test API helper and production Compose auth checks. Existing role/ownership
  tests now pass valid tokens so they test authorization rather than failing
  prematurely at CSRF. Production controls remain excluded.
- `LoggingEmailService`: no reset-link or mail-body logging. SMTP is required for
  real recovery delivery. The test-only mailbox uses the private, token-protected
  control chain and is absent from production artifacts/public routing.
- Runbooks, READMEs, architecture status and environment-example comment: actual
  lifecycle, legacy inventory, compatibility, rollout/rollback and S4b handoff.

No schema/Flyway, entity ownership, service extraction, Redis, RabbitMQ, assertion,
JWT, distributed checkout, dependency remediation or deployment/CD changes.

## Verification record (Windows and Linux containers, 2026-09-20)

| Gate | Actual evidence |
| --- | --- |
| Backend | Final verification: 130 tests passed, zero failures/errors/skips, production JAR packaged, including the extended stale-token matrix |
| Gateway | Final verification: 18 tests passed, zero failures/errors/skips, including CSRF forwarding and CORS preflight; independent executable packaged |
| Frontend | 29 tests passed across 7 files; final production build passed |
| Native browser / gateway / disposable H2 | 18/18 passed, 43.4 seconds; all three owned ports verified free afterward |
| Compose/MySQL browser | Final implementation rerun: 18/18 passed in 1.4 minutes, with production image builds, private ingress/non-root checks, migrations, restart/restore/outage and cleanup; earlier complete run also passed |
| MySQL | All 18 Flyway migrations and Hibernate validation passed; schema-only grants, unique/FK/CHECK constraints and row-lock contention passed |
| Recovery | Preserved-volume recreation retained IDs, password/image hashes, profile sentinel and Flyway checksums; logical backup restore/data parity and database outage readiness/recovery passed |
| Production auth / transport | Cookie attributes, rotation/old-ID rejection, stale-token rejection/logout, CORS, configured redirect origin and reset request without reset-link logs passed |
| Browser auth / security | API/form old-ID rejection, login/logout token invalidation, reload reuse, anonymous/foreign/stale JSON/form/multipart denial, expired-session recovery and reset consumption passed |
| Payment simulation | Existing store/deposit scenarios passed with signed synthetic webhooks, invalid signatures/body mutation rejected, duplicate receipt and read-only return behavior retained; stale checkout produced no order/stock effects and only explicit retry created one order |
| Failure diagnostics | Initial native run: 16 passed, two selector failures; messages were present as inline text, selectors corrected. Traces/screenshots/video retained and owned ports freed. A later container build correctly failed on a newly added test's transaction-fixture issue, retained logs and cleaned owned resources |
| Final static/artifact review | Diff whitespace and JavaScript syntax checks passed; production JARs contain no E2E controls/test libraries; no applied migrations or CI gate changes; selective owner staging list matches all 39 changed source/document paths |
| S4a remote CI | Unverified; owner has not committed/pushed this work. Successful merged S3 CI is prerequisite evidence only |
| Real Stripe / SMTP / deployed data | Not run; synthetic events/provider simulation are not real Stripe sessions or delivered webhooks. Private test mailbox is not SMTP delivery. No developer/deployed DB was imported |

New direct tests obtain tokens from `/api/csrf` and run the production security
chain; they do not bypass CSRF. Ordinary business tests use Spring's standard
valid-CSRF test postprocessor where appropriate. New tests cover JSON, URL-encoded
forms and multipart, missing/malformed/foreign/stale tokens, positive API/legacy
registration/reset/logout, token transition and side-effect denial. Servlet
MockMvc verifies ID rotation; real browser/Compose HTTP verifies the old ID no
longer retrieves authentication. Existing USER/ADMIN/cross-user suites remain.

Two newly added direct tests originally shared JPA state across multiple simulated
HTTP calls in one rollback transaction. Reloading the persistence context between
requests fixed immutable-collection and repeated-picture fixture errors; no
production domain behavior was weakened or refactored to satisfy the tests.

Primary local evidence (ignored/private):

- `back/target/surefire-reports/`, `gateway/target/surefire-reports/`.
- `front/e2e-results/playwright.xml` and native build/server logs.
- `compose-results/autostrada-test-c2865ca87a15f592bd9d2ceabaa2f93e/`: first full successful run, evidence and 18-scenario browser report.
- `compose-results/autostrada-test-e5e52aa3866a3768ee6144f651935cdb/`: build failure with completed owned-resource cleanup.
- `compose-results/autostrada-test-212636cb2d81002ca789ef0110d6947d/`: final successful production/MySQL/browser run; no owned containers, volumes or networks remained.
- `target/s4a/`: final backend/gateway/frontend/native/Compose logs and earlier failure logs; `s4a-native-first-failure/` retains traces, screenshots and video. These private artifacts are ignored and excluded from owner staging.

The final gateway verification initially could not start because automatic approval
review reached a usage limit. After the owner resumed the task, approval succeeded
and the final 18-test verification completed. No approval blocker remains.

CI workflow is unchanged because existing auto-discovered suites include the new
tests. **Backend** still gates backend/gateway builds/tests/container smoke plus
production Compose/MySQL checks; **Frontend** gates tests/build/native and Compose
browser runs. No path filters, optional checks, swallowed errors or exclusions
were added. Always-run diagnostics and cleanup remain. Applied V1-V18 are unchanged.

## Compatibility, rollout and remaining limits

Use the [coordinated rollout/rollback procedure](session-csrf.md#coordinated-rollout-and-rollback).
During a maintenance window stop public ingress/writes, preserve MySQL storage,
deploy matching backend/frontend/gateway images, check readiness, reload browsers
and sign in again. Do not ship either CSRF half independently. Backend restart
expires its existing in-memory sessions. No schema/data rollback is needed.
Rollback restores a compatible image set together while ingress stays closed;
S3's CSRF exemption is a known security regression, not a permanent exposed fallback.
Prefer a protected forward repair. Never delete normal volumes or undo provider
effects by restoring DB state blindly.

Normal username/role/session DTOs and resource ownership are unchanged. Valid
legacy form POSTs keep redirects; thank-you navigation no longer logs users out.
Default log-mail no longer exposes recovery links: configure SMTP for actual use.
Token masking means token-string inequality alone is not proof of rotation;
tests establish rejection of the old token and acceptance of a new one.

Sessions remain in one backend process; no shared store or cross-instance support.
Password-reset consumption keeps its existing password/token behavior; global
revocation of other already-authenticated sessions is not added in this scope.
No browser timeout/load/other-engine/TLS deployment or live payment-provider
reliability claim is made. Chromium H2 and MySQL evidence are listed separately.
Real sandbox success/cancel/deposit/CLI-delivery smoke remains the separate
[owner procedure](browser-e2e.md#separate-manual-stripe-sandbox-smoke-owner-run).

Next after owner review, merge and verified merged-master Backend/Frontend:
**S4b identity boundary preparation**, on `feature/david.subotin_identity-boundary`.
That stage introduces scalar identity references/profile clients before extraction.

## Owner Git commands and suggested PR

No commands below were executed by the assistant. Review the branch/diff and use
the selective path list; do not stage private results, credentials or data.

```powershell
Set-Location C:\Projects\SubotinMotors
git branch --show-current
# Must be feature/david.subotin_session-csrf-foundation
git status --short --branch
git diff --check
git add -- .env.compose.example README.md back/README.md front/README.md gateway/README.md
git add -- back/src/main/java/lithan/autostrada/auctions/config/SecurityConfig.java back/src/main/java/lithan/autostrada/auctions/controller/RegisterController.java back/src/main/java/lithan/autostrada/auctions/controller/api/AuthApiController.java back/src/main/java/lithan/autostrada/auctions/controller/api/SessionApiController.java back/src/main/java/lithan/autostrada/auctions/service/LoggingEmailService.java
git add -- back/src/test/java/e2e/E2eApplication.java back/src/test/java/lithan/autostrada/auctions/CsrfSessionSecurityTests.java back/src/test/java/lithan/autostrada/auctions/CarListingAndAddressIntegrationTests.java back/src/test/java/lithan/autostrada/auctions/MarketplaceFeatureIntegrationTests.java back/src/test/java/lithan/autostrada/auctions/ReactApiSmokeTests.java back/src/test/java/lithan/autostrada/auctions/ReactApiValidationIntegrationTests.java back/src/test/java/lithan/autostrada/auctions/VehicleGalleryIntegrationTests.java
git add -- front/src/api/client.js front/src/api/client.test.js front/src/components/layout/Navbar.jsx front/src/pages/AuctionDetailPage.test.jsx front/e2e/compose.mjs front/e2e/fixtures.js front/e2e/specs/marketplace.spec.js front/e2e/specs/payments.spec.js front/e2e/specs/session-csrf.spec.js
git add -- gateway/src/main/java/lithan/autostrada/gateway/GatewayRoutes.java gateway/src/test/java/lithan/autostrada/gateway/GatewayTransportTests.java
git add -- docs/session-csrf.md docs/session-csrf-pr.md docs/browser-e2e.md docs/docker-compose.md docs/docker-compose-pr.md docs/iroit-architecture.md docs/iroit-baseline.md docs/iroit-service-ownership.md docs/iroit-api-events.md docs/iroit-security.md docs/iroit-migration-plan.md
git diff --cached --check
git diff --cached --stat
git diff --cached
git commit -m "feat: enforce session CSRF protection and login rotation"
git push -u origin feature/david.subotin_session-csrf-foundation
```

Suggested PR title: **feat: enforce session CSRF protection and login rotation**

Suggested body:

> Add anonymous, non-cacheable GET /api/csrf to the existing backend session owner.
> Require session-backed CSRF for unsafe APIs and legacy forms, exempting only
> exact signed POST /webhooks/stripe. Rotate API/form login session IDs and clear
> old tokens; keep the session DTO, JSESSIONID, ownership and S3 topology.
>
> Integrate memory-only frontend tokens, auth refresh, explicit stale-session
> recovery and queued-action cancellation. Never automatically replay mutations,
> checkout or deposits. Preserve multipart boundaries and legacy redirects; move
> thank-you GET session invalidation into the protected registration POST. Stop
> logging reset links and test recovery using a private harness-only mailbox.
>
> Add direct security, frontend lifecycle and browser regression in native H2 and
> isolated Compose/MySQL. Preserve Backend/Frontend gates, diagnostic retention
> and owned-resource cleanup. See docs/session-csrf-pr.md for measured evidence,
> rollout/rollback and limitations. No schema change or service extraction.
>
> Local verification passed: 130 backend, 18 gateway and 29 frontend tests;
> production builds; 18 native H2 and 18 Compose/MySQL browser scenarios; all 18
> migrations, constraints/locking, preserved restart, backup/restore and outage
> recovery. Disposable resources were cleaned; failure diagnostics were retained.
>
> Remote S4a CI and real Stripe sandbox/SMTP delivery remain unverified until owner
> execution. Next is S4b identity boundary preparation.
