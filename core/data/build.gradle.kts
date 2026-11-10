plugins {
    alias(libs.plugins.app.android.library)
    alias(libs.plugins.app.hilt)
    alias(libs.plugins.app.kotlin.serialization)
    alias(libs.plugins.app.detekt)
}

android {
    namespace = "com.app.cookify.core.data"

    buildFeatures {
        buildConfig = true
    }

    /*
     * La direccion del servicio se fija al compilar y no se lee en ejecucion,
     * para que no haya forma de apuntar la app a otro servidor sin recompilar.
     *
     * Se puede sobrescribir al construir, que es lo que se usa para desarrollar
     * contra el servicio local:
     *   ./gradlew :app:assembleDebug -PcookifyApiUrl=http://10.0.2.2:8090
     *
     * 10.0.2.2 y no localhost: dentro del emulador, localhost es el emulador.
     */
    defaultConfig {
        val url = providers.gradleProperty("cookifyApiUrl")
            .getOrElse("https://cookifies.duckdns.org")
        buildConfigField("String", "COOKIFY_API_URL", "\"$url\"")
    }

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

    // Cliente del servicio de precios. Mismo framework que el servidor, que es
    // parte de por que se eligio Ktor: un solo vocabulario en los dos lados.
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.encoding)
    implementation(libs.ktor.serialization.json)

    testImplementation(libs.bundles.unit.test)
    // Motor de mentira: deja probar la cascada sin red y sin servidor.
    testImplementation(libs.ktor.client.mock)
}
