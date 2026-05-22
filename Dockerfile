# 1. Etapa de Construcción (Build)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiamos el pom y el código fuente
COPY pom.xml .
COPY src ./src

# Compilamos el proyecto (ignoramos los tests para que el empaquetado sea rápido)
RUN mvn clean package -DskipTests

# 2. Etapa de Ejecución (Runtime)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiamos el .jar generado desde la etapa de construcción
COPY --from=build /app/target/*.jar app.jar

# Exponemos el puerto de tu API
EXPOSE 8080

# Comando de arranque
ENTRYPOINT ["java", "-jar", "app.jar"]