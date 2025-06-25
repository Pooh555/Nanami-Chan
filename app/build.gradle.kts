plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.nanami"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nanami"
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

    packaging {
        resources {
            pickFirsts.addAll(listOf("META-INF/AL2.0", "META-INF/LGPL2.1"))
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    sourceSets {
        named("main") {
            java.srcDirs("src/main/java", "../../Framework/framework/src/main/java")
        }
    }
}

dependencies {
    // Android
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Live2D SDK
    implementation(files("libs/Live2DCubismCore.aar"))

    // JSON
    implementation(libs.json)

    // MP3 playback
    implementation(libs.jlayer)

    // STT: Vosk
    implementation(libs.appcompat.v131)
    implementation(libs.vosk.android)
    implementation(libs.media3.common)
    implementation(project(":models"))

    // TTS: ElevenLabs
    implementation(libs.volley)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}