# Autostrada Auctions demo walkthrough

Last checked against the React routes on 2026-06-27.

This guide is meant for a live project demo. It goes through the application in
the same order a reviewer would naturally discover it:

1. guest / unregistered visitor
2. registered marketplace user
3. administrator

The canonical frontend URL is:

```text
http://localhost:5173
```

## 0. Start the application

In one terminal, start the backend:

```powershell
cd back
.\mvnw.cmd spring-boot:run
```

In another terminal, start the React frontend:

```powershell
cd front
npm.cmd run dev
```

Open:

```text
http://localhost:5173
```

If you want to demonstrate real Stripe Checkout redirects, start the backend
with sandbox Stripe enabled. The project already has a helper script:

```powershell
.\back\scripts\run-stripe-sandbox.ps1
```

If Stripe is not enabled, still show the cart and deposit screens, but explain
that payment completion is intentionally blocked unless sandbox keys are
configured.

## Seeded demo accounts

Use these accounts during the walkthrough. The final column describes the data
present on a fresh seeded database; actions performed during a demo can change
statuses or add more history.

| Purpose | Username | Display name | Password | Seeded demo data |
| --- | --- | --- | --- | --- |
| Administrator | `admin123` | Admin Admin | `admin123` | Admin and normal-user permissions, auction/part/order moderation, transaction history access, and seeded discussion replies. |
| Basic user | `user123` | User User | `user123` | No owned auction inventory; includes a rejected fixed-price test-ride request and a paid Lexus listing deposit for buyer-history examples. |
| Buyer-heavy user | `demo_bidder` | Mila Bidic | `demo123` | Owns no auctions. Has denied, cancelled, ongoing, paid, and accepted-pending-payment bids; buyer payments; auction test drives; listing test rides; and pending/expired listing deposits. |
| Auction seller | `demo_seller` | Stefan Prodic | `demo123` | Owns six auctions covering active, pending approval, hidden, sold, and reserved states: Toyota RAV4, Honda Civic, Volvo XC60, Ford Focus, Skoda Octavia, and Mercedes-Benz C200. Also has received test-drive requests and seller payment history. |
| Buyer and seller | `demo_trader` | Ana Trgovic | `demo123` | Owns BMW 330i, Mazda MX-5, Tesla Model 3, and Volkswagen Golf auctions. Also has ongoing, paid, and denied bids, buyer/seller payments, auction appointments, an accepted listing test ride, and a paid Land Rover deposit. |
| New-account example | `demo_newcomer` | Luka Novi | `demo123` | No auction or bid history. Useful for an almost-empty dashboard; includes one cancelled fixed-price test-ride record as a lifecycle edge case. |
| Fixed-price seller example | `demo_list_01` | Marko Kovacevic | `demo123` | Owns the active Toyota Corolla Hybrid Comfort listing and has an incoming pending test-ride request from Mila Bidic. |

Quick account choices:

- use `demo_bidder` to show bids, purchases, deposits, and buyer appointments
- use `demo_seller` to show auction ownership and received test-drive requests
- use `demo_trader` for the broadest mixed buyer/seller dashboard
- use `demo_newcomer` for an almost-empty account
- use `demo_list_01` to demonstrate fixed-price listing ownership

### Fixed-price seller account directory

Every account below has `ROLE_USER`, a complete shipping profile, the password
`demo123`, and one seeded fixed-price listing.

