# Autostrada Auctions React Frontend

S4a: the shared request client obtains `GET /api/csrf`, keeps the token in memory
and sends `X-CSRF-TOKEN` on unsafe JSON/multipart requests. It refreshes after
login/logout and stale-token rejection; it never replays a mutation automatically.
Queued mutations are cancelled after an unexpected session change. Reload
reacquires a token using the session cookie. FormData boundaries remain browser
owned. See [contract and coordinated rollout](../docs/session-csrf.md) and
[actual evidence](../docs/session-csrf-pr.md). The new frontend needs the matching
backend; do not deploy either half alone.

S3: the [Compose baseline](../docs/docker-compose.md) builds an empty-API-base
bundle and serves it using non-root Nginx, with SPA fallback and private ingress.
From this directory `npm run test:compose` builds a unique disposable MySQL stack
and runs the browser suite through its gateway. Native `test:e2e` still uses H2.

This folder contains the React application that replaces the former JSP UI.

For S2, open the gateway public origin (`http://localhost:8081`) and use an empty
`VITE_API_BASE_URL`. See [gateway startup](../gateway/README.md). The API proxy
default now points to the gateway; no business API should bypass it in S2.

## Local development

```powershell
npm.cmd install
npm.cmd run dev
```

The Vite dev server runs on <http://localhost:5173>.

For an explicit direct-monolith fallback only, set the backend URL with:

```powershell
$env:VITE_API_BASE_URL = "http://localhost:8080"
```

## Build and test

```powershell
npm.cmd run build
npm.cmd run test
```

The React app consumes backend DTO APIs under `/api/**`. Public marketplace,
auth, user dashboard, cart/order, comments, and admin/store-admin screens are
routed in React.

## Browser E2E

Install dependencies with `npm ci`, then Chromium with `npm run test:e2e:install`
(`npx playwright install --with-deps chromium` on Linux). Run `npm run test:e2e`
for automatic isolated backend/frontend/gateway startup, fixtures, browser tests
through the gateway and cleanup. Gateway build failures also fail this command. JDK 17 and
Node.js 22 are required. See [the E2E runbook](../docs/browser-e2e.md) for local
commands, failure reports and provider-simulation limitations.
