FROM gradle:8-jdk21 AS build
WORKDIR /app

COPY settings.gradle.kts ./
RUN mkdir -p shared-dto user-service
COPY shared-dto/build.gradle.kts ./shared-dto/
COPY user-service/build.gradle.kts ./user-service/
RUN gradle dependencies --no-daemon

COPY shared-dto ./shared-dto
COPY user-service ./user-service
RUN gradle :user-service:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/user-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
