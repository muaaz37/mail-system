import org.gradle.api.tasks.Exec

plugins {
    distribution
}

val frontendInstallDistDir = project(":frontend").layout.buildDirectory.dir("install/frontend")
val backendInstallDistDir = project(":backend").layout.buildDirectory.dir("install/backend")

tasks.named("assemble") {
    dependsOn(":frontend:assemble")
    dependsOn(":backend:assemble")
}

tasks.named("check") {
    dependsOn(":frontend:check")
    dependsOn(":backend:check")
}

tasks.named("build") {
    dependsOn(":frontend:build")
    dependsOn(":backend:build")
}

tasks.named("clean") {
    dependsOn(":frontend:clean")
    dependsOn(":backend:clean")
}

tasks.register("lint") {
    group = "verification"
    description = "Runs static code quality checks for frontend and backend."
    dependsOn(":frontend:lintFrontend")
    dependsOn(":backend:lintBackend")
}

distributions {
    main {
        distributionBaseName = rootProject.name
        contents {
            into("frontend") {
                from(frontendInstallDistDir)
            }
            into("backend") {
                from(backendInstallDistDir)
            }
        }
    }
}

listOf("installDist", "distZip", "distTar").forEach { taskName ->
    tasks.named(taskName) {
        dependsOn(":frontend:$taskName")
        dependsOn(":backend:$taskName")
    }
}

tasks.register<Exec>("startLocal") {
    group = "application"
    description = "Builds and starts the complete local Docker Compose stack."
    commandLine("bash", "scripts/start-local.sh")
}

tasks.register<Exec>("stopLocal") {
    group = "application"
    description = "Stops the local Docker Compose stack."
    commandLine("bash", "scripts/docker-compose-local.sh", "down")
}

tasks.register<Exec>("resetLocal") {
    group = "application"
    description = "Stops the local Docker Compose stack and removes persisted Docker volumes."
    commandLine("bash", "scripts/docker-compose-local.sh", "down", "-v")
}
