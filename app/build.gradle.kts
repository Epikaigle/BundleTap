plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val personalSigningStore = providers.environmentVariable("BUNDLETAP_SIGNING_STORE").orNull

android {
    namespace = "com.epikaigle.bundletap"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.epikaigle.bundletap"
        minSdk = 37
        targetSdk = 37
        versionCode = 3
        versionName = "1.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        if (!personalSigningStore.isNullOrBlank()) {
            create("personalRelease") {
                storeFile = file(personalSigningStore)
                storePassword = providers.environmentVariable("BUNDLETAP_SIGNING_PASSWORD").orNull
                keyAlias = providers.environmentVariable("BUNDLETAP_SIGNING_ALIAS").orNull
                keyPassword = providers.environmentVariable("BUNDLETAP_SIGNING_KEY_PASSWORD").orNull
            }
        }
    }
    buildTypes {
        release {
            if (!personalSigningStore.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("personalRelease")
            }
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    testImplementation("junit:junit:4.13.2")
}
