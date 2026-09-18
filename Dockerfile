# =============================================================================
# Stage 1: Build & Package Spring Boot Application with Maven
# =============================================================================
FROM maven:3.9.9-eclipse-temurin-21-jammy AS builder

WORKDIR /build

# 1. Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. Copy source code and build production fat JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# 3. Extract Spring Boot layertools for optimized image layer caching
RUN java -Djarmode=layertools -jar target/book-corner-*.jar extract

# =============================================================================
# Stage 2: Minimal, Hardened Production Runtime
# =============================================================================
FROM eclipse-temurin:21-jre-jammy AS runtime

LABEL maintainer="Book Corner Platform Engineering <engineering@bookcorner.com>"
LABEL description="Enterprise E-Commerce Online Bookstore Platform REST API"
LABEL version="1.0.0"

# 1. Install dumb-init for PID 1 signal handling and curl for container health probes
RUN apt-get update && \
    apt-get install -y --no-install-recommends dumb-init curl && \
    rm -rf /var/lib/apt/lists/*

# 2. Create non-root system user and group (Security Hardening)
RUN groupadd -g 10001 appgroup && \
    useradd -u 10001 -g appgroup -s /sbin/nologin -d /app appuser

WORKDIR /app

# 3. Copy extracted Spring Boot layers in order of change frequency
COPY --from=builder --chown=appuser:appgroup /build/dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /build/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appgroup /build/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /build/application/ ./

# 4. Set container-optimized JVM runtime flags
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom \
               -Dfile.encoding=UTF-8"

ENV SPRING_PROFILES_ACTIVE="prod"
ENV SERVER_PORT=8080

# 5. Container Health Check Probe (Spring Boot Actuator Liveness)
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:${SERVER_PORT}/actuator/health/liveness || exit 1

# 6. Switch to non-root execution
USER appuser:appgroup

EXPOSE 8080

# 7. Graceful initiation via dumb-init and Spring Boot JarLauncher
ENTRYPOINT ["dumb-init", "--", "sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
