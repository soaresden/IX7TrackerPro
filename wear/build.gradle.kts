plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ix7.tracker.wear"  // ✅ Au lieu de "com.ix7.tracker.com"
    compileSdk = 34  // ALIGNÉ avec app

    defaultConfig {
        applicationId = "com.ix7.tracker.wear"
        minSdk = 30  // Wear OS 3.0+
        targetSdk = 34  // ALIGNÉ avec app
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8  // ALIGNÉ avec app
        targetCompatibility = JavaVersion.VERSION_1_8  // ALIGNÉ avec app
    }

    kotlinOptions {
        jvmTarget = "1.8"  // ALIGNÉ avec app
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"  // ALIGNÉ avec app
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Wear OS
    implementation("androidx.wear:wear:1.3.0")
    implementation("androidx.wear.compose:compose-foundation:1.3.0")
    implementation("androidx.wear.compose:compose-material:1.3.0")

    // Compose (ALIGNÉ avec app) - AJOUTÉES
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")  // ⭐ AJOUTÉE - pour layouts
    implementation("androidx.compose.material:material")      // ⭐ AJOUTÉE - pour Button, Text
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime")         // ⭐ AJOUTÉE - pour state management
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.activity:activity-ktx:1.8.0")

    // Core (ALIGNÉ avec app)
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Room Database
    implementation("androidx.room:room-runtime:2.5.1")
    implementation("androidx.room:room-ktx:2.5.1")

    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    // Sync avec Android Wear
    implementation("com.google.android.gms:play-services-wearable:18.0.0")

    // BouncyCastle
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}