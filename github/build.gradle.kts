plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.apollo)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ksp)
}

val artifact = "dev.datlag.burningseries.github"

apollo {
    service("GitHub") {
        packageName.set(artifact)
        srcDir("src/commonMain/graphql")
        schemaFiles.from(file("src/commonMain/graphql/schema.graphqls"))
    }
}

kotlin {
    jvm()
    androidTarget()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            api(libs.apollo)
            // implementation(libs.kache)
            api(libs.flowredux)
            // implementation(libs.datetime)
            implementation(libs.serialization)
            implementation(libs.tooling)
            api(libs.immutable)
            api(libs.ktorfit)

            implementation(project(":model"))
            implementation(project(":firebase"))
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.ktorfit.ksp)
    add("kspAndroid", libs.ktorfit.ksp)
    add("kspJvm", libs.ktorfit.ksp)
    add("kspIosX64", libs.ktorfit.ksp)
    add("kspIosArm64", libs.ktorfit.ksp)
    add("kspIosSimulatorArm64", libs.ktorfit.ksp)
}

android {
    compileSdk = 34
    namespace = artifact

    defaultConfig {
        minSdk = 23
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_17
    }
}