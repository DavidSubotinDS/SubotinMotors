# S5 owner handoff — locally accepted, ready for review

The original 2026-09-21 local acceptance record below is historical. The owner
subsequently committed and pushed S5 through `ac559c7`; the current branch is
`feature/david.subotin_identity-service`. Remote CI failures were reported by
the owner. No staging, commits, pushes or merges were performed in the CI-fix
session described below.

## 2026-09-22 MySQL CI startup correction

The pinned official MySQL image sources non-executable init scripts. Git records
`back/scripts/mysql-init-users.sh` as mode 100644, so its unscoped `set -eu`
enabled `nounset` in the entrypoint shell. The subsequent official
`mysql_expire_root_user` function then failed on line 342 when
`MYSQL_ONETIME_PASSWORD` was unset. This exact error was reproduced locally
using the committed script and pinned image, without a server or database volume.

The init hook now runs in a subshell. Strict error handling still applies inside
the hook, while shell options and its E2E early `exit` cannot affect the parent.
The owner's existing `AUTOSTRADA_E2E` skip and Compose override are retained.
Shell scripts in `back/scripts` have explicit LF endings for Linux checkouts.
No MySQL password-mode variables were added: `"0"` is nonempty and would enable
one-time password expiry in this image, rather than disable it.

`back/scripts/test-mysql-init.sh` tests the actual image's init-processing and
password-expiry helpers with SQL stubbed. It fails on the original committed
script and passes on the correction, including sourced/executable normal and
E2E paths and missing-required-configuration rejection. The shared Compose
harness runs it before image builds, so both required CI checks enforce it.
JavaScript syntax and `git diff --check` pass. The full `node front/e2e/compose.mjs`
run exited 0 on 2026-09-22: production container builds/startup, five-table copy
parity, migrations and credential isolation, MySQL integrity checks, preserved-data
restart, logical backup/restore, database outage/recovery, and all 20 browser
scenarios passed. Cleanup verified no owned containers, volumes or networks remain.
Evidence is recorded under
`compose-results/autostrada-test-da4666a1eb6c8eca9d1cb8909ca42447`.
These are disposable MySQL and simulated-payment results; they do not establish
real Stripe sandbox or SMTP delivery. Application unit suites were not separately
rerun for this shell/harness correction.

Remote CI has not been rerun for this uncommitted correction. GitHub CLI has no
active login in this environment; the four reported failures are the Backend
and Frontend jobs on push and pull-request events. Verify the required checks
on the new pushed commit before merging.

Selective owner commands for this correction (including the retained local
E2E skip changes):

```powershell
git add -- .gitattributes back/scripts/mysql-init-users.sh back/scripts/test-mysql-init.sh front/e2e/compose.mjs docs/identity-service-pr.md
git diff --cached --check
git diff --cached
git commit -m "fix: isolate MySQL init hook shell options"
git push origin feature/david.subotin_identity-service
```

Suggested existing-PR update:

> Fix MySQL initialization by isolating the sourced init hook's strict shell
> options and E2E early exit. Add a pinned-image regression to the shared Compose
> harness used by Backend and Frontend checks. The original committed script
> reproduces the exact CI error; the corrected full disposable MySQL integration
> and all 20 browser scenarios pass, including verified cleanup. Remote checks
> must pass on the new pushed commit before merge.

## Ownership and changes

`services/identity-service` contains its own Maven build/wrapper, artifact,
Dockerfile, Flyway V1 and identity code for accounts, profiles, pictures, roles,
password reset, account administration, sessions and mail. Five identity tables
move together. Backend keeps marketplace/commerce/payment state and workspace
composition, using scalar account IDs, validated assertions and profile APIs.
Production identity entities/repositories and local login authority were removed
from backend. Test fixture identity entities remain under test sources only.

Gateway moves auth/session/CSRF/profile/admin-user routes together. It privately
exchanges the identity session for a short-lived user assertion before protected
backend calls and strips browser cookies and forged auth headers. Backend validates
issuer, audience, signature, token use and lifetime, and derives owners from trusted
authentication. Role/ownership restrictions, public-profile redaction, multipart,
signed webhook routing and no mutation replay remain acceptance requirements.

Backend V19 requires a parity marker, drops 14 cross-boundary FKs while retaining
scalar columns/indexes, and renames five old tables as archives. Applied V1-V18
SQL is unchanged. Runtime and migration credentials are separated and archive
access is removed during cutover. Mail remains identity-local; S6 was not
implemented.

## Actual evidence and remaining review

See [the current evidence record](identity-service.md#latest-acceptance-status-2026-09-21).
The production `IdentityCopy` utility passed complete row-value parity on
disposable MySQL. V19 passed with all 14 FK removals, archive rename and runtime
credential isolation. Restart, backup/restore, outage and constraints passed.
The final Compose/MySQL browser run passed 20/20 scenarios and verified cleanup.
Backend and identity tests passed; the direct HTTP profile client test passed.
Remaining review is limited to staged diff review, remote CI on the eventual
commit, and optional real Stripe/SMTP owner checks.

## Owner commands for review and push

Run from PowerShell after reviewing the working tree. These paths cover the S5
changes and exclude generated `target`, Compose and browser result directories.
Do not use `git add .` or `git add -A` at repository root. Review the staged diff
before committing, particularly if you have added unrelated work since handoff.

```powershell
Set-Location C:\Projects\SubotinMotors
git switch feature/david.subotin_identity-service
git status --short
git add -- .github/workflows/ci.yml back/pom.xml back/src/main/java back/src/main/resources/application.properties back/src/test compose.yaml front/e2e/compose.mjs front/e2e/fixtures.js front/e2e/run.mjs front/e2e/specs/marketplace.spec.js front/e2e/specs/session-csrf.spec.js gateway/src/main/java/lithan/autostrada/gateway gateway/src/test/java/lithan/autostrada/gateway services/identity-service docs/identity-service.md docs/identity-service-pr.md
git diff --cached --check
git diff --cached --stat
git diff --cached
git commit -m "feat: extract identity service with safe cutover"
git push -u origin feature/david.subotin_identity-service
```

Default/base branch stays `master`. Do not merge based on the historical S4b CI
results. Backend and Frontend must succeed on the actual S5 commit after remaining
acceptance work is complete.

## Suggested draft PR

Title: `feat: extract identity service with safe cutover`

Body:

> Moves account/profile/role/reset/session ownership and identity routes into an
> independently built identity service. Backend uses trusted user assertions and
> private profile APIs; gateway remains the single public origin. Mail stays in
> identity and later extraction stages remain out of scope.
>
> Includes controlled-cutover tooling and backend V19. The production copy utility
> verifies complete row-value parity under a write freeze; runtime database access
> is separated from migration access and cannot read identity archives after cutover.
>
> Evidence: backend and identity tests passed; disposable production/MySQL migration,
> parity, credential isolation, constraint, restart, backup/restore and outage checks
> passed; full Compose browser regression passed 20/20. Remote S5 CI, real Stripe
> sandbox and SMTP delivery remain owner-run follow-up checks.

## Rollout and next stage

After review and successful remote checks, rollout follows the restrictions in
[the extraction record](identity-service.md#rollout-and-rollback-restriction);
never re-enable stale credentials, roles or reset state. The next documented
stage after completed/merged S5 is S6, beginning with S6a RabbitMQ/outbox.
