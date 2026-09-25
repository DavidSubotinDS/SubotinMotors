# Autostrada pass-through gateway (IROIT S2)

S8 working-tree update: exact `POST /webhooks/stripe` and
`/api/payments/**` now route to payment-service. For browser API reads the
identity bridge requests a `payment-service` user assertion; the raw webhook
body/signature is still streamed unchanged. Backend remains the destination for
commerce/marketplace routes. See [S8 contracts](../docs/payment-service.md).

S6 working-tree update: notification inbox and mail delivery now have a separate owner,
RabbitMQ and guarded data cutover. Read the [S6 runbook](../docs/notification-service.md) before starting
the new Compose stack; historical S5 startup/test instructions below need that cutover.


S4a: an explicit `GET /api/csrf` route reaches the existing backend session owner.
All API/legacy unsafe requests are now backend-CSRF protected except exact signed
`POST /webhooks/stripe`. Cookie and `X-CSRF-TOKEN` transport and CORS are tested;
gateway still owns no session or token store. [Contract](../docs/session-csrf.md),
[evidence and rollout](../docs/session-csrf-pr.md). S2/S3 passages below are historical.

S3: [Compose startup and recovery](../docs/docker-compose.md) now supplies private
backend/frontend/MySQL peers and reuses this image with pinned base digests.
The native instructions below remain available; historical S2-only limits should
be read alongside [S3 evidence](../docs/docker-compose-pr.md).

