plugins {
    id("org.jetbrains.compose") version libs.versions.compose.get()
    id("com.android.library")
    kotlin("android")
}

dependencies {
    implementation(project(":compose:common"))
    implementation("androidx.activity:activity-compose:1.5.0")
}

android {
    compileSdk = 31

    defaultConfig {
//        applicationId = "io.github.pdvrieze.process.compose.android"
        minSdk = 24
        targetSdk = 31
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    composeOptions {
        kotlinCompilerExtensionVersion=libs.versions.compose.get()
    }
}
