plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // kotlinx.serialization — needed for @Serializable data classes (Phase 3)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.naveedali.claudecodereview"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.naveedali.claudecodereview"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ── Phase 3: OpenAI API key ───────────────────────────────────────────
        // Reads OPENAI_API_KEY from local.properties (never committed to git).
        // Access at runtime via BuildConfig.OPENAI_API_KEY.
        // If the key is absent the app falls back to the local rule-based analyser.
        val openAiKey = (project.findProperty("OPENAI_API_KEY") as? String) ?: ""
        buildConfigField("String", "OPENAI_API_KEY", "\"$openAiKey\"")
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
        // Enables BuildConfig generation (needed for BuildConfig.OPENAI_API_KEY)
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    // ── Phase 3: networking + JSON ────────────────────────────────────────────
    // OkHttp: HTTP client for the OpenAI Chat Completions call
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)          // logs requests/responses in debug builds
    // kotlinx.serialization: encode request / decode response JSON
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}