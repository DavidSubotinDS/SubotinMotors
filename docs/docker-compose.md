# S3 container and MySQL runbook

S4a uses the same topology and adds [session/CSRF protection](session-csrf.md).
Deploy frontend/backend/gateway as one compatible revision; preserve MySQL data.
Current measured gates and Docker status are in [S4a handoff](session-csrf-pr.md).

The implemented topology is browser -> gateway -> frontend assets or the existing
backend -> MySQL. Backend alone owns sessions, authorization, business operations,
Flyway and payments. There is one backend instance and one schema. No business
service, broker, deployment pipeline or new authentication protocol is introduced.
See [verification and owner handoff](docker-compose-pr.md) for measured evidence.

| Component | Container port | Host access by default | Storage / readiness |
| --- | --- | --- | --- |
| Gateway | 8081 | `127.0.0.1:8081` | Existing WebFlux runtime; bounded backend/frontend probes |
| Frontend | 8080 | None | Nginx, immutable Vite assets; `/healthz` |
| Backend | 8080 | None | Production JAR; liveness is process-only, readiness includes MySQL |
| MySQL 8.4.8 | 3306 | None | Project-scoped named `mysql-data` volume; authenticated SQL healthcheck |

The internal `edge` network connects gateway/frontend/backend. The internal
`database` network connects only backend/MySQL. Backend and gateway also have an
outbound network (sandbox Stripe/SMTP require egress). No host database or source
directory is mounted. Java and Nginx run non-root, with read-only filesystems,
temporary `/tmp`, dropped capabilities and no privilege escalation. The official
MySQL entrypoint initializes ownership as root, then runs mysqld as mysql.
Compose service-health dependencies and `up --wait --wait-timeout` bound startup;
[Docker documents this distinction from merely starting a process](https://docs.docker.com/compose/how-tos/startup-order/).
Unhealthy containers are not automatically repaired or deleted.

## Start deliberately

Requires Docker with a Linux engine, Compose v2 supporting `up --wait`, and network
access to image/Maven/npm registries on the first build. JDK and Node on the host
are unnecessary for normal container startup. Build contexts use allowlists;
host `.env`, databases, build outputs and credentials cannot enter images.
The frontend has an empty build-time API base and receives no runtime secrets.
Base images (including MySQL and the reused gateway bases) are pinned by registry
digest; Maven wrappers and npm lockfile remain in use. OS package repositories
still supply curl during Java image builds; bit-for-bit image identity is not
claimed. Record built image IDs for recovery. Version/security remediation is separate.

Existing immutable Flyway V1-V18 include public demo password hashes, accounts,
listings, parts, images and payment-history fixtures. There is no seed-free mode
in that migration history. S3 does not edit applied scripts or silently delete
their rows. **This is a local demo baseline**, gated by explicit acknowledgement;
it is not a hardened public deployment. Public demo hashes in existing migrations
are fixtures, not newly embedded database/Stripe/SMTP credentials. Demo checkout
history includes old localhost URLs and fake provider IDs; create a new order or
deposit to test current return URLs. Do not try to settle seeded payment history.

From PowerShell in the repository root:

```powershell
Copy-Item .env.compose.example .env.compose
# Edit .env.compose: choose distinct MYSQL_PASSWORD and MYSQL_ROOT_PASSWORD.
# Generate values locally if desired: node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
# Deliberately set APP_DEMO_DATA_ACK=I_ACCEPT_EXISTING_DEMO_DATA.
docker compose --env-file .env.compose -p autostrada config --quiet
docker compose --env-file .env.compose -p autostrada build
docker compose --env-file .env.compose -p autostrada up -d --wait --wait-timeout 240
docker compose --env-file .env.compose -p autostrada ps
```

Open `http://localhost:8081`. `demo_bidder`, `demo_seller`, `demo_trader`,
`demo_newcomer`, and `demo_list_01` through `demo_list_24` use the existing public
demo password `demo123`; see the root README for older admin/user fixtures.
Register a fresh account for new workflows. Initialization happens once per new
volume. Changing `MYSQL_*` later does **not** rotate users in an existing volume;
perform an explicit SQL credential rotation and update runtime configuration.
The backend runtime user receives privileges on business tables only. A separate
migration user runs Flyway and the controlled identity cutover; the identity
service has its own schema and runtime user. Root credentials are supplied only
to MySQL, never to application containers. V19 removes runtime access to the
renamed identity archives after parity verification.

`.env.compose` and backups are ignored. Do not print expanded `compose config`
or container environment inspection into shared logs: they contain runtime secrets.
Use `config --quiet`. Example values are blank deliberately; supply secrets locally.
Use fresh terminals to avoid inherited environment variables overriding the env file.

## Origin, sessions and configuration

| Variable | Meaning |
| --- | --- |
| `PUBLIC_URL` | One origin for gateway forwarding, React redirects, password reset links and Stripe returns; default `http://localhost:8081` |
| `GATEWAY_PORT`, `GATEWAY_BIND_ADDRESS` | Host port and interface; default 8081 and loopback. Change PUBLIC_URL consistently |
| `MYSQL_DATABASE`, `MYSQL_USER` | New-volume schema/user, default `autostrada`; never point tests at this normal database |
| `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD` | Required, distinct local runtime secrets |
| `APP_DEMO_DATA_ACK` | Exact `I_ACCEPT_EXISTING_DEMO_DATA` acknowledgement; production entrypoint refuses any other value |
| `SESSION_COOKIE_SECURE` | False for local HTTP; true with an actual external HTTPS origin/terminator |
| `STRIPE_ENABLED`, `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET` | Disabled by default; sandbox runtime values only, no real provider needed for CI |
| `APP_MAIL_MODE`, `APP_MAIL_FROM`, `SMTP_*` | Default log mode suppresses contents/links; use SMTP for actual recovery delivery |

The gateway strips forged forwarding/identity headers and constructs forwarding
from PUBLIC_URL. The backend enables trusted forwarding only on private ingress
with profiles `mysql,gateway,container`. API CORS permits exactly PUBLIC_URL,
with credentials, using existing gateway rules. There is no wildcard and no
duplicate backend CORS. Keep one host spelling; localhost and 127.0.0.1 differ.
JSESSIONID remains host-only, Path=/, HttpOnly, SameSite=Lax; backend restart
expires in-memory sessions and requires login again. S4a requires CSRF tokens for
all unsafe API/legacy actions and rotates session ID on login. Exact signed
POST `/webhooks/stripe` alone is exempt. See [token lifecycle](session-csrf.md).

Nginx serves SPA fallback for page navigation, never for API/webhook/private
prefixes or missing assets. The gateway's explicit route catalogue still controls
public access: `/api/**` and legacy actions go to backend, only exact
`POST /webhooks/stripe` reaches the signature verifier, canonical GET/HEAD pages
reach Nginx. Existing raw webhook bytes, cookies, statuses and redirects survive.
Stripe CLI, when deliberately used, forwards to
`http://localhost:8081/webhooks/stripe`; set runtime sandbox secrets and restart
backend. The [separate sandbox smoke](browser-e2e.md#separate-manual-stripe-sandbox-smoke-owner-run)
remains required for real provider evidence. HTTP return navigation cannot settle payments.

UTC is explicit for this **new** MySQL/JVM baseline. Existing developer/deployed
LocalDateTime records have not been imported or converted; establish their source
zone before a future migration. Native backend/H2 and Vite development remain
available using the existing READMEs; container settings are opt-in profiles.

## Stop, restart and diagnose

```powershell
docker compose --env-file .env.compose -p autostrada logs --tail 150 gateway backend mysql
docker compose --env-file .env.compose -p autostrada down
# Named data volume survives down. This recreates containers using the same data:
docker compose --env-file .env.compose -p autostrada up -d --wait --wait-timeout 240
```

Do not use `down -v`, `volume prune`, `system prune` or delete `back/data` to fix
startup. Investigate Flyway's version/checksum and first SQL exception instead.
Health: `/actuator/health/liveness` checks gateway process; readiness checks both
upstreams. Backend probes are private and distinguish process from DB readiness.
Gateway/backend logs share a generated request ID and omit request bodies/queries.
Full metrics/tracing dashboards remain S12. MySQL credentials must not be shared
in diagnostics; reset links and mail contents are no longer logged.

For intentional host diagnostics only, add `-f compose.yaml -f compose.diagnostics.yaml`
to the same commands. This publishes backend 18080, frontend 15173 and MySQL 13306
on **127.0.0.1 only**. Loopback clients are trusted in this mode and can reach the
forwarding-aware backend directly. Remove the override and run `up -d` afterward.
Normal startup has no such ports.

## Backup and restore

These PowerShell commands avoid piping SQL through host text encoding. Stop
application writers first; this also pauses the in-process scheduler. Keep the
backup outside Git and protect it as account/payment data. Take a backup before
any image/schema change. Record source SHA, image IDs, schema, Flyway versions,
UTC configuration and checksum alongside it.

```powershell
New-Item -ItemType Directory -Force backups | Out-Null
docker compose --env-file .env.compose -p autostrada stop gateway backend
docker compose --env-file .env.compose -p autostrada exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysqldump -u"$MYSQL_USER" --single-transaction --no-tablespaces --set-gtid-purged=OFF --hex-blob "$MYSQL_DATABASE" > /tmp/autostrada-backup.sql'
if ($LASTEXITCODE -ne 0) { throw 'Backup failed; do not continue' }
docker compose --env-file .env.compose -p autostrada cp mysql:/tmp/autostrada-backup.sql ./backups/autostrada-backup.sql
Get-FileHash ./backups/autostrada-backup.sql -Algorithm SHA256
docker compose --env-file .env.compose -p autostrada images
git rev-parse HEAD
docker compose --env-file .env.compose -p autostrada exec -T mysql rm /tmp/autostrada-backup.sql
docker compose --env-file .env.compose -p autostrada up -d --wait --wait-timeout 240
```

Rehearse restore into a **new, unused project** with its own named volume before
touching the normal database. Here `autostrada-restore-review` must not already
exist. Copy the env file privately, change its public port/origin (e.g. 8082), and
use the same DB name/user as the dump. Keep Stripe disabled during restore.

```powershell
Copy-Item .env.compose .env.compose.restore
# Edit PUBLIC_URL=http://localhost:8082 and GATEWAY_PORT=8082 in that file.
docker compose --env-file .env.compose.restore -p autostrada-restore-review up -d --wait --wait-timeout 180 mysql
docker compose --env-file .env.compose.restore -p autostrada-restore-review cp ./backups/autostrada-backup.sql mysql:/tmp/restore.sql
docker compose --env-file .env.compose.restore -p autostrada-restore-review exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" < /tmp/restore.sql'
if ($LASTEXITCODE -ne 0) { throw 'Restore failed; keep backend stopped' }
docker compose --env-file .env.compose.restore -p autostrada-restore-review exec -T mysql rm /tmp/restore.sql
docker compose --env-file .env.compose.restore -p autostrada-restore-review up -d --build --wait --wait-timeout 240
```

Check readiness, login, IDs, profile data, images, order totals and Flyway history.
Keep the original volume intact. Actual recovery to an older application requires
a schema-compatible image/config or a verified backup and write freeze; a source
checkout cannot reverse provider charges or restore lost data. No real data move
or prior deployed-MySQL upgrade is claimed by the isolated tests.

## Reproducible verification

Node 22 is needed for orchestration. Browser mode additionally needs dependencies
and Chromium (`npm ci` then `npx playwright install chromium` in `front/`; Linux
CI uses `--with-deps`). From repository root:

```powershell
node front/e2e/compose.mjs --integration-only
node front/e2e/compose.mjs
# Expected exit 1; verifies cleanup after an intentional startup-complete failure:
node front/e2e/compose.mjs --integration-only --verify-failure-cleanup
# Expected exit 1; produces an actual browser trace/screenshot/video:
node front/e2e/compose.mjs --verify-browser-failure
```

Each invocation creates a collision-checked `autostrada-test-<random UUID>`
project, random credentials, ephemeral loopback gateway port and named DB volume.
It ignores developer application/Compose environment and env files. Production
images initialize real MySQL using all migrations and Hibernate validation;
SQL checks enforce schema grants, unique/FK/CHECK constraints and row-lock timeout.
It recreates containers with preserved storage and compares IDs, hashes and Flyway
checksums, restores a logical backup, and exercises readiness during a DB outage.

Browser mode then explicitly builds backend target `e2e`, with a random
loopback-only control port and test classpath. Reset accepts only the random
schema on hardcoded `mysql` DNS with a matching per-run guard token stored in
that schema. Gateway never routes controls. The production image contains only
the main JAR; its build rejects test-launcher/config/library entries. Outbound
provider simulation and signed webhook fixtures retain their documented limits.

Logs, migration checksums, evidence and per-run browser reports live under
`compose-results/<project>/`. Test backups/override tokens remain local and are
excluded from CI uploads. `finally` and signal handlers collect logs and delete
only this run's labeled resources and locally tagged test images. If the process
is forcibly killed, first ensure no test invocation is running, then run
`node front/e2e/compose-cleanup.mjs`; it only accepts generated manifests and exact
project labels. Never run cleanup concurrently with an active harness.

Backend CI gates production builds/MySQL integration; Frontend gates native H2
and Compose/MySQL browser scenarios. Both retain their original check names,
have always-run fallback cleanup and artifact uploads, and fail on any test or
cleanup error. Current local and remote evidence is in the handoff; adding CI
steps does not itself prove they have passed remotely.

## S4b identity-reference checks

The disposable integration harness now checks all 14 account reference columns
for existing FK/nullability/index guarantees, rejects orphan writes and deletion
of an account with business history, and compares every existing business row ID
and account reference across preserved-volume restart and logical restore. No
migration changes or extra services are introduced. These MySQL assertions
passed after Docker became available, along with the 20 browser scenarios. An
additional S4a-to-S4b upgrade on the same disposable volume preserved every table
checksum. Both runs verified cleanup. See [S4b evidence](identity-boundary-pr.md).

For an old-runtime upgrade rehearsal, archive the verified old backend into an
ignored directory, then run `node front/e2e/compose.mjs --integration-only
--upgrade-from=target/s4b/s4a-source/back` (on one command line). The option selects
only the initial backend build context; the harness still creates its own random
project/schema/volume, replaces the old backend with current `back/`, compares all
table checksums and runs the remaining integration checks. It never imports or
connects to a normal developer database.
