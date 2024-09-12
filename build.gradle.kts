import org.gradle.kotlin.dsl.support.kotlinCompilerOptions

plugins {
    kotlin("jvm") version "2.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.jokerhub.paper.plugin"
version = "1.0"
description = "OrzMC"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    api("org.apache.httpcomponents:httpasyncclient:4.1.5")
    implementation("net.dv8tion:JDA:5.0.0-beta.8") {
        exclude(module="opus-java")
    }
}

val targetJavaVersion = 22
kotlin {
    jvmToolchain(targetJavaVersion)

}

tasks.build {
    dependsOn("shadowJar")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
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