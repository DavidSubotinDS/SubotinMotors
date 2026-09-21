# S5 identity extraction inventory and contract

Implementation in progress from verified master
`31693606272a067417a06d40e4a9460c9c929a1c` (S4b PR #22). Backend and Frontend
completed successfully on that exact commit in GitHub run 35594282588. The
requested S5 branch already existed at this commit with a clean working tree.
No branch reset, staging, commit, push, PR, merge or tag was performed.

## Inventory before changes

Identity owns five tables: `tb_user` (`UserAccount`/`UserRepository`),
`tb_user_profile` (`UserProfile`/`UserProfileRepository`), `tb_role`
(`Role`/`RoleRepository`), `tb_profile_picture`
(`ProfilePicture`/`ProfilePictureRepository`), and `tb_password_reset_token`
(`PasswordResetToken`/`PasswordResetTokenRepository`). Account and profile IDs
are different identifiers. Images are stored as base64 LONGTEXT with metadata.
Passwords are BCrypt hashes; reset records contain SHA-256 hashes, creation,
expiry and consumption timestamps. The source has no account deletion API.

Identity code includes UserService, AdminService (accounts only),
PasswordResetService, the three mail adapters, CustomUserDetailsService,
CustomUserDetails, login success/session/CSRF configuration, SelfProfileService,
IdentityApiMapper and InProcessProfileClient. Controllers to move/split are
AuthApiController, SessionApiController, UserWorkspaceApiController's profile
methods, AdminApiController's dashboard/users methods, UserController,
RegisterController, LoginController, PasswordResetController and the account
methods in AdminController. MarketplaceApiController's public profile lookup
must move independently of its profile-auctions lookup.

Business profile consumers include bulk API mapping, comments, auction/listing
display, profile-to-account resolution, workspace composition and private checkout
shipping/email snapshots. All 14 business references in 13 entities already use
scalar account IDs. Their existing columns, indexes, uniqueness and NOT NULL
constraints must remain. Existing identity foreign keys cannot reference a new
independently owned schema; removing them requires copy parity, explicit cutover
and archival/runtime grant separation. No cross-schema runtime reads or writes.

Sessions currently belong solely to backend. API/form login rotates session ID
and CSRF; the React client refreshes memory-only tokens and never replays unsafe
requests. Legacy profileLog and registration state are session-local. Legacy
form actions still mutate and must move with their identity owner. Mail stays
identity-local through S5. Stripe/webhook handling and all other business state
remain backend-owned.

## Document reconciliation

Dated statements that S4b is uncommitted or that S4a is next are historical;
verified GitHub state supersedes them. The Phase C design governs S5, while
S4a's implementation describes the CSRF/mutation contract to preserve. The
identity cookie rename is an intentional controlled re-login, not a second
session authority. Public/private profile separation from S4b remains binding.
The future prohibition on cross-service foreign keys applies only at verified
S5 cutover; it does not authorize silently changing V1–V18 or deleting archives.
The backend temporarily contains commerce and workspace composition, so its
fixed service grant must cover those current consumers until later extraction.
RabbitMQ, notification delivery extraction, distributed checkout and retirement
of the remaining backend are later stages.

## Implemented in this worktree so far

Identity production code has been moved to `services/identity-service` with its
own Maven build, wrapper, Spring Boot application, Flyway V1 identity schema,
runtime configuration, health/readiness, and production Dockerfile. The service
owns login, logout, registration, recovery, profile/profile-picture routes,
admin user/dashboard routes, public profile lookup, session/CSRF, SMTP/log mail
adapters, and internal profile APIs. The production jar build rejects test/e2e
classes.

The remaining backend no longer has production JPA entities, repositories,
controllers or services for the five identity tables. It validates short-lived
RS256 user assertions instead of sessions, uses `CurrentIdentity` from trusted
JWT claims, and calls identity through a bounded `RemoteProfileClient` for public
profile batches, profile-id resolution, checkout profile, and self profile
composition. Backend tests keep a local fixture identity model under test sources
only so business behavior can still be exercised without reintroducing production
identity ownership.

The gateway has explicit identity routing for `/api/auth/**`, `/api/session`,
`/api/csrf`, `/api/user/profile`, identity admin-user routes and the moved legacy
identity forms. For backend `api` and `legacy` routes it performs a private
per-request session exchange, strips browser cookies and forged auth headers, and
forwards only the signed user assertion. It preserves signed Stripe webhook bytes
and now preserves URL-encoded legacy form bytes after exchange.

Identity session validation rechecks fresh scalar password-hash and role state on
each admitted request. Password reset and role grant revoke existing sessions;
the old `JSESSIONID` is expired and the identity cookie is `AUTOSTRADA_SESSION`.

## Evidence so far

- Git prerequisite: S4b is merged to master at
  `31693606272a067417a06d40e4a9460c9c929a1c`; GitHub run `35594282588` had
  required Backend and Frontend checks successful on that exact commit.
- Gateway focused S5 transport tests:
  `./mvnw.cmd test '-Dtest=GatewayTransportTests,GatewayUnavailableTests'` in
  `gateway` passed: 18 tests, 0 failures.
- Identity focused suite:
  `./mvnw.cmd test '-Dtest=ExchangeSecurityTests,CsrfSessionSecurityTests,IdentityContractsTests,RecoveryTests'`
  in `services/identity-service` passed: 39 tests, 0 failures.
- Backend test compilation passed:
  `./mvnw.cmd test-compile` in `back`.
- Focused backend boundary/business suite passed:
  `./mvnw.cmd test '-Dtest=IdentityBoundaryIntegrationTests,IdentityBoundaryArchitectureTests,ReactApiSmokeTests,MarketplaceFeatureIntegrationTests,PaymentWorkflowIntegrationTests'`
  in `back`: 24 tests, 0 failures.

## Latest acceptance status (2026-09-21)

**S5 is locally accepted and ready for commit/push review.** The feature branch
is still uncommitted and has not been pushed or merged by this work session.

- Backend full Maven suite and identity-service full Maven suite passed with zero
  failures/errors/skips.
- The direct HTTP profile client test passed, including batching, private actor
  lookup, controlled dependency failure and no mutation replay.
- Native identity/backend/gateway browser suite using disposable H2: 20/20
  scenarios passed earlier in this work. This is not MySQL evidence.
- Production container builds and disposable MySQL checks passed on several
  runs: identity V1, backend V1-V19, schema isolation, all 14 scalar identity
  columns and indexes, identity constraints, business CHECKs, row locks,
  preserved-volume restart, logical backup/restore, and database outage recovery.
- The final Compose/MySQL browser run passed **20/20** scenarios, including
  identity boundary, profile privacy, forged-owner protection, authentication,
  CSRF, marketplace, payments and password reset.
- The final evidence run was `compose-results/autostrada-test-d7c78a4bade0afd8c2b72051512ab817`;
  cleanup verified that no owned containers, volumes or networks remained.
- The production `IdentityCopy` utility verified complete row-value parity for all
  five identity-owned tables before V19. Runtime access to archived identity
  tables was denied after cutover, while business-table access remained available.
- CI identity smoke no longer masks a failed container start with `|| true`.
- Remote S5 CI, real Stripe sandbox transactions and SMTP delivery remain
  unverified. Simulated payment fixtures are not real Stripe evidence.

## Remaining work

S5 implementation and disposable acceptance are complete. Before merge, review
the staged diff and run the remote Backend and Frontend checks on the exact
commit. Real Stripe sandbox transactions, SMTP delivery and remote S5 CI remain
owner-run follow-up checks; simulated payment fixtures are not real Stripe
evidence. Do not advance to S6 until the reviewed S5 commit is merged.

No normal developer database has been used for validation. Ignored logs, dumps,
generated keys/configuration and browser artifacts are private and must not be
staged or published. Do not advance to S6 until S5 is accepted and merged.

## Rollout and rollback restriction

Rollout must freeze all writers, take verified backups, run full source/target
parity, perform V19 with separate migration credentials, remove runtime archive
access, switch routes together and require re-login. Before any new identity writes, an S4b rollback
requires a verified, consistent pre-cutover schema and data; renaming archives
alone does not restore the dropped foreign keys. After any identity writes,
repair forward or reconcile all current identity changes under a write freeze
before switching authority. Never restore stale passwords, roles or reset state
from the old backend archive or a pre-change snapshot.
