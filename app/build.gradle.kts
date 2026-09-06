plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.right9code.anyhome"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.right9code.anyhome"
        minSdk = 26
        targetSdk = 35
        versionCode = getVersionCode()
        versionName = getVersionName()
    }

    buildFeatures {
        viewBinding = true
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = "anyhome123"
            keyAlias = "anyhome"
            keyPassword = "anyhome123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
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
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.preference:preference:1.2.1")
}

fun getVersionCode(): Int {
    return 2
}

fun getVersionName(): String {
    return "0.0.2-alpha"
}
