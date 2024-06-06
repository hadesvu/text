plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("maven-publish")
}

android {
    namespace = "com.advtechgrp.commends.events"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        version = 1
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.jakewharton.timber)
    implementation(libs.joda.time)
    implementation(libs.com.squareup.retrofit2.converter.gson)
    implementation(libs.google.dagger.hilt.android)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.advtechgrp.commends" // Customize this
                artifactId = "events" // Customize this
                version = "1.0.1" // Customize this
            }
        }
    }
}