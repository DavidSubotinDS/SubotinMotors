# Autostrada Auctions

Autostrada Auctions is now split into a Spring Boot backend and a React frontend.
The former JSP pages have been migrated to React routes under `front/`; the backend
serves REST APIs and redirects legacy page views to the React dev/prod URL.

## Repository Structure

```text
back/   Spring Boot backend, REST API, Flyway, tests
front/  React frontend built with Vite
docs/   Project notes and CRUD/lifecycle coverage
documentation/  Longer project documentation
images/ Demo screenshots and supporting documentation images
```

Backend API code belongs in `back/`. React UI code belongs in `front/`.

## Technology

- Backend: Java 17, Spring Boot 3.5, Spring Security, Spring Data JPA, Flyway
- Frontend: React, Vite, React Router
- Databases: H2 for local development and tests, MySQL profile for deployment-like use
- Payments: Stripe Checkout sandbox and signed webhooks

## Run The Backend

The default backend profile uses local H2. No database installation is required.

```powershell
cd back
.\mvnw.cmd spring-boot:run
```

The backend runs on <http://localhost:8080>. Open the React frontend at
<http://localhost:5173> for the application UI.

If an older Oracle Java entry appears first on `PATH`, set `JAVA_HOME` to a JDK
17 installation before running Maven.

Seeded accounts:

- Admin: `admin123` / `admin123`
- User: `user123` / `user123`
- Bidder-only demo: `demo_bidder` / `demo123`
- Seller-only demo: `demo_seller` / `demo123`
- Buyer-and-seller demo: `demo_trader` / `demo123`
- New user with no history: `demo_newcomer` / `demo123`

Local backend data is stored in `back/data/` and ignored by Git.

## Backend Tests And Package

```powershell
cd back
.\mvnw.cmd clean test
.\mvnw.cmd -DskipTests package
java -jar target\autostrada-auctions-0.0.1-SNAPSHOT.jar
```

The packaging command above assumes the preceding tests passed on the same
code. For a single command that compiles, tests, and packages, use
`.\mvnw.cmd clean verify`.

Tests use an in-memory H2 database with Stripe disabled or mocked. They do not
use the running development database. See the
[stabilization checklist](docs/stabilization-checklist.md) for verified checks
and the remaining steps before merging.

Flyway migrations live in `back/src/main/resources/db/migration`.

## Continuous Integration

GitHub Actions runs backend and frontend verification on branch pushes and
pull requests targeting `master`. Backend verification uses Java 17 and
`bash ./mvnw --batch-mode --no-transfer-progress clean verify` from `back/`.
Frontend verification uses Node.js 22, `npm ci`, `npm test`, and
`npm run build`, and the isolated Playwright browser suite from `front/`.
E2E is part of the existing `Frontend` check; both `Backend` and `Frontend`
check names are retained. Workflow runs retain test reports, the backend
JAR, and the frontend build as downloadable artifacts for seven days.

## Browser regression baseline

After `npm ci` and `npm run test:e2e:install` in `front/`, run
`npm run test:e2e`. It builds and starts isolated backend/frontend processes,
tests against a disposable in-memory database, and tears down after failure or
success. No Stripe secrets or developer database are used. See the
[browser E2E runbook](docs/browser-e2e.md) for coverage, known limitations,
failure artifacts, and the separate manual Stripe sandbox smoke.

## Run The React Frontend

```powershell
cd front
npm.cmd install
npm.cmd run dev
```

Open <http://localhost:5173>.

Frontend build and smoke test:

```powershell
cd front
npm.cmd run build
npm.cmd run test
```

Configure the backend URL with:

```powershell
$env:VITE_API_BASE_URL = "http://localhost:8080"
```

The backend allows the Vite dev origin by default. Override it with:

```powershell
$env:APP_CORS_ALLOWED_ORIGINS = "http://localhost:5173"
```

## React API Foundation

React-facing endpoints now cover the former JSP page surface:

- `GET /api/public/summary`
- `GET /api/public/auctions`
- `GET /api/public/auctions/{id}`
- `GET /api/public/listings`
- `GET /api/public/listings/{id}`
- `GET /api/public/parts`
- `GET /api/public/parts/{id}`
- `GET /api/public/part-categories`
- `GET /api/session`
- `/api/auth/**` for login, logout, registration, and password reset
- `/api/user/**` for profile, auctions, listings, bids, appointments, watchlists, notifications, and deposits
- `/api/store/**` for cart, checkout, and orders
- `/api/admin/**` for admin dashboards, moderation, inventory, orders, and transactions

