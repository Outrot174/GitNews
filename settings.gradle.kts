if (file("/data/user/0/com.agy").exists()) {
    System.setProperty("org.sqlite.tmpdir", "/data/user/0/com.agy/files/tmp")
    System.setProperty("java.io.tmpdir", "/data/user/0/com.agy/files/tmp")
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
