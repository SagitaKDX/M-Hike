import java.util.Properties
import java.io.FileInputStream

val localProperties: Properties = run {
    val propertiesFile = rootProject.file("local.properties")
    val properties = Properties()
    if (propertiesFile.exists()) {
        properties.load(FileInputStream(propertiesFile))
    }
    properties
}

val geminiApiKey: String = localProperties.getProperty("gemini.apiKey", "")
val meteoblueApiKey: String = localProperties.getProperty("meteoblue.apiKey", "")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.mobilecw"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.mobilecw"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${geminiApiKey}\""
        )
        buildConfigField(
            "String",
            "METEOBLUE_API_KEY",
            "\"${meteoblueApiKey}\""
        )
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    
    // Room Database
    implementation(libs.room.runtime)
    kapt(libs.room.compiler)
    
    // RecyclerView
    implementation(libs.recyclerview)
    
    // CardView
    implementation(libs.cardview)
    
    // Network
    implementation(libs.volley)
    
    // Image Loading
    implementation(libs.glide)
    
    // Location Services
    implementation(libs.play.services.location)
    
    // Mapsforge (OpenStreetMap - no API key required)
    implementation("org.mapsforge:mapsforge-core:0.21.0")
    implementation("org.mapsforge:mapsforge-map:0.21.0")
    implementation("org.mapsforge:mapsforge-map-reader:0.21.0")
    implementation("org.mapsforge:mapsforge-themes:0.21.0")
    implementation("org.mapsforge:mapsforge-map-android:0.21.0")
    implementation("net.sf.kxml:kxml2:2.3.0")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}