| Username | Display name | Owned listing | Initial status |
| --- | --- | --- | --- |
| `demo_list_01` | Marko Kovacevic | Toyota Corolla Hybrid Comfort | ACTIVE |
| `demo_list_02` | Jelena Petrovic | Honda Jazz 1.5 i-MMD Elegance | ACTIVE |
| `demo_list_03` | Nikola Ilic | Volkswagen Passat Variant TDI | ACTIVE |
| `demo_list_04` | Sara Jovanovic | Mercedes-Benz EQE 350+ | RESERVED |
| `demo_list_05` | Viktor Stankovic | BMW X1 xDrive20d Advantage | ACTIVE |
| `demo_list_06` | Milica Savic | Audi A4 Avant 35 TFSI | ACTIVE |
| `demo_list_07` | Dusan Markovic | Skoda Superb 2.0 TDI Style | SOLD |
| `demo_list_08` | Tamara Popovic | Renault Clio TCe Intens | ACTIVE |
| `demo_list_09` | Lazar Matic | Hyundai Tucson 1.6 T-GDi Hybrid | ACTIVE |
| `demo_list_10` | Ivana Ristic | Kia Sportage 1.6 CRDi EX | ACTIVE |
| `demo_list_11` | Filip Pavlovic | Ford Puma EcoBoost Titanium | INACTIVE |
| `demo_list_12` | Teodora Milosevic | Land Rover Defender 110 D250 | RESERVED |
| `demo_list_13` | Ognjen Lazic | Peugeot 3008 PureTech Allure | ACTIVE |
| `demo_list_14` | Mina Djordjevic | Nissan Qashqai DIG-T N-Connecta | ACTIVE |
| `demo_list_15` | Petar Vasic | Fiat 500 Dolcevita | ACTIVE |
| `demo_list_16` | Anja Nikolic | Volvo V90 B4 Momentum | ACTIVE |
| `demo_list_17` | Bogdan Zivkovic | Porsche Macan S | SOLD |
| `demo_list_18` | Una Stefanovic | Opel Astra 1.2 Turbo Edition | ACTIVE |
| `demo_list_19` | Andrej Cvetkovic | Dacia Duster Blue dCi Prestige | ACTIVE |
| `demo_list_20` | Nina Bogdanovic | Lexus NX 350h Executive | RESERVED |
| `demo_list_21` | Relja Radovic | Mini Cooper 1.5 Chili | ACTIVE |
| `demo_list_22` | Lena Tomic | Seat Leon Sportstourer FR | ACTIVE |
| `demo_list_23` | Mihajlo Peric | Alfa Romeo Giulia Veloce | ACTIVE |
| `demo_list_24` | Isidora Blagojevic | Tesla Model Y Long Range AWD | ACTIVE |

## 1. Guest / unregistered user walkthrough

Start logged out. If you are already logged in, click `Logout`.

### 1.1 Home / overview

Open:

```text
/
```

Show that the home page is a marketplace summary:

- featured live auctions
- fixed-price vehicle listings
- parts store items
- main navigation: `Auctions`, `Listings`, `Store`
- sidebar marketplace navigation

Explain that the public visitor can browse everything, but cannot place bids,
request appointments, comment, add to cart, or check out until signed in.

### 1.2 Auctions catalog

Open:

```text
/auctions
```

Demo steps:

1. Search for a make/model/year, for example `Volkswagen`, `BMW`, or `2020`.
2. Point out the auction card fields: year, status, price, end date, countdown,
   and `Open auction`.
3. Open one auction detail page.

On the auction detail page, show:

- automatic vehicle image gallery
- previous/next controls, image dots, and the image counter
- asking price
- highest bid
- auction end time
- seller name
- comments section

As a guest, the interactive actions are hidden. Say that after login the same
page adds bid, test-drive, follow, and comment actions.

The gallery changes image every five seconds, pauses while the visitor hovers
or uses its controls, and stays still when the operating system requests
reduced motion.

Canonical detail route:

```text
/auctions/{auctionId}
```

### 1.3 Fixed-price vehicle listings

Open:

```text
/listings
```

Demo steps:

1. Search for a listing, for example `Toyota`, `Tesla`, `BMW`, or `Hybrid`.
2. Open a listing detail page.

On the listing detail page, show:

- automatic vehicle image gallery with manual previous/next controls
- listing title and description
- make/model/year
- mileage
- fuel type
- transmission
- full price
- reservation deposit amount
- seller display name

As a guest, request-test-ride and deposit actions are hidden.

Canonical detail route:

```text
/listings/{listingId}
```

### 1.4 Parts store

Open:

```text
/parts
```

Demo steps:

1. Search for parts such as `brake`, `oil`, `filter`, or `battery`.
2. Open a part detail page.

On the part detail page, show:

- category
- SKU
- description
- price
- stock quantity
- comments

