# S4b identity boundary preparation

## Inventory before implementation

Inspected from merged S4a `1a3147baa4cf4c58a2aba5d2958980348439788e`.
Identity owns UserAccount, UserProfile, Role, ProfilePicture and PasswordResetToken,
their five repositories, UserService, PasswordResetService and authentication.
Registration, self profile/image, recovery, session and admin account operations
legitimately use those entities. No account deletion endpoint exists.

| Consumer | Coupling being replaced |
| --- | --- |
| Auctions / bids / auction test drives | Car.user, CarBidding.user, TestDrive.user; UserCarService discovers an entity through UserService; repository parameters and ownership traverse that entity |
| Listings / appointments / deposits | CarListing.seller, ListingTestRide.user, ListingDeposit.buyer; seller/customer repository parameters, permission comparisons and provider email traversal |
| Follows / notifications | AuctionFollow.user, AuctionNotification.user; recipient-scoped queries and scheduler pass account entities |
| Commerce | CartItem.user, StoreOrder.user; cart/order ownership queries, checkout shipping profile and Stripe customer email |
| Comments | ListingComment.author; entity graph fetches author/profile/roles for names and badges |
| Payment audit | PaymentAccount.user, PaymentOrder.buyer/seller; ownership, onboarding display and audit serialization |
| Administration | AdminService mixes identity repositories/account changes with car/bid moderation; ApiModelMapper mixes private identity mapping and business display |
| Public profile consumers | MarketplaceApiController and HomeController traverse every car's user/profile to filter by profile ID |
| Authentication | CustomUserDetails holds a mutable JPA account with profile/roles; business actions resolve accounts by username |

All 14 business identity references in 13 entities use existing non-null INT
columns pointing to tb_user. Account ID and profile ID are distinct. Applied
Flyway V1-V18 define restrictive foreign keys (no ON DELETE CASCADE), uniqueness
for payment account owner, cart owner/part, follow owner/car and notification
deduplication, plus existing indexes. No migration should be edited or FK removed.
UserAccount has reverse CascadeType.ALL collections for cars/bids; removing
these Java cascades intentionally leaves database RESTRICT protection in force.
Physical deletion of an account with business history must fail rather than erase
history. Identity-internal profile/role/image relationships remain local.

The current public profile mapper returns phone/address/shipping data; business
nested user DTOs also carry email and private profile fields. The architecture
requires minimal public allowlists. S4b will retain DTO keys while redacting these
fields on public/business display paths; self/admin identity views retain their
authorized private fields. Checkout receives a separate current-actor-only private
contract. This is a deliberate privacy correction, documented with regression
coverage, rather than preserving accidental entity serialization.

The target architecture's removal of cross-service FKs applies to S5 data cutover,
not S4b's single database. Likewise notification-to-car, payment-to-bid and provider
transaction changes belong to later stages. Backend remains the only session owner.

## Implemented contracts

S4b is implemented locally on `feature/david.subotin_identity-boundary`; it is
not committed or remotely verified. See [owner evidence](identity-boundary-pr.md).
The inventory above describes the starting point; the following is current code.

- `CurrentIdentity.requireUserId()` accepts only the authenticated production
  `CustomUserDetails` principal and verifies that the account still exists.
  Anonymous, unsupported and deleted-account principals are denied. The immutable
  account ID is never derived from a request owner field, profile ID or username.
  Passwords are erased after authentication without mutating the persistent account.
- `ProfileClient.findAll(Collection<Integer>)` returns an immutable account-ID map
  of public display records. Duplicate IDs are collapsed; null/nonpositive IDs
  and missing accounts are omitted. Empty input performs no queries. Each chunk
  of at most 500 IDs uses two scalar queries (display fields and admin badge),
  without hydrating account/password entities. The badge is display-only.
- `ProfileClient.display(id)` falls back to `Unavailable user` when the account
  is missing. An account without a profile uses its username, with no profile ID
  or picture. `findByProfileId(id)` explicitly translates the separate profile ID
  and returns an empty Optional when absent. The public profile route retains
  404 and its auctions route retains an empty list for an unknown profile.
- `PublicProfile` contains account/profile IDs, username, names, city/country,
  biography, picture type/data and a display badge. No email, phone, street,
  postcode, password or role collection crosses this public lookup contract.
- `CheckoutProfileClient.current()` supplies only the trusted current account's
  email, name and shipping fields. It has no arbitrary-owner lookup operation.
  A missing profile yields incomplete shipping fields; checkout rejects an
  incomplete address before reserving stock or writing an order. The record's
  `toString` is redacted. Existing order shipping snapshots stay business-owned.
- `InProcessProfileClient` implements both clients inside the existing backend.
  No HTTP client, remote session exchange, cache, broker or retry was introduced.

`ApiModelMapper.map` preloads distinct display subjects for a page/collection,
including nested bid/auction and payment subjects. Its synchronous thread-local
mapping scope is removed/restored in `finally`; it is not a session cache.
Comment rendering also performs one bulk lookup. Single-detail mapping uses
single-subject lookups. Existing non-identity repository traversal is unchanged.

