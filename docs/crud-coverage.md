# CRUD and lifecycle coverage

Spring Data repositories inherit low-level database CRUD methods, but the
application does not expose those methods directly. Controllers call services
that enforce ownership, roles, and valid marketplace state transitions.

The backend now lives in `back/`. The new React frontend in `front/` consumes
DTO-based API endpoints under `/api/**`; JSP routes listed below remain legacy
compatibility routes until equivalent React flows exist.

## Demonstrable operations

| Area | Create | Read | Update | Delete or lifecycle removal |
| --- | --- | --- | --- | --- |
| Users and profiles | Public two-step registration at `/register/account` and `/register/profile`, submitted to `/register/accountProcess` and `/register/profileProcess` | Users view their own profile and email at `/user/my-profile`; public profiles use `/view-user/...`; administrators list users at `/admin/dashboard` | Users edit their own profile and unique email at `/user/edit-profile`; administrators edit profiles and grant the admin role from `/admin/dashboard` | Hard delete is intentionally not exposed because users can own cars, bids, test drives, and financial records. Preserving the account keeps those relationships attributable. |
| Password resets | `/forgot-password` creates a hashed, expiring token and sends or logs a reset link without revealing whether the account exists | `/reset-password?token=...` validates the token before showing the password form | A valid token changes the BCrypt password and records `consumed_at` | Old tokens for the account are removed when a new request is made; consumed or expired tokens cannot be reused. |
| Cars | Authenticated sellers create a `PENDING` listing at `/user/post-car` | Public catalog/detail pages, seller's `/user/my-posted-car`, and administrator `/admin/car-management` | Administrators approve or reject pending listings; owners edit details/pictures and activate/deactivate listings after review | Deactivation is the application-level soft delete. Sold/reserved cars and their bid/payment history are retained, so hard delete is not exposed. |
| Bids | Authenticated buyers place bids from `/car-bid` | Buyers list their own bids at `/user/bids`; administrators list all managed bids at `/admin/car-management` | Buyers may cancel an `ONGOING` bid; administrators deny it or accept the winner, mark the car sold, and deny competing bids | Bids are never hard-deleted. `CANCELLED`, `DENIED`, and `ACCEPTED` preserve auction history. Older seeded payment statuses remain readable as legacy history. |
| Test drives | Authenticated buyers request a date from `/test-drive/{carId}` with `PENDING` status | `/user/test-drive` shows the buyer's bookings and requests received for cars the user owns, including their status | Owners accept or reject pending requests; requesters can reschedule pending or accepted bookings, with accepted reschedules returning to `PENDING` for approval | Requesters cancel pending or accepted bookings, and owners may cancel accepted appointments, by changing them to `CANCELLED`. Requests are retained as workflow history, and every action checks requester or car ownership. |
| Car parts | Administrators add products at `/admin/store/parts/new`; migration V7 seeds a demonstration catalog | Everyone browses `/parts`; administrators see complete inventory at `/admin/store/parts` | Administrators edit SKU, name, category, description, price, stock, visibility, and optional image URL | Products are hidden/activated rather than hard-deleted so historical order lines retain valid references. |
| Shopping cart | Authenticated users add active in-stock products from product pages | `/cart` lists only the current user's cart | Users change quantities; adding the same part merges into the existing row and stock limits are enforced | Users remove individual rows; successful checkout clears the cart after creating immutable order-line snapshots. |
| Store orders/payments | Checkout converts the current cart into an inventory-reserving order and one Stripe Checkout session | Customers see `/orders`; administrators see `/admin/store/orders` | Verified Stripe webhooks mark orders paid, failed, or expired; failed/expired orders restore inventory | Arbitrary edit/delete is intentionally unavailable. Financial records and order-line price/name snapshots remain available for reconciliation. |
| Legacy auction payments | No longer created from the UI | Administrators can inspect historical rows at `/admin/transactions` | Existing webhook compatibility is retained for historical Checkout sessions | No new seller onboarding or bid checkout is exposed. |
| Webhook events | Created only after a Stripe payload passes signature verification and matches an order | Administrators can inspect legacy events with the transaction register; store orders show provider identifiers | None | None. These rows are an idempotency and audit ledger. Allowing UI edits or deletion could cause duplicate processing or hide payment evidence. |
| Auction follows | Authenticated users follow active auctions from a car detail page | `/user/followed-auctions` lists the current user's watchlist | None | Users unfollow an auction; a unique user/car constraint prevents duplicates. |
| Auction notifications | The scheduled notification service creates one ending-soon notification for each follower | `/user/notifications` lists notifications and the navbar shows an unread count | Users mark one or all notifications read | Notifications are retained as account history; duplicate user/car/type rows are prevented. |

## Authorization and integrity rules

- `/admin/**` management routes require `ROLE_ADMIN`; `/user/**`, bidding, test
  drive, cart, order, and checkout routes require an authenticated `ROLE_USER`.
- Car edits require ownership. Bid cancellation and test-drive
  reschedule/cancellation require ownership of the bid or booking. Test-drive
  acceptance/rejection requires ownership of the associated car.
- Bids can be cancelled or denied only while `ONGOING`.
- Test-drive dates cannot be in the past, and duplicate bookings for the same
  user, car, and date are rejected.
- Test-drive transitions are limited to `PENDING` -> `ACCEPTED`/`REJECTED`,
  pending or accepted requester cancellation -> `CANCELLED`, owner cancellation
  of an accepted appointment -> `CANCELLED`, and accepted reschedule -> `PENDING`.
- Cars cannot be bid on or booked when inactive or after the auction deadline,
  and users cannot bid on or book their own car.
- Auction state is derived from the listing lifecycle status and end time:
  active, ending soon, ended, or sold. UI countdowns are informational; service
  checks enforce the deadline.
- Store inventory is checked again and reserved transactionally when Checkout
  starts. Failed or expired sessions restore it once.
- Payment completion is accepted only from a verified webhook, never from the
  browser success redirect.

This is deliberate lifecycle CRUD: mutable marketplace data has user-facing
management operations, while historical and provider-managed data is exposed
as read-only and changed only by trusted workflows.
