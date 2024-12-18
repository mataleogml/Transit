plugins {
    kotlin("jvm") version "1.9.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.example"
version = "1.0-SNAPSHOT"

// Set Java toolchain to 17
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Set Kotlin to target Java 17
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}

// Set Java compilation to target Java 17
tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()
    options.release.set(17)
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
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation(kotlin("test"))
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("kotlin", "com.example.transit.lib.kotlin")
        minimize()
        manifest {
            attributes(
                "Multi-Release" to true
            )
        }
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