As a guest, the user can read comments, but cannot add the part to cart or post
new comments.

Canonical detail route:

```text
/parts/{partId}
```

### 1.5 Static public pages

Open:

```text
/about-us
/contact-us
```

Use these to show that the application has normal public content pages in
addition to marketplace features.

### 1.6 Public seller profile

Open, on a fresh seeded database:

```text
/profiles/4
```

This shows a public seller profile and the seller's active auctions. The
profile ID can vary if the local database has been changed, so treat this as an
optional page in the live demo.

### 1.7 Login, registration, and password reset

Open:

```text
/login
```

Show:

- username/password login
- link to `Forgot password`
- route to registration

Then open:

```text
/forgot-password
```

Explain that local development logs password-reset links instead of sending
email by default. The reset page itself is:

```text
/reset-password?token={generatedToken}
```

Then open:

```text
/register
```

You can either simply show the form or register a temporary user. If you create
one during a demo, use a unique username such as:

```text
demo_exam_01
```

After successful registration, the app goes to:

```text
/register/thank-you
```

### 1.8 Not-found page

Open:

```text
/not-a-real-page
```

Show that unknown routes land on a clean not-found page instead of a broken
browser error.

## 2. Registered user walkthrough

Log in as:

```text
demo_trader / demo123
```

Explain that logging in adds the account navigation section:

- My profile
- My auctions
- My listings
- Watchlist
- Appointments
- Bids
- Cart
- Orders

Also explain that all public marketplace pages now have extra actions.

### 2.1 Profile page

Open:

```text
/user/profile
```

Show:

- name
- email
- phone
- location
- shipping address status
- profile picture upload
- `Edit` button

Then open:

```text
/user/profile/edit
```

Show that the user can update identity and shipping fields. Shipping fields are
important because store checkout is blocked until the profile has a complete
shipping address.

### 2.2 Logged-in auction actions

Open:

```text
/auctions
```

Open an active auction that is not owned by the current user. On the detail
page, show:

- bid amount field and `Bid`
- `Request test drive`
- `Follow` / `Unfollow`
- signed-in comment form

Suggested talking points:

- bids are tied to the logged-in user
- duplicate or invalid test-drive dates are rejected by the backend
- followed auctions appear in the watchlist
- auction comments can include text and optionally an image

### 2.3 Watchlist

After following an auction, open:

```text
/user/followed-auctions
```

Show that followed auctions appear as normal auction cards. Open one from the
watchlist to prove it links back to the exact auction detail page.

Aliases:

```text
/user/favorites
/user/watchlist
```

### 2.4 Bids

Open:

```text
/user/bids
```

Show:

- auction vehicle linked to each bid
- bid amount
- bid status
- cancel action where available

If `demo_trader` does not show the richest data on your local database, switch
to:

```text
demo_bidder / demo123
```

That account has more seeded buyer-side bid history.

### 2.5 Appointments

Open:

```text
/user/appointments
```

Show the four appointment sections:

- booked auction test drives
- received auction test-drive requests
- booked listing test rides
- received listing test-ride requests

Important demo point: clicking an appointment row now opens the exact related
vehicle:

- auction appointments go to `/auctions/{auctionId}`
- fixed-price listing test rides go to `/listings/{listingId}`

Actions:

- buyers can cancel their own requests
- sellers can accept or reject received requests

Useful account switches:

- `demo_bidder / demo123` shows buyer-side booked requests
- `demo_seller / demo123` shows received auction test-drive requests
- `demo_list_01 / demo123` shows a fixed-price listing seller example

Aliases:

```text
/user/test-drive
/user/test-rides
```

### 2.6 Notifications

Open:

```text
/user/notifications
```

Show:

- notification message
- created date
- read/unread state
- `Read`
- `Mark all read`

This page is useful after following auctions or when the backend has generated
auction lifecycle notifications.

### 2.7 My auctions

Open:

```text
/user/auctions
```

Show:

- the user's posted auction vehicles
- status badge
- edit action
- activate action
- hide/deactivate action

Create a new auction:

```text
/user/auctions/new
```

Show required fields:

