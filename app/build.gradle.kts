// build.gradle.kts (Module: app)

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Read local.properties to get the API key
val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { this.load(it) }
    }
}

android {
    namespace = "com.example.daktarsaab"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.daktarsaab"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GEOAPIFY_API_KEY", "\"${localProperties.getProperty("geoapifyApiKey", "6acbf75b57b74b749fd87b61351b7c77")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packagingOptions {
        resources {
            excludes += "com/j256/ormlite/core/README.txt"
            // Add any other specific resource excludes if they pop up
        }
    }
}

// --- Add this block to your build.gradle.kts ---
configurations.all {
    exclude(group = "com.j256.ormlite", module = "ormlite-core")
}
// --- End of new block ---

dependencies {
    // START: osmdroid dependencies for OpenStreetMap (base for Geoapify)
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("org.slf4j:slf4j-android:1.7.30")
    // END: osmdroid dependencies

    // Keep this for clarity, though the global exclude might make it redundant.
    // It specifically prevents ormlite-android from pulling in ormlite-core.
    implementation("com.j256.ormlite:ormlite-android:6.1") {
        exclude(group = "com.j256.ormlite", module = "ormlite-core")
    }

    // Your existing Compose and other dependencies
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("com.airbnb.android:lottie-compose:6.0.0")
    implementation("androidx.compose.foundation:foundation:1.5.1")
    implementation("androidx.paging:paging-compose:3.3.0")
    implementation("androidx.compose.material3:material3:1.1.1")

    // Accompanist libraries
    implementation("com.google.accompanist:accompanist-navigation-animation:0.34.0")
    implementation("com.google.accompanist:accompanist-pager:0.34.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.34.0")
    implementation("com.google.accompanist:accompanist-navigation-material:0.34.0")

    implementation("androidx.compose.animation:animation:1.5.1")

    // Core AndroidX and Compose dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Additional dependencies for osmdroid or other libraries
    implementation("org.osmdroid:osmdroid-mapsforge:6.1.18")
    implementation("org.osmdroid:osmdroid-geopackage:6.1.18")
}