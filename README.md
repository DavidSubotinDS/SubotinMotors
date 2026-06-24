# Autostrada Auctions

Autostrada Auctions is being split into a Spring Boot backend and a React frontend.
The legacy JSP UI is still present for compatibility while React pages and API
coverage are built out.

## Repository Structure

```text
back/   Spring Boot backend, MVC/JSP compatibility views, REST API, Flyway, tests
front/  React frontend built with Vite
docs/   Project notes and CRUD/lifecycle coverage
documentation/  Longer project documentation
images/ Demo screenshots and supporting documentation images
```

Backend API code belongs in `back/`. React UI code belongs in `front/`.

## Technology

- Backend: Java 17, Spring Boot 3.5, Spring MVC, Spring Security, Spring Data JPA, Flyway
- Frontend: React, Vite, React Router
- Legacy compatibility: JSP, JSTL, Bootstrap assets under `back/src/main/webapp/view`
- Databases: H2 for local development and tests, MySQL profile for deployment-like use
- Payments: Stripe Checkout sandbox and signed webhooks

## Run The Backend

The default backend profile uses local H2. No database installation is required.

```powershell
cd back
.\mvnw.cmd spring-boot:run
```

Open <http://localhost:8080>.

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
.\mvnw.cmd clean package
java -jar target\autostrada-auctions-0.0.1-SNAPSHOT.war
```

Flyway migrations live in `back/src/main/resources/db/migration`.

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

The first React-facing endpoints are public read endpoints:

- `GET /api/public/summary`
- `GET /api/public/auctions`
- `GET /api/public/listings`
- `GET /api/public/parts`
- `GET /api/public/part-categories`
- `GET /api/session`

They return DTOs rather than JPA entities. Sensitive account data such as
passwords and email addresses is not exposed by the session DTO.

## JSP Compatibility Status

JSP is no longer the target frontend technology. The JSP views remain under
`back/src/main/webapp/view` so existing behavior keeps working while React
replacement pages are developed.

Still JSP-backed in this branch:

- Login, registration, password reset, profile editing, and image uploads
- Bidding, comments, auction follow actions, and test-drive/test-ride workflows
- Cart, checkout, order history, and Stripe redirect flows
- Admin dashboard, car moderation, store inventory, and store order details

React currently provides the application shell, navigation, marketplace summary,
auction/listing/store browse pages, API client layer, and migration status page.

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
