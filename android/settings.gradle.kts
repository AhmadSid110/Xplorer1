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

        // 🔒 Kotlin compiler + stdlib
        exclusiveContent {
            forRepository {
                gradlePluginPortal()
            }
            filter {
                includeGroup("org.jetbrains.kotlin")
            }
        }

        // 🔒 KotlinX (coroutines, serialization, etc.)
        exclusiveContent {
            forRepository {
                google()
            }
            filter {
                includeGroup("org.jetbrains.kotlinx")
            }
        }

        // Android + Jetpack
        google()

        // Everything else
        mavenCentral()
    }
}

rootProject.name = "android"
include(":app")
