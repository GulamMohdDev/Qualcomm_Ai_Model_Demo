
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.gm.qualcommaimodelsdemo"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.gm.qualcommaimodelsdemo"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "MODE", "\"FREE\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        mlModelBinding = true
        buildConfig = true
    }
    aaptOptions {
        noCompress("tflite")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.tensorflow.lite.metadata)
    implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite.gpu)
    implementation(libs.object1.detection.common)
    implementation(libs.object1.detection)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose.v260)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.litert.gpu.api)
    implementation(libs.androidx.media3.exoplayer)
    kapt(libs.hilt.android.compiler)
    implementation(libs.accompanist.permissions)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.androidx.media3.exoplayer.v131)
    implementation(libs.androidx.media3.ui)
}