- make
- model
- year
- price
- auction end datetime
- up to eight vehicle images; the first selected image becomes the cover

Edit an existing auction from the card:

```text
/user/auctions/{auctionId}/edit
```

On edit, newly selected images are appended to the existing gallery. This
allows a seller to add more views without replacing the cover image.

Aliases:

```text
/user/my-posted-car
/user/my-posted-cars
/user/my-auctions
/user/post-car
/user/edit-posted-car?idCar={auctionId}
```

### 2.8 My fixed-price listings

Open:

```text
/user/listings
```

Show:

- listing title
- vehicle summary
- price
- status
- edit action
- activate/hide actions

Create a listing:

```text
/user/listings/new
```

Show required fields:

- title
- make
- model
- year
- mileage
- fuel type
- transmission
- full price
- deposit amount
- description
- up to eight vehicle images; the first selected image becomes the cover

Edit an existing listing:

```text
/user/listings/{listingId}/edit
```

Editing a listing can append more images to its existing gallery.

If the account has no seeded fixed-price listings, either create one live or
briefly switch to:

```text
demo_list_01 / demo123
```

Aliases:

```text
/user/my-listings
/user/car-listings
```

### 2.9 Logged-in fixed-price listing actions

Open:

```text
/listings
```

Open a listing not owned by the current user. Show:

- `Request test ride`
- `Place deposit`

If Stripe sandbox is enabled, `Place deposit` redirects to Stripe Checkout and
then back to:

```text
/listing-deposits/success?session_id={stripeSessionId}
```

If Stripe is disabled, explain that the button is intentionally disabled or the
backend blocks checkout.

### 2.10 Listing deposits

Open:

```text
/user/listing-deposits
```

Show:

- listing
- deposit amount
- status
- created date

Alias:

```text
/user/deposits
```

### 2.11 Parts cart and checkout

Open:

```text
/parts
```

Open a part, then click:

```text
Add to cart
```

Then open:

```text
/cart
```

Show:

- cart line items
- quantity
- line totals
- remove action
- total
- checkout action

If the profile does not have a complete shipping address, go back to
`/user/profile/edit` and complete street, city, postal code, and country.

If Stripe sandbox is enabled, click `Checkout`, complete Stripe Checkout, and
confirm the app returns to:

```text
/store/checkout/success?session_id={stripeSessionId}
```

The success page should show a human-readable order confirmation, not raw JSON.

### 2.12 Orders

Open:

```text
/orders
```

Show paged order history. Open an order:

```text
/orders/{orderId}
```

Show:

- customer
- shipping address
- total
- created date
- paid date / pending status
- purchased item lines

## 3. Administrator walkthrough

Log out, then log in as:

```text
admin123 / admin123
```

Explain that the admin account also has normal user permissions, but it receives
an extra `Admin` navigation group.

### 3.1 Admin users

Open:

```text
/admin/users
```

Show:

- regular user table
- admin table
- usernames
- emails
- profile names
- `Make admin` action

Aliases:

```text
/admin
/admin/dashboard
```

### 3.2 Auction moderation

Open:

```text
/admin/cars
```

Show:

- auction vehicles
- prices
- status badges
- a contextual `Approve`, `Show`, or `Hide` action based on the current status

Click a pending-approval vehicle row. It opens:

```text
/admin/cars/{auctionId}/preview
```

This admin-only preview uses the same automatic image gallery and auction
information layout as the marketplace detail page. Review every image, the
seller, asking price, highest bid, end time, and status, then approve the
request directly from the preview.

Then show the bids table:

- auction vehicle
- bidder
- bid amount
- bid status
- approve action
- deny action

Aliases:

```text
/admin/car-management
/admin/auctions
/admin/listings
```

### 3.3 Transactions and Stripe webhook log

Open:

```text
/admin/transactions
```

Show:

- payment/order ID
- amount
- status
- buyer
- seller
- webhook event table
- processed webhook event timestamps

This is the administrative proof that checkout is not just a frontend redirect:
completed payments are recorded as backend payment orders and signed webhook
events.

### 3.4 Store parts inventory

Open:

```text
/admin/store/parts
```

Show:

