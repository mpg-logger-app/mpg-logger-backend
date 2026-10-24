# Container image for Google Cloud Run (also runs anywhere Docker does).
# Build:  docker build -t mpg-logger-backend .
# Run:    docker run -p 8080:8080 -e DB_URL=... -e DB_USERNAME=... -e DB_PASSWORD=... mpg-logger-backend

# ---- Build: compile and package the jar (tests run in CI / locally, not here) ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
# Dependencies first, so they're cached between builds unless pom.xml changes.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sh ./mvnw -q -B dependency:go-offline
COPY src src
RUN sh ./mvnw -q -B -DskipTests package

# ---- Run: just a JRE and the jar ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Use most of the container's memory for the heap, and start faster (helps Cloud Run cold starts).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:TieredStopAtLevel=1"
# Cloud Run sends traffic to $PORT (8080 by default); application.properties reads it.
EXPOSE 8080
USER 1000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
