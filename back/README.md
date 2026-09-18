# Autostrada Auctions Backend

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
