# S2 API Gateway: evidence and owner handoff

Branch: `feature/david.subotin_api-gateway`. No commits, pushes, PRs or merges
were performed for S2. Default branch remains master. No baseline tag was created.

## Prerequisite evidence

Fetched master after the owner merged S1 PR #18. Master is
`35007b5eb71e602afa96e09f1c5f9d38e3f7406b`; its tree equals the S1 branch's
`fc59eb632c98c6dab5a3932122cf27ea7b9facb7` tree (squash merge, not an ancestry merge).
All six architecture documents, including `iroit-api-events.md`, and both browser
runbooks are present and were read. The already-created requested S2 branch was
at that exact fresh master with a clean tree and was reused. Other local branches
were preserved, including the architecture branch's unpushed commits.

[Merged-master run 34818110319](https://github.com/DavidSubotinDS/SubotinMotors/actions/runs/34818110319)
passed Backend and Frontend. This is S1 evidence only. Required branch protection
remains user-reported; unauthenticated protection inspection returned HTTP 401.
Local and remote tag lists were empty at prerequisite inspection.

## Implemented behavior

- Independent Java 17 Boot 3.5.15 / Cloud 2025.0.3 / Gateway WebFlux 4.3.5 executable,
  wrapper, tests, container definition and runbook. No backend build/domain/database
  dependency and no Stripe SDK.
- Every API method, exact signed webhook POST and explicit legacy action family
  routes to backend. Canonical React GET/HEAD pages take precedence over MVC views;
  assets route to frontend. No catch-all SPA for API/private/webhook errors.
- Cookies, sessions, roles/ownership, CSRF baseline, statuses, errors, redirects,
  multipart and raw webhook bytes preserved. Forged identity/forwarding headers
  stripped; public origin constructed from configuration. Gateway owns precise
  credential-compatible API CORS. Backend still owns all business authorization.
- Bounded proxy/pool timeouts, correlated safe logs, in-process request metrics,
  independent liveness and dependency-aware readiness. No payload/query/secret logs.
- Browser application/API/legacy/provider-return/signed-webhook traffic now uses
  gateway. Private test controls are excluded from routing. Harness refuses occupied
  ports, uses only disposable memory H2, retains failure evidence and cleans up.
- Backend legacy detail/edit model IDs survive redirects; browser tests assert
  selected details and reloads rather than the old catalog destination.
- Gateway tests/image smoke gate existing Backend; browser failures gate Frontend.
  Neither required check was renamed. Compose/MySQL remain S3.

## Verification record (Windows, 2026-09-14; final follow-up 2026-09-17)

| Gate | Actual evidence |
| --- | --- |
| Backend clean verify | 105 tests passed; production JAR packaged; includes 9 new selected-resource redirect cases |
| Frontend component tests / production build | 12 tests passed across 6 files; build passed |
| Independent gateway clean verify | 17 tests passed; standalone executable packaged; HTTP stub transport and no-upstream startup/health tests |
| Browser through gateway | Final 2026-09-17 run: 12 passed, 0 failed/skipped, 30.1 s for scenarios; includes oversized multipart rejection and detail reloads |
| Browser-failure cleanup/artifacts | First run failed its outdated 403 control-route assertion (gateway returns 404); screenshot, video and trace produced; all three ports free afterward. Preserved locally under `front/e2e-results/s2-failure-evidence/` |
| Post-startup failure probe | `--verify-failure-cleanup` failed intentionally and stopped all three servers; port checks passed |
| Production artifact isolation | Backend JAR contains no E2E launcher/provider/controls; gateway contains no business entities, JPA/Flyway/JDBC or Stripe SDK (Hibernate Validator is validation, not ORM) |
| Container | Dockerfile and CI build/smoke implemented. Local build attempted; Docker Desktop Linux engine pipe unavailable, including after a start attempt. Image/runtime not verified locally |
| S2 remote CI | Not run: owner has not committed/pushed these changes. Successful S1 master CI is not S2 evidence |
| Real Stripe sandbox / MySQL / other browsers | Not run; separate manual smoke and S3+ tasks |
| Static checks | Workflow YAML parsed with Backend/Frontend names intact; helper PowerShell/Node syntax, local documentation links, new-file whitespace and git diff --check passed |

Gateway transport coverage includes method/body/query encoding, multiple cookies
and expiry, Location/status, chunked UTF-8 webhook bytes/signature, 1 MB multipart
streaming beyond codec buffer limits, backend JSON errors, unavailability and
timeout, private routes, SPA precedence, legacy form transport, exact CORS and
forged identity/forwarding headers. Browser coverage retains real-backend
owner/role enforcement, adds forged-header and legacy CSRF denial, and checks
the backend file-size limit through gateway. A disposable direct-backend diagnostic
confirmed the existing oversized-file response is empty HTTP 400, also returned
through gateway; no 413 or JSON body is invented. The temporary diagnostic was
removed from the final harness; only control calls use the private backend port.

Logs/reports are ignored local artifacts under `gateway/target/`,
`gateway/target-verification.log`, and `front/e2e-results/`. Full native commands,
route catalogue, configuration and recovery are in [gateway/README.md](../gateway/README.md).

Applicable common gates: independent build/startup, current functionality/browser
flows, transport/security contracts, proxy health/logging and failure recovery
have local evidence. CI/container checks are implemented but S2 remote execution
and local container runtime remain unverified. No schema/data move, schema-owner
grant, MySQL migration or broker gate is claimed applicable/completed in S2.

Remaining behavior: catalog filters do not initialize from transported query
strings; CHECKOUT_CREATED/pending/cancel UI semantics are unchanged; signed
failure/expiry, not navigation, releases reservations. Legacy admin account
editing still uses the users screen. No identity extraction, JWT browser auth,
session sharing, CSRF redesign, distributed checkout or broad Compose was added.

## Recovery and next task

Restore the direct backend API/proxy URL and coherent frontend, reset and Stripe
return/webhook configuration together; rebuild React if its build-time API base
changed. Stop only owned gateway/test processes. No production data rollback or
normal DB deletion is required. Backend restart loses its in-memory sessions.
After the owner merges S2 with required CI passing, start **S3** on freshly fetched
master: `feature/david.subotin_docker-compose-baseline`.

## Owner commands (PowerShell)

Review before staging. These commands stage only S2 paths, not unrelated work:

```powershell
Set-Location C:\Projects\SubotinMotors
git branch --show-current
# Must be feature/david.subotin_api-gateway
git status --short --branch
git diff --check
git add -- gateway .github/workflows/ci.yml .gitignore README.md back/README.md front/README.md front/.env.example front/vite.config.js front/vite.e2e.config.js front/playwright.config.js front/e2e back/scripts/run-stripe-sandbox.ps1 back/src/main/java/lithan/autostrada/auctions/config/ReactFrontendRedirectConfig.java back/src/main/java/lithan/autostrada/auctions/config/GatewayRequestLoggingFilter.java back/src/main/resources/application-gateway.properties back/src/test/java/lithan/autostrada/auctions/LegacyDetailRedirectTests.java back/src/test/java/e2e/SimulatedStripeGateway.java back/src/test/resources/application-e2e.properties docs/iroit-architecture.md docs/iroit-baseline.md docs/iroit-service-ownership.md docs/iroit-api-events.md docs/iroit-security.md docs/iroit-migration-plan.md docs/browser-e2e.md docs/browser-e2e-pr.md docs/api-gateway-pr.md
git diff --cached --check
git diff --cached --stat
git diff --cached
git commit -m "feat: route monolith traffic through independent WebFlux gateway"
git push -u origin feature/david.subotin_api-gateway
```

Suggested PR title: **feat: add pass-through API gateway with browser regression coverage**

Create the PR yourself after inspecting the actual pushed CI result. With an
authenticated GitHub CLI, use this body file (real newlines, no flattened shell text):

```powershell
$gatewayPrBody = @'
Introduce an independently built Java 17 Spring Cloud Gateway in front of the existing monolith. Route APIs, exact Stripe webhook POST and legacy actions to backend; serve canonical React pages through the same public origin. Backend retains authentication, sessions, authorization, persistence and payment ownership.

Preserve cookie/query/body/status/error/redirect contracts, sanitize untrusted forwarded and identity headers, define precise credentialed CORS, and add bounded timeouts, request correlation and health. Repair selected-resource legacy redirects. Move the isolated Playwright stack and signed synthetic webhook traffic through the gateway; keep test controls private. Gateway build/tests/container smoke gate Backend, and browser failures gate Frontend.

Local verification: 105 backend tests, 17 gateway tests and 12 frontend tests passed, with packaged builds; 12 browser scenarios passed through gateway. Cleanup and failure artifacts verified. Container build attempted but local Docker engine unavailable; required CI now builds/smokes the image. Real Stripe sandbox, MySQL and other browsers remain unverified. See docs/api-gateway-pr.md and gateway/README.md for evidence, configuration, limitations and recovery. No service extraction, auth redesign, database move or Compose topology included.
'@
$gatewayPrBodyPath = Join-Path ([IO.Path]::GetTempPath()) 'subotinmotors-api-gateway-pr.md'
Set-Content -LiteralPath $gatewayPrBodyPath -Value $gatewayPrBody -Encoding utf8
gh pr create --repo DavidSubotinDS/SubotinMotors --base master --head feature/david.subotin_api-gateway --title "feat: add pass-through API gateway with browser regression coverage" --body-file $gatewayPrBodyPath
```
