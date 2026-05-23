FROM openjdk:25-ea-21-jdk-slim
WORKDIR /app
COPY requirements.txt .
RUN 
COPY . .
EXPOSE 8000
CMD ["java", "-jar", "app.jar"]