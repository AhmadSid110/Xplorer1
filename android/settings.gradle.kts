pluginManagement {
    repositories {
        gradlePluginPortal()   // Kotlin compiler MIRROR (critical)
        google()
        mavenCentral()         // kept for non-Kotlin libs
    }
    plugins {
        id("com.android.application") version "8.2.2"
        id("com.android.library") version "8.2.2"
        id("org.jetbrains.kotlin.android") version "1.9.22"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
        // 🔥 FORCE Kotlin artifacts to come from Gradle Plugin Portal
        exclusiveContent {
            forRepository {
                gradlePluginPortal()
            }
            filter {
                includeGroup("org.jetbrains.kotlin")
            }
        }

        // Normal Android deps
        google()

        // Everything else
        mavenCentral()
    }
}

rootProject.name = "android"
include(":app")
