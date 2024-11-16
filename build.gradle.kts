plugins {
    kotlin("jvm") version "1.9.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    implementation(kotlin("stdlib"))
    
    // Add test dependencies
    testImplementation(kotlin("test"))
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("kotlin", "com.example.transit.lib.kotlin")
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
    
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
}