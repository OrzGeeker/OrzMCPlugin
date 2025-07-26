import org.gradle.kotlin.dsl.support.serviceOf
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

group = property("plugin_group_id") as String
version = property("plugin_version") as String
description = property("plugin_description") as String

// PaperMC 插件开发，项目配置文档: https://docs.papermc.io/paper/dev/project-setup
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(property("plugin_jdk_min_version") as String))
}
repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
}
dependencies {
    compileOnly("io.papermc.paper:paper-api:${property("plugin_use_papermc_api_version") as String}-R0.1-SNAPSHOT")
    // WebSocket Client For NapCat QQBot
    implementation("org.java-websocket:Java-WebSocket:1.5.7")
    // Java Discord API
    implementation("net.dv8tion:JDA:5.1.0") {
        exclude(module = "opus-java")
        exclude(module = "jackson-databind")
        exclude(module = "jackson-core")
        exclude(module = "tink")
    }
}

// 项目编译时插件添加
plugins {
    kotlin("jvm") version "2.2.0"
    id("com.gradleup.shadow") version "8.3.8"
    // 工程内直接调试服务端插件：https://docs.papermc.io/paper/dev/debugging#using-direct-debugging
    id("xyz.jpenilla.run-paper") version "2.3.1"
    // 自动发布版本配置文档：https://docs.papermc.io/misc/hangar-publishing/
    id("io.papermc.hangar-publish-plugin") version "0.1.3"
}

// 版本发布相关
fun executeGitCommand(vararg command: String): String {
    val byteOut = ByteArrayOutputStream()
    serviceOf<ExecOperations>().exec {
        commandLine = listOf("git", *command)
        standardOutput = byteOut
    }
    return byteOut.toString(Charsets.UTF_8.name()).trim()
}

fun latestCommitMessage(): String {
    return executeGitCommand("log", "-1", "--pretty=%B")
}

val github_run_number = System.getenv("GITHUB_RUN_NUMBER")
val github_branch_name = System.getenv("GITHUB_REF_NAME")
val timestamp_string = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmSS"))
val versionString: String = version as String
val isRelease: Boolean = (github_branch_name == "main")
val suffixedVersion: String = if (isRelease) {
    "${versionString}.${github_run_number}"
} else {
    "${versionString}_${timestamp_string}"
}

// Use the commit description for the changelog
val changelogContent: String = latestCommitMessage()

hangarPublish {
    publications.register("plugin") {
        version = suffixedVersion
        channel = if (isRelease) "Release" else "Snapshot"
        changelog = changelogContent
        id = property("plugin_name") as String
        apiKey = System.getenv("HANGAR_API_TOKEN")
        platforms {
            paper {
                jar = tasks.shadowJar.flatMap { it.archiveFile }
                platformVersions = (property("plugin_support_paper_versions") as String).split(",").map { it.trim() }
            }
        }
    }
}

tasks {
    // 配置工程内直接调试服务端插件
    // gradle-plugin: https://github.com/jpenilla/run-task#basic-usage
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion(property("plugin_debug_server_version") as String)
    }
    // Mojang mappings: https://docs.papermc.io/paper/dev/project-setup/#mojang-mappings
    jar {
        manifest {
            attributes["paperweight-mappings-namespace"] = "mojang"
        }
    }
    shadowJar {
        manifest {
            attributes["paperweight-mappings-namespace"] = "mojang"
        }
        // 移除 `-all` 后缀（可选）
        archiveClassifier.set("")
    }
    build {
        dependsOn("shadowJar")
    }
}