# Frontend and Backend Separation

This branch establishes a two-folder structure:

- `back/` contains the Spring Boot backend, service layer, repositories, Flyway
  migrations, tests, and REST APIs.
- `front/` contains the React application, Vite build config, routes,
  reusable UI components, API client, and frontend tests.

## Backend Boundary

The backend remains authoritative for validation, authorization, business
workflow transitions, persistence, Stripe webhook verification, and Flyway
migrations. New React-facing endpoints should return DTOs from `/api/**` and
reuse existing services rather than moving business logic into controllers.

React-facing endpoints:

- `GET /api/session`
- `/api/public/**` for public content, auctions, fixed-price listings, parts, seller profiles, and summaries
- `/api/auth/**` for login, logout, registration, and password reset
- `/api/user/**` for profile, auctions, fixed-price listings, bids, watchlists, notifications, appointments, and deposits
- `/api/store/**` for cart, checkout, and customer orders
- `/api/comments/**` for auction and part discussions
- `/api/admin/**` for user, auction, bid, transaction, inventory, and store-order administration

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

## JSP Migration Status

No JSP files remain in the repository. JSP/JSTL/Jasper dependencies and Spring
MVC JSP view prefix/suffix configuration were removed from the backend.

Former JSP routes now have React route coverage. Legacy MVC controllers are
kept only as compatibility shims for existing links and old form routes; any
non-redirect view name is redirected to the React frontend using
`APP_FRONTEND_BASE_URL`.

New frontend work belongs in `front/`. New backend UI data/actions belong under
`/api/**` and should return DTOs rather than JPA entities.
