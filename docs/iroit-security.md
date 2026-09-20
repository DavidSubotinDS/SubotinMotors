# IROIT authentication and authorization design

Current S4a implementation (2026-09-20): **Phase B is implemented in the working
tree**, with [contract/runbook](session-csrf.md) and [actual evidence](session-csrf-pr.md).
S3 merged as `8d13104` with successful exact-commit Backend/Frontend checks.
Backend remains sole session owner; JSESSIONID and the S3 topology are retained.
S4b identity boundary work is next; Phase C, target cookie rename, session exchange
and internal assertions remain proposed. Dated S2/S3 paragraphs describe history.

Historical S3 added private container ingress, schema-scoped DB credentials, runtime secret
injection and explicit demo-data acknowledgement. Production images exclude test
controls; only the isolated harness publishes a random loopback control port.
Compose makes current JSESSIONID HttpOnly/host-only/Path=/ and SameSite=Lax explicit,
with Secure configurable for HTTPS (local HTTP default false). Backend is still
the sole session/authorization owner. At S3, API CSRF exemptions and legacy
enforcement were preserved; Phase B/C were proposals. S4a implements Phase B
as described above. See [container runbook](docker-compose.md).

S2 implementation status: Phase A below is implemented by the
[pass-through gateway](../gateway/README.md). The backend remains the sole
session/authentication/authorization owner, with unchanged API and legacy CSRF
behavior. Gateway sanitizes forwarding/identity headers and creates forwarding
values from its configured public origin, owns precise API CORS, and streams
webhook bytes untouched. Backend forwarding/log correlation are opt-in via the
`gateway` profile on private ingress; test controls are not gateway-routable.
Phase B/C, the target cookie name, JWT assertions and session exchange were
proposals at S2. Only Phase B is now implemented; no S2 protection is inferred.

The original document was a proposed staged design. Phase A/B now have
implementation evidence; Phase C remains proposed. See [migration stages](iroit-migration-plan.md).

## Verified S4a boundary

- [SecurityConfig](../back/src/main/java/lithan/autostrada/auctions/config/SecurityConfig.java)
  configures Spring Security sessions/form login, BCrypt, USER/ADMIN route
  permissions, credentialed CORS for configured React origins, API JSON 401/403,
  and public signed webhooks. CSRF uses HttpSession storage and default masked
  tokens. Only exact `POST /webhooks/stripe` is exempt; all API and legacy unsafe
  actions are protected, including anonymous login/registration/password reset.
- [AuthApiController](../back/src/main/java/lithan/autostrada/auctions/controller/api/AuthApiController.java)
  invokes framework session-ID rotation and CSRF cleanup before explicitly saving
  `SecurityContext`. API logout invalidates the session. Form login/logout retain
  the framework lifecycle; old identifiers/tokens are rejected by regression tests.
- [Frontend client](../front/src/api/client.js) sends cookies with
  `credentials: 'include'` and `X-CSRF-TOKEN` on every unsafe request. It stores
  the token only in memory, refreshes after login/logout/stale-token rejection,
  and never automatically replays mutations. Queued work is cancelled on an
  unexpected session change. Ordinary role/ownership failures remain distinct.
- Existing session response is `{authenticated,username,displayName,roles}`.
  The browser must retain this contract and never receive password/email data
  through this DTO. Profile APIs have their own authorized fields.
- Services currently discover the user through `UserService.getUserLogin()`
  and perform resource ownership checks. Role strings are `ROLE_USER` and
  `ROLE_ADMIN`; seller/buyer/store-team labels are not separate authorization
  roles in this design.

## Phase A: gateway pass-through

Route all existing auth/API/webhook/legacy actions to `back/` first. It remains
the sole session owner. Forward `Cookie`, `Set-Cookie`, status, query strings,
multipart bodies and redirects correctly. Do not add a second gateway login or
assume a gateway session authenticates the backend. Use one backend instance;
no shared session DB or Redis is needed at this stage. Test session reuse and
logout through the gateway before changing any authentication mechanics.

