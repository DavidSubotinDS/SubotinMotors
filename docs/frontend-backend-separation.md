# Frontend and Backend Separation

This branch establishes a two-folder structure:

- `back/` contains the Spring Boot backend, service layer, repositories, Flyway
  migrations, tests, and temporary JSP compatibility views.
- `front/` contains the React application, Vite build config, routes,
  reusable UI components, API client, and frontend tests.

## Backend Boundary

The backend remains authoritative for validation, authorization, business
workflow transitions, persistence, Stripe webhook verification, and Flyway
migrations. New React-facing endpoints should return DTOs from `/api/**` and
reuse existing services rather than moving business logic into controllers.

Current React-facing endpoints:

- `GET /api/session`
- `GET /api/public/summary`
- `GET /api/public/auctions`
- `GET /api/public/listings`
- `GET /api/public/parts`
- `GET /api/public/part-categories`

## Frontend Boundary

The React frontend owns the new UI shell and consumes the backend API through
`front/src/services`. The backend base URL is configured with
`VITE_API_BASE_URL`.

Initial reusable components:

- `AppLayout`
- `Navbar`
- `DashboardSidebar`
- `PageHeader`
- `Card`
- `Button`
- `Alert`
- `EmptyState`
- `LoadingState`
- marketplace summary cards

The sidebar already accepts session roles so regular user, admin, and store
team navigation can be expanded without duplicating layout code.

## Legacy JSP Compatibility

JSP remains in `back/src/main/webapp/view` only as compatibility. It should not
be treated as the target frontend implementation for new work.

Remaining JSP-backed flows include authentication, registration, profile
management, bidding mutations, comments, watchlists, test-drive/test-ride
state changes, cart and checkout flows, order history, and admin management.

Do not delete a JSP page until the equivalent React route and backend API flow
exist and have been verified.
