# Development-friendly Dockerfile with hot-reload support
FROM maven:3.9.6-eclipse-temurin-17

WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy pom.xml first to cache dependencies
COPY pom.xml .

# Download dependencies (will use cache if pom.xml doesn't change)
RUN mvn dependency:go-offline -B || true

# Expose port
EXPOSE 8080

# Expose debug port for IDE attachment
EXPOSE 5005

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run Maven spring-boot:run for hot-reload
# Source code will be mounted as volume from docker-compose
# Spring Boot DevTools will automatically restart on file changes
CMD ["mvn", "spring-boot:run", "-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"]
