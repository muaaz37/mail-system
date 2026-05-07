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
// Note: the main class in Kotlin has a "Kt" suffix when compiled,
// so we need to specify it here for the application plugin to work correctly
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
    // Zur kompilieren und zur Laufzeit da
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    // nur für Test
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // Nur zum laufzeit aber nicht zum kompilieren nötig
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