import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    android {
        namespace = "com.hereliesaz.meatpiety.shared"
        compileSdk = 37
        minSdk = 28
        // Runs commonTest on the JVM; wasm tests need a browser.
        withHostTest {}
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("meatpiety")
        browser {
            commonWebpackConfig {
                outputFileName = "meatpiety.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}


// Azrienoch variable font (SIL OFL 1.1, see OFL-azrienoch.txt) ships as a Compose resource.
compose.resources {
    packageOfResClass = "com.hereliesaz.meatpiety.resources"
}
