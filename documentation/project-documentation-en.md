# Autostrada Auctions Project Documentation

> Update note, 24 June 2026: the repository has been reorganized into
> `back/` for the Spring Boot backend and `front/` for the React frontend.
> Sections that describe JSP views document the legacy compatibility UI that is
> being replaced by React.

Student: David Subotin IT-40/2022  
GitHub repository: https://github.com/DavidSubotinDS/SubotinMotors  
Project type: Spring Boot web marketplace  
Version: June 2026

## Application Readiness Check

The application was reviewed against the submission notice and the attached writing guidelines. The available evidence in the repository shows that the project is complete enough for defense: the source code implements the main marketplace workflows, the documentation includes the GitHub link, and the automated test suite passes.

| Requirement | Status | Evidence |
| --- | --- | --- |
| Completed project for defense | Fulfilled | The application contains public, user, and administrator workflows for accounts, car auctions, fixed-price listings, test drives, car parts, carts, orders, and payments. |
| Project documentation | Fulfilled by this file | This documentation describes the goal, architecture, actors, workflows, security rules, validation, testing, and local execution process. |
| GitHub link in the documentation | Fulfilled | The repository link is listed at the beginning of the document. |
| Project specification requirements | Fulfilled according to repository evidence | The implemented source code and existing `docs/crud-coverage.md` cover CRUD/lifecycle behavior, role authorization, moderation, validation, payment processing, and retained audit history. |
| Quality and verification | Fulfilled | The backend test suite was executed from `back/` successfully with 81 tests, 0 failures, and 0 errors. |
| Documentation guidelines | Fulfilled | The document uses formal wording, structured headings, references to official sources, and avoids unreliable sources such as Wikipedia. |

## 1. Introduction

Autostrada Auctions is a web application for buying, selling, and managing vehicles and car parts. The system combines an auction marketplace with a conventional car-parts store. It supports public browsing, authenticated user actions, administrator moderation, test-drive management, payment tracking, and realistic demonstration data.

The backend is implemented as a Spring Boot project with a layered architecture. React now lives in a separate `front/` folder as the target frontend, while JSP views remain in `back/` as legacy compatibility during migration. The default configuration uses an embedded H2 database, while a MySQL profile is available for deployment-like environments.

## 2. Project Objectives

The main objective is to provide a complete marketplace workflow for vehicles and related products. The application is designed to demonstrate account management, role-based authorization, listing creation, auction participation, store checkout, and administrator supervision.

The functional objectives are:

- Allow visitors to browse public cars, fixed-price listings, and car parts.
- Allow users to register, authenticate, manage their profiles, and recover passwords.
- Allow sellers to create car listings that require administrator approval before public activation.
- Allow buyers to place bids, request test drives, follow auctions, and receive notifications.
- Allow administrators to manage users, approve or reject listings, manage inventory, inspect transactions, and supervise marketplace activity.
- Keep important business history, such as bids, payments, orders, and test-drive states, instead of deleting audit-relevant records.
- Provide automated tests that verify important security, validation, and workflow behavior.

## 3. Technology Stack

The project uses the following technologies:

- Java 17
- Spring Boot 3.5
- Spring MVC
- Spring Security
- Spring Data JPA
- Jakarta Bean Validation
- JSP and JSTL
- Bootstrap
- Flyway database migrations
- H2 for local development and tests
- MySQL for the optional database profile
- Stripe sandbox checkout and signed webhook handling
- Maven Wrapper for consistent build execution

## 4. System Actors

The system has several main actors.

Visitor: A non-authenticated user can view public pages, browse active vehicle listings, search the car catalog, and browse the car-parts catalog.

Registered user: An authenticated user can manage a profile, post cars, place bids, request test drives, follow auctions, manage a cart, start store checkout, and view personal order or bidding history.

Seller: A registered user who owns a listing can edit owned cars, upload pictures, review test-drive requests for owned cars, and manage seller-side listing activity.

Administrator: An administrator can access management pages, approve or reject pending listings, manage users, manage car-parts inventory, view orders, and review payment or webhook audit data.

Stripe sandbox: Stripe is used only as a sandbox payment provider for store checkout. Payment completion is accepted only through verified webhook events.

Scheduled notification service: A scheduled backend process checks followed auctions and creates ending-soon notifications for followers.

## 5. Functional Description

### 5.1 Public Marketplace

