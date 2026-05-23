# Build stage
FROM maven:3.9.8-openjdk-21-slim as builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=builder /build/target/backend-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]