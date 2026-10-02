# ---- Build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S boticario && adduser -S boticario -G boticario
COPY --from=build /app/target/boticario-backend.jar app.jar
USER boticario
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
