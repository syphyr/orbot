import org.gradle.api.initialization.resolve.RepositoriesMode

pluginManagement {
    repositories {
        maven { url = uri("/media/delta/T5/android/orbot-latest/maven") }
        google()
        mavenCentral()
        // used for foojay resolver, should be last
        //developer.android.com/build/optimize-your-build#gradle_plugin_portal
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("/media/delta/T5/android/orbot-latest/maven") }
        google()
        mavenCentral()
        maven { url = uri("https://raw.githubusercontent.com/guardianproject/gpmaven/master") }
    }
}

rootProject.name = "Orbot"
include(
    ":app",
    ":OrbotLib",
)
