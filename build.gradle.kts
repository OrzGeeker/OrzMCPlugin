plugins {
    kotlin("jvm") version "2.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.jokerhub.paper.plugin"
version = "1.0"
description = "OrzMC"

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
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation("org.apache.httpcomponents:httpasyncclient:4.1.5")
    implementation("net.dv8tion:JDA:5.1.0") {
        exclude(module="opus-java")
    }
    implementation("org.java-websocket:Java-WebSocket:1.5.7")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(22))
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