- SKU
- part name
- category
- price
- stock quantity
- edit action
- current `ACTIVE` / `HIDDEN` visibility
- one contextual `Hide` or `Show` action, so hidden products stay recoverable

Create a new part:

```text
/admin/store/parts/new
```

Show fields:

- SKU
- name
- category
- price minor
- stock quantity
- image URL
- description

Edit an existing part:

```text
/admin/store/parts/{partId}/edit
```

Aliases:

```text
/admin/store
/admin/parts
```

### 3.5 Store orders

Open:

```text
/admin/store/orders
```

Show:

- all customer orders, not just the current user
- order status
- total
- created date
- customer

Open an order:

```text
/admin/store/orders/{orderId}
```

Show the same order-detail view, but loaded through the admin API.

Alias:

```text
/admin/orders
```

## 4. How Stripe is implemented

Stripe is integrated as a backend-owned sandbox payment flow. The React app
never receives the secret key and never decides that a payment succeeded. The
current UI demonstrates two Stripe Checkout paths:

1. a complete parts-store cart order
2. a reservation deposit for a fixed-price vehicle listing

Auction-bid checkout is intentionally retired from the browser demo; bidding
and moderation remain available, while online payment is demonstrated through
the store and listing-deposit workflows.

### 4.1 Checkout creation flow

For a parts order, React calls:

```text
POST /api/store/checkout
```

The backend verifies the signed-in user, requires a complete shipping address,
checks that every product is active and in stock, creates an order snapshot,
reserves inventory, and asks the Stripe gateway to create a Checkout Session.
The cart is converted into Stripe line items using server-side names, SKUs,
quantities, and prices. React receives only the hosted Checkout URL and redirects
the browser to Stripe.

For a vehicle reservation, React calls:

```text
POST /api/user/listings/{listingId}/deposit
```

The backend locks and validates the listing, prevents self-reservations and
duplicate active deposits, creates a deposit record, marks the listing
`RESERVED`, and creates a Stripe Checkout Session for the configured deposit
amount.

### 4.2 Return pages are not payment proof

Stripe sends the browser back with its Checkout Session ID:

```text
/store/checkout/success?session_id={CHECKOUT_SESSION_ID}
/listing-deposits/success?session_id={CHECKOUT_SESSION_ID}
```

Those pages load only an order or deposit that belongs to the signed-in user.
They present the current backend status, but the success redirect itself never
marks anything paid. This prevents a user from faking payment by manually
opening a success URL.

### 4.3 Signed, idempotent webhooks

Stripe CLI forwards events to:

```text
POST /webhooks/stripe
```

The backend verifies the `Stripe-Signature` header with the configured
`whsec_...` secret before processing the payload. It handles:

- `checkout.session.completed`
- `checkout.session.async_payment_succeeded`
- `checkout.session.async_payment_failed`
- `checkout.session.expired`

Processed Stripe event IDs are stored, so retrying the same webhook does not
apply the payment twice. A paid event marks the matching order or deposit
`PAID`. Failure or expiry releases a listing reservation or restores store
inventory. The admin transactions page exposes the persisted payment and
webhook audit trail.

### 4.4 Sandbox configuration and demo

The project rejects live Stripe credentials and accepts only test/restricted
test keys. The key settings are supplied through environment variables rather
than committed source code:

```text
STRIPE_ENABLED=true
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_CURRENCY=eur
APP_BASE_URL=http://localhost:8080
```

For a live demo, use the helper:

```powershell
.\back\scripts\run-stripe-sandbox.ps1
```

Then add a part to the cart or open an active fixed-price listing, start
Checkout, use a Stripe test card, and return to the human-readable confirmation
page. Finish at `/admin/transactions` to show that the signed webhook—not the
browser redirect—recorded the result.

## 5. Canonical page checklist

Use this list when checking that the demo touched every important screen.

### Public / guest

- `/`
- `/auctions`
- `/auctions/{auctionId}`
- `/listings`
- `/listings/{listingId}`
- `/parts`
- `/parts/{partId}`
- `/about-us`
- `/contact-us`
- `/profiles/{idProfile}`
- `/login`
- `/forgot-password`
- `/reset-password?token={token}`
- `/register`
- `/register/thank-you`
- unknown route, for example `/not-a-real-page`

