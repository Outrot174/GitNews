if (file("/data/user/0/com.agy").exists()) {
    System.setProperty("org.sqlite.tmpdir", "/data/user/0/com.agy/files/tmp")
    System.setProperty("java.io.tmpdir", "/data/user/0/com.agy/files/tmp")
    gradle.beforeProject {
        extra.set("android.aapt2FromMaven", "false")
        extra.set("android.overrideAapt2", "/data/user/0/com.agy/files/usr/bin/aapt2")
    }
}

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = java.net.URI("https://mobileads.yandex.net/android") }
        maven { url = java.net.URI("https://jitpack.io") }
    }
}

rootProject.name = "GitNews"
include(":app")
