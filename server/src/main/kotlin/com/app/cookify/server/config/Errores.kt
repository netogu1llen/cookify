package com.app.cookify.server.config

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

/**
 * Los errores que el servicio sabe nombrar.
 *
 * Son excepciones y no valores de retorno a proposito: la capa de negocio no
 * tiene que arrastrar un tipo Resultado por cada llamada, y el ruteo no tiene
 * que acordarse de traducirlos uno por uno. StatusPages hace la traduccion en
 * un solo lugar, asi que un error nuevo se mapea una vez.
 *
 * La regla que esto hace cumplir: **ningun error viaja con codigo 200**. Un
 * error con codigo de exito rompe a quien consume el servicio, porque obliga a
 * leer el cuerpo para saber si algo salio bien.
 */
sealed class ErrorDelServicio(
    val codigo: String,
    val estado: HttpStatusCode,
    override val message: String,
    val detalles: List<DetalleError> = emptyList(),
) : RuntimeException(message) {

    class Validacion(
        mensaje: String = "El cuerpo de la peticion no es valido",
        detalles: List<DetalleError> = emptyList(),
    ) : ErrorDelServicio("VALIDACION", HttpStatusCode.BadRequest, mensaje, detalles)

    class NoAutorizado(
        mensaje: String = "Falta la cabecera X-API-Key o no corresponde",
    ) : ErrorDelServicio("NO_AUTORIZADO", HttpStatusCode.Unauthorized, mensaje)

    class NoEncontrado(
        recurso: String,
        identidad: String,
    ) : ErrorDelServicio(
        "NO_ENCONTRADO",
        HttpStatusCode.NotFound,
        "No existe $recurso con identidad $identidad",
    )

    class Conflicto(
        mensaje: String,
    ) : ErrorDelServicio("CONFLICTO", HttpStatusCode.Conflict, mensaje)

    class FuenteNoDisponible(
        mensaje: String,
    ) : ErrorDelServicio("FUENTE_NO_DISPONIBLE", HttpStatusCode.ServiceUnavailable, mensaje)
}

@Serializable
data class DetalleError(val campo: String, val problema: String)

@Serializable
data class CuerpoError(val error: Contenido) {
    @Serializable
    data class Contenido(
        val codigo: String,
        val mensaje: String,
        val detalles: List<DetalleError> = emptyList(),
    )
}

/**
 * Acumula problemas de validacion en lugar de fallar en el primero.
 *
 * Quien manda un cuerpo con tres campos malos preferiria enterarse de los tres
 * en una respuesta antes que descubrirlos de a uno en tres intentos.
 */
class Validador {
    private val problemas = mutableListOf<DetalleError>()

    fun exigir(condicion: Boolean, campo: String, problema: String) {
        if (!condicion) problemas += DetalleError(campo, problema)
    }

    fun rendir() {
        if (problemas.isNotEmpty()) throw ErrorDelServicio.Validacion(detalles = problemas.toList())
    }
}

inline fun validar(bloque: Validador.() -> Unit) {
    Validador().apply(bloque).rendir()
}
