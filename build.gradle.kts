// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.18.1"
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.hilt) apply false
}

apiValidation {
    // These modules belong to the demo application, not the customer SDK.
    ignoredProjects.addAll(listOf("app", "data", "domain", "core-network", "core-crypto"))
}
