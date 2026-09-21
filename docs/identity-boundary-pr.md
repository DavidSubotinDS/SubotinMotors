# S4b evidence and owner handoff

Branch: `feature/david.subotin_identity-boundary`, from freshly fetched master
`1a3147baa4cf4c58a2aba5d2958980348439788e`. Default branch remains `master`.
Implementation is local/uncommitted. No staging, commits, pushes, PRs, merges,
tags or subagents were used. No normal developer database or unrelated Docker
resource was changed. Local acceptance passed; remote S4b CI/merge remain outstanding.

## Prerequisite and scope

Before creating the branch, Git status/branches/remotes and repository/ancestor
instructions were inspected. Master was clean and matched origin/master. No
applicable AGENTS.md was found. All six architecture documents, requested
READMEs, S4a/Compose/gateway/browser runbooks/handoffs, CI and relevant code were
read. Unmerged S4a changes were not carried into this branch.

GitHub public REST confirmed [S4a PR #21](https://github.com/DavidSubotinDS/SubotinMotors/pull/21)
merged to master at the exact SHA above, and both
[Backend](https://github.com/DavidSubotinDS/SubotinMotors/actions/runs/35546068946/job/106172081995)
and [Frontend](https://github.com/DavidSubotinDS/SubotinMotors/actions/runs/35546068946/job/106172081871)
completed successfully on that merged commit. GH CLI was unauthenticated; public
REST supplied the verification. Earlier in-progress responses were not treated
as success. The PR screenshot/branch checks alone were not used as master evidence.

The [inventory and implemented contract](identity-boundary.md) covers all 14
references in 13 entities, permitted identity-owned access and remaining coupling.
Business JPA associations/repository parameters are now scalar account IDs;
identity owns public batch projections and current-actor private checkout data.
Principal IDs are immutable and credential state erasable. Marketplace moderation
is separated from account administration; self profile and private mapping are
identity-owned. Account ID and profile ID are explicitly distinct.

Public/business profile DTO keys remain but accidental private-field exposure is
redacted; self/admin account views and order shipping snapshots remain authorized.
That privacy requirement takes precedence over retaining private data in public
responses. No schema/Flyway change, extraction, new session owner, remote client,
distributed infrastructure, dependency remediation or deployment is included.

## Actual local evidence (2026-09-21)

| Gate | Result |
| --- | --- |
| Backend Java 17 Maven clean verify | 142 tests, zero failures/errors/skips; production JAR built |
| Final backend verify after compatibility refinements | 142 tests, zero failures/errors/skips; production JAR rebuilt |
| Gateway independent clean verify | 18 tests passed; JAR built |
| Frontend Vitest | 29 tests in 7 files passed |
| Frontend production build | Passed |
| Production artifact inspection | Backend JAR excludes test launchers/controls; frontend dist excludes test control routes/token names |
| Native gateway / disposable H2 Chromium | 20 scenarios passed; owned processes stopped and in-memory DB discarded |
| Final native rerun after compatibility refinements | 20 scenarios passed; cleanup completed |
| Boundary regression | 10 integration and 2 source/principal architecture tests included in the 142 |
| Compose script syntax | `node --check front/e2e/compose.mjs` passed |
| Container builds / Compose startup | Passed on Docker Engine 28.3.3; production containers non-root, upstream ports private |
| MySQL migrations, new identity constraints/index checks, locking | Passed: V1-V18, all 14 identity references, grants/unique/FK/CHECK constraints and competing row lock |
| MySQL preserved-data restart, backup/restore and outage recovery | Passed; existing business IDs/references, hashes and migration checksums preserved |
| MySQL old-runtime-to-new-runtime upgrade | Passed: archived S4a production backend replaced with current S4b on the same disposable MySQL volume; every table checksum unchanged |
| Compose/gateway/MySQL browser | 20 scenarios passed; owned containers, volumes and networks cleaned and absence verified |
| S4b remote Backend / Frontend | Not run: owner has not committed/pushed this work |
| Real Stripe sandbox / SMTP delivery | Not exercised; payment browser tests use deterministic provider simulation and real signed fixture webhook verification |

The backend tests cover batch lookup query counts/no identity entity hydration,
missing account/profile fallback, separate account/profile IDs, public privacy,
private self access, unsupported/deleted principal denial, immutable principals,
forged owner fields, existing FK rejection and bulk business DTO mapping. Existing
tests retain USER/ADMIN restrictions and ownership denial without side effects.
The browser suite covers registration/login/recovery, session rotation, CSRF,
stale-session no-replay, profile/upload/display, auction creation/moderation/bids,
appointments, follows, checkout/deposit ownership and signed webhook outcomes.

Initial development compile/test failures were fixed before the passing full
run: scalar caller/fixture changes, the legacy public-profile redirect adapter,
and a test's production_year column name. Logs were retained. These were not
production validation relaxations; tests now authenticate with production-shaped
principals instead of making production accept arbitrary string principals.

An earlier read-only Docker probe returned Engine 28.3.3. At the initial execution,
`npm run test:compose` failed because `dockerDesktopLinuxEngine` did not exist;
a bounded `docker version`, context and process check confirmed it was unavailable.
No reset, prune, normal-volume deletion, WSL change or machine setting change was
attempted. Project `autostrada-test-241a1690cf80075aea0f64c1a608381d` failed its
initial collision check before `owned=true`, so no containers/volumes/networks
were created by this attempt. Historical S4a Docker results are not S4b evidence.

Local logs are in ignored `target/s4b/`: `backend.log`, `backend-final.log`,
`gateway.log`, `frontend-test.log`, `frontend-build.log`, `native.log`,
`native-final.log`, `compose.log` and earlier failure logs. Browser reports/logs
remain in `front/playwright-report`, `front/test-results`, `front/e2e-results`.
Compose failure diagnostics remain under the named `compose-results` directory.
Treat test artifacts as private; do not commit traces or test payloads.

## Docker follow-up and rerun commands

Docker Desktop was subsequently started by the owner. The full run and a separate
old-runtime upgrade rehearsal passed. Evidence:

- Full containers/MySQL/browser: `compose-results/autostrada-test-65547144a3825ed13e3753dc26ec2cc2/evidence.json`.
- S4a-to-S4b upgrade and integration: `compose-results/autostrada-test-780452b9ef92064b9e8ecc8662c51002/evidence.json`.
- Both runs verified no owned containers, volumes or networks remained. Logs:
  `target/s4b/compose-final.log` and `target/s4b/upgrade-final.log`.

To repeat the full check from the repository root:

```powershell
docker version
node front/e2e/compose.mjs
```

This builds production containers, starts a uniquely named isolated MySQL stack,
validates V1-V18, constraints/indexes/locking, all 14 identity columns, preserved
row IDs/references on restart/restore, outage recovery and the 20 browser scenarios.
It retains diagnostics and cleans only its owned project on either outcome.
Do not substitute the normal Compose project/database for this harness. The
upgrade rehearsal used an archive of the verified S4a commit, with data created
by its production backend and a profile sentinel. It is not a rehearsal against
a copy of a deployed database. Repeat it with:

```powershell
New-Item -ItemType Directory -Force target/s4b/s4a-source | Out-Null
git archive --format=tar --output=target/s4b/s4a-source.tar 1a3147baa4cf4c58a2aba5d2958980348439788e back
tar -xf target/s4b/s4a-source.tar -C target/s4b/s4a-source
node front/e2e/compose.mjs --integration-only --upgrade-from=target/s4b/s4a-source/back
```

No applied migration changed. The optional archived-runtime rehearsal is local
S4b evidence; the regular clean/init, restart/restore and browser checks remain
in the existing required CI jobs.

CI retains required names `Backend` and `Frontend`. The existing Backend Maven
step automatically runs the new boundary tests; Backend also blocks on container
and isolated MySQL failures. Frontend automatically discovers the two added
browser tests in both topologies, and failures remain blocking. No workflow gate
was skipped or made advisory. Verify exact final-commit checks before merging,
then verify merged-master checks as the next stage prerequisite.

## Rollout, rollback and next stage

Follow the [contract rollout/rollback](identity-boundary.md#rollout-and-rollback).
Keep backend-owned sessions, the single gateway origin and S4a enforcement.
Re-login after backend replacement because principal serialization changed.
Retained columns/IDs allow a code rollback to the verified S4a backend without a
data restore; that rollback also restores S4a's broader profile exposure. Keep
backups/image/config references and avoid overwriting newer writes.

Next documented stage is **S5 identity-service extraction**, after S4b acceptance
and merge. Its five-table migration, independent process/schema/routes, private
session exchange and internal trust are deferred. Existing restrictive identity
FKs must only be reconsidered with that validated cutover.

## Suggested PR

Title: `feat: prepare in-process identity boundary with scalar account references`

Body:

```markdown
Business entities and services previously traversed identity JPA entities for
ownership and display. Replace 14 associations across 13 business entities with
the existing scalar account ID columns and introduce in-process public batch
profile and current-actor checkout contracts. Keep identity/session ownership in
the backend and all existing database FKs; no schema migration or extraction.

Use immutable authenticated account IDs, separate account administration from
marketplace moderation, and enforce permitted identity persistence owners with
boundary tests. Preserve request/session contracts, role/ownership restrictions,
S4a CSRF/session rotation and no mutation replay. Retain public DTO keys while
redacting private identity fields; self/admin account views remain private.

Validation: backend 142, gateway 18 and frontend 29 tests passed. Browser tests
passed 20 scenarios in each topology (native/H2 and Compose/MySQL); production Java/frontend/container builds
passed. MySQL constraints, restart/restore/outage recovery and S4a-to-S4b upgrade
with every table checksum preserved passed. Both disposable projects were
cleaned. Record exact-commit Backend/Frontend success before merge. Real Stripe
sandbox and SMTP delivery were not exercised.

See docs/identity-boundary.md and docs/identity-boundary-pr.md for inventory,
remaining coupling, evidence, limitations and rollout/rollback. S5 is deferred.
```

Update the validation paragraph with actual remote CI results before merge.

## Selective owner Git commands

Review the file list and diffs before executing the commands below. These commands
are provided for the owner and have not been executed by the assistant. If further
local edits are made, review those hunks too; explicit file selection does not
isolate unrelated changes within a selected file.

```powershell
Set-Location C:\Projects\SubotinMotors
git branch --show-current
git status --short
git diff --check
git diff

# Must show feature/david.subotin_identity-boundary before proceeding.
$s4bPaths = @(
  'README.md',
  'back/README.md',
  'back/src/main/java/lithan/autostrada/auctions/config/CustomUserDetails.java',
  'back/src/main/java/lithan/autostrada/auctions/config/ReactFrontendRedirectConfig.java',
  'back/src/main/java/lithan/autostrada/auctions/controller/AdminController.java',
  'back/src/main/java/lithan/autostrada/auctions/controller/HomeController.java',
  'back/src/main/java/lithan/autostrada/auctions/controller/PartsStoreController.java',
  'back/src/main/java/lithan/autostrada/auctions/controller/UserCarController.java',
  'back/src/main/java/lithan/autostrada/auctions/controller/api/AdminApiController.java',
  'back/src/main/java/lithan/autostrada/auctions/controller/api/ApiModelMapper.java',
  'back/src/main/java/lithan/autostrada/auctions/controller/api/MarketplaceApiController.java',
  'back/src/main/java/lithan/autostrada/auctions/controller/api/SessionApiController.java',
  'back/src/main/java/lithan/autostrada/auctions/controller/api/StoreAdminApiController.java',
  'back/src/main/java/lithan/autostrada/auctions/controller/api/StoreApiController.java',
  'back/src/main/java/lithan/autostrada/auctions/controller/api/UserWorkspaceApiController.java',
  'back/src/main/java/lithan/autostrada/auctions/entity/AuctionFollow.java',
  'back/src/main/java/lithan/autostrada/auctions/entity/AuctionNotification.java',
  'back/src/main/java/lithan/autostrada/auctions/entity/Car.java',
  'back/src/main/java/lithan/autostrada/auctions/entity/CarBidding.java',
  'back/src/main/java/lithan/autostrada/auctions/entity/CarListing.java',
  'back/src/main/java/lithan/autostrada/auctions/entity/CartItem.java',
  'back/src/main/java/lithan/autostrada/auctions/entity/ListingComment.java',
  'back/src/main/java/lithan/autostrada/auctions/entity/ListingDeposit.java',
  'back/src/main/java/lithan/autostrada/auctions/entity/ListingTestRide.java',
  'back/src/main/java/lithan/autostrada/auctions/entity/PaymentAccount.java',
  'back/src/main/java/lithan/autostrada/auctions/entity/PaymentOrder.java',
  'back/src/main/java/lithan/autostrada/auctions/entity/StoreOrder.java',
  'back/src/main/java/lithan/autostrada/auctions/entity/TestDrive.java',
  'back/src/main/java/lithan/autostrada/auctions/entity/UserAccount.java',
  'back/src/main/java/lithan/autostrada/auctions/identity/CheckoutProfile.java',
  'back/src/main/java/lithan/autostrada/auctions/identity/CheckoutProfileClient.java',
  'back/src/main/java/lithan/autostrada/auctions/identity/CurrentIdentity.java',
  'back/src/main/java/lithan/autostrada/auctions/identity/IdentityApiMapper.java',
  'back/src/main/java/lithan/autostrada/auctions/identity/InProcessProfileClient.java',
  'back/src/main/java/lithan/autostrada/auctions/identity/ProfileClient.java',
  'back/src/main/java/lithan/autostrada/auctions/identity/PublicProfile.java',
  'back/src/main/java/lithan/autostrada/auctions/identity/SelfProfileService.java',
  'back/src/main/java/lithan/autostrada/auctions/identity/SessionCurrentIdentity.java',
  'back/src/main/java/lithan/autostrada/auctions/payment/DisabledStripeGateway.java',
  'back/src/main/java/lithan/autostrada/auctions/payment/StripeConnectGateway.java',
  'back/src/main/java/lithan/autostrada/auctions/payment/StripeGateway.java',
  'back/src/main/java/lithan/autostrada/auctions/repository/AuctionFollowRepository.java',
  'back/src/main/java/lithan/autostrada/auctions/repository/AuctionNotificationRepository.java',
  'back/src/main/java/lithan/autostrada/auctions/repository/CarBiddingRepository.java',
  'back/src/main/java/lithan/autostrada/auctions/repository/CarListingRepository.java',
  'back/src/main/java/lithan/autostrada/auctions/repository/CarRepository.java',
  'back/src/main/java/lithan/autostrada/auctions/repository/CartItemRepository.java',
  'back/src/main/java/lithan/autostrada/auctions/repository/ListingCommentRepository.java',
  'back/src/main/java/lithan/autostrada/auctions/repository/ListingDepositRepository.java',
  'back/src/main/java/lithan/autostrada/auctions/repository/ListingTestRideRepository.java',
  'back/src/main/java/lithan/autostrada/auctions/repository/PaymentAccountRepository.java',
  'back/src/main/java/lithan/autostrada/auctions/repository/PaymentOrderRepository.java',
  'back/src/main/java/lithan/autostrada/auctions/repository/StoreOrderRepository.java',
  'back/src/main/java/lithan/autostrada/auctions/repository/TestDriveRepository.java',
  'back/src/main/java/lithan/autostrada/auctions/service/AdminService.java',
  'back/src/main/java/lithan/autostrada/auctions/service/AdminServiceImpl.java',
  'back/src/main/java/lithan/autostrada/auctions/service/AuctionFollowService.java',
  'back/src/main/java/lithan/autostrada/auctions/service/AuctionNotificationService.java',
  'back/src/main/java/lithan/autostrada/auctions/service/CarListingServiceImpl.java',
  'back/src/main/java/lithan/autostrada/auctions/service/CartServiceImpl.java',
  'back/src/main/java/lithan/autostrada/auctions/service/ListingCommentServiceImpl.java',
  'back/src/main/java/lithan/autostrada/auctions/service/ListingDepositServiceImpl.java',
  'back/src/main/java/lithan/autostrada/auctions/service/MarketplaceAdminService.java',
  'back/src/main/java/lithan/autostrada/auctions/service/PaymentServiceImpl.java',
  'back/src/main/java/lithan/autostrada/auctions/service/StoreOrderServiceImpl.java',
  'back/src/main/java/lithan/autostrada/auctions/service/UserCarServiceImpl.java',
  'back/src/main/java/lithan/autostrada/auctions/service/UserServiceImpl.java',
  'back/src/test/java/e2e/SimulatedStripeGateway.java',
  'back/src/test/java/lithan/autostrada/auctions/AccountAuctionFeatureIntegrationTests.java',
  'back/src/test/java/lithan/autostrada/auctions/AutostradaAuctionsApplicationTests.java',
  'back/src/test/java/lithan/autostrada/auctions/CarListingAndAddressIntegrationTests.java',
  'back/src/test/java/lithan/autostrada/auctions/CheckoutRedirectIntegrationTests.java',
  'back/src/test/java/lithan/autostrada/auctions/DemoSeedDataIntegrationTests.java',
  'back/src/test/java/lithan/autostrada/auctions/IdentityBoundaryArchitectureTests.java',
  'back/src/test/java/lithan/autostrada/auctions/IdentityBoundaryIntegrationTests.java',
  'back/src/test/java/lithan/autostrada/auctions/LegacyDetailRedirectTests.java',
  'back/src/test/java/lithan/autostrada/auctions/ListingCommentIntegrationTests.java',
  'back/src/test/java/lithan/autostrada/auctions/ListingRouteSmokeTests.java',
  'back/src/test/java/lithan/autostrada/auctions/MarketplaceFeatureIntegrationTests.java',
  'back/src/test/java/lithan/autostrada/auctions/PaymentWorkflowIntegrationTests.java',
  'back/src/test/java/lithan/autostrada/auctions/ReactApiSmokeTests.java',
  'back/src/test/java/lithan/autostrada/auctions/ReactApiValidationIntegrationTests.java',
  'back/src/test/java/lithan/autostrada/auctions/StoreAdminOrderIntegrationTests.java',
  'back/src/test/java/lithan/autostrada/auctions/StoreWorkflowIntegrationTests.java',
  'back/src/test/java/lithan/autostrada/auctions/TestIdentity.java',
  'back/src/test/java/lithan/autostrada/auctions/UserCarSecurityIntegrationTests.java',
  'back/src/test/java/lithan/autostrada/auctions/VehicleGalleryIntegrationTests.java',
  'back/src/test/java/lithan/autostrada/auctions/WithIdentity.java',
  'back/src/test/java/lithan/autostrada/auctions/WithIdentityFactory.java',
  'docs/browser-e2e.md',
  'docs/docker-compose.md',
  'docs/identity-boundary-pr.md',
  'docs/identity-boundary.md',
  'docs/iroit-api-events.md',
  'docs/iroit-architecture.md',
  'docs/iroit-baseline.md',
  'docs/iroit-migration-plan.md',
  'docs/iroit-security.md',
  'docs/iroit-service-ownership.md',
  'docs/session-csrf-pr.md',
  'front/README.md',
  'front/e2e/compose.mjs',
  'front/e2e/specs/identity-boundary.spec.js'
)
git add -- $s4bPaths
git diff --cached --check
git diff --cached --stat
git diff --cached
git commit -m "feat: prepare in-process identity boundary with scalar account references"
git push -u origin feature/david.subotin_identity-boundary
```

Create the PR yourself with base `master`, using the suggested title/body above.
Do not merge until applicable local/CI gates have actual successful evidence.
