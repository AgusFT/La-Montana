FROM maven:3.9.16-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17.0.20_8-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN groupadd --gid 10001 lamontana && useradd --uid 10001 --gid lamontana --no-create-home lamontana \
    && mkdir -p /app/archivos && chown -R lamontana:lamontana /app
WORKDIR /app
COPY --from=build --chown=lamontana:lamontana /build/target/lamontana.jar ./app.jar
USER lamontana
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=45s --retries=6 CMD ["curl", "--fail", "--silent", "http://localhost:8080/actuator/health/readiness"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
