# Stage 1: Build & Dependency Resolution
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

ENV MAVEN_OPTS="-Djava.net.preferIPv4Stack=true"

# Download dependencies first to take advantage of Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and package application
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal & Secure Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Security: run as non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy packaged jar from build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=25s --retries=3 \
  CMD sh -c 'wget -q -O /dev/null "http://127.0.0.1:${PORT:-8080}/actuator/health/liveness" || exit 1'

ENTRYPOINT ["java", "-jar", "app.jar"]
