package com.app.cookify.core.data.red

import com.app.cookify.core.data.BuildConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.json.Json

/** La raíz del servicio de precios. Se fija al compilar, no en tiempo de ejecución. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class UrlDelServicio

@Module
@InstallIn(SingletonComponent::class)
internal object RedModule {

    @Provides
    @Singleton
    @UrlDelServicio
    fun urlDelServicio(): String = BuildConfig.COOKIFY_API_URL

    @Provides
    @Singleton
    fun http(): HttpClient = HttpClient(Android) {
        // El cliente no lanza por un código de error: el repositorio decide qué
        // hacer con un 503, y para él no es una excepción sino una respuesta.
        expectSuccess = false

        install(ContentNegotiation) {
            json(
                Json {
                    // Que el servicio agregue un campo no puede tumbar una app
                    // ya instalada. Es la contraparte del criterio de versionado
                    // del contrato: agregar campos no sube la versión.
                    ignoreUnknownKeys = true
                    explicitNulls = false
                }
            )
        }

        // Los 98 precios de una cadena son 17 KB de JSON que comprimen a menos
        // de 2 KB. En un teléfono con datos móviles esa diferencia importa.
        install(ContentEncoding) { gzip() }

        install(HttpTimeout) {
            // Cortos a propósito: si el servicio no responde pronto, la app
            // tiene precios en caché y no vale la pena hacer esperar a nadie.
            connectTimeoutMillis = ESPERA_CONEXION_MS
            requestTimeoutMillis = ESPERA_RESPUESTA_MS
        }
    }

    private const val ESPERA_CONEXION_MS = 5_000L
    private const val ESPERA_RESPUESTA_MS = 10_000L
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RedBindsModule {

    @Binds
    @Singleton
    abstract fun preciosRemotos(impl: ClientePrecios): PreciosRemotos
}
