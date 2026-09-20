# Suggested PR title

Current S3 container/MySQL evidence and owner commands are in
[docker-compose-pr.md](docker-compose-pr.md); this file retains the original S1 handoff.

Historical S1 handoff: PR #18 is now merged as `35007b5` and merged-master CI
passed. Do not repeat its commit/PR commands. Current S2 commands and evidence
are in [api-gateway-pr.md](api-gateway-pr.md).

test: add isolated Playwright browser regression baseline

# Suggested PR body

Add a reproducible browser baseline before gateway routing and service extraction.
The suite builds the current React app, starts the real Spring backend with a
disposable in-memory H2 database, resets deterministic fixtures per scenario, and
exercises authentication, roles/ownership, auctions/deadlines, listings and both
appointment types, address/cart/orders, notifications, admin order items and
legacy navigation.

Extend the existing application Clock to auction API status/bid validation and
appointment validation so deadline assertions can run at exact boundaries.
Production keeps its system clock. The test-only launcher and provider adapter
are excluded from the deployable JAR; no production security configuration or
Flyway migration changes are included.

Simulate outbound checkout creation without credentials. Post signed synthetic
events through the real Stripe SDK verifier and webhook handlers, testing paid,
unpaid and expired outcomes, signature/body rejection, deduplication and read-only
return navigation. Document the separate owner-run real Stripe sandbox smoke.

Run browser E2E inside the existing required Frontend check, preserving Backend
and Frontend names. Always retain failure traces/screenshots/videos and server
logs, and verify process cleanup. Record existing legacy detail-ID/query-filter
and pending/cancel UX limitations rather than claiming they are fixed.

Validation: 12 Playwright scenarios, 96 backend tests and 12 frontend tests pass
locally; both production builds pass. Cleanup after browser failure and an
intentional post-startup failure was verified. See the dated results and limits
in [the E2E runbook](browser-e2e.md). Remote CI and the manual live sandbox smoke
remain owner-run after submission.

# Owner commands (PowerShell)

No commit, push, PR or merge has been performed. Review the exact files below;
these commands intentionally leave any other work unstaged.

```powershell
Set-Location C:\Projects\SubotinMotors
git branch --show-current
# Must be feature/david.subotin_browser-e2e-baseline
git status --short --branch
git diff --check
git add -- .github/workflows/ci.yml .gitignore README.md back/README.md front/README.md docs/browser-e2e.md docs/browser-e2e-pr.md docs/iroit-architecture.md docs/iroit-migration-plan.md front/package.json front/package-lock.json front/vite.config.js front/vite.e2e.config.js front/playwright.config.js front/e2e back/src/test/java/e2e back/src/test/resources/application-e2e.properties back/src/main/java/lithan/autostrada/auctions/controller/api/ApiModelMapper.java back/src/main/java/lithan/autostrada/auctions/entity/Car.java back/src/main/java/lithan/autostrada/auctions/service/CarListingServiceImpl.java back/src/main/java/lithan/autostrada/auctions/service/UserCarServiceImpl.java
git diff --cached --check
git diff --cached --stat
git diff --cached
git commit -m "test: add isolated Playwright browser regression baseline"
git push -u origin feature/david.subotin_browser-e2e-baseline
```

For GitHub CLI, authenticate yourself if needed, then create the PR against
master using a body file with real newlines:

```powershell
$e2ePrBody = @'
Add an isolated Playwright browser baseline for the current React/Spring monolith before gateway routing and service extraction. Cover authentication/session/logout, roles and ownership, auctions and exact deadlines, listings and appointments, address/cart/orders, notifications, admin order items and legacy navigation.

Use a test-only backend launcher, disposable H2 fixtures and controlled business/browser time. Simulate outbound checkout creation; exercise the real Stripe SDK signature verifier and webhook handlers with signed synthetic paid/unpaid/expired results. Return navigation cannot settle payments. A separate real sandbox smoke is documented.

Keep Backend and Frontend check names; browser failures fail Frontend. Retain failure traces/screenshots/videos and process logs, with cleanup on failure. See docs/browser-e2e.md for verification evidence and existing legacy-detail/pending-state limitations. No gateway, service extraction, security redesign or dependency remediation is included.

Local verification: 12 browser scenarios, 96 backend tests and 12 frontend tests passed; both builds succeeded. Cleanup after browser failure and an intentional startup-complete failure was verified. Remote CI and real Stripe sandbox smoke remain owner-run.
'@
$e2ePrBodyPath = Join-Path ([System.IO.Path]::GetTempPath()) 'subotinmotors-browser-e2e-pr.md'
Set-Content -LiteralPath $e2ePrBodyPath -Value $e2ePrBody -Encoding utf8
gh pr create --repo DavidSubotinDS/SubotinMotors --base master --head feature/david.subotin_browser-e2e-baseline --title "test: add isolated Playwright browser regression baseline" --body-file $e2ePrBodyPath
```

After the owner merges this PR, preserve local work, fetch master, and start S2:

```powershell
git switch master
git fetch origin master
git pull --ff-only origin master
git switch -c feature/david.subotin_api-gateway
```

Do not reset a dirty/diverged checkout. No baseline tag is assumed or created.
