# Multi-stage build for ProtecTalk Spring Boot application
FROM openjdk:21-jdk AS build

# Set working directory
WORKDIR /app

# Copy gradle wrapper and build files
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# Make gradlew executable
RUN chmod +x gradlew

# Download dependencies (this layer will be cached if dependencies don't change)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src/ src/

# Build the application
RUN ./gradlew build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre

# Create application user for security
RUN useradd -r -s /bin/false protectalk

# Create directories for the application and Firebase config
RUN mkdir -p /opt/protectalk/config && \
    chown -R protectalk:protectalk /opt/protectalk

# Set working directory
WORKDIR /opt/protectalk

# Copy the built JAR from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Change ownership of the JAR file
RUN chown protectalk:protectalk app.jar

# Switch to non-root user
USER protectalk

# Expose the application port
EXPOSE 8080

# Set default environment variables
ENV SPRING_PROFILES_ACTIVE=production
ENV FIREBASE_SERVICE_ACCOUNT_PATH=/opt/protectalk/config/firebase-service-account.json

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
