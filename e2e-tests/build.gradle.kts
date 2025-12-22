plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.spring") version "2.2.10"
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
}

group = "com.github.butvinmitmo"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2025.0.0"

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

dependencies {
    implementation(project(":shared-dto"))
    implementation(project(":shared-clients"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    // Apache HttpClient5 for PATCH method support
    implementation("io.github.openfeign:feign-hc5:13.5")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    dependsOn(":dockerComposeUp")

    systemProperty(
        "GATEWAY_URL",
        System.getProperty("GATEWAY_URL") ?: System.getenv("GATEWAY_URL") ?: "http://localhost:8080",
    )

    doFirst {
        println("═══════════════════════════════════════════════════════════════════")
        println("E2E Tests - Pre-Running Application Mode")
        println("═══════════════════════════════════════════════════════════════════")
        println("Gateway URL: ${systemProperties["GATEWAY_URL"]}")
        println("")
        println("⚠ IMPORTANT: Services must be running before tests execute!")
        println("")
        println("To start services:")
        println("  → docker compose up -d")
        println("")
        println("If you modified code, rebuild containers first:")
        println("  → docker compose up -d --build")
        println("")
        println("To stop services after testing:")
        println("  → docker compose down")
        println("═══════════════════════════════════════════════════════════════════")
        println("")
    }

    doLast {
        println("")
        println("═══════════════════════════════════════════════════════════════════")
        println("E2E Tests Completed")
        println("═══════════════════════════════════════════════════════════════════")
        println("")
        println("⚠ REMINDER: If you modified code, rebuild containers before next run:")
        println("  → docker compose up -d --build")
        println("")
        println("To stop services:")
        println("  → docker compose down")
        println("═══════════════════════════════════════════════════════════════════")
    }
}

ktlint {
    version.set("1.5.0")
}

// Disable bootJar since this is a test-only module
tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}
