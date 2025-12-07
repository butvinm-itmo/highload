FROM gradle:8-jdk21 AS build
WORKDIR /app

COPY settings.gradle.kts ./
COPY build.gradle.kts ./
RUN mkdir -p eureka-server
COPY eureka-server/build.gradle.kts ./eureka-server/
RUN gradle dependencies --no-daemon

COPY eureka-server ./eureka-server
RUN gradle :eureka-server:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/eureka-server/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
