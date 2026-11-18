import java.util.Properties

if (file("/data/user/0/com.agy").exists()) {
    System.setProperty("org.sqlite.tmpdir", "/data/user/0/com.agy/files/tmp")
    System.setProperty("java.io.tmpdir", "/data/user/0/com.agy/files/tmp")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.reader(Charsets.UTF_8).use { load(it) }
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "ru.dev.gitnews"
    compileSdk = 36
    buildToolsVersion = "35.0.0"

    signingConfigs {
        create("release") {
            val keystorePath = localProperties.getProperty("RELEASE_STORE_FILE")
                ?: localProperties.getProperty("KEYSTORE_FILE")
                ?: System.getenv("RELEASE_STORE_FILE")
                ?: System.getenv("KEYSTORE_FILE")
            val keystorePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
                ?: localProperties.getProperty("KEYSTORE_PASSWORD")
                ?: System.getenv("RELEASE_STORE_PASSWORD")
                ?: System.getenv("KEYSTORE_PASSWORD")
            val keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
                ?: localProperties.getProperty("KEY_ALIAS")
                ?: System.getenv("RELEASE_KEY_ALIAS")
                ?: System.getenv("KEY_ALIAS")
            val keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
                ?: localProperties.getProperty("KEY_PASSWORD")
                ?: System.getenv("RELEASE_KEY_PASSWORD")
                ?: System.getenv("KEY_PASSWORD")

            val keystoreFile = when {
                keystorePath.isNullOrBlank() -> null
                rootProject.file(keystorePath).exists() -> rootProject.file(keystorePath)
                file(keystorePath).exists() -> file(keystorePath)
                File(keystorePath).exists() -> File(keystorePath)
                else -> null
            }

            if (keystoreFile != null && keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.outrot.gitnews"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val clientId = localProperties.getProperty("GITHUB_CLIENT_ID")
            ?: System.getenv("GITHUB_CLIENT_ID")
            ?: (project.findProperty("GITHUB_CLIENT_ID") as? String)
            ?: ""
        buildConfigField("String", "GITHUB_CLIENT_ID", "\"$clientId\"")

        val googleClientId = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID")
            ?: System.getenv("GOOGLE_WEB_CLIENT_ID")
            ?: (project.findProperty("GOOGLE_WEB_CLIENT_ID") as? String)
            ?: ""
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleClientId\"")

        val yandexBannerId = localProperties.getProperty("YANDEX_BANNER_ID")
            ?: System.getenv("YANDEX_BANNER_ID")
            ?: (project.findProperty("YANDEX_BANNER_ID") as? String)
            ?: "demo-banner-yandex"
        buildConfigField("String", "YANDEX_BANNER_ID", "\"$yandexBannerId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseConfig = signingConfigs.getByName("release")
            if (releaseConfig.storeFile != null && releaseConfig.storeFile!!.exists()) {
                signingConfig = releaseConfig
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        disable += listOf("MobileAdsSdkOutdatedVersion")
    }
}

configurations.all {
    exclude(group = "org.jetbrains", module = "annotations-java5")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.browser)
    implementation(libs.coil.compose)

    implementation(libs.mlkit.translate)
    implementation(libs.mlkit.language.id)

    implementation("androidx.annotation:annotation:1.9.1")
    implementation(libs.androidx.work.runtime.ktx)
    implementation("com.yandex.android:mobileads:7.18.8")
}

ksp {
    arg("room.generateKotlin", "true")
}
