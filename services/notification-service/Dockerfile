FROM eclipse-temurin:17-jdk-jammy@sha256:51d32af96fb8a13ffbc96c719d7b05447a04ca69f314627d64050a64ecd13057 AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
COPY src src
RUN ./mvnw --batch-mode --no-transfer-progress clean verify
RUN if jar tf target/notification-service-0.0.1-SNAPSHOT.jar | grep -E '(^|/)(e2e/|application-e2e.properties|spring-boot-test)'; then exit 1; fi

FROM build AS test-build
RUN ./mvnw --batch-mode --no-transfer-progress dependency:copy-dependencies -DincludeScope=test -DoutputDirectory=target/test-libs

FROM eclipse-temurin:17-jre-jammy@sha256:e85989f3e4d136b3d7dde921e157fddb9c7016805a225c1ec483326b825b3ca5 AS runtime
RUN groupadd --system notification && useradd --system --gid notification notification \
    && apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
USER notification
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=6 CMD curl --fail --silent http://127.0.0.1:8080/actuator/health/readiness || exit 1

FROM runtime AS e2e
COPY --from=test-build /build/target/classes /test/classes
COPY --from=test-build /build/target/test-classes /test/test-classes
COPY --from=test-build /build/target/test-libs /test/lib
ENTRYPOINT ["java", "-Duser.timezone=UTC", "-cp", "/test/test-classes:/test/classes:/test/lib/*", "e2e.E2eApplication"]

FROM runtime AS production
COPY --from=build /build/target/notification-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-Duser.timezone=UTC", "-jar", "/app/app.jar", "--server.address=0.0.0.0", "--server.port=8080"]
