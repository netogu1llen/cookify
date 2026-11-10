package com.app.cookify.core.data.red

import com.app.cookify.core.model.Supermercado
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import javax.inject.Inject
import javax.inject.Singleton

/**
 * El único punto de la app que habla con el servicio de precios.
 *
 * Que esté detrás de una interfaz es lo que permite probar el repositorio sin
 * red, con un cliente de mentira que devuelve lo que la prueba necesita.
 */
internal interface PreciosRemotos {
    suspend fun deCadena(supermercado: Supermercado): Resultado

    sealed interface Resultado {
        data class Hay(val precios: PreciosVigentesDto) : Resultado

        /**
         * No se pudo traer. Lleva el motivo para poder registrarlo, pero quien
         * lo recibe no lo muestra al usuario: a quien está armando su semana no
         * le sirve un error de red, le sirve que la semana se arme igual.
         */
        data class NoSePudo(val motivo: String) : Resultado
    }
}

@Singleton
internal class ClientePrecios @Inject constructor(
    private val http: HttpClient,
    @UrlDelServicio private val urlBase: String,
) : PreciosRemotos {

    override suspend fun deCadena(supermercado: Supermercado): PreciosRemotos.Resultado =
        runCatching {
            val respuesta = http.get("$urlBase/api/v1/precios") {
                parameter("supermercado", supermercado.name)
            }
            if (!respuesta.status.isSuccess()) {
                return PreciosRemotos.Resultado.NoSePudo(
                    "el servicio respondio ${respuesta.status.value}"
                )
            }
            PreciosRemotos.Resultado.Hay(respuesta.body<PreciosVigentesDto>())
        }.getOrElse { error ->
            // Sin red, con el servicio caído o con una respuesta que no se
            // entiende, todo termina igual: no hay precios nuevos. La app tiene
            // de dónde sacarlos de todos modos.
            PreciosRemotos.Resultado.NoSePudo(error.message ?: error::class.simpleName.orEmpty())
        }
}
