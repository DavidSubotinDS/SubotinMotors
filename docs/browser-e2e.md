# Browser regression baseline (IROIT S1 + S2)

S6 working-tree update: notification inbox and mail delivery now have a separate owner,
RabbitMQ and guarded data cutover. Read the [S6 runbook](notification-service.md) before starting
the new Compose stack; historical S5 startup/test instructions below need that cutover.


S4a extends the shared suite to 18 scenarios in native gateway/H2 and isolated
Compose/MySQL. Six new scenarios cover API/form session rotation and old-ID/token
rejection, missing/invalid/foreign JSON/form/multipart tokens, stale checkout
without replay, expired sessions, and browser password reset through a private
test mailbox. Existing direct API setup/authorization probes now obtain valid
tokens so role/ownership checks are still reached. [Contract](session-csrf.md)
and [actual results](session-csrf-pr.md) supersede historical S1/S2 limits below.

S3 adds `npm run test:compose` in `front/`: the same scenarios run through the
container gateway/Nginx/backend against a unique MySQL schema/volume. URLs and
report destinations come only from the harness; test controls require its token
and database guard. Native `test:e2e` retains disposable H2. See [Compose runbook](docker-compose.md)
and [current evidence](docker-compose-pr.md); historical MySQL/Compose gaps below
refer to S1/S2. Real Stripe sandbox smoke remains separate and unverified.

This suite runs React and the existing Spring monolith through the S2 gateway.
It introduces no business service extraction, database migration, or authentication redesign. Read it alongside
[S1 and the common gates](iroit-migration-plan.md), [baseline](iroit-baseline.md),
[architecture](iroit-architecture.md), [ownership](iroit-service-ownership.md),
[API/events](iroit-api-events.md), and [security](iroit-security.md).

## Run locally

Prerequisites: JDK 17 (`JAVA_HOME`), Node.js 22, and internet access for the first
Maven/npm/browser install. From a fresh checkout on Windows PowerShell:

```powershell
Set-Location C:\Projects\SubotinMotors\front
npm.cmd ci
npm.cmd run test:e2e:install
npm.cmd run test:e2e
```

On Linux/macOS use `npm` instead of `npm.cmd`; on Linux install the browser and
its OS libraries with `npx playwright install --with-deps chromium`. After these
one-time dependencies, `npm run test:e2e` compiles the test launcher, builds React,
builds the independent gateway, starts all three servers, waits for gateway readiness,
runs Chromium through the gateway, and stops its processes.
Do not run a backend `clean` build concurrently with the launcher.

Useful variations (all use the same isolated startup):

```powershell
npm.cmd run test:e2e -- --grep "deadline"
npm.cmd run test:e2e -- --headed
npm.cmd run test:e2e:report
node e2e/assert-stopped.mjs
# Deliberately exits 1 after all three servers are ready, to test failure cleanup:
npm.cmd run test:e2e -- --verify-failure-cleanup
node e2e/assert-stopped.mjs
```

Do not use bare `playwright test` or increase workers: the configuration requires
the launcher's per-run token and intentionally uses one worker. It never attaches
to an existing backend. Ports **18080** (backend), **15173** (React preview), and
**18081** (public gateway)
must be free; a conflict fails before startup without stopping the existing app.
All servers bind to `127.0.0.1`. Interrupt with Ctrl+C for cleanup. A hard OS kill
cannot run a JavaScript finally block; a CI runner teardown then removes its
processes. No persistent application database needs cleaning.

## Isolation and fixtures

- Browser base URL, application API requests, legacy navigation, provider return
  links and signed webhook posts all use `http://127.0.0.1:18081`. React preview
  has **no API proxy**. Fixtures assert gateway `X-Request-ID` and fail if browser
  requests bypass the gateway to a private backend/frontend port. Only harness
  reset/clock/readiness calls use backend port 18080 and a random control token.
  The same control paths through the gateway return 404, even with a token.

- `back/src/test/java/e2e/E2eApplication.java` is outside the production component
  scan and exists only on the test classpath. Neither its control endpoints nor
  the provider simulator is included in the production JAR. Normal tests and
  normal `spring-boot:run` do not import it.
