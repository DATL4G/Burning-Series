plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    jvm()
    androidTarget()

    linuxArm64 {
        binaries {
            sharedLib()
        }
    }
    linuxX64 {
        binaries {
            sharedLib()
        }
    }
    mingwX64 {
        binaries {
            sharedLib()
        }
    }
    macosArm64 {
        binaries {
            sharedLib()
        }
    }
    macosX64 {
        binaries {
            sharedLib()
        }
    }

    iosArm64 {
        binaries {
            sharedLib()
        }
    }
    iosSimulatorArm64 {
        binaries {
            sharedLib()
        }
    }
    iosX64 {
        binaries {
            sharedLib()
        }
    }
    androidNativeX86 {
        binaries {
            sharedLib()
        }
    }
    androidNativeX64 {
        binaries {
            sharedLib()
        }
    }
    androidNativeArm32 {
        binaries {
            sharedLib()
        }
    }
    androidNativeArm64 {
        binaries {
            sharedLib()
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            api(libs.sekret)
        }

        val jniNativeMain by creating {
            nativeMain.orNull?.let { dependsOn(it) } ?: dependsOn(commonMain.get())
            androidNativeMain.orNull?.dependsOn(this)
            linuxMain.orNull?.dependsOn(this)
            mingwMain.orNull?.dependsOn(this)
            macosMain.orNull?.dependsOn(this)
        }

        val jniMain by creating {
            dependsOn(commonMain.get())
            androidMain.orNull?.dependsOn(this)
            jvmMain.orNull?.dependsOn(this)
        }
    }
}

android {
    compileSdk = 35
    namespace = "dev.datlag.burningseries.sekret"

    defaultConfig {
        minSdk = 23
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_17
    }
}