## Permitted dependencies and ownership

Business entities/repositories contain scalar account IDs only. Business services
consume CurrentIdentity and public/private lookup interfaces, while authorization
still uses Spring Security USER/ADMIN restrictions plus scoped queries and scalar
owner comparisons. Provider adapters receive explicit customer email or seller
ID/display name rather than walking account entities. Signed webhook bytes,
signature validation, deduplication and payment transition behavior are unchanged.

Identity entity/repository access remains in registration, login, recovery,
account administration, self profile/upload and identity adapters.
`SelfProfileService` owns self profile operations; `IdentityApiMapper` owns private
self/admin DTO mapping. `AdminService` owns account operations only;
`MarketplaceAdminService` owns auction/bid moderation. Existing admin controllers
remain mixed route adapters, explicitly allowlisted, but delegate to separate
services/mappers. `IdentityBoundaryArchitectureTests` enforces an explicit list
of permitted persistence owners and rejects identity types/repositories/services
and identity table names elsewhere. It also rejects reverse car cascades and JPA
state in the principal. New identity owners require an intentional test update.

Remaining preparation coupling: one backend/database/session owner, identity-local
JPA relations, legacy `profileLog` session compatibility data, identity adapters
under old service/controller paths, and restrictive database FKs. Existing roles
are authentication snapshots, not live remote claims. None constitutes extraction.

## Database and request compatibility

| Entity | Retained identity column(s) |
| --- | --- |
| Car, CarBidding, TestDrive | `tb_car.id_user`, `tb_car_bid.id_user`, `tb_test_drive.id_user` |
| CarListing, ListingTestRide, ListingDeposit | `tb_car_listing.id_seller`, `tb_listing_test_ride.id_user`, `tb_listing_deposit.id_buyer` |
| AuctionFollow, AuctionNotification, ListingComment | `tb_auction_follow.id_user`, `tb_auction_notification.id_user`, `tb_listing_comment.id_user` |
| CartItem, StoreOrder | `tb_cart_item.id_user`, `tb_store_order.id_user` |
| PaymentAccount, PaymentOrder | `tb_payment_account.id_user`, `tb_payment_order.id_buyer`, `tb_payment_order.id_seller` |

All references retain the original account IDs and non-null columns. There are
no new migrations, renamed columns, data copies or edits to applied V1-V18.
Existing FK/index/unique guarantees remain authoritative. Removing account-side
Java car/bid cascades prevents deletion of business history; referenced identity
deletion is restricted by the database. No new delete workflow is provided.
FK removal belongs to the later independently validated extraction/data cutover.

API routes, request DTOs, session DTO keys, JSESSIONID, role restrictions and
legacy redirects remain. The deliberate response change is redaction: business
nested users retain keys but email is null, roles empty, and private profile
fields null. Public profiles expose city/country only, with no fallback to the
legacy free-text address; shipping completeness is false and formatted shipping
address empty. Public root account ID/username remain null as before; nested
user summaries retain their account ID/username. Self/admin account views retain
authorized email/phone/address/roles. Order shipping snapshots remain available
on the existing owner/admin protected order routes. The React UI requires no code
change; profile display/upload and business browser flows cover these contracts.

S4a CSRF/session rotation, memory-only tokens, coordinated refresh, multipart
boundaries and no mutation replay remain active. Only exact signed POST
`/webhooks/stripe` retains its CSRF exemption. One public gateway origin, private
upstreams, native H2 and isolated Compose/MySQL harnesses are unchanged.

## Rollout and rollback

Local container/MySQL/Compose browser gates and the S4a-to-S4b upgrade rehearsal
have passed; see the handoff for exact evidence. Before merging, require successful
Backend/Frontend checks on the exact final commit. Back up the database using the existing Compose runbook; preserve image
and configuration references. No data migration or identity cutover is needed.
Deploy the backend with the existing gateway/frontend configuration. Restart
backend sessions and require re-login: the principal's serialized shape changed,
so do not carry old persisted sessions into the new binary. Check login/CSRF,
self upload, public privacy, bidding, owned cart/order/deposit and admin routes.

Rollback by restoring the previously verified S4a backend image/config and
restarting sessions. Retained IDs/columns and the sole database writer allow the
old mappings to read current data; no database restore is needed for a code-only
rollback. Keep S4a enforcement intact. Rollback restores S4a's broader public
profile serialization, so consider a forward privacy fix if rollback is needed.
Never reset a normal database or copy a stale backup over newer writes.

Next is documented **S5 identity-service extraction**, including its own data,
routes, session exchange and internal trust. It is not implemented here.

Framework references: [Spring Security authentication architecture](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/architecture.html)
for trusted principals and credential erasure, and
[Jakarta Persistence 3.1](https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1.html)
for basic mappings, scalar projections and relationship cascades. Database FKs
remain independent of the removal of Java relationships.
