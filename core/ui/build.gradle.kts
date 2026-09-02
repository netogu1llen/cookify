plugins {
    alias(libs.plugins.app.android.library)
    alias(libs.plugins.app.android.library.compose)
    alias(libs.plugins.app.detekt)
}

android {
    namespace = "com.app.cookify.core.ui"
}

dependencies {
    api(projects.core.model)

    api(libs.bundles.compose)
    api(libs.androidx.compose.animation)
    api(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.collections.immutable)

    androidTestImplementation(libs.bundles.android.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
