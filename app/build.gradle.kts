import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Versioning contract (HereLiesAz/workflows android-release): CI passes -PversionCode and
// -PversionName; local builds fall back to the pair recorded in version.properties.
// Nothing here increments anything.
val versionProps = Properties().apply {
    rootProject.file("version.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val appVersionCode = (findProperty("versionCode") ?: versionProps.getProperty("versionCode") ?: "1").toString().toInt()
val appVersionName = (findProperty("versionName") ?: versionProps.getProperty("versionName")
    ?: "${versionProps.getProperty("versionMajor", "0")}.${versionProps.getProperty("versionMinor", "1")}.${versionProps.getProperty("versionPatch", "0")}").toString()

// Upload key arrives from CI as KEYSTORE_FILE / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD.
val keystoreFile = System.getenv("KEYSTORE_FILE")?.takeIf { it.isNotBlank() }

android {
    namespace = "com.hereliesaz.meatpiety"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hereliesaz.meatpiety"
        minSdk = 28
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (keystoreFile != null) {
            create("release") {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD") ?: System.getenv("KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (keystoreFile != null) signingConfig = signingConfigs.getByName("release")
            // R8 shrinks the release build and emits mapping.txt, which Play requires to
            // deobfuscate crash reports.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.activity:activity-compose:1.13.0")
}
