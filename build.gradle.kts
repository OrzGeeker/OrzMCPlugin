group = "com.jokerhub.paper.plugin"
version = "1.0"
description = "OrzMC"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    // 参考：https://docs.papermc.io/paper/dev/debugging#using-direct-debugging
    // gradle-plugin: https://github.com/jpenilla/run-task#basic-usage
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21.3")
    }
}

plugins {
    kotlin("jvm") version "2.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

// Paper Project Setup: https://docs.papermc.io/paper/dev/project-setup
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("io.papermc.paper:paper-api:1.21.3-R0.1-SNAPSHOT")
    implementation("org.apache.httpcomponents:httpasyncclient:4.1.5")
    implementation("net.dv8tion:JDA:5.1.0") {
        exclude(module = "opus-java")
    }
    implementation("org.java-websocket:Java-WebSocket:1.5.7")
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.jar {
    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }
}
// if you have shadowJar configured
tasks.shadowJar {
    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }
}