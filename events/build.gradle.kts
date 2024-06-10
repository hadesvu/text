plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("maven-publish")
}

android {
    namespace = "com.advtechgrp.common.events"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
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
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
}

dependencies {
    implementation(libs.jakewharton.timber)
    implementation(libs.joda.time)
    implementation(libs.com.squareup.retrofit2.converter.gson)
    implementation(libs.google.dagger.hilt.android)
    implementation (libs.androidx.ui)
    implementation (libs.androidx.material)
    implementation (libs.ui.tooling)
    implementation (libs.androidx.runtime.livedata)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.advtechgrp.common" // Customize this
                artifactId = "events" // Customize this
                version = "1.0.0" // Customize this
            }
        }
    }
}