plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.spring") version "2.2.10"
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

dependencies {
    api(project(":shared-dto"))
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2025.0.0"))
    api("org.springframework.cloud:spring-cloud-starter-openfeign")
    api("io.github.openfeign.form:feign-form:3.8.0")
    api("io.github.openfeign.form:feign-form-spring:3.8.0")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

ktlint {
    version.set("1.5.0")
}
