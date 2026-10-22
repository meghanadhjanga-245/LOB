# Stage 1: Build the unified application
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY engine engine/
COPY flow flow/
COPY experiments experiments/
COPY benchmarks benchmarks/
COPY notebooks notebooks/
COPY service service/
COPY web web/

# Build Java service (with embedded static React frontend)
RUN mvn clean package -pl service -am -DskipTests
RUN ls /app/service/target/*.jar | grep -v "plain" | head -n 1 | xargs -I {} cp {} /app/service/target/app.jar

# Stage 2: Minimal Production JRE image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/service/target/app.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Xmx1024m", "-jar", "app.jar"]
