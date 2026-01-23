plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.droidexplorer.websim"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.droidexplorer.websim"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = project.findProperty("versionName") as String? ?: "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        // ✅ Correct for Compose 1.6.x + Kotlin 1.9.x
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    // ─────────────────────────────────────────────
    // 🔒 CRITICAL: Explicit coroutine version
    // (REQUIRED for CI stability)
    // ─────────────────────────────────────────────
    val coroutinesVersion = "1.7.3"

    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.6.4")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended:1.6.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.4")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // Storage / SAF
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.documentfile:documentfile:1.0.1")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // EXIF
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Security - Encrypted Storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // HTTP Client for TorBox API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // WorkManager for background downloads
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Room (download persistence)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // ─────────────────────────────────────────────
    // ✅ REQUIRED EXPLICIT DEPENDENCIES (DO NOT REMOVE)
    // Fixes GitHub Actions Maven 403 failures
    // ─────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
}
