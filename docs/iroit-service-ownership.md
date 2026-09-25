# IROIT source inventory and data ownership

> 2026-09-25 implementation update: the owner deferred further S9–S11 extraction.
> The observability branch adds private metrics, sanitized HTTP traces and curated
> request logs for the existing four application owners and gateway, plus opt-in
> local CD checks. Route, data and session ownership are unchanged. RabbitMQ trace
> propagation and the broader proposed S12 operational dashboards remain future
> work. See [actual behavior, evidence and limitations](observability.md)
> and [course defence guide](observability-defense.md); proposed chapters below are
> not evidence that those future capabilities exist.

**Selected course scope (2026-09-25):** S8 is merged at
`5ee4cb8b5c0445b8ffded0577709475cd876af8e`; Backend and Frontend passed on that
exact commit. Its local CD run failed; deployment success is not established.
The owner has deferred S9-S11 service extraction. The selected delivery keeps
backend, identity, notification and payment as four application owners, plus
the gateway. Commerce/marketplace coupling remains in backend. Continue with
static analysis, observability/reactive evidence, then defence preparation.
This decision supersedes later "next S9" statements and the five-service target;
the extraction chapters remain a future design, not acceptance requirements
for this delivery. See [the current requirement matrix](iroit-requirements-status.md).
Earlier dated paragraphs below are historical records.


Current status (2026-09-24): S7 is merged at `42cde105`. S8 is implemented in
the working tree. Payment-service owns provider integration, signed receipts,
reconciliation, its attempt/outbox tables and copied historical payment audit.
Backend owns stock/order/listing/deposit business state and an idempotent result
inbox; its old checkout attempt is a coexistence projection. See
[implemented S8 ownership](payment-service.md). S9 and later ownership remains
proposed.

Current status (2026-09-22): S5 is merged at
`cddde6da41d32d3d37fae9a8eaa71cc4027cca73`, with Backend and Frontend success
verified on that exact commit (Actions run 35673771885). Identity owns sessions,
authentication, accounts and profiles. S6 notification extraction is implemented
and locally accepted in the working tree; see [current ownership/runbook](notification-service.md)
and [actual evidence and pending gates](notification-service-pr.md).
Older dated S4a/S4b/S5 status paragraphs below are historical. Proposed later-stage
architecture does not establish implemented behavior; the S6 runbook takes
precedence for current route, data, session and mail-delivery ownership.

S4a: backend still owns the only session, CSRF repository and security decisions.
`SessionApiController` now also owns `GET /api/csrf`; API and form login rotate
session ID and clear the old token. No entity/repository/table ownership or
Flyway history changed. [Request inventory and lifecycle](session-csrf.md).
S4b scalar identity references/profile clients remain unimplemented and next.

S3 implemented topology: [Compose](docker-compose.md) places the same backend
and unchanged V1-V18 migration history in one MySQL schema. Only backend receives
that schema's credential; MySQL root credentials stay with MySQL. Gateway and
frontend have no DB credentials or ports into the database network. Test resets
use a separate random schema/volume and explicit test image, never normal data.
No table/entity ownership moved; extraction inventory below remains proposed.

S2 implementation: [`gateway/`](../gateway/README.md) owns only routing and edge
transport hygiene. Every existing business/API/webhook operation remains owned
by `back/`, including sessions and the single business database. No entity,
table, migration history or Stripe handler was moved. Canonical React GET/HEAD
pages are served from `front/`; legacy action methods still reach backend.
The ownership inventory below describes future extraction, not current services.

Proposed ownership, inspected at `c74e283806d82fc0806e159fa259ce028a4db120`.
See the [architecture decision record](iroit-architecture.md) for status and
requirements provenance. Paths below are relative to the repository root.

## Entity and table inventory

Sources: [entity classes](../back/src/main/java/lithan/autostrada/auctions/entity)
and [Flyway V1-V18](../back/src/main/resources/db/migration). All 25 mapped
entities are covered below. There is no independent category table: category
is a value on `CarPart`. `Role` maps directly to `tb_role`, including `id_user`;
there is no extra user-role join table.

