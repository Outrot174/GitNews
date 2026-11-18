System.setProperty("org.sqlite.tmpdir", "/data/user/0/com.agy/files/tmp")
System.setProperty("java.io.tmpdir", "/data/user/0/com.agy/files/tmp")

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}
