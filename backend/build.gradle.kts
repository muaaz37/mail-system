import org.gradle.kotlin.dsl.withType

plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("dev.detekt") version "2.0.0-alpha.3"
    application
}

val springBootVersion = "4.1.0"
val kotlinVersion = "2.3.21"
val springIntegrationVersion = "7.1.0"
val springSecurityVersion = "7.1.0"
val springDocOpenApiVersion = "3.0.3"
val jacksonVersion = "3.1.4"
val awsSdkVersion = "2.25.60"
val postgresqlVersion = "42.7.11"
val h2Version = "2.4.240"
val junitPlatformVersion = "6.0.3"

apply(plugin = "java")
application {
    // Kotlin top-level main functions are compiled with a Kt suffix.
    mainClass = "de.thm.mni.backend.BackendApplicationKt"
}
group = "de.thm.mni"
version = "0.0.1"
description = "backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("software.amazon.awssdk:bom:$awsSdkVersion"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-security:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-validation:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-mail:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-integration:$springBootVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocOpenApiVersion")
    implementation("org.springframework.integration:spring-integration-mail:$springIntegrationVersion")
    implementation("software.amazon.awssdk:s3:$awsSdkVersion")
    implementation("tools.jackson.module:jackson-module-kotlin:$jacksonVersion")

    runtimeOnly("org.postgresql:postgresql:$postgresqlVersion")
    runtimeOnly("com.h2database:h2:$h2Version")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test:$springBootVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")
    testImplementation("org.springframework.security:spring-security-test:$springSecurityVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

detekt {
    // Keep detekt pinned so static analysis is reproducible across machines.
    toolVersion = "2.0.0-alpha.3"

    // The directories where detekt looks for source files.
    // Defaults to `files("src/main/java", "src/test/java", "src/main/kotlin", "src/test/kotlin")`.
    source.setFrom("src/main/java", "src/main/kotlin")

    // Specify the base path for file paths in the formatted reports.
    // If not set, all file paths reported will be absolute file path.
    basePath.set(projectDir)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register("lintBackend") {
    group = "verification"
    description = "Runs static code quality checks for the backend."
    dependsOn("detekt")
}
