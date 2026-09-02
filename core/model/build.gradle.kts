plugins {
    alias(libs.plugins.app.jvm.library)
    alias(libs.plugins.app.kotlin.serialization)
    alias(libs.plugins.app.detekt)
}

dependencies {
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.collections.immutable)
}
