plugins {
    alias(libs.plugins.app.android.feature)
    alias(libs.plugins.app.detekt)
}

android {
    namespace = "com.app.cookify.feature.home"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.model)

    testImplementation(libs.bundles.unit.test)
    androidTestImplementation(libs.bundles.android.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
