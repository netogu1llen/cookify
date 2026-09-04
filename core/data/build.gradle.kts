plugins {
    alias(libs.plugins.app.android.library)
    alias(libs.plugins.app.hilt)
    alias(libs.plugins.app.kotlin.serialization)
    alias(libs.plugins.app.detekt)
}

android {
    namespace = "com.app.cookify.core.data"

    /*
     * El catalogo entra al classpath del unit test como recurso.
     *
     * Antes el test lo leia con File("src/main/assets/..."), una ruta que Gradle no
     * ve: la tarea quedaba UP-TO-DATE aunque el catalogo hubiera cambiado y el test
     * pasaba en verde sin llegar a correr. Asi fue como un valor de enum roto en
     * recetas.json llego hasta el telefono. Declarado como recurso, Gradle rastrea
     * los archivos y vuelve a correr el test cuando cambian, que es lo unico que
     * convierte ese test en una red de seguridad de verdad.
     */
    sourceSets {
        named("test") {
            resources.srcDir("src/main/assets")
        }
    }
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.database)

    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.bundles.unit.test)
}
