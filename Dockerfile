# Этап 1: сборка (builder)
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests

# Этап 2: финальный образ (только JAR)
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/app/target/app-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]