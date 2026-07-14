import org.gradle.api.tasks.JavaExec
import java.io.File

plugins {
    kotlin("jvm") version "1.9.22"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val javafxVersion = "21"
val osName = System.getProperty("os.name").lowercase()
val osArch = System.getProperty("os.arch").lowercase()

val osClassifier = when {
    osName.contains("win") -> "win"
    osName.contains("linux") -> "linux"
    osName.contains("mac") -> {
        // Если это Mac и архитектура arm64/aarch64 — задаем правильный классификатор
        if (osArch.contains("aarch64") || osArch.contains("arm64")) "mac-aarch64" else "mac"
    }

    else -> "win"
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("org.openjfx:javafx-base:$javafxVersion:$osClassifier")
    implementation("org.openjfx:javafx-controls:$javafxVersion:$osClassifier")
    implementation("org.openjfx:javafx-graphics:$javafxVersion:$osClassifier")
    implementation("org.openjfx:javafx-fxml:$javafxVersion:$osClassifier")
}

application {
    mainClass.set("ui.MainApp")
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<JavaExec> {
    doFirst {
        val javafxModules = configurations.runtimeClasspath.get().files.filter {
            it.name.contains("javafx") && it.name.contains(osClassifier)
        }

        if (javafxModules.isNotEmpty()) {
            val modulePath = javafxModules.joinToString(File.pathSeparator) { it.absolutePath }
            jvmArgs = listOf(
                "--module-path", modulePath,
                "--add-modules", "javafx.base,javafx.controls,javafx.graphics,javafx.fxml"
            )
        }
    }
}