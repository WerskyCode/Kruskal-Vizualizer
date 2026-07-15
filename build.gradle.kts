import org.gradle.api.tasks.JavaExec
import java.io.File

plugins {
    kotlin("jvm") version "1.9.22"
    application
    // 1. Добавили плагин Shadow для сборки FAT JAR
    id("com.github.johnrengelman.shadow") version "8.1.1"
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
        if (osArch.contains("aarch64") || osArch.contains("arm64")) "mac-aarch64" else "mac"
    }
    else -> "win"
}

dependencies {
    testImplementation(kotlin("test"))

    // Для локального запуска оставляем твою текущую ОС
    implementation("org.openjfx:javafx-base:$javafxVersion:$osClassifier")
    implementation("org.openjfx:javafx-controls:$javafxVersion:$osClassifier")
    implementation("org.openjfx:javafx-graphics:$javafxVersion:$osClassifier")
    implementation("org.openjfx:javafx-fxml:$javafxVersion:$osClassifier")

    // 2. Чтобы у препода точно запустилось на ЛЮБОЙ ОС, пакуем все платформы внутрь JAR:
    val platforms = listOf("win", "linux", "mac", "mac-aarch64")
    for (platform in platforms) {
        if (platform != osClassifier) { // Избегаем дублирования твоей текущей ОС
            runtimeOnly("org.openjfx:javafx-base:$javafxVersion:$platform")
            runtimeOnly("org.openjfx:javafx-controls:$javafxVersion:$platform")
            runtimeOnly("org.openjfx:javafx-graphics:$javafxVersion:$platform")
            runtimeOnly("org.openjfx:javafx-fxml:$javafxVersion:$platform")
        }
    }
}

// 3. Для обычного запуска через Gradle оставляем лаунчер
application {
    mainClass.set("ui.AppLauncherKt")
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

// 4. Настройка сборщика Shadow JAR — указываем точку входа
tasks.named<org.gradle.api.Task>("shadowJar") {
    doFirst {
        val jarTask = this as? com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
        jarTask?.manifest {
            attributes["Main-Class"] = "ui.AppLauncherKt"
        }
    }
}