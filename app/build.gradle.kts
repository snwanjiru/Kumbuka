import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

// Load secrets.properties if it exists on this machine.
// The file is gitignored so it will not exist on CI or a fresh clone
// until the developer creates their own copy.
val secretsFile = rootProject.file("secrets.properties")
val secrets = if (secretsFile.exists()) {
    Properties().apply { load(secretsFile.inputStream()) }
} else {
    Properties()
}

android {
    namespace = "com.example.kumbuka"
    compileSdk = 35   // ← plain integer, not the release() DSL block

    defaultConfig {
        applicationId = "com.example.kumbuka"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Reads from secrets.properties — falls back to the emulator address
        // if the file doesn't exist (safe default for local development)
        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"${secrets.getProperty("API_BASE_URL", "http://10.0.2.2:8080/")}\""
        )
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

    buildFeatures {
        compose = true
        buildConfig = true   // generates the BuildConfig class so API_BASE_URL is accessible
    }
}

dependencies {
    // Firebase BoM and Libraries
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)

    implementation("com.google.android.gms:play-services-auth:21.5.1")

    // Lifecycle ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")

    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.05.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Material 3
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Activity + Lifecycle
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Core KTX
    implementation("androidx.core:core-ktx:1.13.1")
    implementation(libs.androidx.preference.ktx)
}