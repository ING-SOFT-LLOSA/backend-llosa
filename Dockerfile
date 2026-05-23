# ==========================================
# 1. ETAPA DE CONSTRUCCIÓN (Build)
# ==========================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiamos el archivo de configuración de dependencias
COPY pom.xml .

# Copiamos todo el código fuente de tu aplicación
COPY src ./src

# Compilamos y empaquetamos el .jar (saltamos los tests para ahorrar tiempo)
RUN mvn clean package -DskipTests

# ==========================================
# 2. ETAPA DE EJECUCIÓN (Runtime)
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiamos el archivo .jar generado en la etapa anterior
COPY --from=build /app/target/*.jar app.jar

# CRUCIAL: Copiamos el archivo .env para que la librería 'spring-dotenv' no falle
COPY .env .env

# Informamos el puerto en el que escucha la app
EXPOSE 8080

# Comando optimizado para arrancar Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]