The public part of the application includes the home page, car catalog, car details, fixed-price listing pages, and car-parts catalog. Visitors can search and sort cars by make, model, year, and price. Public catalog pages only show vehicles that are allowed to be visible in the current marketplace state.

The catalog supports pagination and sorting so that larger data sets remain usable. Search validation prevents invalid input combinations, such as a minimum price greater than the maximum price.

### 5.2 Registration, Login, and Profiles

Registration is implemented as a two-step process for account and profile data. Usernames and email addresses are validated and must be unique. Passwords are stored with BCrypt hashing through Spring Security.

The profile module allows authenticated users to review and edit personal details, including contact and address information. The address is used by store checkout to create a shipping snapshot for orders. Public profile pages allow marketplace participants to inspect seller or buyer information without exposing private management functions.

The password reset workflow creates a random token, stores only a hash of that token, applies an expiration period, and consumes the token after a successful password change. The response does not reveal whether a submitted username or email exists.

### 5.3 Car Listings and Moderation

Authenticated users can create car listings. New cars start in a pending state and require administrator approval before they become public. This prevents unreviewed listings from appearing directly in the public catalog.

Administrators can approve or reject pending cars from the car-management area. Listing owners can edit their own cars and upload images, while ownership checks prevent users from editing cars that belong to another account.

Hard deletion is intentionally avoided for cars that are connected to bids, test drives, or payments. Instead, lifecycle states such as active, inactive, reserved, sold, pending, or rejected preserve business history.

### 5.4 Auction and Bidding Workflow

Vehicle auctions support bid placement by authenticated users. The system rejects invalid bids, bids on inactive cars, bids after the auction deadline, and attempts by owners to bid on their own cars.

Bids use lifecycle states such as ongoing, accepted, denied, and cancelled. Buyers can cancel their own ongoing bids. Administrators can accept a winning bid, deny competing bids, and mark the related car as sold or reserved according to the workflow.

Auction pages show countdown information. The client-side countdown improves the user experience, while backend service checks remain authoritative for deadline enforcement.

### 5.5 Test-Drive Workflow

Authenticated users can request test drives for eligible vehicles. Test-drive dates must be in the future. Duplicate bookings for the same user, car, and date are prevented.

The workflow includes pending, accepted, rejected, and cancelled states. Vehicle owners can accept or reject pending requests for cars they own. Requesters can reschedule pending or accepted bookings; an accepted booking returns to pending after rescheduling so that the owner can approve the new time. Accepted appointments can also be cancelled when necessary.

The workflow keeps historical requests instead of deleting them, which helps preserve marketplace accountability.

### 5.6 Car-Parts Store

The car-parts store provides a searchable product catalog with stock information. Administrators can create and edit store products, including SKU, category, description, price, stock, visibility, and optional image URL.

Authenticated users can add products to a persistent shopping cart. Adding the same part more than once merges cart rows and updates quantities within available stock limits. Users can update quantities or remove items from the cart.

### 5.7 Store Checkout and Payments

Store checkout converts cart contents into an order and starts a Stripe-hosted sandbox checkout session. Inventory is reserved while checkout is pending. The order is marked as paid only after a signed Stripe webhook is verified and processed.

If checkout expires or fails, reserved inventory is restored. Webhook events are processed idempotently so duplicate provider events do not create duplicate business effects.

Stripe is intentionally sandbox-only in this project. Live Stripe credentials are rejected, and no production payment endpoint is configured.

### 5.8 Administrator Module

The administrator module provides management pages for users, car moderation, store inventory, store orders, and transaction records. Administrator routes require the administrator role, while anonymous users and regular users are denied access.

The transaction page displays historical payment data, provider identifiers, webhook audit fields, and sorted payment information. This allows the application to keep financial records visible for review without allowing unsafe manual edits.

### 5.9 Notifications and Discussions

Users can follow active auctions and later review followed auctions from their account area. The application creates ending-soon notifications for followed auctions, while uniqueness rules prevent duplicate notifications for the same user, car, and notification type.

The application also supports listing and part discussions. Authenticated users can post comments, and uploaded comment images are validated before being accepted.

## 6. Architecture

The application follows a layered Spring MVC architecture.

Controllers handle HTTP routes, form binding, validation results, and view selection. Services contain business rules such as ownership checks, lifecycle transitions, payment decisions, and inventory handling. Repositories provide persistence access through Spring Data JPA. Entities model the stored business data, while DTO and form classes model user input.

