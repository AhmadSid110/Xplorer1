pluginManagement {
    repositories {
        gradlePluginPortal()   // Kotlin + compiler artifacts
        google()
        mavenCentral()
    }
    plugins {
        id("com.android.application") version "8.2.2"
        id("org.jetbrains.kotlin.android") version "1.9.22"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {

        // 🔒 Kotlin compiler & stdlib
        exclusiveContent {
            forRepository {
                gradlePluginPortal()
            }
            filter {
                includeGroup("org.jetbrains.kotlin")
            }
        }

        // Android / Jetpack
        google()

        // KotlinX, Coil, Coroutines, everything else
        mavenCentral()
    }
}

rootProject.name = "android"
include(":app")
