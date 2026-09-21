plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.securevault.sdk.core"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(
                org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
            )
        }
    }
}

dependencies {
    testImplementation(libs.junit)
}

kotlin {
    explicitApi()
}

// Check the reviewed dump as well as apiCheck's comparison against compiled bytecode.
val checkSdkBoundary by tasks.registering {
    dependsOn("apiCheck")
    val dump = layout.projectDirectory.file("api/securevault-core.api")
    inputs.file(dump)
    doLast {
        val text = dump.asFile.readText()
        val forbidden = listOf(
            "com/securevault/domain/", "com/securevault/data/",
            "com/securevault/core/", "retrofit2/", "okhttp3/",
            "dagger/", "javax/inject/", "androidx/room/",
        )
        check(forbidden.none { it in text }) {
            "The SDK API exposes an implementation type. See API.md."
        }
        val projectDependencies = configurations.flatMap { configuration ->
            configuration.dependencies.withType<ProjectDependency>()
                .filter { it.path != project.path }
        }
        check(projectDependencies.isEmpty()) {
            "The standalone SDK must not depend on demo projects. See API.md."
        }
    }
}

tasks.named("check") {
    dependsOn(checkSdkBoundary)
}
