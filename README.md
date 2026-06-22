# Subotin Motors

Subotin Motors is a Spring MVC marketplace for listing used cars, placing bids,
requesting test drives, and purchasing car parts.

## Technology

- Java 17+
- Spring Boot 3.5
- Spring MVC, Security, Data JPA, and Validation
- JSP and Bootstrap
- Flyway database migrations
- H2 for zero-configuration local development
- MySQL for deployment
- Stripe Checkout and signed webhooks for sandbox store payments

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
history, test-drive requests in each lifecycle state, followed auctions, and an
unread ending-soon notification. Seeded email addresses use the pattern
`username@subotinmotors.local`.

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

## Account email and password reset

Email is required, validated, unique, shown on profile/admin pages, and can be
updated from **Edit Profile**. Login remains username-based.

The login page links to **Forgot password?**. A user can enter either a username
or email. The application stores only a SHA-256 hash of the random reset token,
expires it after 30 minutes by default, and consumes it after a successful
password change. The response is intentionally the same whether or not an
account exists.

Local development uses safe log delivery, so the reset link appears in the
application log and no SMTP credentials are needed:

```powershell
$env:APP_MAIL_MODE = "log"
```

To opt into SMTP, provide credentials through environment variables; none are
stored in the repository:

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

## Auction timing, following, and notifications

Every listing has an auction end date/time. Public cards, detail pages, bid
pages, seller/admin tables, and watchlists show a live countdown. Auctions
within 24 hours of ending receive highlighted visual feedback; the final hour
uses a pulse animation. The server remains authoritative and rejects bids after
the deadline even if JavaScript is disabled.

Authenticated users can follow or unfollow an active auction from its detail
page and review the watchlist under **Followed Auctions**. Duplicate follows are
prevented by both service logic and a database constraint.

The application scans followed auctions and creates one in-app
`AUCTION_ENDING_SOON` notification per user and auction. Notifications have
read/unread state and appear in the account menu with an unread badge. Configure
the window and scan interval with:

```powershell
$env:AUCTION_ENDING_SOON_WINDOW = "PT24H"
$env:AUCTION_NOTIFICATION_SCAN_MS = "300000"
```

## Car-parts store and Stripe sandbox checkout

This integration is intentionally **sandbox-only**. It does not accept real
payments, and the application accepts only Stripe sandbox credentials
(`sk_test_`, `rk_test_`, or claimable-sandbox `rkcs_test_`). Never use real card
or identity information.

Stripe is disabled by default, so the application still runs without payment
credentials. The `Car Parts` catalog remains browsable, while the checkout
button explains how to enable the sandbox.

The store demonstrates a conventional e-commerce workflow:

1. Browse or search the seeded parts catalog.
2. Add one or more products to the persistent user cart.
3. Start one Stripe-hosted Checkout session for the complete cart.
4. Reserve inventory while Checkout is pending.
5. Mark the order paid only after a verified, idempotently processed webhook.
6. Restore reserved inventory when Checkout expires or reports failure.
7. Review customer order history or the administrator order register.

Auction winners are recorded as accepted sales and do not use online checkout.
There is no seller onboarding, connected-account requirement, split payout, or
identity-verification flow.

Set these environment variables:

```powershell
$env:STRIPE_ENABLED = "true"
$env:STRIPE_SECRET_KEY = "sk_test_replace_me"
$env:STRIPE_CURRENCY = "eur"
$env:APP_BASE_URL = "http://localhost:8080"
```

Store prices are persisted in minor currency units. No credentials are stored
in source control.

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

At Checkout, use Stripe's successful sandbox card:

- Card number: `4242 4242 4242 4242`
- Expiration: any future date
- CVC: any three digits
- Postal code: any valid-looking value

These values work only in Stripe sandboxes and move no real money.

Never mark a store order paid from the browser success redirect. The signed
webhook is the source of truth.

### One-command Windows sandbox startup

After installing the Stripe CLI and rotating any exposed sandbox key, run:

```powershell
.\scripts\run-stripe-sandbox.ps1
```

The script uses the private Stripe CLI sandbox profile when one is available,
including claimable-sandbox restricted credentials. Otherwise, it securely
prompts for a sandbox key. It obtains the temporary
webhook signing secret, starts webhook forwarding in the background, and starts
the application. It stores no credentials in the repository. Stop the
application with `Ctrl+C`; the background listener is stopped automatically.
Ngrok is not required for local Stripe CLI forwarding.

## Current features

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

## CRUD and lifecycle coverage

The controller/UI coverage, authorization rules, and intentional restrictions
for each persisted business area are documented in
[`docs/crud-coverage.md`](docs/crud-coverage.md). This includes user/profile,
car, bid, test-drive, parts, cart, store-order, and webhook-event management.
