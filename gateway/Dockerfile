FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
COPY src src
RUN ./mvnw --batch-mode --no-transfer-progress clean verify

FROM eclipse-temurin:17-jre-jammy
RUN groupadd --system gateway && useradd --system --gid gateway gateway \
    && apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /build/target/gateway-0.0.1-SNAPSHOT.jar app.jar
USER gateway
ENV GATEWAY_BIND_ADDRESS=0.0.0.0
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 CMD curl --fail --silent http://127.0.0.1:${GATEWAY_PORT:-8081}/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
