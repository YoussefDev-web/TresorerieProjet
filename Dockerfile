From eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY . .

Expose 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
