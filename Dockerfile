# Stage 1: Build the application
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Fix permissions for the maven user
RUN chown -R maven:maven /build
USER maven

# Copy files with correct ownership
COPY --chown=maven:maven pom.xml .
COPY --chown=maven:maven src ./src

ARG FIREBASE_API_KEY
ENV FIREBASE_API_KEY=${FIREBASE_API_KEY}

# Maven now writes to /home/maven/.m2/repository without permission blocks
RUN mvn clean package -DskipTests

# Stage 2: Run the application (Replacing deprecated openjdk image)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Alpine uses 'adduser' instead of 'useradd'
RUN adduser -D -u 1001 appuser

COPY --from=builder /build/target/backend-*.jar app.jar
RUN chown appuser:appuser app.jar

USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]