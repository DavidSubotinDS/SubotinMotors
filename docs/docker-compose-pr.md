# S3 evidence and owner handoff

Historical handoff: S3 subsequently merged as `8d13104` via PR #20 at
2026-09-20T10:15:21Z. Both exact merged-commit required checks passed in
[run 35504550026](https://github.com/DavidSubotinDS/SubotinMotors/actions/runs/35504550026),
verified during S4a. Do not repeat owner commands below. Current work is
[S4a](session-csrf-pr.md); the dated pre-merge and Docker incident record is retained.

Branch: `feature/david.subotin_docker-compose-baseline`. Default branch remains
`master`. The branch already existed with a clean tree at freshly fetched master,
so it was reused without resetting/recreating it. No commits, pushes, PRs, merges
or tags were performed. Other branches and ignored developer databases were preserved.

## Verified prerequisite

On 2026-09-19 (Europe/Budapest), fetched master was
`a08cb14d781464cc8a7008f7cb837e896d56f48e`. GitHub's public API confirms
[S2 PR #19](https://github.com/DavidSubotinDS/SubotinMotors/pull/19) merged at
2026-09-18T21:57:21Z with that SHA. Its tree equals branch `db53120`.
[Master CI 35399306269](https://github.com/DavidSubotinDS/SubotinMotors/actions/runs/35399306269)
passed [Backend](https://github.com/DavidSubotinDS/SubotinMotors/actions/runs/35399306269/job/105775300195)
and [Frontend](https://github.com/DavidSubotinDS/SubotinMotors/actions/runs/35399306269/job/105775299990).
Job steps independently confirm gateway tests/image build/no-upstream container
smoke and browser-through-gateway tests/cleanup succeeded. This is merged S2
evidence, not a claim of S3 remote CI success. Required branch protection remains
owner-reported. CLI was unauthenticated; public REST supplied the evidence.
Remote default/HEAD is master; local/remote tag lists were empty.

All six shared architecture documents, gateway/browser runbooks/handoffs,
backend/frontend READMEs, CI, current gateway implementation, browser harness,
MySQL profile and V1-V18 migration history were reviewed. No applicable AGENTS.md
was found in the repository or ancestors. Initial sandbox access to Git/Docker
was restricted; authorized host checks then confirmed Docker Desktop 4.45.0,
Linux Engine 28.3.3 and Compose 2.39.2. The S2 local engine limitation no longer applies.

## Changes and boundaries

- `compose.yaml`, `.env.compose.example`, `compose.diagnostics.yaml`: one gateway
  origin; private frontend/backend/MySQL ports; internal edge/database networks;
  named MySQL storage; SQL/service health dependencies and bounded startup.
- `back/Dockerfile`, `.dockerignore`, entrypoint and container profile: independent
  tested production JAR, non-root/read-only runtime, explicit demo acknowledgement,
  runtime credentials, liveness/readiness and coherent cookie/forwarding settings.
  The MySQL dialect uses the current `MySQLDialect`; the three exact health URLs
  are permitted by backend security. No session/CSRF business logic changed.
- `front/Dockerfile`, `.dockerignore`, `nginx.conf`: locked dependency installation,
  same-origin production bundle, non-root static server, page fallback and no
  fallback for API/private/webhook or missing-asset paths. Gateway container reused;
  all base images and MySQL are pinned by digest.
- `front/e2e/compose*.mjs`, configurable fixtures/reports, test-only Java launcher:
  production initialization/restart/restore first, then isolated MySQL browser
  scenarios. Random projects, credentials and ports; schema-name and token guards;
  no normal database reset. Native H2 remains supported. Login helper now waits
  for the app's existing explicit reload to avoid interrupted navigation.
- CI preserves **Backend** and **Frontend** names. Backend runs container/MySQL
  integration; Frontend runs native plus Compose browser tests. Failures fail
  these checks. Logs/reports and cancellation-fallback cleanup use `always()`.
- Architecture status, root/component READMEs and [runbook](docker-compose.md)
  describe implemented behavior, configuration, deliberate seed acceptance and
  backup/recovery separately from proposed S4+ architecture.

Flyway V1-V18 are unchanged. They already embed public demo password hashes,
data and fake provider history; startup requires an explicit acknowledgement.
Real DB/Stripe/SMTP credentials are runtime-only, never build args or frontend
configuration. Default log-mail can contain reset links; logs/backups stay private.
MySQL app grants are schema-only; the backend uses that owner credential for both
Flyway and application SQL. Sessions still live in backend memory. Backend restart
requires login again. No broker, extracted service, payment redesign, dependency
remediation or deployment/CD was added.

## Local verification (2026-09-19; handoff review 2026-09-20)

| Gate | Actual result |
| --- | --- |
| Production backend clean verify inside Linux image build | 105 tests, no failures/errors/skips; JAR packaged; build rejects test launcher/config/library entries |
| Independent gateway clean verify/image | 17 tests passed and executable/image built; runtime non-root verified |
| Frontend tests and build | 12 tests in 6 files passed on Windows; native and Linux image production builds passed |
| Compose config / production startup | Validated; three application images built; MySQL 8.4.8 initialized; private ports and non-root Java/Nginx verified |
| Real MySQL migration/constraint integration | All 18 successful Flyway versions/checksums; Hibernate validation; schema-only grants; invalid FK, duplicate user and negative stock rejected; competing row update hit MySQL 1205 lock timeout |
| Preserved-volume restart | `down` without volumes, then `up --wait`; IDs, account hashes, profile sentinel, gallery image hashes and migration checksums unchanged |
| Backup/restore | Logical dump restored into the same isolated, recreated schema while writers stopped; data parity and fresh Flyway/Hibernate startup passed |
| Outage recovery | Gateway liveness remained UP, readiness returned 503 during MySQL outage; recovery passed without data reset |
| Configuration/security smoke | Production JSESSIONID attributes; same-origin behavior and unapproved-origin denial; forged forwarding cannot alter redirect origin; generated reset link uses public gateway origin and validates |
| Browser through Compose/MySQL | Final 2026-09-19 run: 12/12 scenarios passed, no skips, 1.2 minutes; real backend and signed synthetic webhook handling |
| Native browser through gateway/H2 | Final rerun 12/12 passed in 44.1 seconds; all three native ports free afterward |
| Failure diagnostics | Initial build failure and corrected same-origin CORS assertion failure retained logs and cleaned all owned containers/networks/volumes; native navigation failure retained trace/video/screenshots before the helper fix |
| Intentional Compose browser failure | One deliberate failure produced screenshot/video/trace and JUnit/HTML reports; command failed, and all owned containers/networks/volumes were verified absent afterward. Backend process liveness/DB readiness were also checked during this run |
| Demo acknowledgement | Production entrypoint refused startup without the explicit acknowledgement before starting Java; normal acknowledged initialization passed |
| Static checks | Workflow and both Compose YAML files parsed; required names preserved; Compose diagnostic override validated; Node syntax, changed/new-file whitespace, local Markdown links and backup UTF-8 checked |
| S3 remote CI | Not run: owner has not committed/pushed S3. S2 merged-master success is prerequisite evidence only |
| Real Stripe sandbox / deployed-data migration | Not run. Provider simulation is not a real Stripe session or delivered webhook; no developer/deployed database was imported |

Primary ignored local evidence:
`compose-results/autostrada-test-f47d8c69ed867126b321eec8a5e90dc6/` (final full run including seed guard, configuration checks and 12 browser scenarios),
`compose-results/autostrada-test-2c412b3b5eed13a9ceed94e8b6221000/` (production/MySQL),
`compose-results/autostrada-test-d1b7a5a0bb642eaf093ad3aa8ab9e3f4/` (12 Compose browser scenarios),
`compose-results/autostrada-test-e1de6ba122e86348ba96a5cdb6a0ec47/` (configuration checks and intentional browser failure),
`front/e2e-results/playwright.xml` (final native run), and
`front/e2e-results/s3-native-first-failure/` (preserved failure evidence).
Each Compose directory records its project, checksums, evidence and logs; browser
reports are separated by run. Local fake-data backups and control overrides are
excluded from CI uploads. Container build cache/base image layers can remain;
owned runtime containers, volumes, networks and service-image tags are cleaned.

Follow-up on 2026-09-20: the final retained Compose JUnit report confirms 12 tests,
zero failures/errors/skips, 70.93 seconds, with completed cleanup evidence. Native
JUnit confirms 12 tests and zero failures/errors/skips. The harness command runner
was then hardened to decode UTF-8 across chunks (SQL backups) and observe early
lock-holder failures; its actual command runner passed a split-codepoint output
and child-exit check, with Node syntax checks. No application/image configuration
changed after the successful full run. Docker's Linux engine was initially absent
today; a Desktop start attempt and engine-info query then stalled. Only those two
owned CLI clients were stopped; Desktop/containers were not killed. These two
harness-only safeguards have not received another full container run.
Re-run `npm run test:compose` once the engine
is available; the prior runtime evidence is retained, not represented as a new run.

Applicable common gates cover existing functionality/security contracts, independent
builds, container startup, real MySQL clean install/restored copy, constraint/lock
checks, browser regression, health/correlation, recovery and documentation.
This is not evidence of a real deployed-database upgrade or a distributed business
service. Remote required CI must pass on the owner's eventual commit and merged
master. Real Stripe sandbox smoke remains separate; other browsers, TLS deployment,
metrics/tracing dashboards, package remediation and original course-rubric audit
remain unverified/later stages.

## Recovery and next stage

Use [the backup/restore commands](docker-compose.md#backup-and-restore) before
changing image/schema configuration. `docker compose --env-file .env.compose -p
autostrada down` preserves the normal named volume; `up -d --wait --wait-timeout
240` reuses it. Restore into a new project first and verify parity. Never delete
the normal DB or prune Docker resources to repair startup. Revert only to an
image/config compatible with the current schema, or restore a verified backup
under a write freeze and reconcile external payment state. A source tag cannot
reverse provider operations.

After S3 review/merge and successful merged-master checks, next is **S4a** on
`feature/david.subotin_session-csrf-foundation`: coordinated browser CSRF tokens,
session rotation and negative security/browser tests. Then S4b establishes the
identity boundary. Service extraction starts later; RabbitMQ waits for S6.

## Owner commands

Review before staging. No blanket `git add .`; the list below excludes ignored
credentials, test artifacts and normal data. Keep the default branch master.

```powershell
Set-Location C:\Projects\SubotinMotors
git branch --show-current
# Must be feature/david.subotin_docker-compose-baseline
git status --short --branch
git diff --check
git add -- .gitattributes .gitignore .env.compose.example compose.yaml compose.diagnostics.yaml .github/workflows/ci.yml README.md back/README.md back/Dockerfile back/.dockerignore back/docker-entrypoint.sh back/src/main/resources/application-container.properties back/src/main/resources/application-mysql.properties back/src/main/java/lithan/autostrada/auctions/config/SecurityConfig.java back/src/test/java/e2e/ComposeE2eApplication.java back/src/test/java/e2e/E2eApplication.java back/src/test/java/e2e/SimulatedStripeGateway.java front/README.md front/Dockerfile front/.dockerignore front/nginx.conf front/package.json front/playwright.config.js front/e2e/compose.mjs front/e2e/compose-cleanup.mjs front/e2e/run.mjs front/e2e/fixtures.js front/e2e/specs/marketplace.spec.js front/e2e/specs/payments.spec.js gateway/Dockerfile gateway/README.md docs/docker-compose.md docs/docker-compose-pr.md docs/browser-e2e.md docs/iroit-architecture.md docs/iroit-baseline.md docs/iroit-service-ownership.md docs/iroit-api-events.md docs/iroit-security.md docs/iroit-migration-plan.md
git add -- docs/api-gateway-pr.md docs/browser-e2e-pr.md
git diff --cached --check
git diff --cached --stat
git diff --cached
git commit -m "feat: add reproducible Compose and MySQL baseline"
git push -u origin feature/david.subotin_docker-compose-baseline
```

Suggested PR title: **feat: add reproducible Compose and MySQL baseline**.
For authenticated GitHub CLI (authenticate yourself if necessary):

```powershell
$s3PrBody = @'
Containerize the existing backend and frontend assets alongside the gateway and MySQL. Serve the application through one gateway origin with private upstream/database ports, persistent named storage, health checks, non-root application runtimes and runtime configuration. Preserve backend session/CSRF/payment ownership and unchanged Flyway history; deliberately acknowledge existing demo seeds.

Add uniquely isolated Compose/MySQL initialization, constraints/locking, preserved-volume restart and backup/restore checks, followed by browser regression through the gateway. Keep test controls out of the production artifact. Preserve Backend and Frontend CI names, gate them on the new checks, and always retain diagnostics and clean owned resources. Document startup, secrets, coherent reset/Stripe URLs, recovery and S4 handoff.

Local evidence: 105 backend tests, 17 gateway tests, 12 frontend tests, successful production image builds, all 18 MySQL migrations, constraint/row-lock checks, preserved-volume restart and logical restore, and 12 browser scenarios in each Compose/MySQL and native/H2 topology. See docs/docker-compose-pr.md for detailed evidence and failure-cleanup verification. Remote S3 CI and real Stripe sandbox smoke remain unverified until owner execution. No service extraction, RabbitMQ, auth/payment redesign or CD.
'@
$s3PrBodyPath = Join-Path ([IO.Path]::GetTempPath()) 'subotinmotors-s3-pr.md'
Set-Content -LiteralPath $s3PrBodyPath -Value $s3PrBody -Encoding utf8
gh pr create --repo DavidSubotinDS/SubotinMotors --base master --head feature/david.subotin_docker-compose-baseline --title "feat: add reproducible Compose and MySQL baseline" --body-file $s3PrBodyPath
```

After reviewing the diff and successful **Backend**/**Frontend** checks, the owner
may merge through GitHub, then fetch master and verify its merged-commit checks.
Do not infer those future actions or a baseline tag from this handoff.
