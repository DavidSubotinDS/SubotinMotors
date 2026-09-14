# Autostrada Auctions React Frontend

This folder contains the React application that replaces the former JSP UI.

## Local development

```powershell
npm.cmd install
npm.cmd run dev
```

The Vite dev server runs on <http://localhost:5173>.

Set the backend URL with:

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
for automatic isolated startup, fixtures, browser tests and cleanup. JDK 17 and
Node.js 22 are required. See [the E2E runbook](../docs/browser-e2e.md) for local
commands, failure reports and provider-simulation limitations.
