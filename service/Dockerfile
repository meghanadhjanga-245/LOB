# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/lob-simulator-1.0.0-SNAPSHOT.jar /app/lob-service.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/lob-service.jar"]