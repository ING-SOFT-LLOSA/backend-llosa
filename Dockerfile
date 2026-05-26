# Stage 1: Build the application
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# 1. Crear explícitamente el directorio caché de Maven dentro de la zona de trabajo
RUN mkdir -p /build/.m2/repository && chmod -R 777 /build

COPY pom.xml .
COPY src ./src

ARG FIREBASE_API_KEY
ENV FIREBASE_API_KEY=${FIREBASE_API_KEY}

# 2. Forzar a Maven a escribir localmente usando parámetros del sistema
RUN mvn clean package -DskipTests -Dmaven.repo.local=/build/.m2/repository

# Stage 2: Run the application (Replacing deprecated openjdk image)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# curl es necesario para el healthcheck definido en docker-compose.yml
RUN apk add --no-cache curl

# Alpine uses 'adduser' instead of 'useradd'
RUN adduser -D -u 1001 appuser

COPY --from=builder /build/target/backend-*.jar app.jar
RUN chown appuser:appuser app.jar

RUN mkdir -p /app/secrets && chown appuser:appuser /app/secrets

USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]