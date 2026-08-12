plugins {
    java
}

group = "smorki"
version = "1.5"
description = "DonutRtp Random Teleport Plugin"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.3")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.9")
    compileOnly("me.clip:placeholderapi:2.11.6")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    processResources {
        filesMatching("plugin.yml") {
            expand("version" to version)
        }
    }
}
