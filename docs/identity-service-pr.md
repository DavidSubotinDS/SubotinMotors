# S5 owner handoff — locally accepted, ready for review

Status on 2026-09-21: the extraction and disposable acceptance gates are complete
in the working tree. The branch remains uncommitted and has not been pushed,
reviewed, or merged by this work session.

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
