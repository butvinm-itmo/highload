FROM gradle:8-jdk21 AS build
WORKDIR /app
COPY settings.gradle.kts ./
COPY shared-dto ./shared-dto
COPY tarot-service ./tarot-service
RUN gradle :tarot-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/tarot-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
