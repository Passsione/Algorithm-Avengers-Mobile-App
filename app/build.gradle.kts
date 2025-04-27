plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.pbdvmobile.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pbdvmobile.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.recyclerview)
    implementation(libs.firebase.crashlytics.buildtools)
    // Room components
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")

    // Gson dependency
    implementation("com.google.code.gson:gson:2.12")
    // Optional - Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:$roomVersion")

    // Optional - Test helpers
    testImplementation("androidx.room:room-testing:$roomVersion")

    // If you're using Kotlin instead of Java, you should use kapt instead of annotationProcessor
    // kapt("androidx.room:room-compiler:$roomVersion")

    implementation(libs.appcompat)
    implementation(libs.glide)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.activity)
    implementation(libs.annotation)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}