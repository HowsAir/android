plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.mborper.breathbetter"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mborper.breathbetter"
        minSdk = 26
        targetSdk = 34
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
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.constraintlayout.v220)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.retrofit)
    implementation(libs.converter.gson.v290)
    implementation(libs.logging.interceptor)
    androidTestImplementation(libs.runner)
    androidTestImplementation(libs.core)
    androidTestImplementation(libs.rules)
    testImplementation(libs.mockito.mockito.core)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation (libs.code.scanner)
    implementation(libs.biometric)
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
}