They return DTOs rather than JPA entities. Sensitive account data such as
passwords and email addresses is not exposed by the session DTO.

## JSP Migration Status

No `.jsp` files remain in the project. JSP/JSTL/Jasper dependencies and Spring
MVC JSP view prefix/suffix configuration were removed from the backend.

Legacy MVC controllers are still present as compatibility shims for old form and
bookmark URLs. Non-redirect view names are resolved by a React redirect resolver
using `APP_FRONTEND_BASE_URL` (default `http://localhost:5173`). New UI work
should be implemented in React and use `/api/**`.

See [`docs/frontend-backend-separation.md`](docs/frontend-backend-separation.md)
for the migration boundary.

## MySQL Profile

```powershell
cd back
$env:SPRING_PROFILES_ACTIVE = "mysql"
$env:DB_URL = "jdbc:mysql://localhost:3306/autostrada_auctions?createDatabaseIfNotExist=true&serverTimezone=UTC"
$env:DB_USERNAME = "autostrada_auctions"
$env:DB_PASSWORD = "replace-me"
.\mvnw.cmd spring-boot:run
```

Never commit database passwords.

## Mail And Password Reset

Local development logs reset links by default:

```powershell
$env:APP_MAIL_MODE = "log"
```

SMTP mode:

```powershell
$env:APP_MAIL_MODE = "smtp"
$env:APP_MAIL_FROM = "no-reply@example.com"
$env:SMTP_HOST = "smtp.example.com"
$env:SMTP_PORT = "587"
$env:SMTP_USERNAME = "replace-me"
$env:SMTP_PASSWORD = "replace-me"
$env:SMTP_AUTH = "true"
$env:SMTP_STARTTLS = "true"
```

`PASSWORD_RESET_EXPIRY` accepts an ISO-8601 duration such as `PT30M`.

## Stripe Sandbox

Stripe is disabled by default. This project accepts only sandbox credentials
(`sk_test_`, `rk_test_`, or `rkcs_test_`).

```powershell
cd back
$env:STRIPE_ENABLED = "true"
$env:STRIPE_SECRET_KEY = "sk_test_replace_me"
$env:STRIPE_CURRENCY = "eur"
$env:APP_BASE_URL = "http://localhost:8080"
```

For local webhook forwarding:

```powershell
stripe listen --api-key $env:STRIPE_SECRET_KEY --forward-to localhost:8080/webhooks/stripe
```

Copy the `whsec_...` value, then run:

```powershell
cd back
$env:STRIPE_WEBHOOK_SECRET = "whsec_replace_me"
.\mvnw.cmd spring-boot:run
```

One-command Windows sandbox startup:

```powershell
.\back\scripts\run-stripe-sandbox.ps1
```

The script reads the test key from the Stripe CLI `default` profile unless
`STRIPE_SECRET_KEY` is explicitly set in the current terminal. After renewing
your CLI login, ignore any old terminal key with:

```powershell
.\back\scripts\run-stripe-sandbox.ps1 -UseCliLogin
```

Use `-StripeProfile "profile-name" -UseCliLogin` to select another saved CLI
profile. The script never selects a key from a different profile and clears
temporary credential variables even when startup fails.

The local listener forwards:

- `checkout.session.completed`
- `checkout.session.async_payment_succeeded`
- `checkout.session.async_payment_failed`
- `checkout.session.expired`

Never mark a store order paid from the browser success redirect. The signed
webhook is the source of truth.

## Current Backend Features

- Registration with unique validated email and BCrypt authentication
- Secure forgot-password and reset-token flow with log or SMTP mail delivery
- User and administrator roles
- Profile and image management
- Car listing and moderation
- Search by make, model, year, and price
- Timed auctions with countdowns and post-deadline bid rejection
- Auction watchlists and read/unread ending-soon notifications
- Test-drive requests with seller approval, rejection, rescheduling, and cancellation
- Searchable car-parts catalog with stock management
- Persistent shopping cart and customer order history
- Administrator product/inventory and store-order management
- Stripe-hosted checkout for complete store carts
- Signed, idempotent payment webhooks

CRUD coverage and authorization rules are documented in
[`docs/crud-coverage.md`](docs/crud-coverage.md).
