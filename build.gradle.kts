import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    kotlin("jvm") version "2.2.10" apply false
    kotlin("plugin.spring") version "2.2.10" apply false
    kotlin("plugin.jpa") version "2.2.10" apply false
    id("org.springframework.boot") version "3.5.6" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2" apply false
    id("jacoco")
}

group = "com.github.butvinmitmo"
version = "0.1.0-SNAPSHOT"
description = "Tarology Web Service"

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "jacoco")

    tasks.withType<Test>().configureEach {
        testLogging {
            events("passed", "failed", "skipped")
        }
        finalizedBy("jacocoTestReport")
    }

    tasks.withType<JacocoReport>().configureEach {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}

tasks.register<Exec>("dockerComposeUp") {
    group = "docker"
    description = "Rebuild and start all Docker containers, waiting for health checks"
    commandLine("docker", "compose", "up", "-d", "--build", "--wait")
}

tasks.register<Exec>("dockerComposeDown") {
    group = "docker"
    description = "Stop and remove all Docker containers"
    commandLine("docker", "compose", "down")
}

tasks.register("e2e") {
    group = "verification"
    description = "Rebuild containers, wait for health checks, then run e2e tests"
    dependsOn(":e2e-tests:test")
}