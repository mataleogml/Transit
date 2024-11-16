rootProject.name = "Transit"

plugins {
    id("com.gradle.develocity") version "3.18.1"
}

// Configure Develocity
develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"
    }
}