# Autostrada Auctions Backend

This folder contains the Spring Boot backend. It also contains the legacy JSP
views while the React frontend in `../front` replaces them.

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
java -jar target\autostrada-auctions-0.0.1-SNAPSHOT.war
```

## API And Legacy UI

React-facing API endpoints are under `/api`. Public marketplace read endpoints
are under `/api/public`. Existing JSP routes remain available for flows that do
not yet have React replacements.

Flyway migrations are in `src/main/resources/db/migration`.