Flyway migrations create and evolve the database schema. The project currently includes migrations for the initial schema, Stripe payment support, test-drive statuses, demo marketplace data, password reset and auction tracking, car-parts store, listing comments, structured addresses, fixed-price listings, listing test rides, deposits, payment purposes, shipping snapshots, fixed-price demo data, and branding.

Important entity groups include:

- Accounts, roles, profiles, profile pictures, and password reset tokens.
- Cars, car pictures, bids, follows, notifications, and listing statuses.
- Test drives, fixed-price listings, listing test rides, and deposits.
- Car parts, cart items, store orders, and store order items.
- Payment orders, payment accounts, and webhook event records.
- Listing comments and comment image attachments.

## 7. Security and Validation

Security is implemented with Spring Security. Public routes are separated from authenticated user routes and administrator routes. User pages require authentication, and administrator pages require the administrator role.

Ownership checks are enforced in service logic. A user cannot edit another user's car, manage another user's bid, decide test-drive requests for a car owned by someone else, or access another user's protected workflow as if it were their own.

The application includes validation at form and entity boundaries:

- Production year must be realistic and cannot be in the future.
- Phone numbers must match accepted international or national formats.
- Bid prices must be positive directly on the entity.
- Test-drive dates must be in the future.
- Car search price ranges must be valid.
- Uploaded images must have an allowed declared MIME type, matching file signature, and decodable image content.
- Email addresses are required, validated, and unique.

Payment security is handled by accepting completed store payments only from verified Stripe webhook events. Browser success redirects do not mark orders as paid.

## 8. Database and Demo Data

The default local database is H2, which makes the project easy to run without installing a separate database server. The application also provides a MySQL profile through environment variables.

Flyway applies migrations from `back/src/main/resources/db/migration`. The demo data includes administrator and user accounts, active auctions, pending approvals, inactive and sold vehicles, bid history, test-drive examples, followed auctions, notifications, fixed-price listings, store products, orders, and discussions.

Seeded accounts documented in the repository include:

- `admin123` / `admin123`
- `user123` / `user123`
- `demo_bidder` / `demo123`
- `demo_seller` / `demo123`
- `demo_trader` / `demo123`
- `demo_newcomer` / `demo123`

## 9. Testing and Quality Assurance

The project contains automated tests for the most important workflows and rules. The full Maven test suite was executed successfully:

```powershell
cd back
.\mvnw.cmd test
```

Result:

```text
Tests run: 81, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The test suite covers registration, duplicate usernames and emails, search, pagination, sorting, administrator authorization, validation failures, test-drive workflow, transaction display, password reset, auction deadlines, auction following, notifications, route protection, listing comments, image validation, demo data readiness, store checkout, inventory reservation, webhook idempotency, and Stripe sandbox credential restrictions.

## 10. Local Execution

Java 17 or newer is required. The application can be started locally with:

```powershell
cd back
.\mvnw.cmd spring-boot:run
```

The local application is available at:

```text
http://localhost:8080
```

The project can be packaged with:

```powershell
cd back
.\mvnw.cmd clean package
```

For MySQL execution, the `mysql` profile and database connection environment variables must be configured. Payment-related execution remains sandbox-only and requires Stripe sandbox credentials if checkout is being demonstrated.

## 11. Limitations and Future Improvements

The current implementation is suitable for a project defense and local demonstration. Several decisions are intentionally limited to preserve safety and clarity:

- Stripe integration is sandbox-only and does not process real payments.
- Store checkout is implemented for car parts, while auction winners are handled as accepted marketplace sales.
- Historical payment, bid, order, and webhook records are not editable from the user interface.
- Hard deletion is avoided for records that are connected to marketplace or payment history.

Possible future improvements include production deployment, richer administrator reporting exports, a production email provider configuration, additional accessibility review, and a dedicated deployment guide.

## 12. References

- Spring Boot Documentation: https://docs.spring.io/spring-boot/
- Spring Security Documentation: https://docs.spring.io/spring-security/reference/
- Spring Data JPA Documentation: https://docs.spring.io/spring-data/jpa/reference/
- Hibernate Validator Documentation: https://hibernate.org/validator/documentation/
- Flyway Documentation: https://documentation.red-gate.com/fd
- Stripe Documentation: https://docs.stripe.com/

## 13. Conclusion

Autostrada Auctions implements a complete vehicle and car-parts marketplace with public browsing, authenticated user workflows, administrator moderation, validation, payment audit behavior, and automated verification. Based on the repository review and the successful test run, the application satisfies the checked project-readiness requirements for documentation submission and defense preparation.
