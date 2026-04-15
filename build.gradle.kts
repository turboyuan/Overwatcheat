import com.github.jengelman.gradle.plugins.shadow.ShadowApplicationPlugin.SHADOW_INSTALL_TASK_NAME
import com.github.jengelman.gradle.plugins.shadow.ShadowApplicationPlugin.SHADOW_SCRIPTS_TASK_NAME
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    id("overwatcheat-kotlin-project")

    application

    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "org.jire.overwatcheat"
version = "5.1.0"

kotlin {
    jvmToolchain(21)
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "21"
    targetCompatibility = "21"
}

dependencies {
    implementation(libs.slf4j.api)
    runtimeOnly(libs.slf4j.simple)

    implementation(libs.fastutil)
    implementation(libs.javacv.platform)

    implementation(libs.affinity)
    implementation(libs.chronicle.core)

    implementation(libs.jna)
    implementation(libs.jna.platform)
}

application {
    applicationName = "Overwatcheat"
    mainClass.set("org.jire.overwatcheat.Main")
    applicationDefaultJvmArgs += arrayOf(
        "-Xmx4g",
        "-Xms1g",

        "-XX:+UnlockExperimentalVMOptions",
        "-XX:+UseZGC",

        // JNA requires access to sun.misc.Unsafe on JDK 21+
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.base/java.time=ALL-UNNAMED",

        "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED",
        "--add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED"
    )
}

tasks {
    // 配置 `run` task，直接 ./gradlew run 即可运行，无需打包
    named<JavaExec>("run") {
        // 工作目录设为项目根目录，这样能找到 overwatcheat.cfg 和 interception.dll
        workingDir = rootDir
        // 构建 DLL 搜索路径：项目根目录 + Interception/library/x64 + 系统默认
        val dllSearchPaths = mutableListOf(rootDir.absolutePath)
        val interceptionDir = file("Interception/library/x64")
        if (interceptionDir.exists()) {
            dllSearchPaths += interceptionDir.absolutePath
        }
        val defaultLibPath = System.getProperty("java.library.path") ?: ""
        dllSearchPaths += defaultLibPath
        val joinedPath = dllSearchPaths.joinToString(File.pathSeparator)
        systemProperty("java.library.path", joinedPath)
        systemProperty("jna.library.path", joinedPath)
    }

    configureShadowJar()
    configureOverwatcheat()
}

fun TaskContainerScope.configureShadowJar() {
    shadowJar {
        archiveBaseName.set("overwatcheat")
        archiveClassifier.set("")
        archiveVersion.set("")

        isZip64 = true
        //minimize() // needs to be updated for Java 19 support
    }
    named<Zip>("distZip").configure {
        enabled = false
    }
    named<Tar>("distTar").configure {
        enabled = false
    }
    named<CreateStartScripts>("startScripts").configure {
        enabled = false
    }
    named<CreateStartScripts>(SHADOW_SCRIPTS_TASK_NAME).configure {
        enabled = false
    }
    named(SHADOW_INSTALL_TASK_NAME).configure {
        enabled = false
    }
    named("shadowDistTar").configure {
        enabled = false
    }
    named("shadowDistZip").configure {
        enabled = false
    }
}

fun TaskContainerScope.configureOverwatcheat() {
    register("overwatcheat") {
        dependsOn(shadowJar)
        doLast {
            val name = "overwatcheat"

            val buildDir = file("build/")

            val dir = buildDir.resolve(name)
            // Graceful delete: if dir is locked (e.g. run.bat still running), fall through
            // and just overwrite the individual files instead of failing the whole build.
            if (dir.exists()) dir.deleteRecursively()
            dir.mkdirs()

            val jarName = "${name}.jar"
            val jar = dir.resolve(jarName)
            val allJar = buildDir.resolve("libs/overwatcheat.jar")
            // Use NIO Files.copy with REPLACE_EXISTING + ATOMIC_MOVE for robustness
            // when the jar is still held open by a running process.
            Files.copy(allJar.toPath(), jar.toPath(), StandardCopyOption.REPLACE_EXISTING)

            dir.writeStartBat(name, jarName)

            fun File.copyFromRoot(path: String) {
                val src = file(path)
                val dst = resolve(path)
                Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }

            dir.copyFromRoot("overwatcheat.cfg")
            dir.copyFromRoot("LICENSE.txt")
            dir.copyFromRoot("README.md")

            // 自动复制 interception.dll（如果存在）
            val interceptionDll = file("Interception/library/x64/interception.dll")
            if (interceptionDll.exists()) {
                Files.copy(
                    interceptionDll.toPath(),
                    dir.resolve("interception.dll").toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
                println("✓ interception.dll copied to ${dir.resolve("interception.dll")}")
            } else {
                println("⚠ WARNING: interception.dll not found at ${interceptionDll.absolutePath}")
                println("  Please extract it from Interception.zip: Interception/library/x64/interception.dll")
            }
        }
    }
}

fun File.writeStartBat(name: String, jarName: String) =
    resolve("run.bat")
        .writeText(
            """@echo off
cd /d "%~dp0"
title $name
java -Djava.library.path=. -Djna.library.path=. ${application.applicationDefaultJvmArgs.joinToString(" ")} -jar "$jarName"
pause"""
        )
