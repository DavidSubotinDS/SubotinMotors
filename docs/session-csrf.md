# S4a session and CSRF contract

Implemented in the existing backend; no identity service or new session owner.
See [actual evidence and owner handoff](session-csrf-pr.md). S4b identity boundary
preparation remains next. Architecture Phase C and distributed services are proposals.

## Request contract and lifecycle

`GET /api/csrf` is public and explicitly routed to backend by the gateway's `csrf`
route, before its API route. It returns only `{"token":"<opaque value>"}` with
`Cache-Control: no-store`. Resolving Spring's deferred token creates an anonymous
session if necessary. Clients include credentials on this read and subsequent
requests. No username, email, password, role or session ID is returned here.
`GET /api/session` remains `{authenticated,username,displayName,roles}`.

Backend uses Spring Security 6.5.11's `HttpSessionCsrfTokenRepository`. The expected
token stays in backend memory inside HttpSession. The default XOR request handler
returns a masked token and validates that representation in `X-CSRF-TOKEN` or the
legacy `_csrf` form body parameter. Different GET responses can contain different
masked strings for the same expected token: a reread is **not** token rotation.
An earlier representation remains valid until authentication/logout/session expiry.
The repository, token masking, deferred loading and authentication cleanup follow
[Spring's CSRF documentation](https://docs.spring.io/spring-security/reference/6.5/servlet/exploits/csrf.html).

All unsafe application methods require CSRF, including anonymous login,
registration, reset request and reset consumption. The only public unsafe
exemption is **exact `POST /webhooks/stripe`**. Matching checks method and exact
request URI; trailing slash, suffix/prefix lookalikes and PUT are not exempt.
Gateway only forwards that exact webhook method/path. Body and Stripe-Signature
remain untouched, and the existing Stripe SDK verifier/handlers stay authoritative.
No CORS, SameSite, role or ownership rule replaces CSRF validation.

API CSRF rejection is HTTP 403 JSON:

```json
{"code":"CSRF_INVALID","message":"Your security token has expired. Refresh and try again.","fieldErrors":{}}
```

This code is emitted by the security filter before the controller. It covers
missing, malformed, stale, expired-session and foreign-session tokens without
revealing which check failed. No business operation has run. Ordinary API
authentication/authorization failures retain JSON 401/403 without this code.
An expired-session mutation can first get CSRF 403; after acquiring a valid
anonymous token, a protected mutation gets 401. Legacy failures remain 403,
and valid forms retain their existing redirects and validation behavior.

Successful API login invokes `ChangeSessionIdAuthenticationStrategy`, then
`CsrfAuthenticationStrategy`, before saving a new SecurityContext through
`HttpSessionSecurityContextRepository`. Form login uses Spring's filter lifecycle
with explicit `changeSessionId` fixation protection and CSRF cleanup. The old
anonymous ID cannot retrieve the authenticated session. Non-security session
attributes survive rotation; the old expected CSRF token does not. Wrong-password
login does not authenticate or rotate. See
[Spring session authentication guidance](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/session-management.html)
and [explicit context persistence](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/persistence.html).

API logout invalidates the session and clears its security context. Form logout
remains POST with CSRF and the framework logout handlers. The next token read
creates a fresh anonymous session; old authenticated tokens cannot authorize it.
The backend remains a single instance with in-memory sessions: restart requires
sign-in. Cookie remains `JSESSIONID`, host-only, HttpOnly, Path=/; Compose keeps
SameSite=Lax and its existing configurable Secure attribute for HTTPS.

## Frontend behavior

`front/src/api/client.js` keeps tokens and pending acquisition only in module
memory, with credentialed requests. No storage, URL, console or logging usage.
The first unsafe request fetches a token; reload naturally reacquires one using
the existing session cookie. All current mutations use this shared client.
JSON and multipart requests carry `X-CSRF-TOKEN`. FormData/URLSearchParams bodies
are passed intact without setting Content-Type, preserving browser boundaries.

Unsafe requests are serialized within the tab. This avoids multiple initial
anonymous-session creations and lets login/logout refresh finish before the
next mutation. GET reads continue independently. Successful login/logout clears
the cached token and fetches a fresh one. If that read fails, authentication still
counts as successful; the existing UI navigates/reloads and the next mutation
must reacquire. It cannot accidentally turn a completed login into a failed one.

On `403 + CSRF_INVALID`, the client clears the token, cancels already queued
mutations, fetches a fresh token and rejects the original action with an explicit
message that it was not performed. The user decides whether to submit again.
This handles another tab's authentication transition and session expiry without
silently sending queued work under a changed session. HTTP 401 or a network error
also clears cached state and cancels queued work. A failed acquisition sends no
mutation and gives a connection/token-refresh message. Logout errors are visible
and its button is disabled while the operation is pending.

**There are zero automatic mutation retries**, including CSRF failures, auth
failures, network errors, 5xx, checkout and deposits. Token refresh is a GET only.
Network errors can be ambiguous: the client tells users to inspect the current
result before retrying. This is not a new business idempotency or distributed
checkout mechanism. Existing payment/stock state machines are unchanged.

## Inspected unsafe request inventory

Inventory was made against freshly fetched S3 before editing, including all
controller mappings and `front/src/services/reactApi.js`. Before S4a every API
was CSRF-exempt and manual API login saved a context directly to the session.
Legacy unsafe forms were protected; `/register/thank-you` was a mutating GET.
The tables below describe the new requirement for every affected request.
Braced alternatives denote separate existing routes, not a new wildcard matcher.

All API routes below require the header, credentials, and their existing
authorization. `POST` unless a method is written explicitly.

| Domain | API path(s) | Body / caller |
| --- | --- | --- |
| Login/logout | `/api/auth/{login,logout}` | JSON / public, successful login rotates |
| Registration | `/api/auth/register` | JSON / public |
| Recovery | `/api/auth/password-reset`, `/api/auth/password-reset/complete` | JSON / public; reset token is additional to CSRF |
| Profile | `PUT /api/user/profile`, `/api/user/profile/picture` | JSON; multipart `imageFile` / self |
| Auctions | `/api/user/auctions`, `PUT /api/user/auctions/{id}` | Multipart `imageFiles`; JSON / owner |
| Auction images | `/api/user/auctions/{id}/{picture,pictures}` | Multipart `imageFile` or `imageFiles` / owner |
| Auction state | `/api/user/auctions/{id}/{activate,deactivate}` | Empty JSON / owner |
| Bids | `/api/user/auctions/{id}/bid`, `/api/user/bids/{id}/cancel` | JSON bidPrice; empty JSON / bidder |
| Follow | `/api/user/auctions/{id}/{follow,unfollow}` | Empty JSON / USER |
| Notifications | `/api/user/notifications/{id}/read`, `/api/user/notifications/read-all` | Empty JSON / recipient |
| Auction appointments | `/api/user/auctions/{id}/test-drives`, `/api/user/test-drives/{id}/{reschedule,cancel,accept,reject,owner-cancel}` | JSON date; reschedule date query plus JSON; empty JSON / requester or owner |
| Listings | `/api/user/listings`, `PUT /api/user/listings/{id}` | Multipart imageFiles/form fields / owner |
| Listing state/deposit | `/api/user/listings/{id}/{activate,deactivate,deposit}` | Empty JSON / owner or buyer; deposit is never replayed |
| Listing appointments | `/api/user/listings/{id}/test-rides`, `/api/user/listing-test-rides/{id}/{reschedule,cancel,accept,reject,owner-cancel}` | JSON scheduledAt; reschedule query plus JSON; empty JSON / requester or owner |
| Comments | `/api/comments/cars/{id}`, `/api/comments/parts/{id}` | Multipart body/optional imageFile / USER |
| Cart | `/api/store/cart/items`, `PUT /api/store/cart/items/{id}`, `/api/store/cart/items/{id}/remove` | JSON / owner |
| Checkout | `/api/store/checkout` | Empty JSON / USER; never replayed |
| Admin accounts | `PUT /api/admin/users/{idProfile}`, `/api/admin/users/{idUser}/mark-admin` | JSON / ADMIN |
| Admin marketplace | `/api/admin/cars/{id}/{activate,deactivate}`, `/api/admin/bids/{id}/{approve,deny}` | Empty JSON / ADMIN |
| Admin inventory | `/api/admin/store/parts`, `PUT /api/admin/store/parts/{id}`, `/api/admin/store/parts/{id}/active` | JSON / ADMIN |

Legacy actions below remain POST and accept header tokens or URL-encoded `_csrf`
body fields. Multipart clients use the header (validation happens before the MVC
multipart controller). There are no JSP pages to inject forms into; current
browser UI uses React. Existing field names, redirect targets and ownership
checks remain. Tokens must never be placed in query strings.

| Domain | Legacy paths | Body |
| --- | --- | --- |
| Authentication | `/loginUser`, `/logout` | Form username/password; logout token |
| Registration | `/register/accountProcess`, `/register/profileProcess` | Account/profile form fields |
| Recovery | `/forgot-password`, `/reset-password` | Identifier or reset token/password form |
| Profile | `/user/editProfileProcess`, `/user/uploadPicture` | Profile form; multipart imageFile |
| Auctions | `/user/postCarProcess`, `/user/editCarProcess`, `/user/{activate,deactivate}/{id}`, `/user/uploadCarPicture` | Multipart create/upload; edit/state forms |
| Bids | `/postCarBidding`, `/user/bids/{id}/cancel` | carId/bidPrice; token |
| Appointments | `/test-drive/testDriveProcess`, `/listings/{id}/test-rides`, `/user/test-drives/{id}/{reschedule,cancel,accept,reject,owner-cancel}`, `/user/listing-test-rides/{id}/{reschedule,cancel,accept,reject,owner-cancel}` | Existing date/scheduledAt/action forms |
| Follow/inbox | `/user/auctions/{id}/{follow,unfollow}`, `/user/notifications/{id}/read`, `/user/notifications/read-all` | Existing returnTo/action forms |
| Listings/deposits | `/user/listings`, `/user/listings/{id}`, `/user/listings/{id}/{activate,deactivate,deposit}` | Multipart create/edit; action forms |
| Comments | `/cars/{id}/comments`, `/parts/{id}/comments` | Multipart body/optional imageFile |
| Commerce | `/cart/items`, `/cart/items/{id}`, `/cart/items/{id}/remove`, `/store/checkout` | Existing cart/action forms |
| Admin | `/admin/editProfileProcess`, `/admin/mark-admin/{id}`, `/admin/{activate,deactivate}/{id}`, `/admin/{approve-bid,deny-bid}/{id}` | Existing profile/action forms |
| Admin inventory | `/admin/store/parts`, `/admin/store/parts/{id}/active` | Existing part/action forms |
| Retired payments | `/payments/{id}/checkout` | Existing redirect-only action, still protected |

The only discovered GET security-state mutation was session invalidation in
`GET /register/thank-you`. It now only redirects/renders; successful legacy
`POST /register/profileProcess` performs the former invalidation after saving
registration. Thus a third-party navigation cannot log a user out through the
thank-you page. Successful API registration still does not sign in or invalidate
the caller. Legacy page redirects, health, reset-validity lookup and provider
success/cancel navigation remain reads; return navigation never settles payment.

## Operations and compatibility

Keep the S3 public origin, private ports/networks, trusted forwarding, reset/Stripe
URLs, health checks and native H2 development. Gateway and native backend CORS
allow `X-CSRF-TOKEN` with precise configured origins and credentials. No broad
webhook or health-path CSRF exemptions were added. No Flyway/schema changes.

Default `app.mail.mode=log` now records only a suppression notice, without
recipient, subject, reset link or body. Configure the existing SMTP adapter for
usable development/production recovery mail. Tests capture mail in a private
test-classpath mailbox, accessible only with the per-run control token and never
through gateway; it is absent from production JAR/images. No real SMTP delivery
is implied by these tests. Keep failure traces private: they may contain disposable
fixture credentials/cookies/reset links from HTTP traffic; application logs must
not contain those values. Existing correlation logs omit URLs, queries and bodies.

## Coordinated rollout and rollback

Build backend, frontend and gateway from the same reviewed S4a revision. During a
local release maintenance window stop public ingress/writes, preserve the MySQL
volume, replace all three images together and start health-checked dependencies
before reopening gateway. Serve the new frontend assets and require open tabs to
reload. Backend restart expires existing in-memory sessions; sign-in obtains an
anonymous token, rotates session ID and obtains the authenticated token. Smoke
registration/login/reload/logout, uploads and simulated checkout/deposit before
reopening writes. Use the [S3 backup procedure](docker-compose.md#backup-and-restore)
as usual. No data migration or reset is required.

The frontend requires `/api/csrf`; old frontend writes fail against enforced S4a.
Do not deploy either half alone or add a production enforcement-off switch.
For rollback, close public ingress, restore the last compatible backend/frontend/
gateway image set together, preserve DB data and keep writes closed until a
CSRF-protected correction is ready. S3 restores the prior vulnerable API boundary
and is not an acceptable permanently exposed rollback. Prefer repairing S4a
forward. Never prune/reset volumes or route to a stale data copy. Payment provider
effects cannot be undone by reverting source or restarting containers.

## Verification commands

From `back/`: `.\mvnw.cmd --batch-mode --no-transfer-progress clean verify`.
From `gateway/`: the same independent command.
From `front/`: `npm.cmd test`, `npm.cmd run build`, `npm.cmd run test:e2e`, and
`npm.cmd run test:compose`. Linux CI uses the existing shell equivalents.
Backend/Frontend check names are unchanged; new tests are auto-discovered in
the existing required suites. Container/MySQL/browser failures still fail those
checks, with always-run artifact upload and owned-resource cleanup.

Only uniquely named disposable projects/schemas/volumes and memory H2 are used.
The Compose harness checks production images, fresh migrations, constraints,
locking, preserved restart, logical restore, outage recovery and browser flows.
No H2 result substitutes for MySQL. Signed fixtures exercise the real verifier;
provider creation remains simulated. Real Stripe sandbox smoke is separate.
See the [handoff](session-csrf-pr.md) for measured results and unverified gates.
