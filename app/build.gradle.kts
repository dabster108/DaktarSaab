//import java.util.Properties
//
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

// Read local.properties to get the API key
//val localProperties = Properties().apply {
//    val localFile = rootProject.file("local.properties")
//    if (localFile.exists()) {
//        localFile.inputStream().use { this.load(it) }
//    }
//}

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

        // Expose GROQ_API_KEY from local.properties to BuildConfig

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
configurations.all {
    exclude(group = "com.j256.ormlite", module = "ormlite-core")
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.compose.ui.graphics)
    androidTestImplementation(platform(libs.compose.bom))

    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    implementation(libs.material) // Added Material Components for Android

    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation(libs.paging.compose)

    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.navigation.animation)
    implementation(libs.accompanist.navigation.material)
    implementation(libs.accompanist.pager)
    implementation(libs.accompanist.pager.indicators)

    implementation(libs.coil.compose)
    implementation(libs.lottie.compose)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    implementation(libs.play.services.location)
    implementation(libs.osmdroid.android)
    implementation(libs.osmdroid.geopackage)
    implementation(libs.osmdroid.mapsforge)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
}
