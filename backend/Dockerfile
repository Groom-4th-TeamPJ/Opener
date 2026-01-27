# Multi-stage build for Spring Boot application

# Stage 1: Build
FROM gradle:8.14-jdk21 AS builder

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Download dependencies (cached layer)
RUN gradle dependencies --no-daemon

# Copy source code
COPY src src

# Build application (skip tests for faster build)
RUN gradle bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Netty native(QUIC) 및 SSL/TLS 연결에 필요한 런타임 라이브러리
# - libgcc, libstdc++: Netty 네이티브 라이브러리 의존성
# - openssl: SSL/TLS 암호화 라이브러리 (reactor-netty HttpClientSecure 필요)
# - ca-certificates: HTTPS 연결을 위한 루트 인증서
# - libc6-compat: glibc 호환성 레이어 (일부 네이티브 라이브러리 필요)
RUN apk add --no-cache libgcc libstdc++ openssl ca-certificates libc6-compat

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Change ownership to non-root user
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider https://opener.deving.xyz:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