### Registered user

- `/user/profile`
- `/user/profile/edit`
- `/user/auctions`
- `/user/auctions/new`
- `/user/auctions/{auctionId}/edit`
- `/user/listings`
- `/user/listings/new`
- `/user/listings/{listingId}/edit`
- `/user/followed-auctions`
- `/user/notifications`
- `/user/appointments`
- `/user/bids`
- `/user/listing-deposits`
- `/cart`
- `/orders`
- `/orders/{orderId}`
- `/store/checkout/success?session_id={stripeSessionId}`
- `/listing-deposits/success?session_id={stripeSessionId}`

### Administrator

- `/admin/users`
- `/admin/cars`
- `/admin/cars/{auctionId}/preview`
- `/admin/transactions`
- `/admin/store/parts`
- `/admin/store/parts/new`
- `/admin/store/parts/{partId}/edit`
- `/admin/store/orders`
- `/admin/store/orders/{orderId}`

## 6. Route aliases and legacy-compatible URLs

The app keeps several old or alternate URLs so bookmarks from the JSP version
still land on the React screens. During the demo, use the canonical URL unless
you specifically want to mention backwards compatibility.

| Screen | Canonical route | Aliases |
| --- | --- | --- |
| Auctions catalog | `/auctions` | `/cars`, `/live-auctions`, `/browse-auctions`, `/auction-listings` |
| Auction detail | `/auctions/{id}` | `/auction/{id}`, `/cars/{make}/{model}/{year}/{id}`, `/test-drive/{id}`, `/car-bid?idCar={id}` |
| Fixed-price listings | `/listings` | `/car-listings`, `/cars-for-sale`, `/vehicle-listings` |
| Listing detail | `/listings/{id}` | `/car-listings/{id}`, `/cars-for-sale/{id}` |
| Parts catalog | `/parts` | `/store`, `/store/parts`, `/car-parts`, `/parts-store` |
| Part detail | `/parts/{id}` | `/store/parts/{id}`, `/car-parts/{id}` |
| Public profile | `/profiles/{idProfile}` | `/view-user/{firstName}/{idProfile}` |
| Profile | `/user/profile` | `/user`, `/user/my-profile`, `/user/upload-picture` |
| Profile edit | `/user/profile/edit` | `/user/edit-profile` |
| My auctions | `/user/auctions` | `/user/my-posted-car`, `/user/my-posted-cars`, `/user/my-auctions` |
| New auction | `/user/auctions/new` | `/user/post-car` |
| Auction edit | `/user/auctions/{id}/edit` | `/user/edit-posted-car?idCar={id}` |
| My listings | `/user/listings` | `/user/my-listings`, `/user/car-listings` |
| Watchlist | `/user/followed-auctions` | `/user/favorites`, `/user/watchlist` |
| Appointments | `/user/appointments` | `/user/test-drive`, `/user/test-rides` |
| Deposits | `/user/listing-deposits` | `/user/deposits` |
| Admin users | `/admin/users` | `/admin`, `/admin/dashboard` |
| Admin auction moderation | `/admin/cars` | `/admin/car-management`, `/admin/auctions`, `/admin/listings` |
| Admin store parts | `/admin/store/parts` | `/admin/store`, `/admin/parts` |
| Admin store orders | `/admin/store/orders` | `/admin/orders` |

## 7. Quick demo order if time is short

If you only have a few minutes, use this compact route order:

1. Guest: `/`, `/auctions`, auction detail, `/listings`, listing detail,
   `/parts`, part detail, `/login`.
2. User: log in as `demo_trader`, show `/user/profile`, bid/follow an auction,
   `/user/followed-auctions`, `/user/appointments`, `/cart`, `/orders`.
3. Admin: log in as `admin123`, show `/admin/users`, `/admin/cars`,
   `/admin/transactions`, `/admin/store/parts`, `/admin/store/orders`.

That covers the public marketplace, authenticated user workflows, payments, and
administrative moderation without getting stuck in every alias route.
