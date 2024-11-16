plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.serialization)
}

kotlin {
    jvm()
    androidTarget()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    macosX64()
    macosArm64()

    linuxX64()
    linuxArm64()

    mingwX64()

    js(IR) {
        nodejs()
        browser()
        binaries.executable()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.tooling)
            implementation(project(":model"))
        }

        val firebaseMain by creating {
            dependsOn(commonMain.get())

            androidMain.orNull?.dependsOn(this)
            jvmMain.orNull?.dependsOn(this)
            iosMain.orNull?.dependsOn(this)
            jsMain.orNull?.dependsOn(this)

            dependencies {
                implementation(libs.firebase.auth)
                implementation(libs.firebase.store)
            }
        }

        androidMain.dependencies {
            // implementation(libs.firebase.android)
            // implementation(libs.firebase.android.analytics)
            // implementation(libs.firebase.android.auth)
            // implementation(libs.firebase.android.crashlytics)

            implementation(libs.firebase.crashlytics)
        }
        jvmMain.dependencies {
            implementation(libs.firebase.java)
        }

        val iosMain by getting {
            dependencies {
                implementation(libs.firebase.crashlytics)
            }
        }
    }
}

android {
    compileSdk = 35
    namespace = "dev.datlag.burningseries.firebase"

    defaultConfig {
        minSdk = 23
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_17
    }
}