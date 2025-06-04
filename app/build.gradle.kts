import java.util.Properties // Keep this import as requested

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

        // Expose GROQ_API_KEY from local.properties to BuildConfig
        buildConfigField("String", "GROQ_API_KEY", "\"${localProperties.getProperty("groqApiKey")}\"")
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Removed redundant ORMLite exclusion if not specifically needed for packaging
            // excludes += "com/j256/ormlite/core/README.txt"
        }
    }
}

// Exclude ORMLite core if it's causing conflicts or is not directly used
// Keep this only if you specifically know ORMLite is causing issues and you're not using it.
// If you are using ORMLite, this line should be removed, and the correct ORMLite dependencies added.
configurations.all {
    exclude(group = "com.j256.ormlite", module = "ormlite-core")
}

dependencies {
    // --- Core Compose Dependencies (Managed by BOM for consistency) ---
    // Using 2024.05.00 as it's the latest stable at the time of writing (assuming June 2025 context implies recent versions)
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))

    // Compose UI and Material3 modules (using BOM versions)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")

    // Extended Material Icons (for VolumeUp, Mic, Stop icons)
    implementation("androidx.compose.material:material-icons-extended")

    // Activity Compose (for ComponentActivity and rememberLauncherForActivityResult)
    implementation("androidx.activity:activity-compose:1.9.0")

    // ViewModel for Compose and Lifecycle runtime ktx
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0") // Updated to 2.8.0
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0") // Updated to 2.8.0

    // --- Networking Libraries for Groq API ---
    // Retrofit for making HTTP requests
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // GSON Converter for JSON parsing with Retrofit
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp (dependency for Retrofit and direct usage if any, ensuring single version)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // OkHttp Logging Interceptor for seeing network requests in Logcat
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")


    // --- Third-Party Libraries ---
    // OSMDroid for maps (latest stable)
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("org.osmdroid:osmdroid-mapsforge:6.1.18") // Specific module
    implementation("org.osmdroid:osmdroid-geopackage:6.1.18") // Specific module

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

    // Accompanist libraries (ensure consistent version)
    implementation("com.google.accompanist:accompanist-navigation-animation:0.34.0")
    implementation("com.google.accompanist:accompanist-pager:0.34.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.34.0")
    implementation("com.google.accompanist:accompanist-navigation-material:0.34.0")

    // --- Core AndroidX and Compose dependencies (using libs aliases if defined) ---
    // Assuming libs.androidx.core.ktx points to androidx.core:core-ktx
    implementation(libs.androidx.core.ktx)
    // Removed redundant lifecycle.runtime.ktx here as it's already defined with 2.8.0
    // Removed redundant activity.compose here as it's already defined with 1.9.0
    // Removed redundant ui, ui.graphics, ui.tooling.preview, material3 as they are managed by compose-bom above
    // If you prefer to use aliases for these, ensure they point to the BOM-managed versions or remove the explicit BOM line.

    // --- Testing dependencies ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom)) // Ensures testing modules use same Compose versions
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}