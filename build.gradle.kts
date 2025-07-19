// PaperMC 插件开发，项目配置文档: https://docs.papermc.io/paper/dev/project-setup

group = "com.jokerhub.paper.plugin"
version = "1.21.7"
description = "OrzMC"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
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
    compileOnly("io.papermc.paper:paper-api:1.21.7-R0.1-SNAPSHOT")
    // WebSocket Client For NapCat QQBot
    implementation("org.java-websocket:Java-WebSocket:1.5.7")
    // Java Discord API
    implementation("net.dv8tion:JDA:5.1.0") {
        exclude(module = "opus-java")
        exclude(module = "commons-collections4")
        exclude(module = "jackson-databind")
        exclude(module = "jackson-core")
        exclude(module = "tink")
    }
}

// 项目编译时插件添加
plugins {
    kotlin("jvm") version "2.2.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    // 工程内直接调试服务端插件：https://docs.papermc.io/paper/dev/debugging#using-direct-debugging
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

tasks {
    // 配置工程内直接调试服务端插件
    // gradle-plugin: https://github.com/jpenilla/run-task#basic-usage
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21.7")
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