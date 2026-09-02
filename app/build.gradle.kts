plugins {
    alias(libs.plugins.app.android.application)
    alias(libs.plugins.app.android.application.compose)
    alias(libs.plugins.app.hilt)
    // Necesario: las rutas de Navigation3 son @Serializable y sin el plugin las
    // anotaciones compilan como no-op y la app revienta al guardar el back stack.
    alias(libs.plugins.app.kotlin.serialization)
    alias(libs.plugins.app.detekt)
    alias(libs.plugins.app.spotless)
}

android {
    namespace = "com.app.cookify"

    defaultConfig {
        applicationId = "com.app.cookify"
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(projects.core.ui)
    implementation(projects.core.model)
    implementation(projects.core.data)
    implementation(projects.core.domain)
    implementation(projects.core.common)

    implementation(projects.feature.home)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.planning)
    implementation(projects.feature.week)

    implementation(libs.androidx.core)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    implementation(libs.bundles.compose)
    implementation(libs.bundles.navigation3)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization)

    testImplementation(libs.bundles.unit.test)
    androidTestImplementation(libs.bundles.android.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
