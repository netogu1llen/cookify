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
    // api y no implementation: el formato de pesos lo necesita cualquiera que use
    // estos componentes para armar sus propios textos de precio.
    api(projects.core.common)

    api(libs.bundles.compose)
    api(libs.androidx.compose.animation)
    api(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.collections.immutable)

    androidTestImplementation(libs.bundles.android.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
