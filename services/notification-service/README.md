# Notification service

Independent Java 17 / Spring Boot service owning the notification inbox, read
state, message dedupe and encrypted reset-mail delivery. It uses JDBC/Flyway,
its own MySQL credentials and public identity verification keys; it has no
shared JPA/domain jar or login/session authority.

Build with `./mvnw clean verify` (`.\mvnw.cmd clean verify` on Windows).
The production Docker target excludes private test launchers and broker probes.
Required settings are documented in `src/main/resources/application.properties`
and the root `.env.compose.example`. Do not copy example secret placeholders
into a deployed environment. DB readiness is independent of RabbitMQ; broker
outage leaves inbox reads available and durable producer work pending.

See [ownership, contracts, cutover and recovery](../../docs/notification-service.md)
and [acceptance evidence](../../docs/notification-service-pr.md).
