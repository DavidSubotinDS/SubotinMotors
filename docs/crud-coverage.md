# CRUD and lifecycle coverage

Spring Data repositories inherit low-level database CRUD methods, but the
application does not expose those methods directly. Controllers call services
that enforce ownership, roles, and valid marketplace state transitions.

## Demonstrable operations

| Area | Create | Read | Update | Delete or lifecycle removal |
| --- | --- | --- | --- | --- |
| Users and profiles | Public two-step registration at `/register/account` and `/register/profile`, submitted to `/register/accountProcess` and `/register/profileProcess` | Users view their own profile at `/user/my-profile`; public profiles use `/view-user/...`; administrators list users at `/admin/dashboard` | Users edit their own profile at `/user/edit-profile`; administrators edit profiles and grant the admin role from `/admin/dashboard` | Hard delete is intentionally not exposed because users can own cars, bids, test drives, and financial records. Preserving the account keeps those relationships attributable. |
| Cars | Authenticated sellers create a listing at `/user/post-car` | Public catalog/detail pages, seller's `/user/my-posted-car`, and administrator `/admin/car-management` | Owners edit details/pictures and activate/deactivate their listings; administrators can moderate active/deactive state | Deactivation is the application-level soft delete. Sold/reserved cars and their bid/payment history are retained, so hard delete is not exposed. |
| Bids | Authenticated buyers place bids from `/car-bid` | Buyers list their own bids at `/user/bids`; administrators list all managed bids at `/admin/car-management` | Buyers may cancel an `ONGOING` bid; administrators deny an ongoing bid or accept it into the payment lifecycle | Bids are never hard-deleted. `CANCELLED`, `DENIED`, `ACCEPTED_PENDING_PAYMENT`, `PAID`, `PAYMENT_FAILED`, and `EXPIRED` statuses preserve auction and payment history. |
| Test drives | Authenticated buyers request a date from `/test-drive/{carId}` | `/user/test-drive` shows the buyer's bookings and requests received for cars the user owns | The requester can reschedule a booking while the car remains active | The requester can cancel a booking, which deletes the appointment row because it is not a financial/audit record. Ownership is checked before update or delete. |
| Orders/payments | An administrator accepting a valid bid creates the order; a buyer creates Stripe Checkout from `/user/payments` | Buyers and sellers see their own orders at `/user/payments`; administrators have a read-only transaction register at `/admin/transactions` | Status changes are workflow-controlled: Checkout creation updates the order, and verified Stripe webhooks mark it paid, failed, or expired | Arbitrary edit/delete is intentionally unavailable. Financial records are immutable except for defined state transitions and must remain available for reconciliation. |
| Payment accounts | Created on first seller onboarding request | The owner sees connection status on profile/payment pages | Refreshed from Stripe after hosted onboarding | No manual edit/delete endpoint: provider IDs and capability state come from Stripe and are system-managed. |
| Webhook events | Created only after a Stripe payload passes signature verification | Administrators can inspect the latest events in the read-only section of `/admin/transactions` | None | None. These rows are an idempotency and audit ledger. Allowing UI edits or deletion could cause duplicate processing or hide payment evidence. |

## Authorization and integrity rules

- `/admin/**` management routes require `ROLE_ADMIN`; `/user/**`, bidding, test
  drive, and payment routes require an authenticated `ROLE_USER`.
- Car edits require ownership. Bid cancellation and test-drive
  reschedule/cancellation require ownership of the bid or booking.
- Bids can be cancelled or denied only while `ONGOING`.
- Test-drive dates cannot be in the past, and duplicate bookings for the same
  user, car, and date are rejected.
- Cars cannot be bid on or booked when inactive, and users cannot bid on or
  book their own car.
- Payment completion is accepted only from a verified webhook, never from the
  browser success redirect.

This is deliberate lifecycle CRUD: mutable marketplace data has user-facing
management operations, while historical and provider-managed data is exposed
as read-only and changed only by trusted workflows.