Use a single public origin for the deployed React assets and API. Configure
`APP_FRONTEND_BASE_URL`, application reset links and Stripe return URLs to that
origin; validate forwarded host/proto only from the trusted gateway. The gateway
must strip client-provided `Forwarded`, `X-Forwarded-*`, `X-User-*`, role and
internal authentication headers before constructing its own values. Preserve
the raw Stripe body and `Stripe-Signature`; no JSON rewrite/body logging.

## Phase B: secure browser boundary before extraction

Implemented by S4a; the following is the original design contract. Actual masked
token format, explicit-user retry behavior and coordinated rollout are documented
in [S4a](session-csrf.md). The legacy thank-you GET no longer invalidates sessions;
that action moved to successful protected registration POST. Log-mail now omits
message contents/reset links; use SMTP for delivery, or the private test mailbox.

Add `GET /api/csrf` to the current session owner. It creates/uses an anonymous
session and returns a CSRF token in a non-cacheable response; the expected token
is server-side and React keeps the returned token in memory. Extend the shared
API client to send `X-CSRF-TOKEN` on unsafe requests, including login, logout,
registration, reset and multipart uploads. Rotate session ID on login, refresh
the token after login/logout, and handle stale tokens with an explicit refresh
and user-safe retry (never replay a payment blindly). Protect legacy unsafe
forms too. Only the exact signed webhook endpoint is browser-CSRF-exempt.
Spring provides the [session CSRF token mechanism and SPA considerations](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).

Do not rely on CORS or SameSite as the only CSRF defense. Roll out frontend
token handling first in a coordinated release, then enforce it with positive
and negative browser/API tests. No critical route gets a blanket `/api/**`
exemption in the target. This change is a focused future task because it changes
the request contract, despite preserving screens and user actions.

## Phase C: identity session owner and internal assertions

Selected course-scale design: identity owns the browser session; gateway uses
a private session-exchange endpoint to obtain short-lived signed user assertions.
This avoids sharing session tables or adding an external identity provider. It
is an application-internal protocol, not a claim to implement OAuth/OIDC login.
Use maintained Spring Security/Nimbus JWT support, not custom cryptography.

1. Move accounts/profiles/roles/reset and authentication to identity after
   scalar-reference adapters are ready. Preserve account IDs and BCrypt hashes.
   At the controlled cutover expire the old cookie and require one re-login;
   serialized monolith sessions/principals are not copied into a different app.
2. Identity handles public `/api/auth/**`, `/api/session`, `/api/csrf` and profile
   requests through gateway, including their CSRF checks. One identity instance
   initially uses its own in-memory sessions: restart requires re-login. Add
   identity-only JDBC session persistence later only if a measured need or
   rubric requires it; no other service receives session-table access.
3. For a protected business request, gateway calls
   `POST /internal/v1/session-exchange` on identity using its own authenticated
   service credential and the browser session cookie. Include the intended
   downstream audience and original method; include the supplied CSRF token
   for unsafe methods. Identity validates current session/account/roles and
   CSRF, permits only configured audiences, and issues a 60-second user JWT.
   Failure denies forwarding. Anonymous public reads need no exchange;
   personalized public reads exchange if a valid cookie is present.
4. Gateway replaces external `Authorization` and identity headers with this
   assertion, strips the browser cookie before sending to business services,
   and forwards over the private network. It does not accept browser bearer
   tokens as an alternate authentication mode. Never cache user assertions
   across browser requests; check the session each time. Identity logout/reset
   revokes sessions; already admitted requests may finish within the short TTL.
