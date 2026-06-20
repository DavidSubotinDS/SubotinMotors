# Subotin Motors

Subotin Motors is a Spring MVC marketplace for listing used cars, placing bids, and requesting test drives.

## Technology

- Java 17+
- Spring Boot 3.5
- Spring MVC, Security, Data JPA, and Validation
- JSP and Bootstrap
- Flyway database migrations
- H2 for zero-configuration local development
- MySQL for deployment
- Stripe Connect Accounts v2 and Stripe Checkout for marketplace payments

## Run locally

The default profile uses a local H2 database. No database installation or credentials are required.

Ensure `java -version` reports Java 17 or newer. On Windows, an older Oracle Java
entry can appear earlier on `PATH`; point `JAVA_HOME` and `PATH` to the JDK used
by Maven if that happens.

```powershell
.\mvnw.cmd spring-boot:run
```

Open <http://localhost:8080>.

Seeded accounts:

- Admin: `admin123` / `admin123`
- User: `user123` / `user123`
- Bidder-only demo: `demo_bidder` / `demo123`
- Seller-only demo: `demo_seller` / `demo123`
- Buyer-and-seller demo: `demo_trader` / `demo123`
- New user with no history: `demo_newcomer` / `demo123`

The demo data includes active auctions, listings with no bids yet, listings
awaiting administrator approval, inactive/reserved/sold cars, bid and payment
history, and test-drive requests in each lifecycle state.

Local data is stored in `data/` and is ignored by Git.

## Run tests

```powershell
.\mvnw.cmd clean test
```

Tests use a separate in-memory H2 database.

## Build an executable application

```powershell
.\mvnw.cmd clean package
java -jar target\abc-cars-0.0.1-SNAPSHOT.war
```

## Use MySQL

Create a restricted MySQL user and provide credentials through environment variables:

```powershell
$env:SPRING_PROFILES_ACTIVE = "mysql"
$env:DB_URL = "jdbc:mysql://localhost:3306/abc_cars?createDatabaseIfNotExist=true&serverTimezone=UTC"
$env:DB_USERNAME = "abc_cars"
$env:DB_PASSWORD = "replace-me"
.\mvnw.cmd spring-boot:run
```

Never commit database passwords. Flyway applies migrations from
`src/main/resources/db/migration`.

## Stripe Connect sandbox (school project only)

This integration is intentionally **sandbox-only**. It does not accept real
payments, and the application rejects every Stripe secret key that does not
start with `sk_test_`. Never use real card or identity information.

Stripe is disabled by default, so the application still runs without payment
credentials. Because this project uses Stripe Accounts v2, create a **general
Sandbox** from the Stripe Dashboard rather than relying on the built-in test
mode, which only partially supports v2.

1. Create or sign in to a Stripe account.
2. Open <https://dashboard.stripe.com/sandboxes>.
3. Select **Create sandbox**, give it a school-project name, and enter it using
   the Dashboard account picker.
4. In that sandbox, enable/configure **Connect** as a marketplace/platform.
5. Open **Developers > API keys** inside the sandbox and copy the sandbox secret
   key beginning with `sk_test_`.
6. Do not copy or use any key beginning with `sk_live_`.

Set these environment variables:

```powershell
$env:STRIPE_ENABLED = "true"
$env:STRIPE_SECRET_KEY = "sk_test_replace_me"
$env:STRIPE_CURRENCY = "eur"
$env:STRIPE_PLATFORM_FEE_BPS = "250"
$env:APP_BASE_URL = "http://localhost:8080"
```

`STRIPE_PLATFORM_FEE_BPS=250` means a 2.5% marketplace fee. Prices and bids are
stored as whole major currency units and converted to minor units before being
sent to Stripe.

For local webhook testing, install the Stripe CLI. In terminal 1, forward
events using the same sandbox key:

```powershell
stripe listen --api-key $env:STRIPE_SECRET_KEY --forward-to localhost:8080/webhooks/stripe
```

Copy the `whsec_...` value printed by `stripe listen`. In terminal 2, set it and
start the application:

```powershell
$env:STRIPE_WEBHOOK_SECRET = "whsec_replace_me"
.\mvnw.cmd spring-boot:run
```

The local listener forwards these events:

- `checkout.session.completed`
- `checkout.session.async_payment_succeeded`
- `checkout.session.async_payment_failed`
- `checkout.session.expired`

For this school project, there is no production endpoint.

### Sandbox test data

During connected-account onboarding, use fictional data only. Stripe documents
`000-000` as the SMS code for sandbox connected accounts. Follow Stripe's
Connect testing guide for its verification tokens.

At Checkout, use Stripe's successful sandbox card:

- Card number: `4242 4242 4242 4242`
- Expiration: any future date
- CVC: any three digits
- Postal code: any valid-looking value

These values work only in Stripe sandboxes and move no real money.

The payment workflow is deliberately webhook-driven:

1. A seller completes Stripe-hosted payout onboarding.
2. An administrator accepts a bid, reserving rather than selling the car.
3. The buyer opens Stripe Checkout from **Payments**.
4. A verified, idempotently processed Stripe webhook marks the bid paid and the
   car sold.
5. Failed or expired Checkout sessions release the reservation.

Never mark a car sold from a browser redirect. The signed webhook is the source
of truth.

## Current features

- Registration and BCrypt authentication
- User and administrator roles
- Profile and image management
- Car listing and moderation
- Search by make, model, year, and price
- Bidding
- Test-drive requests with seller approval, rejection, rescheduling, and cancellation
- Stripe-hosted seller onboarding
- Accepted-bid checkout with destination charges and platform fees
- Signed, idempotent payment webhooks

## CRUD and lifecycle coverage

The controller/UI coverage, authorization rules, and intentional restrictions
for each persisted business area are documented in
[`docs/crud-coverage.md`](docs/crud-coverage.md). This includes user/profile,
car, bid, test-drive, order/payment, payment-account, and webhook-event
management.