| Current entity | Current table | Future owner and transformation |
| --- | --- | --- |
| `UserAccount` | `tb_user` | identity; preserve account ID, username, email and password hash |
| `UserProfile` | `tb_user_profile` | identity; preserve distinct profile ID and shipping fields |
| `Role` | `tb_role` | identity; existing `ROLE_USER` / `ROLE_ADMIN` membership |
| `ProfilePicture` | `tb_profile_picture` | identity; copy stored image bytes and metadata |
| `PasswordResetToken` | `tb_password_reset_token` | identity; hash/expiry/consumption remain private |
| `Car` | `tb_car` | marketplace; seller becomes scalar `userId` |
| `CarPicture` | `tb_car_picture` | marketplace; primary image |
| `CarGalleryPicture` | `tb_car_gallery_picture` | marketplace; preserve gallery order |
| `CarBidding` | `tb_car_bid` | marketplace; bidder ID reference, local car relationship |
| `TestDrive` | `tb_test_drive` | marketplace; auction test drive and status |
| `CarListing` | `tb_car_listing` | marketplace; fixed-price listing and seller ID |
| `CarListingPicture` | `tb_car_listing_picture` | marketplace; primary image |
| `CarListingGalleryPicture` | `tb_car_listing_gallery_picture` | marketplace; ordered gallery |
| `ListingTestRide` | `tb_listing_test_ride` | marketplace; listing appointment and customer ID |
| `AuctionFollow` | `tb_auction_follow` | marketplace; watchlist and notification recipient eligibility |
| `ListingDeposit` | `tb_listing_deposit` | marketplace business reservation; Stripe session/intent/URL become payment-owned fields; retain payment ID and payment-status projection |
| `CarPart` | `tb_car_part` | commerce; SKU, price in minor units, category, available stock and version |
| `CartItem` | `tb_cart_item` | commerce; scalar customer ID, local part FK |
| `StoreOrder` | `tb_store_order` | commerce; order, immutable shipping snapshot and fulfillment state; Stripe fields move to payment attempts |
| `StoreOrderItem` | `tb_store_order_item` | commerce; preserve SKU/name/price/quantity snapshots and local optional product link |
| `ListingComment` | `tb_listing_comment` | split by subject: `id_car` rows to marketplace auction comments; `id_part` rows to commerce part comments, including attachment fields |
| `PaymentAccount` | `tb_payment_account` | payment; legacy connected-account audit and owner ID; do not reactivate retired onboarding |
| `PaymentOrder` | `tb_payment_order` | payment; legacy auction payment audit. Current non-null unique bid FK means this table cannot simply represent all new payment purposes without migration |
| `PaymentWebhookEvent` | `tb_payment_webhook_event` | payment; provider event IDs and processing history from all three current webhook handlers |
| `AuctionNotification` | `tb_auction_notification` | notification; inbox/read state, recipient ID and auction reference/snapshot; no live JPA relation to marketplace |

`CarListingStatus` and `TestDriveStatus` are marketplace enums, not tables.
Repository classes follow their entity owners; aggregate children without
separate repositories remain within their aggregate. Split
`ListingCommentRepository`; payment migration replaces cross-domain queries in
`PaymentOrderRepository`. No shared persistence library is proposed.

S7 provided the backend-local `tb_checkout_attempt`, holds and verified-event
inbox used as the S8 copy source. S8 now stores independent `payment_attempt`,
`payment_webhook_receipt`, `payment_outbox` and copy-checkpoint records, keyed by
the existing attempt UUID and namespaced source/business reference. It copies
legacy account/auction audit separately instead of overloading the mandatory bid
FK. The backend retains its attempt row as a business/coexistence projection and
stores normalized terminal events in `tb_payment_result_inbox`; this coupling is
removed with the owning commerce/marketplace stages.

## Controller ownership

Source: [controllers](../back/src/main/java/lithan/autostrada/auctions/controller).
Mixed controllers must split by route/operation. A package move is insufficient.

| Current API controller | Future owner(s) |
| --- | --- |
| `AuthApiController`, `SessionApiController` | identity: auth, reset, `/api/session` |
| `MarketplaceApiController` | marketplace: auctions/listings and profile auctions; commerce: parts/categories; identity: public profiles. Gateway composes `/api/public/summary`; static `/content/{page}` may remain a gateway compatibility response or move to frontend with tests |
| `UserWorkspaceApiController` | identity: profile/picture; marketplace: auctions, bids, appointments, follows, listings and deposits; notification: inbox/read actions; gateway composes `/workspace` |
| `StoreApiController` | commerce: cart, checkout, orders and success lookup. Its `/payments` returns a retired-auction-checkout message with `redirectUrl: /orders`; preserve this compatibility response, not a new payment ledger |
| `StoreAdminApiController` | commerce: inventory and order management |
| `AdminApiController` | identity: dashboard of paged users/admins, user edits and mark-admin; marketplace: car moderation and bid approval/denial; payment: transactions and webhook audit |
| `CommentApiController` | marketplace for `/cars/**`, commerce for `/parts/**` |
| `ApiModelMapper` | split into domain DTO mappers; remove cross-service entity traversal. Gateway composition operates on DTOs only |
| `ApiExceptionHandler` | reproduce compatible error contract in each service; no shared business implementation |