5. Each service verifies signature, fixed issuer, its explicit audience, allowed
   algorithm, expiry/not-before and `tokenUse=user`; converts roles consistently;
   enforces ownership and valid state transitions itself. It never trusts a
   bare username/user-ID/role header. Use configured or fetched public keys with
   `kid` rotation overlap; only identity holds the signing private key.
   [Spring Security JWT resource-server validation](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
   supplies the implementation foundation; audience and application claims need
   explicit validation.
6. While `back/` remains, its security adapter accepts identity user assertions
   for already switched flows and uses scalar owner IDs. Once identity cuts
   over, disable legacy local credential/session authentication for business
   routes; no indefinite two-authentication-source mode.

Required assertion claims: `iss`, `aud`, `sub` (account ID), `roles`, `iat`,
`nbf`, `exp`, `jti`, `tokenUse=user`. Keep private profile/address/reset data out.
Gateway read composition obtains an audience-specific assertion for each
destination through the same validated session. Its connection limits and
timeouts must be bounded; identity failure returns a controlled 503/401 as
appropriate, never anonymous elevation or cached access to a protected route.

### Business service credentials

For service-initiated REST, identity issues short-lived `tokenUse=service` JWTs
through a private service-token endpoint with distinct per-client secrets kept
outside Git. Fixed grants permit commerce to read checkout-profile/capabilities
and create STORE_ORDER payments; marketplace may create LISTING_DEPOSIT payments;
gateway alone may exchange browser sessions. A service cannot ask for arbitrary
roles, user claims or audiences. Token endpoint bypasses browser CSRF because
it authenticates service credentials, not cookies, and is not publicly routed.

The checkout-profile endpoint is restricted to commerce. Commerce derives the
requested user ID from its validated browser assertion and passes it over the
authenticated internal call; it cannot accept an arbitrary browser-selected
buyer ID. This is deliberate trust in the commerce service's authorization
implementation. Test that user A cannot fetch B's profile through commerce.
Payment checks caller purpose grants in addition to payload consistency. Internal
user tokens never grant service-command scopes, and service tokens never grant
interactive ADMIN access. Cache service tokens only until shortly before expiry.

Business service ports and `/internal/**` are not publicly published. Private
network isolation complements credentials; it is not proof of caller identity.
Use HTTPS externally; internal HTTP is acceptable for single-host course Compose
with an explicit trust boundary. Use TLS if traffic crosses hosts/untrusted
networks. Signing keys, service secrets, Stripe sandbox key/webhook secret, DB
passwords and SMTP credentials use environment/secret injection, never images,
Git, frontend bundles, message payloads or logs.

## Cookies, CORS and authorization rules

Target deployed cookie: opaque `AUTOSTRADA_SESSION`, HttpOnly, Secure,
SameSite=Lax, Path=/, host-only (no Domain). Its value is an identity session ID,
not a JWT. Local HTTP development has an explicit non-Secure override and the
same host spelling for frontend/gateway; do not mix localhost and 127.0.0.1.
Expire old `JSESSIONID` on the identity cutover to avoid conflicting cookies.
Set idle/absolute limits explicitly (proposed 30 minutes / 8 hours) and test
logout, password reset and role change invalidation/refresh.

Prefer same-origin Vite proxy or gateway-routed development. If Vite directly
calls a different gateway port, gateway alone allows the exact configured origin
(initially `http://localhost:5173`), credentials, required methods and
`Content-Type`, `X-CSRF-TOKEN`, `Idempotency-Key`. Handle OPTIONS before auth;
no wildcard credentialed origin and no duplicate downstream CORS headers.
Production same-origin calls need no permissive CORS. GET endpoints must remain
read-only even if navigated from another site.

| Service | Mandatory permission examples |
| --- | --- |
| identity | Self profile edit; ADMIN account administration/role grant; public profile allowlist excludes private address/email/hash; reset response avoids account enumeration |
| marketplace | USER bids/follows/comments; own auction/listing updates; buyer/seller appointment permissions; self-bid/deposit denial; ADMIN moderation; deadline/state checks in transaction |
| commerce | Own cart/orders; ADMIN inventory/order administration; server-computed totals and stock validation; order lookup by Stripe session still checks owner |
| payment | Owner/admin payment read; service-scoped checkout creation; verified provider signature on webhook; no settlement through browser parameters |
| notification | Recipient-only list/count/read/read-all; service-scoped delivery commands; no arbitrary mail-send browser endpoint |

Required tests include forged gateway headers, direct service calls without
valid assertions, wrong audience/issuer/algorithm/expiry/token use, USER calling
ADMIN routes, cross-user IDs, invalid/stale CSRF on JSON/form/multipart/login,
unapproved CORS origins, and webhook signature/body mutation. Check both gateway
and direct service boundaries: hiding a button or authorizing only at the
gateway is insufficient. Log subject IDs and decisions where useful, excluding
passwords, session/JWT values, reset links and payment secrets.