- The launcher ignores inherited Spring/Stripe/mail/Vite environment configuration
  and frontend `.env` files. The Java entry point sets a unique
  `jdbc:h2:mem:e2e_<UUID>` URL for both JPA and Flyway with command-line priority;
  it loads only `application-e2e.properties`. It cannot select file H2 or MySQL.
  Normal `back/data/` is never read, reset, or deleted.
- Flyway V1–V18 installs and validates the real schema. Before **each** test,
  including any explicitly requested repeat/retry, the control resets business
  tables and identities, preserving Flyway history. It checks JDBC metadata for
  the exact memory-database prefix before executing reset statements. This is an
  H2 reset, not a migration or MySQL compatibility test.
- `/__e2e/reset`, `/__e2e/clock`, and `/__e2e/ready` require a random 256-bit
  per-run header token. Ordinary application routes retain their production
  Spring Security filter chain. The test control chain applies only to
  `/__e2e/**`; the browser never receives a blanket authentication bypass.
- Every test gets a new browser context. Four accounts (`buyer`, `seller`,
  `other`, `admin`, password `E2e-pass-123!`) have deliberately distinct ownership;
  only `admin` has ADMIN as well as USER. Profiles initially have no shipping
  address. Fixtures contain two auctions, one listing, and one part with stock 10
  and price EUR 25. No scenario selects a mutable demo record.
- Auction business time starts at `2030-06-15T10:00:00Z`. Tests coordinate the
  Spring `Clock` and browser `Date` in UTC, including one second before, exactly
  at, and one second after the deadline. Bidding, auction creation/edit validation,
  appointment validation, API status mapping, follows and notifications use the
  existing clock bean. Production still uses `Clock.systemDefaultZone()`.
  Audit/payment timestamps and Stripe signature tolerance use real wall time;
  tests assert invariants rather than exact timestamps. This is not a universal
  replacement of every legacy/entity clock read.