The current admin dashboard only composes identity data. The public summary
and user workspace span business owners. Avoid assigning all `/api/public/**`,
`/api/user/**` or `/api/admin/**` to one service based on a wildcard.

| Current legacy controller/advice | Destination of behavior |
| --- | --- |
| `AdminController` | split account actions to identity and moderation to marketplace; transaction reads to payment; preserve redirects |
| `AuctionUserController` | marketplace follows; notification inbox |
| `CarController`, `CarListingController`, `CarSalesController` | marketplace browsing, bidding and test drives |
| `UserCarController`, `UserListingController` | marketplace vehicle management, both appointment models, listing deposits |
| `UserController` | identity profiles/images; frontend user landing page |
| `RegisterController`, `LoginController`, `PasswordResetController` | identity actions; frontend page routes |
| `PartsStoreController`, `StoreAdminController` | commerce actions; frontend pages |
| `ListingCommentController` | marketplace auction comments / commerce part comments |
| `ListingDepositController` | marketplace success lookup; frontend redirect, never payment settlement |
| `PaymentController` | preserve retired-flow redirects to parts/orders; retain audit in payment; no new onboarding or auction checkout |
| `StripeWebhookController` | payment alone after full provider-record import; currently dispatches store, deposit, then legacy payment handlers |
| `HomeController` | frontend static pages; identity public seller information plus marketplace seller auctions |
| `ListingRouteAliasController` | gateway/compatibility redirect catalogue; preserve queries and route precedence |
| `NavigationModelAdvice` | remove with legacy MVC rendering after equivalent UI/session tests; no business service |

Legacy controllers still contain mutating form actions. They are not all
redirect-only shims. Keep them in the backend until each action delegates to its
new owner or an explicitly tested retirement is reviewed. The final gateway
must distinguish API/webhook/form actions from React SPA paths to avoid loops
when the frontend shares its public origin.

## Business service ownership

Interfaces and their `Impl` classes move together where present.

| Current service(s) | Destination / boundary work |
| --- | --- |
| `UserService`, `UserServiceImpl` | identity for registration/profile/image; replace `getUserLogin()` in other domains with validated principal ID plus explicit profile client/read model |
| `PasswordResetService` | identity owns tokens and business decision; submits a restricted notification delivery command |
| `EmailService`, `LoggingEmailService`, `SmtpEmailService` | notification delivery adapter; retain temporary local adapter until messaging stage |
| `AdminService`, `AdminServiceImpl` | identity user/profile/role operations; marketplace car/bid operations |
| `CarService`, `CarServiceImpl` | marketplace catalog/search |
| `UserCarService`, `UserCarServiceImpl` | marketplace ownership, auctions/images, bids and auction test drives |
| `CarListingService`, `CarListingServiceImpl` | marketplace fixed-price listings and listing test rides |
| `AuctionFollowService` | marketplace; replace direct inbox creation with durable notification intent |
| `AuctionNotificationScheduler` | marketplace eligibility scan, temporarily in backend until marketplace moves |
| `AuctionNotificationService` | split: eligibility/follow lookup and intent creation in marketplace; inbox/list/count/read in notification |
| `CarPartService`, `CarPartServiceImpl` | commerce product/stock administration |
| `CartService`, `CartServiceImpl` | commerce cart and validation |
| `StoreOrderService`, `StoreOrderServiceImpl` | commerce order/reservation transaction and payment client; move Stripe calls and provider webhook interpretation to payment |
| `ListingCommentService`, `ListingCommentServiceImpl` | separate auction/part implementations in marketplace/commerce; preserve visibility, image validation and author badges |
| `ListingDepositService`, `ListingDepositServiceImpl` | marketplace reservation lifecycle plus payment client; payment owns provider adapter and verification |
| `PaymentService`, `PaymentServiceImpl` | payment audit/attempt lifecycle; any remaining car/bid updates become marketplace-owned event consumption. Retired public workflows stay retired |

`StripeGateway`, `StripeConnectGateway`, `DisabledStripeGateway`, provider DTOs
and `StripeProperties` belong to payment. Replace their JPA-entity arguments
with provider-neutral snapshots. `CustomUserDetailsService`, password encoding
and session authentication belong to identity; every service gets its own
security filter/permission rules. Clock and validation helpers may be small
local utilities; do not create a shared domain jar. Image bytes stay in the
owning database initially, and part image URLs retain existing semantics.

