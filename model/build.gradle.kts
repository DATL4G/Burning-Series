plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.atomicfu)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget()
    jvm()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    linuxX64()
    linuxArm64()

    mingwX64()

    macosX64()
    macosArm64()

    js(IR) {
        browser()
        nodejs()
        binaries.executable()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.serialization)
            api(libs.immutable)
            implementation(libs.coroutines)
            implementation(libs.tooling)
        }

        androidMain.dependencies {
            implementation(libs.service)
        }
    }
}

android {
    compileSdk = 35
    namespace = "dev.datlag.burningseries.model"
    sourceSets["main"].aidl.srcDirs("src/androidMain/aidl")

    defaultConfig {
        minSdk = 23
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        aidl = true
    }
}