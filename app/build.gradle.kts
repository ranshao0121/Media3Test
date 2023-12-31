
import com.jason.cloud.buildsrc.Dependencies
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.jason.cloud.media3test"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jason.cloud.media3test"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        ndk.abiFilters.add("arm64-v8a")
        ndk.abiFilters.add("armeabi-v7a")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:${Dependencies.androidx_core_ktx}")
    implementation("androidx.appcompat:appcompat:${Dependencies.androidx_appcompat}")
    implementation("com.google.android.material:material:${Dependencies.google_material}")
    implementation("androidx.constraintlayout:constraintlayout:${Dependencies.androidx_constraintlayout}")
    implementation("com.geyifeng.immersionbar:immersionbar:${Dependencies.immersionbar}")
    implementation("com.tencent:mmkv:${Dependencies.mmkv}")
    implementation("com.squareup.okhttp3:okhttp:${Dependencies.okhttp3}")

    implementation(project(mapOf("path" to ":media3")))
}