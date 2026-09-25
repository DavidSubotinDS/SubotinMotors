# Autostrada payment service

Independent Java 17 / Spring Boot 3.5 service for provider checkout, signed
webhook ingestion, reconciliation and normalized payment results. It owns its
Flyway schema and has no dependency on backend entities or repositories.

From this directory:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress clean verify
docker build --tag autostrada-payment-service:s8 .
```

Production requires `PAYMENT_DB_URL`, `PAYMENT_DB_USERNAME`,
`PAYMENT_DB_PASSWORD`, `IDENTITY_VERIFICATION_JWKS`, RabbitMQ configuration and
the public gateway URL. `STRIPE_ENABLED=false` is the safe default and requires
no provider secret. When enabled, `STRIPE_SECRET_KEY` and
`STRIPE_WEBHOOK_SECRET` belong only to this service.

Health is available privately at `/actuator/health/liveness` and
`/actuator/health/readiness`. See [the S8 runbook](../../docs/payment-service.md)
for routes, trust, copy/cutover, operations and recovery.