- Browser interactions use accessible labels/roles and awaited responses or
  retrying assertions. There are no business-flow sleeps. The launcher's bounded
  readiness polls are the only polling delay outside Playwright assertions.
  See [Playwright clocks](https://playwright.dev/docs/clock) and
  [shared browser/API cookies](https://playwright.dev/docs/api/class-apirequestcontext).

## Coverage

| Scenario | Browser evidence and backend assertions |
| --- | --- |
| Authentication | Registration, wrong password, login, profile navigation/reload, session DTO, HttpOnly cookie, logout, old-cookie replay rejected, anonymous 401 |
| Roles and ownership | USER admin-page/API denial, another seller's auction/listing read and mutation denied, ADMIN access; cross-user cart/order/deposit/appointment/notification denial |
| Auctions | Multipart image creation, edit, pending approval, admin approval, visible detail image; self-bid, invalid minimum, accepted bid before deadline, rejection at/after deadline and unchanged bid count |
| Fixed-price listings | Multipart create, edit, public detail; reservation behavior in deposit scenarios |
| Appointments | Both auction test drive and listing test ride requested in browser; another user cannot accept/cancel; seller accepts, requester cancels |
| Notifications | Follow two ending-soon auctions, watchlist, two unread notifications, cross-recipient denial, read-one/read-all and reload persistence |
| Shipping/cart/orders | Missing address blocks checkout in UI/API, profile address save/reload, add/merge two cart units, checkout consumes cart and reserves stock, totals and item snapshots, order details and shipping snapshot survives later profile edit |
| Store payment | Return before payment remains CHECKOUT_CREATED, invalid signature and modified raw body rejected, signed paid result, identical-event replay, paid browser confirmation; cancel and unpaid completion never pay; signed expiry releases stock once and displays EXPIRED |
| Deposits | Self-deposit and second buyer rejected, return remains unpaid, owner-only lookup, signed success confirms deposit but listing stays RESERVED, cancellation remains pending until signed expiry releases listing |
| Admin orders | Admin list-to-detail navigation, SKU/name/quantity/unit and total values, shipping snapshot and paid status |
| Legacy compatibility | Backend search query handoff, catalog and account aliases, retired payment/onboarding redirects, actual store/deposit success session query handoff, React detail aliases; current detail-ID loss recorded below |

The suite supplements the existing backend and frontend tests; it does not
duplicate every CRUD permutation or claim full route/security coverage.

## Three distinct kinds of payment evidence

1. **CI provider simulation:** `SimulatedStripeGateway` substitutes only outbound
   checkout creation, returning a deterministic session ID and a local URL. The
   browser intercepts only `/__provider/checkout` to display two return links.
   No application API responses, session responses, totals or order state are
   mocked. It makes no Stripe network call and uses no account credentials.
2. **Signed fixture over real HTTP:** Node signs synthetic Stripe-shaped JSON
   using the public, test-only `whsec_e2e_public_fixture_secret`. It posts the
   original bytes to the real `/webhooks/stripe` controller. The simulator delegates
   inbound parsing to production `StripeConnectGateway`, which invokes the Stripe
   SDK's signature/timestamp verification and deserializer. Production order/
   deposit handlers and receipt deduplication persist the result. Tampered bytes
   and invalid signatures are rejected. This adds signed HTTP evidence beyond
   backend tests that directly pass `StripeWebhookEvent`/mock the gateway, but is
   **not a real Stripe-delivered webhook or live Checkout test**.
3. **Manual sandbox smoke:** the procedure below exercises actual provider
   creation, hosted Checkout, account configuration and Stripe CLI delivery. It is
   separate and requires the owner's sandbox credentials. Never inject these
   credentials into the ordinary CI suite. Stripe documents
   [signature verification](https://docs.stripe.com/webhooks/signature).

## Existing limitations and stage boundaries

- Chromium desktop is the initial browser. Firefox/WebKit, mobile layouts, MySQL,
  Compose, real provider outages and race/load tests are not covered here.
  S2 adds gateway forwarding, stub transport/outage tests and native topology;
  S3 adds the container/MySQL baseline.
- S4a protects APIs/forms with session-backed CSRF and rotates session IDs on
  API/form login. Future internal-token security remains proposed. See the S4a
  handoff for actual positive/negative evidence and remaining gates.
- S2 resolves the selected-item gap: `/car-listings/1` ends at `/listings/1`,
  `/store/parts/1` at `/parts/1`, and legacy auction URLs retain `/auctions/{id}`.
  Tests now assert the chosen detail and reload behavior. The backend resolver
  preserves supported detail/edit model IDs without leaking other attributes.
  Legacy admin account editing still uses the existing users screen; not every
  legacy form UI is reproduced. See the gateway route catalogue for precedence.
- Catalog query strings survive the legacy handoff, but current React catalog
  filters initialize from component state rather than reading those URL queries.
  This baseline verifies query transport, not restored filter selection.
- Current unpaid/processing checkout stays `CHECKOUT_CREATED`; there is no
  distinct PROCESSING backend state. The success view shows awaiting confirmation
  and refreshes only on reload. Cancel query flags have no dedicated message;
  they do not release a stock/listing reservation. A signed expiry/failure does.
  Orders/deposits display EXPIRED, but an expired success URL still uses the generic
  pending heading. These are current UX limitations, not the proposed S7 state
  machine. No pending-state redesign is included in this baseline.
- Actual SMTP delivery, all image-gallery permutations, every legacy form POST,
  and all provider event reorder/crash cases remain covered only where existing
  backend/component tests exercise them, or remain follow-up gaps. Scheduler
  delivery is disabled here; notification creation uses the real follow-inside-
  ending-window path.
- Dependency vulnerability remediation remains separate. The Playwright install
  reported the existing seven npm advisories; no audit fix was applied.

## CI and recovery

The existing **Backend** check runs backend and independent gateway `clean verify`,
gateway image build and no-upstream container smoke. The existing
**Frontend** check runs `npm ci`, frontend tests, production build, browser
installation and this E2E command. An E2E failure therefore fails the already
required Frontend check; no optional new check needs to be added to protection.
Required protection remains owner-reported, not independently inspected here.

The orchestrator stops only child processes it started in `finally` and on
SIGINT/SIGTERM, including after failed tests/startup. Windows uses the owned PID
tree; POSIX uses the owned process group. An `always()` CI step verifies the two
ports plus the gateway port are free. Another always step retains HTML/JUnit results, failed-test traces,
screenshots/videos, and build/server logs for seven days. Artifacts use only fake
fixture users/data, but may contain the short-lived local session/control tokens.

Recovery requires removing/disabling only the new test orchestration if needed.
There is no production data rollback or migration. Retain isolation, existing
Backend/Frontend checks, and documented gaps. Never clean `back/data` to repair
an E2E failure.

## Separate manual Stripe sandbox smoke (owner-run)

Run this in fresh PowerShell terminals. Use an authenticated Stripe CLI sandbox
profile. The existing helper handles key lookup/listener cleanup; do not paste
keys into Git, the test fixtures, screenshots, or this document.

Backend terminal, explicitly using a disposable memory database for the smoke:

```powershell
Set-Location C:\Projects\SubotinMotors\back
$smokeDb = 'jdbc:h2:mem:stripe_smoke_' + [guid]::NewGuid().ToString('N') + ';MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1'
$env:SPRING_PROFILES_ACTIVE = 'gateway'
$env:SPRING_DATASOURCE_URL = $smokeDb
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME = 'org.h2.Driver'
$env:SPRING_DATASOURCE_USERNAME = 'sa'
$env:SPRING_DATASOURCE_PASSWORD = ''
$env:SPRING_FLYWAY_URL = $smokeDb
$env:SPRING_FLYWAY_USER = 'sa'
$env:SPRING_FLYWAY_PASSWORD = ''
$env:APP_FRONTEND_BASE_URL = 'http://localhost:18084'
$env:APP_CORS_ALLOWED_ORIGINS = 'http://localhost:18084'
.\scripts\run-stripe-sandbox.ps1 -Port 18083 -PublicBaseUrl http://localhost:18084 -UseCliLogin
```

Frontend terminal:

```powershell
Set-Location C:\Projects\SubotinMotors\front
$env:VITE_API_BASE_URL = ''
npm.cmd run build
npm.cmd run preview -- --host 127.0.0.1 --port 15174 --strictPort
```

Gateway terminal (the helper's Stripe listener delivers to this gateway, not directly to backend):

```powershell
Set-Location C:\Projects\SubotinMotors\gateway
.\mvnw.cmd --batch-mode --no-transfer-progress package
& "$env:JAVA_HOME/bin/java.exe" -jar target/gateway-0.0.1-SNAPSHOT.jar --server.port=18084 --gateway.public-url=http://localhost:18084 --gateway.backend-url=http://127.0.0.1:18083 --gateway.frontend-url=http://127.0.0.1:15174
```

1. Open `http://localhost:18084`, register a smoke buyer and complete shipping.
   Confirm the backend log names the `stripe_smoke_` memory DB before mutations.
   Select an in-stock part, note stock, quantity, total and shipping, then checkout.
   Confirm the browser reaches actual hosted Stripe Checkout in sandbox mode.
2. Pay with Stripe's `4242 4242 4242 4242` test card, a future expiry and any
   three-digit CVC. Verify CLI delivery returns HTTP 200, then reload the return
   page until it shows confirmed payment. Verify the order and admin item details,
   amount, shipping and stock. These are Stripe's
   [documented interactive test values](https://docs.stripe.com/testing).
3. Start another checkout and cancel in hosted Checkout. Verify cancel navigation
   alone leaves the order unpaid and stock reserved. Revisit its success URL:
   it must still be unpaid. Expire the actual session in Stripe/CLI, confirm the
   signed expiry is delivered, then verify EXPIRED and restored stock.
4. Log in as a separate seller and create an active fixed-price listing with an
   image and deposit. As the buyer, pay its deposit through actual Checkout.
   Confirm signed delivery, Deposit confirmed, and RESERVED (not SOLD). Repeat
   cancel/expiry with a second listing if validating release behavior.
5. Record commit SHA, date, sandbox identity (no secret), session/event IDs,
   webhook HTTP outcomes, order/deposit states and screenshots. Stop only these
   three terminals' processes; closing the backend discards the smoke DB. Close the
   terminals to discard their environment settings. No normal database is erased.

Do not treat `stripe trigger` with an unrelated synthetic session as proof that
the application-created Checkout session settled.

## Verification record and owner handoff

Inspected on 2026-09-14: clean local master and freshly fetched origin/master at
`588b593b1687593304fe66b7f5a5e2b8ad4fcc4a`. That commit contains all six architecture
files, including `iroit-api-events.md`. Remote HEAD is master. Local/remote tag
lists were empty. Created `feature/david.subotin_browser-e2e-baseline` from that
remote SHA. No commit, push, PR, merge, baseline tag or remote CI run was created.

Local verification on Windows with Java 17 and Chromium:

| Check | Actual result |
| --- | --- |
| Playwright full suite | 12 passed, 0 failed/skipped; 31.6 seconds for tests, excluding build/startup |
| Existing frontend tests | 12 passed across 6 files |
| Frontend production build | Passed; isolated preview build also passed |
| Backend wrapper clean verify | 96 tests passed, no failures/errors/skips; JAR packaging passed |
| Cleanup after real failed browser tests | Both test ports free; failure screenshots, videos and traces generated |
| Intentional post-startup failure probe | Failed as intended, then stopped both servers; separate port check passed |
| Production JAR inspection | E2E launcher, provider simulator and test properties are absent |
| Syntax/whitespace | Node syntax checks and git diff --check passed |

The successful Playwright JUnit/HTML reports remain in ignored local output
directories. Full backend output is `front/e2e-results/backend-verification.log`.
No remote CI run was created because the owner performs the commit/push. Live
sandbox smoke, Linux GitHub Actions execution and MySQL verification remain
unverified; they are not implied by local success.

Applicable common gates: current topology starts, current builds/tests and browser
flows pass, CI gates Frontend and retains diagnostics, and recovery discards only
isolated test state. Independent business-service build/startup, new containers,
cross-service telemetry, broker contracts, data cutover/restore and MySQL migration
changes are not applicable to this stage because no such infrastructure or data
change is introduced. H2 migration startup is evidence only for H2.

Changed files are limited to the Playwright dependency/lockfile and configuration,
`front/e2e/` orchestration/fixtures/scenarios, two test-only Java launcher/provider
classes and their test properties, four backend clock-related classes
(`ApiModelMapper`, `Car`, `UserCarServiceImpl`, `CarListingServiceImpl`), the existing
CI workflow, ignore entries, three READMEs, and S1 architecture/runbook/handoff
documentation. No React production component or production security configuration
was modified. Exact staging commands and suggested commit/PR text are in
[the owner handoff](browser-e2e-pr.md).

The verification table above is historical S1 evidence. S1 subsequently merged
as `35007b5`; merged-master Backend/Frontend passed in run 34818110319.
Current S2 results, remaining gates and owner commands are in
[the gateway handoff](api-gateway-pr.md). Next after S2 is S3 Compose/MySQL.

Final S2 local verification on 2026-09-17: 12 browser scenarios passed through
gateway (30.1 s), with selected-detail reloads, oversized multipart rejection,
forged-header denial and unchanged legacy CSRF. All three test ports were free
after cleanup. Gateway clean verify passed 17 tests; frontend passed 12 tests and
its build. Backend clean verify passed 105 tests on 2026-09-14; its production
code has not changed since that run. S2 container runtime and remote CI remain
unverified; see the handoff for the Docker engine failure and existing S1 CI evidence.

## S4b additions (2026-09-21)

`identity-boundary.spec.js` adds public profile/upload/display privacy and scalar
cart/bid ownership scenarios. The suite now contains 20 scenarios. All 20 passed
through both the native gateway with disposable H2 and Compose gateway with
isolated MySQL. Docker was started after the initial unavailable-engine attempt.
Current S4b evidence and verified cleanup are in the [S4b handoff](identity-boundary-pr.md).
