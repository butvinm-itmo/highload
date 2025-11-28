FROM gradle:8-jdk21 AS build
WORKDIR /app
COPY settings.gradle.kts ./
COPY shared-dto ./shared-dto
COPY user-service ./user-service
RUN gradle :user-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/user-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
