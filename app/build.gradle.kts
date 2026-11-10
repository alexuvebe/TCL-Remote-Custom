import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

val signingProperties = Properties().apply {
    val signingFile = rootProject.file("key.properties")
    if (signingFile.exists()) {
        signingFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "dev.cavatz.keybindertv"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.cavatz.keybindertv"
        minSdk = 30
        targetSdk = 34
        versionCode = providers.gradleProperty("APP_VERSION_CODE").orElse("1").get().toInt()
        versionName = providers.gradleProperty("APP_VERSION_NAME").orElse("1.0.0").get()
    }

    signingConfigs {
        create("release") {
            if (signingProperties.isNotEmpty()) {
                storeFile = rootProject.file(signingProperties.getProperty("storeFile"))
                storePassword = signingProperties.getProperty("storePassword")
                keyAlias = signingProperties.getProperty("keyAlias")
                keyPassword = signingProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    sourceSets {
        getByName("main") {
            kotlin.srcDirs("src/main/kotlin")
        }
        getByName("test") {
            kotlin.srcDirs("src/test/kotlin")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    // Compose BOM — aligne toutes les versions Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // TV Compose
    implementation(libs.tv.material)

    // Navigation
    implementation(libs.navigation.compose)

    // DataStore (Proto/preferences avec sérialisation manuelle)
    implementation(libs.datastore)

    // Sérialisation JSON
    implementation(libs.kotlinx.serialization.json)

    // Coil — chargement d'images
    implementation(libs.coil.compose)

    // ZXing — génération QR codes
    implementation(libs.zxing.android)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Tests unitaires
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)

    // Tests instrumentés
    androidTestImplementation(libs.junit.ext)
}
