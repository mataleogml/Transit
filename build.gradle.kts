plugins {
    kotlin("jvm") version "1.9.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.gradle.enterprise") version "3.18.1"
}

group = "com.example"
version = "1.0-SNAPSHOT"

// Set Java toolchain
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Ensure Kotlin targets Java 17
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}

// Ensure Java compilation also targets Java 17
tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    implementation(kotlin("stdlib"))
    
    testImplementation(kotlin("test"))
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("kotlin", "com.example.transit.lib.kotlin")
        minimize()
    }
    
    processResources {
        filesMatching("plugin.yml") {
            expand(
                "version" to project.version,
                "name" to project.name
            )
        }
    }

    test {
        useJUnitPlatform()
    }
}

// Configure Gradle Enterprise Plugin
gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}