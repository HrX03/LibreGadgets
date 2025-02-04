import com.google.protobuf.gradle.id

plugins {
    kotlin("kapt")
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.protobuf)
}

android {
    namespace = "dev.hrx.libregadgets.core"
    compileSdk = 34

    defaultConfig {
        minSdk = 28

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.play.services.wearable)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.squareup.okhttp)
    implementation(libs.squareup.okhttp.coroutines)
    implementation(libs.protobuf.kotlin.lite)

    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.material3)

    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.26.1"
    }
    plugins {
        create("javalite") {
            artifact = "com.google.protobuf:protoc-gen-javalite:3.0.0"
        }
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.63.0"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.3.0:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
                create("kotlin") {
                    option("lite")
                }
            }
            task.plugins {
                create("grpc") {
                    option("lite")
                }
                create("grpckt") {
                    option("lite")
                }
            }
        }
    }
}