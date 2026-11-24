pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        maven("https://raw.githubusercontent.com/HereLiesAz/AzNavRail/update-maven/") {
            content {
                includeModule("com.github.HereLiesAz.AzNavRail", "aznavrail-updates")
                includeModule("com.github.HereLiesAz.AzNavRail", "aznavrail-updates-play")
            }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "MeatPiety"
include(":app", ":shared")
