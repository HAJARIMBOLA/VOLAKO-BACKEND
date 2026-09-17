# --- Build stage ---
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S volako && adduser -S volako -G volako
COPY --from=build /app/target/*.jar app.jar
RUN chown volako:volako app.jar
USER volako

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
