import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import com.mikepenz.aboutlibraries.plugin.DuplicateRule
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.konfig)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktorfit)
    alias(libs.plugins.moko.resources)
    alias(libs.plugins.osdetector)
    alias(libs.plugins.sekret)
    alias(libs.plugins.serialization)
}

val artifact = "dev.datlag.burningseries"
val appVersion = "6.0.0"
val appVersionCode = 600

group = artifact
version = appVersion

multiplatformResources {
    resourcesPackage.set(artifact)
    resourcesClassName.set("MokoRes")
}

composeCompiler {
    enableStrongSkippingMode.set(true)
    enableNonSkippingGroupOptimization.set(true)
}

aboutLibraries {
    includePlatform = true
    duplicationMode = DuplicateMode.MERGE
    duplicationRule = DuplicateRule.GROUP
    excludeFields = arrayOf("generated")
}

buildkonfig {
    packageName = artifact

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "packageName", artifact)
    }
}

sekret {
    properties {
        enabled.set(true)
        packageName.set(artifact)

        nativeCopy {
            androidJNIFolder.set(project.layout.projectDirectory.dir("src/androidMain/jniLibs"))
            desktopComposeResourcesFolder.set(project.layout.projectDirectory.dir("src").dir("jvmMain").dir("resources"))
        }
    }
}

kotlin {
    androidTarget()
    jvm()

    /*listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }*/

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.moko.resources.compose)

            implementation(libs.kodein)
            implementation(libs.kodein.compose)

            implementation(libs.haze)
            implementation(libs.haze.materials)

            implementation(libs.decompose)
            implementation(libs.decompose.compose)

            implementation(libs.tooling.decompose)

            implementation(libs.windowsize)
            implementation(libs.ktor)
            implementation(libs.ktor.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.datetime)

            implementation(libs.kmpalette)
            implementation(libs.kolor)
            implementation(libs.kache)
            implementation(libs.blurhash)
            implementation(libs.qrose)

            implementation(libs.coil)
            implementation(libs.coil.network)
            implementation(libs.coil.svg)
            implementation(libs.coil.compose)

            implementation(libs.kast)
            implementation(libs.nanoid)
            implementation(libs.serialization.json)
            implementation(libs.serialization.protobuf)
            implementation(libs.oidc)
            implementation(libs.aboutlibraries)
            implementation("dev.datlag.sheets-compose-dialogs:option:2.0.0-SNAPSHOT")
            implementation("dev.datlag.k2k:k2k:1.0.0-SNAPSHOT")

            implementation(project(":settings"))
            implementation(project(":network"))
            implementation(project(":firebase"))
            implementation(project(":database"))
            implementation(project(":github"))
        }

        val androidMain by getting {
            apply(plugin = "kotlin-parcelize")
            apply(plugin = libs.plugins.crashlytics.get().pluginId)

            dependencies {
                implementation(libs.android)
                implementation(libs.activity)
                implementation(libs.activity.compose)
                implementation(libs.multidex)
                implementation(libs.androidx.window)
                implementation(libs.ackpine)

                implementation(libs.ktor.jvm)
                implementation(libs.coroutines.android)
                implementation(libs.okhttp.doh)

                implementation(libs.bundles.android.media)
                implementation(libs.webview)
                implementation(libs.splashscreen)
            }
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)

            implementation(libs.ktor.jvm)
            implementation(libs.coroutines.swing)
            implementation(libs.okhttp.doh)
            implementation(libs.vlcj)
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar)
}

android {
    sourceSets["main"].setRoot("src/androidMain/")
    sourceSets["main"].res.srcDirs("src/androidMain/res", "src/commonMain/resources")
    sourceSets["main"].assets.srcDirs("src/androidMain/assets", "src/commonMain/assets")
    compileSdk = 35
    namespace = artifact

    defaultConfig {
        applicationId = artifact
        minSdk = 23
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersion

        multiDexEnabled = true
        vectorDrawables.useSupportLibrary = true

        addManifestPlaceholders(
            mapOf("oidcRedirectScheme" to "burningseries")
        )
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            merges += "values**"
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
    }
}

compose {
    desktop {
        application {
            mainClass = "$artifact.MainKt"

            jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED")
            when (getHost()) {
                Host.Linux -> {
                    jvmArgs("--add-opens", "java.desktop/sun.awt.X11=ALL-UNNAMED")
                    jvmArgs("--add-opens", "java.desktop/sun.awt.wl=ALL-UNNAMED")
                }
                else -> { }
            }

            nativeDistributions {
                packageName = "Burning-Series"
                packageVersion = appVersion
                description = "Watch any series from Burning-Series using this (unofficial) app."
                copyright = "© 2020 Jeff Retz (DatLag). All rights reserved."
                licenseFile.set(rootProject.file("LICENSE"))

                outputBaseDir.set(rootProject.layout.buildDirectory.asFile.get().resolve("release"))
                appResourcesRootDir.set(project.layout.projectDirectory.dir("src").dir("jvmMain").dir("resources"))

                when (getHost()) {
                    Host.Linux -> targetFormats(
                        TargetFormat.AppImage, TargetFormat.Deb, TargetFormat.Rpm
                    )
                    Host.MAC -> targetFormats(
                        TargetFormat.Dmg
                    )
                    Host.Windows -> targetFormats(
                        TargetFormat.Exe, TargetFormat.Msi
                    )
                }

                linux {
                    iconFile.set(File(rootProject.project("composeApp").projectDir, "src/commonMain/moko-resources/assets/png/launcher_128.png"))
                    rpmLicenseType = "GPL-3.0"
                    debMaintainer = "Jeff Retz (DatLag)"
                    appCategory = "Video"
                }
                windows {
                    iconFile.set(File(rootProject.project("composeApp").projectDir, "src/commonMain/moko-resources/assets/ico/launcher_128.ico"))
                    upgradeUuid = "3487d337-1ef5-4e01-87cb-d1ede6e10752"
                }
                macOS {
                    iconFile.set(File(rootProject.project("composeApp").projectDir, "src/commonMain/moko-resources/assets/icns/launcher.icns"))
                }

                includeAllModules = true
            }
        }
    }
    web { }
}

fun getHost(): Host {
    return when (osdetector.os) {
        "linux" -> Host.Linux
        "osx" -> Host.MAC
        "windows" -> Host.Windows
        else -> {
            val hostOs = System.getProperty("os.name")
            val isMingwX64 = hostOs.startsWith("Windows")

            when {
                hostOs == "Linux" -> Host.Linux
                hostOs == "Mac OS X" -> Host.MAC
                isMingwX64 -> Host.Windows
                else -> throw IllegalStateException("Unknown OS: ${osdetector.classifier}")
            }
        }
    }
}

enum class Host(val label: String) {
    Linux("linux"),
    Windows("win"),
    MAC("mac");
}
