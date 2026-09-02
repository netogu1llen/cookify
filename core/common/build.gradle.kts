plugins {
    alias(libs.plugins.app.jvm.library)
    alias(libs.plugins.app.detekt)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    api(libs.java.inject)
}
