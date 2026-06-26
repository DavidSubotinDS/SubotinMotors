# Autostrada Auctions Backend

This folder contains the Spring Boot backend. The React frontend in `../front`
owns the UI; this backend provides REST APIs, persistence, security, Flyway
migrations, Stripe webhook handling, and legacy route redirects.

## Run

```powershell
.\mvnw.cmd spring-boot:run
```

## Test

```powershell
.\mvnw.cmd clean test
```

## Build

```powershell
.\mvnw.cmd clean package
java -jar target\autostrada-auctions-0.0.1-SNAPSHOT.jar
```

## API And Legacy Routes

React-facing API endpoints are under `/api`. Public marketplace read endpoints
are under `/api/public`. No JSP files remain; old MVC view names redirect to
the React frontend using `APP_FRONTEND_BASE_URL` for bookmark compatibility.

Flyway migrations are in `src/main/resources/db/migration`.
