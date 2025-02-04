plugins {
    kotlin("kapt")
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.hilt)
}

android {
    namespace = "dev.hrx.libregadgets"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.hrx.libregadgets"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        vectorDrawables {
            useSupportLibrary = true
        }

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.12"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.play.services.wearable)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.wear.compose)
    implementation(libs.androidx.wear.foundation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.wear.compose)
    implementation(libs.androidx.wear.tooling)
    implementation(libs.androidx.wear.foundation)
    implementation(libs.androidx.wear.tiles)
    implementation(libs.androidx.wear.protolayout)
    implementation(libs.androidx.wear.protolayout.material)
    implementation(libs.androidx.watchface.complications.data.source.ktx)
    implementation(libs.horologist.compose.layout)
    implementation(libs.horologist.tiles)
    implementation(libs.androidx.wear.input)
    implementation(libs.protobuf.kotlin.lite)
    implementation(libs.hilt.android)
    implementation(libs.guava)
    implementation(libs.guava.coroutines)
    implementation(libs.androidx.glance.wear.tiles)
    kapt(libs.hilt.android.compiler)
    implementation(project(mapOf("path" to ":core")))

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation(libs.androidx.wear.tiles.renderer)
}

kapt {
    correctErrorTypes = true
}