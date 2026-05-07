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
apply(plugin = "dev.detekt")
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


tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register("helloTask"){
    group="hello"
    description="A simple hello world task"
    dependsOn(tasks.test)

    doFirst {
        println("Executed first during the execution phase")
    }

    doLast{
        println("Executed last after the execution phase")
    }
}