## Data references and transaction boundaries

Preserve numeric public IDs, including the distinction between `idUser` and
`idProfile`. Service-to-service/event contracts carry IDs as strings to allow
future expansion; compatibility DTOs retain their current number types. Import
sequences above the maximum copied ID. New payment attempts/events use UUIDs.

| Relationship crossing the boundary | Replacement |
| --- | --- |
| Business entity -> `UserAccount` / `UserProfile` | Scalar owner/customer/author ID; authenticated principal for permission decisions; identity REST for private profile/shipping, small public profile projection for display |
| `AuctionNotification` -> `Car` and user | Recipient ID plus auction snapshot/reference. Compatibility mapper reconstructs existing nested auction DTO; batch refresh public fields if needed, never an N+1 DB join |
| `PaymentOrder` -> bid/buyer/seller | Namespaced business reference and actor IDs; immutable amount/currency; marketplace owns bid transition |
| Store order / deposit -> Stripe session/intent | Payment attempt ID and local result projection. Success lookup uses an authorized payment lookup to resolve business ID, then owner-scoped local read |
| `ListingComment` -> auction or part | Split row sets by target with local subject FK and scalar author ID. Quarantine rows with both/neither subject and resolve before cutover |
| Order -> current profile/product description | Shipping and product snapshots retained. Later profile/catalog edits must not rewrite historical orders |

The legacy `PaymentResponse` also embeds a bid and buyer/seller DTOs. Import
the audit snapshots needed to reconstruct that shape into payment ownership,
then use explicit identity/marketplace DTO clients for any intentionally live
display fields. Preserve the current admin transaction screen without keeping
`PaymentOrder.bid` as a cross-service JPA association.

Foreign keys are local only in the target. Business services must not read an
identity replica for credential/role enforcement. Minimal public display
projections may be stale and must not expose private email/address or drive
authorization; profiles can be refreshed by batch REST. Display freshness and
admin/seller badges require compatibility tests, since current mappers traverse
live users. Deactivate subjects and retain historical references; no cross-service
cascade delete or new account-deletion behavior is implied.

Local atomic units: identity account/profile/roles or reset consumption;
marketplace bid+auction state or listing+deposit reservation;
commerce stock+order/items+cart consumption; payment webhook receipt+attempt
transition+outbox; notification inbox+consumer dedupe or read state. Every
producer writes its outbox in its own transaction. No `@Transactional` scope
spans HTTP, RabbitMQ or Stripe; distributed workflows use durable states and
compensation described in [API/events](iroit-api-events.md).

## Repeatable cutover procedure

1. Add backward-compatible columns/adapters in legacy Flyway migrations; never
   edit applied V1-V18 scripts. Replace foreign entity traversal and runtime
   foreign table reads **before** removing the old owner. Identity extraction
   requires scalar user references in all remaining backend domains first.
2. Create the target schema with a new service-local Flyway history and baseline
   migrations for only its tables. Use distinct DB users with no cross-schema
   grants. `ddl-auto=validate` stays enabled; test MySQL, not just H2.
3. Rehearse export/import on a copy. Produce row counts, IDs/max sequences,
   checksums including image blobs, money totals, status distributions and
   unresolved-reference reports. Record timestamp/time zone conversions.
4. Take a backup and briefly freeze affected writes, consumers and schedulers.
   Quiesce legacy transactions and outbox publishing; record pending provider
   attempts and broker/outbox positions. Import final data and dedupe/history
   records. Allow only one producer/consumer owner per business operation.
5. Check parity and permissions; switch explicit routes/adapters, enable the
   new owner, then reopen writes. Old tables become immutable archives; revoke
   runtime access and disable old jobs. No application-level dual writes.
6. Before new writes, recovery can restore routing to the unchanged legacy
   owner. After new writes, do **not** route to stale tables: freeze, export and
   reconcile the new delta with a tested reverse migration, or roll forward.
   Provider charges cannot be undone by restoring a DB backup.
7. Remove archived tables only in a later reviewed cleanup after backup/restore,
   replay and rollback-window checks. Keep baseline source tag and cutover
   manifests; never share a Flyway history across services.

During coexistence, `back/` may temporarily contain multiple **not-yet-extracted**
domains in its schema. Extracted data is accessible only through the owner API
or an explicitly owned projection. Keeping an old table read-only for offline
reconciliation does not authorize runtime shared-table access.
