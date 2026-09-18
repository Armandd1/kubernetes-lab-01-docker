# syntax=docker/dockerfile:1

# ---- Stage 1: build the fat JAR with Maven + JDK 25 (Alpine) ----
FROM maven:3.9.16-eclipse-temurin-25-alpine AS build

WORKDIR /build

# Copy the POM first so the dependency layer is cached independently of sources.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

# Now copy the sources and build the packaged (fat) JAR.
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Stage 2: minimal JRE-only runtime (Alpine) ----
FROM eclipse-temurin:25-jre-alpine AS runtime

# Create a dedicated non-root user (uid 1000). busybox wget is used for the healthcheck.
RUN addgroup -S -g 1000 app \
    && adduser -S -D -H -u 1000 -G app app

WORKDIR /app

# Only the built artifact is copied from the builder: no Maven, no JDK, no sources.
COPY --from=build /build/target/*.jar /app/app.jar

RUN chown -R app:app /app

USER app

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=5 \
    CMD wget -qO- http://127.0.0.1:8080/health || exit 1

ENTRYPOINT ["java","-jar","/app/app.jar"]
