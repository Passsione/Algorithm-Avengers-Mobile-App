plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
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
    implementation(libs.play.services.wallet)
    // Room components
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")

    // Gson dependency
    implementation("com.google.code.gson:gson:2.10.1")
    // Optional - Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:$roomVersion")

        implementation("com.google.android.gms:play-services-wallet:19.4.0")
        implementation("com.google.android.material:material:1.12.0")

    // Optional - Test helpers
    testImplementation("androidx.room:room-testing:$roomVersion")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")


    implementation("com.squareup.retrofit2:retrofit:2.9.0") // Or the latest version
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // For JSON conversion
    implementation("com.squareup.okhttp3:okhttp:4.11.0") // Or the latest version
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0") // Optional, for logging requests

    // FireBase
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage") // For file uploads later



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