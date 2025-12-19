plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.antimoshennik.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.antimoshennik.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 4
        versionName = "1.2.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file("../release-key.jks")
            storePassword = "antimosh123"
            keyAlias = "antimoshennik"
            keyPassword = "antimosh123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.alphacephei:vosk-android:0.3.47")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
