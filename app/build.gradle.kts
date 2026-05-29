import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")   // for FCM
    id("com.google.dagger.hilt.android")   // Hilt DI (Dependency Injection)
    id("com.google.devtools.ksp")          // KSP (replaces kapt — faster)
}

// Load secrets.properties.
// The file (secrets.properties) is gitignored so it will not exist on CI or a fresh clone
// until the developer creates their own copy.
// Should have the following:
//   API_BASE_URL=http:...      ← emulator hitting localhost
//   API_BASE_URL=http:...   ← real device on same Wi-Fi
//   API_BASE_URL=https://api.kumbuka...   ← production
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

        // Reads from secrets.properties — falls back to the emulator/mobile device address
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

    // ── Firebase — Auth REMOVED, keeping BoM + analytics for FCM later ────────
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)

    // ── Hilt — dependency injection ───────────────────────────────────────────
    implementation("com.google.dagger:hilt-android:2.59.2")
    ksp("com.google.dagger:hilt-android-compiler:2.59.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // ── Retrofit + OkHttp — Spring Boot API calls ─────────────────────────────
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ── DataStore — secure JWT token storage ──────────────────────────────────
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ── Compose BOM ───────────────────────────────────────────────────────────
    val composeBom = platform("androidx.compose:compose-bom:2024.05.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ── Material 3 ────────────────────────────────────────────────────────────
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // ── Activity + Lifecycle ──────────────────────────────────────────────────
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")

    // ── Navigation ────────────────────────────────────────────────────────────
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ── Core KTX ─────────────────────────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.13.1")

    // ── Unit tests ────────────────────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("com.google.dagger:hilt-android-testing:2.59.2")
    kspTest("com.google.dagger:hilt-android-compiler:2.59.2")

    // ── UI / instrumented tests ───────────────────────────────────────────────
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.navigation:navigation-testing:2.7.7")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.59.2")
    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.59.2")
    androidTestImplementation("io.mockk:mockk-android:1.13.11")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
