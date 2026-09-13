# Stabilization before merging the React migration

Verified on 2026-09-12 on
`feature/david.subotin_jsp-payment-pages-migration-ui-ux`.
The local remote-default reference points to `origin/master`.

On 2026-09-13, the user confirmed that the application works after receiving
the final UI and Stripe sandbox check instructions. This is user-reported
acceptance; the automated results below were verified separately.

## Completed

- [x] Adapt business integration tests to the REST APIs consumed by React.
  Coverage includes filtered and sorted catalogues, independent admin pages,
  order items and totals, comments, appointments, and payment audit data.
- [x] Keep explicit checks for legacy redirects, aliases, and preserved search
  query parameters.
- [x] Restore login redirects for anonymous requests to protected legacy URLs.
  Protected API requests return JSON with HTTP 401; insufficient roles return
  HTTP 403. Regression tests cover HTML, JSON, and wildcard Accept headers.
- [x] Validate registration fields before persistence, including password length
  before hashing. Invalid requests return HTTP 400 with field errors.
- [x] Validate auction creation and updates, including production year, positive
  price, and future closing time. Tests verify valid submissions, rejected input
  without database changes, and rejection of another user's edit.
- [x] Exercise API registration, login, session reuse, and logout.
- [x] Run all backend tests with Java 17: **96 passed, 0 failed, 0 skipped**.
  This includes the existing payment and webhook regression tests.
- [x] Package the executable backend JAR using the Maven wrapper.
- [x] Run frontend tests: **12 passed across 6 files**.
- [x] Build the frontend successfully with Vite.
- [x] Check the tracked diff for whitespace errors.

Backend verification used `mvn test`, followed by
`.\mvnw.cmd -DskipTests package` on the same source. Frontend verification used
`npm.cmd test` and `npm.cmd run build`. Backend tests use in-memory H2 and
disabled or mocked Stripe; they do not establish that a live sandbox checkout
was completed. No MySQL deployment was tested in this pass.

## Before merge

- [ ] Review the complete branch diff, including the preceding React migration
  and Stripe startup script changes.
- [ ] With the latest backend running, check registration/login/logout and one
  auction create/edit flow through the React UI.
- [ ] Complete a Stripe sandbox parts purchase and check the signed webhook,
  paid order in customer/admin views, and the resulting stock quantity.
- [ ] Check the cancelled checkout path and one fixed-price listing deposit.
- [ ] Commit the reviewed changes and open/update the PR against the confirmed
  default branch. Refresh remote state and resolve conflicts if necessary.
- [ ] Confirm the PR's required checks, then merge. Re-run relevant verification
  if conflict resolution changes the code.

## Next project work

- [ ] Add CI for Java 17 backend verification and frontend installation from the
  lockfile, tests, and build.
- [ ] Make those checks required before merging future changes.
- [ ] Continue the IROIT implementation plan from this tested baseline: container
  setup, deployment, monitoring, and the architecture required by the course.

This checklist records the current stabilization work, not completion of the
course requirements or a full production-readiness review.
