plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.expensetracker"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.expensetracker"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mpandroidchart)
    // AndroidX
    implementation(libs.appcompat)
    implementation(libs.androidx.core.ktx)

    // Material Design
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.biometric)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

    // Room compiler for JAVA
    annotationProcessor(libs.room.compiler)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}