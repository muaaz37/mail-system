import org.gradle.kotlin.dsl.withType
plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.spring") version "2.2.20"
    id("org.springframework.boot") version "4.0.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("dev.detekt") version "2.0.0-alpha.1"
    application
}

apply(plugin = "java")
application {
    // Kotlin top-level main functions are compiled with a Kt suffix.
    mainClass = "de.thm.mni.backend.BackendApplicationKt"
}
group = "de.thm.mni"
version = "0.0.1-SNAPSHOT"
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
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.22.0")
    implementation(platform("software.amazon.awssdk:bom:2.25.60"))
    implementation("software.amazon.awssdk:s3")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.springframework.boot:spring-boot-starter-integration")
    implementation("org.springframework.integration:spring-integration-mail")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")


    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

detekt {
    // Version of detekt that will be used. When unspecified the latest detekt
    // version found will be used. Override to stay on the same version.
    toolVersion = "2.0.0-alpha.1"

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
