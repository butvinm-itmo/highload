FROM gradle:8-jdk21 AS build
WORKDIR /app

ENV GRADLE_USER_HOME=/home/gradle/.gradle

COPY . .

RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle :user-service:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/user-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
