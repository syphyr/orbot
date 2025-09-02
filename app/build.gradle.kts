import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import java.util.Properties
import java.io.FileInputStream
import java.util.Date


val BaseVersionCode = 1750300200
plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.application)
}

// Gets the version name from the latest Git tag, stripping the leading v off
val getVersionName = {
    providers.exec {
        commandLine("git", "describe", "--tags", "--always")
    }.standardOutput.asText.get().trim()
}

kotlin {
    jvmToolchain(21)
}
android {
    namespace = "org.torproject.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.torproject.android"
        versionName = getVersionName()
        minSdk = 24
        targetSdk = 36
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            val keystoreProperties = Properties()
            if (keystorePropertiesFile.canRead()) {
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
            }
            if (!keystoreProperties.stringPropertyNames().isEmpty()) {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }

    buildTypes {
        getByName("release") {
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.txt")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    buildTypes {
        getByName("release") {
            isShrinkResources = false
            isMinifyEnabled = false
        }
        getByName("debug") {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("x86", "armeabi-v7a", "x86_64", "arm64-v8a")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    flavorDimensions += "free"

    productFlavors {
        create("fullperm") {
            dimension = "free"
            applicationId = "org.torproject.android"
            versionCode = BaseVersionCode
            versionName = getVersionName()
        }

        create("nightly") {
            dimension = "free"
            applicationId = "org.torproject.android.nightly"
            versionName = getVersionName()
            versionCode = (Date().time / 1000).toInt()
        }
    }

    packaging {
        resources {
            excludes += listOf("META-INF/androidx.localbroadcastmanager_localbroadcastmanager.version")
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        disable += "InvalidPackage"
        htmlReport = true
        lintConfig = file("../lint.xml")
        textReport = false
        xmlReport = false
    }
}

// Increments versionCode by ABI type
android.applicationVariants.all {
    outputs.configureEach { ->
        if (versionCode == BaseVersionCode) {
            val abiCodeMap = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2, "x86" to 4, "x86_64" to 5)
            val baseCode: Int? = abiCodeMap[filters.find { it.filterType == "ABI" }?.identifier] ?: 0
            (this as ApkVariantOutputImpl).versionCodeOverride = BaseVersionCode + baseCode!!
        }
    }
}

dependencies {
    implementation(project(":OrbotLib"))
    implementation(project(":orbotservice"))
    implementation(libs.android.material)
    implementation(libs.android.volley)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.localbroadcast)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.retrofit.converter)
    implementation(libs.retrofit.lib)
    implementation(libs.rootbeer.lib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.appiconnamechanger)

    testImplementation(libs.junit.jupiter)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso)
    androidTestImplementation(libs.androidx.rules)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.screengrab)
    androidTestUtil(libs.androidx.orchestrator)
}

tasks.register<Copy>("copyLicenseToAssets") {
    from(layout.projectDirectory.file("LICENSE"))
    into(layout.projectDirectory.dir("src/main/assets"))
}

tasks.named("preBuild") {
    dependsOn("copyLicenseToAssets")
}
