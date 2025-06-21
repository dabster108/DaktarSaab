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
            signingConfig = signingConfigs.getByName("debug")
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Exclude duplicate license files from mail libraries
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE.md"
            excludes += "/META-INF/NOTICE.txt"
        }
    }
}

// Exclude ORMLite core if it's causing conflicts or is not directly used
configurations.all {
    exclude(group = "com.j256.ormlite", module = "ormlite-core")
}

dependencies {
    implementation(platform(libs.compose.bom))

    // Firebase dependencies - use BoM to ensure version compatibility
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx:24.10.2")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-database-ktx")

    implementation(libs.compose.ui.graphics)
    androidTestImplementation(platform(libs.compose.bom))

    // Cloudinary SDK for image upload
    implementation("com.cloudinary:cloudinary-android:2.3.1")

    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    // Add Compose-LiveData integration
    implementation(libs.androidx.runtime.livedata)

    implementation(libs.material) // Added Material Components for Android

    // JavaMail API for SMTP email sending
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")

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

    // Google Sign-In dependency
    implementation("com.google.android.gms:play-services-auth:20.7.0")

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

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Lottie Animation
    implementation("com.airbnb.android:lottie-compose:6.3.0")

    // Compose Animation
    implementation("androidx.compose.animation:animation:1.5.4")

    // Material Design Icons
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
}