Independent Java 17 executable. Spring Boot **3.5.15**, Spring Cloud BOM
**2025.0.3**, Gateway Server WebFlux **4.3.5**. The
[Cloud release announcement](https://spring.io/blog/2026/06/11/spring-cloud-2025-0-3-aka-northfields-has-been-released/)
explicitly pairs these versions; [Boot 3.5 requires Java 17](https://docs.spring.io/spring-boot/3.5/system-requirements.html).
The [WebFlux starter](https://docs.spring.io/spring-cloud-gateway/reference/4.3/spring-cloud-gateway-server-webflux/starter.html)
is used, not the MVC gateway. This preserves the backend dependency versions.
The selected Cloud train's OSS support ended June 2026; compatibility is not a
claim of current support or a dependency security audit. Remediation is separate.

The gateway has no login, session store, business classes, JPA, Flyway, database,
or Stripe SDK. `back/` is the sole session, permission, transaction and payment
owner. There is no service extraction or Compose topology in S2.

## Build and tests

From `gateway/`: `./mvnw --batch-mode --no-transfer-progress clean verify`.
On Windows use `.\mvnw.cmd`. This builds only this directory. HTTP tests start
local Reactor Netty stubs; another context starts with both upstreams unavailable.
Reports: `target/surefire-reports/`. No database or Stripe credentials are used.

## Route precedence

Executable catalogue: [GatewayRoutes.java](src/main/java/lithan/autostrada/gateway/GatewayRoutes.java).
Queries are forwarded as received; no path rewriting or body filters are used.

| Priority / route | Destination and methods |
| --- | --- |
| Local private guard | `/__*`, `/internal/**`, and actuator endpoints other than the three health URLs return 404; never proxy test controls |
| `csrf` | GET `/api/csrf`, backend; precedes general API routing |
| `api` | `/api`, `/api/**`, every method, backend; unknown API paths/errors never become SPA HTML |
| `stripe-webhook` | **POST `/webhooks/stripe` only**, payment-service; other methods and suffixes are not routed |
| `payment-api` | `/api/payments/**`, payment-service through the identity assertion bridge |
| `spa` | GET/HEAD canonical React pages: root/auth/static pages; `/auctions`, `/listings`, `/parts` and their detail IDs; cart/orders/details; profiles; both checkout success pages; canonical user/admin screens and supported edit pages |
| `legacy` | Remaining methods in the explicitly listed legacy families, including login/logout/registration/reset, bids, follows, comments, uploads, checkout, appointment and admin actions; legacy GET aliases such as `/cars`, `/car-listings`, `/store/parts`, `/user/my-auctions`, retired `/payments/**` go to backend |
| `frontend-assets` | GET/HEAD `/assets/**`, `/images/**`, favicon and listed Vite development assets go to frontend |
| Unmatched | 404 JSON, including unrecognized webhook paths; no broad frontend fallback |

Canonical page GETs take precedence over same-path MVC views. For example,
`/car-listings/42` goes to the backend alias, then `/listings/42` serves React.
`POST /listings/42/test-rides` still reaches backend authorization and CSRF.
`/cars/Make/Model/2024/41` uses the backend view resolver, now preserving `/auctions/41`.
The backend resolver also preserves supported listing/part/order/profile/detail/edit
model IDs when accessed directly. It exposes no model attributes in the query.
Queries survive transport; React catalog filter initialization remains unchanged.
Admin legacy account editing still lands on the existing users screen because
there is no separate React account-edit route. This is not a general legacy-form UI rewrite.

## Trust, sessions and errors

Deploy one backend instance on private ingress. The edge strips external
`Forwarded`, all `X-Forwarded-*`, `X-User-*`, `X-Roles*`, `X-Auth-*`, `X-Internal-*`,
Authorization/proxy credentials and the test-control header. No browser bearer
authentication is introduced. `OwnedForwardingHeaders` runs **after** SCG's
removal filters and constructs host/proto/port from `GATEWAY_PUBLIC_URL` and
client address from the socket. Client Host cannot change the configured origin.
No upstream proxy is trusted for original client IP; with a TLS terminator,
configure the external HTTPS public URL and restrict ingress appropriately.

Backend profile `gateway` opts into framework forwarding and binds to loopback.
Only use forwarding support behind this private boundary. Container backends
will need private-network binding in S3, not a public backend port.
Cookie/Set-Cookie, including multiple headers and expiry attributes, pass through.
Opaque JSESSIONID authentication and backend roles/ownership remain. S4a enforces
API/legacy CSRF and login rotation while preserving signed webhook verification.

Gateway owns API CORS: exact `GATEWAY_ALLOWED_ORIGINS` plus public origin,
credentials, explicit methods and Content-Type/X-CSRF-TOKEN/Idempotency-Key/
X-Request-ID headers. It handles preflight and removes Origin before API proxying
so backend CORS cannot duplicate headers. No wildcard origins. Use the same
host spelling throughout. Same-origin browser requests need no cross-site cookie changes.

Webhook request data is never decoded/deserialized/rewritten. Multipart is streamed;
the backend still applies its existing **5 MB per file / 40 MB per request** limits.
In the tested backend, a 6 MB file is rejected before the controller with an empty
HTTP 400, both directly and through gateway. S2 preserves this existing response;
it does not claim a JSON 413 upload contract.
The gateway does not aggregate uploads or retry mutations. Backend statuses,
JSON validation errors and Location headers are forwarded unchanged. Connection
failure is 502 JSON; response timeout is 504 JSON. Never route a failed API to React.

## Configuration and health

| Variable | Default / purpose |
| --- | --- |
| `GATEWAY_PORT`, `GATEWAY_BIND_ADDRESS` | `8081`, `127.0.0.1`; container binds `0.0.0.0` |
| `GATEWAY_PUBLIC_URL` | `http://localhost:8081`; HTTP(S) origin, no path/query/credentials |
| `GATEWAY_BACKEND_URL` | `http://127.0.0.1:8080` |
| `GATEWAY_FRONTEND_URL` | `http://127.0.0.1:5173`; asset server must never proxy page navigation back to gateway |
| `GATEWAY_ALLOWED_ORIGINS` | `http://localhost:5173`; comma-separated exact optional development origins |
| Backend `APP_FRONTEND_BASE_URL`, `APP_BASE_URL` | Set **both** to gateway public URL; covers React handoff, password reset and Stripe return URLs |
| Frontend `VITE_API_BASE_URL` | Empty for same origin (build-time); remove stale `.env` backend URLs |
| Frontend `VITE_API_PROXY_TARGET` | Gateway URL for optional direct Vite development browsing |

[Proxy budgets](https://docs.spring.io/spring-cloud-gateway/reference/4.3/spring-cloud-gateway-server-webflux/http-timeouts-configuration.html):
2 s connect, 30 s response, 100 pooled connections, 2 s pool acquisition and
30 s idle expiry. Overrides use standard Spring property environment names
under `SPRING_CLOUD_GATEWAY_SERVER_WEBFLUX_HTTPCLIENT_...`.
There are no blocking database/provider calls on gateway event loops.

`GET /actuator/health/liveness` is process-only. Readiness probes backend health
and frontend availability concurrently with independent 2 s budgets; unavailable
required upstream means 503. Startup and liveness survive missing peers.
`/actuator/health` includes readiness dependencies. Health reveals no details.
SCG request metrics are instrumented in-process; only health is publicly exposed.
Full metrics export/dashboards/traces remain later observability work.

Every request gets a newly generated `X-Request-ID`, forwarded to backend and
returned to callers. Logs use service, request ID, route, method, status and
duration; backend `gateway` profile enables matching request logs. Standard
trace headers pass through, but S2 does not claim a distributed tracing backend.
No URLs/queries, bodies, Cookie, Stripe-Signature or exception text enter these
logs. Wiretap is disabled. Do not enable HTTP wiretap or payload debug logging.

## Reproducible native startup and teardown

For the complete disposable regression stack, from `front/`:
`npm run test:e2e` (Windows `npm.cmd`). This builds all three executables/assets,
starts only private test processes, waits for gateway readiness and cleans up in
finally. [Browser runbook](../docs/browser-e2e.md) includes installation and failure probes.

For interactive use, open three fresh PowerShell terminals. The example below
uses a disposable production-classpath memory DB, never the normal developer DB.
Backend terminal:

```powershell
Set-Location C:\Projects\SubotinMotors\back
.\mvnw.cmd --batch-mode --no-transfer-progress clean verify
$gatewayDemoDb = 'jdbc:h2:mem:gateway_demo_' + [guid]::NewGuid().ToString('N') + ';MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1'
& "$env:JAVA_HOME/bin/java.exe" -jar target/autostrada-auctions-0.0.1-SNAPSHOT.jar --spring.profiles.active=gateway --server.port=8080 "--spring.datasource.url=$gatewayDemoDb" --spring.datasource.driver-class-name=org.h2.Driver --spring.datasource.username=sa --spring.datasource.password= "--spring.flyway.url=$gatewayDemoDb" --spring.flyway.user=sa --spring.flyway.password= --app.frontend.base-url=http://localhost:8081 --app.base-url=http://localhost:8081 --payments.stripe.base-url=http://localhost:8081 --payments.stripe.enabled=false
```

Frontend terminal:

```powershell
Set-Location C:\Projects\SubotinMotors\front
npm.cmd ci
$env:VITE_API_BASE_URL = ''
npm.cmd run build
npm.cmd run preview -- --host 127.0.0.1 --port 5173 --strictPort
```

Gateway terminal:

```powershell
Set-Location C:\Projects\SubotinMotors\gateway
.\mvnw.cmd --batch-mode --no-transfer-progress clean verify
& "$env:JAVA_HOME/bin/java.exe" -jar target/gateway-0.0.1-SNAPSHOT.jar
```

Open `http://localhost:8081`. Verify gateway readiness; register a demo user.
Stop each owned terminal with Ctrl+C. Closing the backend discards only this
memory DB. Ports already in use must be resolved deliberately, never killed by name.
Do not run backend clean builds while E2E uses its test classpath.

## Container and CI

```powershell
Set-Location C:\Projects\SubotinMotors\gateway
docker build --tag autostrada-gateway:s2 .
# Back/frontend must be reachable from Docker; host.docker.internal is Docker Desktop's host address.
docker run --rm --name autostrada-gateway-s2 -p 127.0.0.1:8081:8081 -e GATEWAY_PUBLIC_URL=http://localhost:8081 -e GATEWAY_BACKEND_URL=http://host.docker.internal:8080 -e GATEWAY_FRONTEND_URL=http://host.docker.internal:5173 autostrada-gateway:s2
# In a separate terminal when finished:
docker stop autostrada-gateway-s2
```

Native loopback-only servers may not be reachable via the Docker Desktop host
address; use a deliberately private reachable interface/firewall if testing that
mixed topology. On Linux containers `localhost` is the container, not the host;
use reachable private service DNS or an explicitly configured host gateway.
The image builds/tests only `gateway/`, runs as a non-root user and has a readiness
healthcheck. `.dockerignore` allowlists source, pom and wrapper; no local config,
build output, credentials or database can enter the build context. S3 supplies
the broader private-network topology; S2 does not silently publish backend/DB ports.

Existing **Backend** CI now includes gateway clean verify, container build and
container no-upstream liveness/readiness/non-root smoke, with always-run cleanup
and report/log upload. **Frontend** includes browser-through-gateway E2E, all-three-port
cleanup checks and failure artifacts. Either failure blocks an existing required
check. Required branch protection remains owner-reported; check names are unchanged.

## Recovery and remaining gates

No schema/data move occurred. Stop gateway; restore the prior direct backend
API/proxy URL and set backend frontend/return/reset URL configuration to that
topology together. Rebuild React if its API base changed. Point the Stripe listener
or registered sandbox endpoint to the single active backend webhook route;
never run two payment handlers. Restarting backend expires its in-memory sessions.
Do not reset H2 files, MySQL data or volumes to repair routing.

See [S2 evidence and owner handoff](../docs/api-gateway-pr.md) for actual results.
Real Stripe sandbox smoke, other browsers, MySQL/Compose, distributed payment
reliability and dependency remediation remain separate gates/tasks. Next: S3.
