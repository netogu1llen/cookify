plugins {
    alias(libs.plugins.app.jvm.library)
    alias(libs.plugins.app.detekt)
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.java.inject)

    testImplementation(libs.bundles.unit.test)
}
