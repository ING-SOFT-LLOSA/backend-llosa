FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src

ARG FIREBASE_API_KEY
ENV FIREBASE_API_KEY=${FIREBASE_API_KEY}

RUN mvn clean package -DskipTests

FROM openjdk:21-jdk-slim
WORKDIR /app

RUN useradd -m -u 1001 appuser

COPY --from=builder /build/target/backend-*.jar app.jar
RUN chown appuser:appuser app.jar

USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]