# Autostrada Auctions Backend

S6 working-tree update: notification inbox and mail delivery now have a separate owner,
RabbitMQ and guarded data cutover. Read the [S6 runbook](../docs/notification-service.md) before starting
the new Compose stack; historical S5 startup/test instructions below need that cutover.


S4a: backend remains sole session owner. `GET /api/csrf` creates/reuses an
anonymous session and returns non-cacheable `{token}`. All unsafe API/legacy
requests require `X-CSRF-TOKEN` (or legacy `_csrf` body field); only exact signed
`POST /webhooks/stripe` is exempt. API/form login rotate JSESSIONID and invalidate
old tokens. `/api/session` stays unchanged. See [contract/runbook](../docs/session-csrf.md)
and [evidence](../docs/session-csrf-pr.md). No schema changes. Log-mail no longer
prints reset links; configure SMTP to deliver mail. S4b remains next.

S3: [container/MySQL runbook](../docs/docker-compose.md). `Dockerfile` defaults to
the non-root production JAR target. Compose activates `mysql,gateway,container`
on private ingress, uses MySQLDialect with Hibernate validation, and exposes
separate liveness/readiness (DB) probes. Existing migrations require deliberate
demo acknowledgement. Target `e2e` is harness-only; it is never the production image.

S2: backend remains the sole session and business owner behind the independent
[`gateway/`](../gateway/README.md). The opt-in `gateway` profile binds backend to
loopback, enables trusted forwarding/correlated logs and uses the public gateway
origin for default redirects/returns. Set `APP_FRONTEND_BASE_URL` and `APP_BASE_URL`
together. Existing default/direct startup remains available for recovery.
`scripts/run-stripe-sandbox.ps1 -PublicBaseUrl http://localhost:8081` sends both
return URLs and the CLI webhook listener through the gateway; its default remains
the backend port for direct development. No session/CSRF redesign or data move.

This folder contains the Spring Boot backend. The React frontend in `../front`
owns the UI; this backend provides REST APIs, persistence, security, Flyway
migrations, Stripe webhook handling, and legacy route redirects.

## Run

```powershell
.\mvnw.cmd spring-boot:run
```

## Test

```powershell
.\mvnw.cmd clean test
```

## Build

```powershell
.\mvnw.cmd clean package
java -jar target\autostrada-auctions-0.0.1-SNAPSHOT.jar
```

## API And Legacy Routes

React-facing API endpoints are under `/api`. Public marketplace read endpoints
are under `/api/public`. No JSP files remain; old MVC view names redirect to
the React frontend using `APP_FRONTEND_BASE_URL` for bookmark compatibility.

Flyway migrations are in `src/main/resources/db/migration`.

The browser baseline uses a separate launcher under `src/test/java/e2e`, excluded
from the production JAR. Start it through `npm run test:e2e` in `../front`, which
forces a fresh in-memory H2 database and cleans up its processes. See the
[E2E runbook](../docs/browser-e2e.md); normal startup does not expose test controls.

## Identity boundary (S4b)

Business code uses `CurrentIdentity`, `ProfileClient` and current-actor-only
`CheckoutProfileClient`; do not import identity entities/repositories or legacy
identity services into business code. Existing account ID columns/FKs remain.
`IdentityBoundaryArchitectureTests` enforces permitted persistence owners in the
normal Backend suite. [Contract and rollout](../docs/identity-boundary.md);
[actual verification and limitations](../docs/identity-boundary-pr.md).
