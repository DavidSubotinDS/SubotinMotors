# Subotin Motors

Subotin Motors is a Spring MVC marketplace for listing used cars, placing bids, and requesting test drives.

## Technology

- Java 17+
- Spring Boot 3.5
- Spring MVC, Security, Data JPA, and Validation
- JSP and Bootstrap
- Flyway database migrations
- H2 for zero-configuration local development
- MySQL for deployment

## Run locally

The default profile uses a local H2 database. No database installation or credentials are required.

Ensure `java -version` reports Java 17 or newer. On Windows, an older Oracle Java
entry can appear earlier on `PATH`; point `JAVA_HOME` and `PATH` to the JDK used
by Maven if that happens.

```powershell
.\mvnw.cmd spring-boot:run
```

Open <http://localhost:8080>.

Seeded accounts:

- Admin: `admin123` / `admin123`
- User: `user123` / `user123`

Local data is stored in `data/` and is ignored by Git.

## Run tests

```powershell
.\mvnw.cmd clean test
```

Tests use a separate in-memory H2 database.

## Build an executable application

```powershell
.\mvnw.cmd clean package
java -jar target\abc-cars-0.0.1-SNAPSHOT.war
```

## Use MySQL

Create a restricted MySQL user and provide credentials through environment variables:

```powershell
$env:SPRING_PROFILES_ACTIVE = "mysql"
$env:DB_URL = "jdbc:mysql://localhost:3306/abc_cars?createDatabaseIfNotExist=true&serverTimezone=UTC"
$env:DB_USERNAME = "abc_cars"
$env:DB_PASSWORD = "replace-me"
.\mvnw.cmd spring-boot:run
```

Never commit database passwords. Flyway applies migrations from
`src/main/resources/db/migration`.

## Current features

- Registration and BCrypt authentication
- User and administrator roles
- Profile and image management
- Car listing and moderation
- Search by make, model, year, and price
- Bidding
- Test-drive requests

Payment processing is intentionally not included yet. The next planned integration is Paddle after the application baseline is verified.
