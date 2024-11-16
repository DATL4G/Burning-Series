plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.sqldelight)
}

kotlin {
    jvm()
    androidTarget()

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.sqldelight.coroutines)
                api(project(":model"))
                implementation(libs.sqldelight.adapter)
                implementation(libs.tooling)
                implementation(libs.datetime)
            }
        }

        androidMain.get().dependencies {
            implementation(libs.sqldelight.android)
            api(libs.android.sqlite.framework)
        }

        jvmMain.get().dependencies {
            implementation(libs.sqldelight.jvm)
        }

        iosMain.get().dependencies {
            implementation(libs.sqldelight.native)
        }
    }
}

sqldelight {
    databases {
        create("BurningSeries") {
            packageName.set("dev.datlag.burningseries.database")
            srcDirs("src/commonMain/bs")
        }
    }
}

android {
    compileSdk = 35
    namespace = "dev.datlag.burningseries.database"

    defaultConfig {
        minSdk = 23
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_17
    }
}