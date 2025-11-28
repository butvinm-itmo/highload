FROM gradle:8-jdk21 AS build
WORKDIR /app

COPY settings.gradle.kts ./
COPY build.gradle.kts ./
RUN mkdir -p config-server
COPY config-server/build.gradle.kts ./config-server/
RUN gradle dependencies --no-daemon

COPY config-server ./config-server
RUN gradle :config-server:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y curl git openssh-client && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/config-server/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
