import com.github.gradle.node.npm.task.NpmTask

plugins {
  base
  distribution
  id("com.github.node-gradle.node") version "7.1.0"
}

node {
  download = true
  version = "24.12.0"
  npmVersion = "11.6.2"
}

val npmCi = tasks.register<NpmTask>("npmCi") {
  group = "build"
  description = "Installs frontend dependencies exactly from package-lock.json."
  dependsOn("npmSetup")
  args.set(listOf("ci"))
  inputs.files("package.json", "package-lock.json")
  outputs.dir("node_modules")
}

val npmBuild = tasks.register<NpmTask>("npmBuild") {
  group = "build"
  description = "Builds the Angular frontend bundle."
  dependsOn(npmCi)
  args.set(listOf("run", "build"))
}

tasks.named("assemble") {
  dependsOn(npmBuild)
}

tasks.register<NpmTask>("lintFrontend") {
  group = "verification"
  description = "Runs ESLint for the Angular frontend."
  dependsOn(npmCi)
  args.set(listOf("run", "lint"))
}

listOf("installDist", "distZip", "distTar").forEach { taskName ->
  tasks.named(taskName) {
    dependsOn(npmBuild)
  }
}

distributions {
  main {
    distributionBaseName = project.name
    contents {
      from(layout.buildDirectory.dir("angular/browser"))
      from(layout.buildDirectory.file("angular/3rdpartylicenses.txt"))
      from(layout.buildDirectory.file("angular/prerendered-routes.json"))
    }
  }
}

tasks.named<Delete>("clean") {
  delete(layout.projectDirectory.dir(".angular"))
  delete(layout.buildDirectory)
}
