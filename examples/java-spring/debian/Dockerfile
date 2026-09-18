# syntax=docker/dockerfile:1

# ---- Stage 1: build the fat JAR with Maven + JDK 25 ----
FROM maven:3.9.16-eclipse-temurin-25 AS build

WORKDIR /build

# Copy the POM first so the dependency layer is cached independently of sources.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

# Now copy the sources and build the packaged (fat) JAR.
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Stage 2: minimal JRE-only runtime ----
FROM eclipse-temurin:25-jre AS runtime

# curl is required for the container HEALTHCHECK below.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Create a dedicated non-root user with uid/gid 1000. The Ubuntu-based temurin
# image ships an "ubuntu" user at uid/gid 1000, so remove it first to free the id.
RUN userdel -r ubuntu 2>/dev/null || true; \
    groupdel ubuntu 2>/dev/null || true; \
    groupadd --gid 1000 app \
    && useradd --uid 1000 --gid 1000 --create-home --shell /usr/sbin/nologin app

WORKDIR /app

# Only the built artifact is copied from the builder: no Maven, no JDK, no sources.
COPY --from=build /build/target/*.jar /app/app.jar

RUN chown -R app:app /app

USER app

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=5 \
    CMD curl -fsS http://127.0.0.1:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
