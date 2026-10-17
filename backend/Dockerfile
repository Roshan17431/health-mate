# Override image references with approved, scanned digests for a release.
ARG JDK_IMAGE=eclipse-temurin:17-jdk-jammy
ARG JRE_IMAGE=eclipse-temurin:17-jre-jammy
FROM ${JDK_IMAGE} AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src/ src/
RUN rm -f src/main/resources/data.sql
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp verify

FROM ${JRE_IMAGE}
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 spring && useradd --uid 10001 --gid spring --no-create-home spring
WORKDIR /app
COPY --from=build --chown=spring:spring /workspace/target/health-mate-0.0.1-SNAPSHOT.jar app.jar
USER 10001:10001
ENV SPRING_PROFILES_ACTIVE=prod JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
    CMD curl --fail --silent "http://127.0.0.1:${SERVER_PORT:-${PORT:-8080}}/actuator/health/readiness" > /dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
