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

        // Expose GEOAPIFY_API_KEY from local.properties to BuildConfig
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
        buildConfig = true // Enable BuildConfig generation
    }

    packagingOptions {
        resources {
            // Exclude problematic files for ORMLite
            excludes += "com/j256/ormlite/core/README.txt"
        }
    }
}

// Exclude ORMLite core if it's causing conflicts or is not directly used
configurations.all {
    exclude(group = "com.j256.ormlite", module = "ormlite-core")
}



dependencies {
    // --- Core Compose Dependencies (Managed by BOM for consistency) ---
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))

    // Compose UI and Material3 modules
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")

    // --- Other AndroidX Dependencies ---
    implementation("androidx.activity:activity-compose:1.9.0")

    // --- Third-Party Libraries ---
    // OSMDroid for maps
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // Google Play Services for location
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // Accompanist permissions
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.6.0")
    // Lottie for animations
    implementation("com.airbnb.android:lottie-compose:6.4.0")
    // Paging for lists
    implementation("androidx.paging:paging-compose:3.3.0")

    // Accompanist libraries
    implementation("com.google.accompanist:accompanist-navigation-animation:0.34.0")
    implementation("com.google.accompanist:accompanist-pager:0.34.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.34.0")
    implementation("com.google.accompanist:accompanist-navigation-material:0.34.0")

    // --- Core AndroidX and Compose dependencies ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // --- Testing dependencies ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // --- Specific OSMDroid modules ---
    implementation("org.osmdroid:osmdroid-mapsforge:6.1.18")
    implementation("org.osmdroid:osmdroid-geopackage:6.1.18")
}