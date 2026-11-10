package com.app.cookify.core.model

/**
 * El resultado de preguntarle al servicio de precios si está ahí.
 *
 * Existe para que la app pueda **probar la conexión a propósito**, y no solo
 * enterarse de refilón al armar una semana. Antes, la única forma de saber si el
 * teléfono alcanzaba el servicio era borrar los datos de la app, poner modo
 * avión y comparar dos totales.
 */
sealed interface PruebaDeConexion {

    /** El servicio contestó. [milisegundos] es lo que tardó el viaje completo. */
    data class Respondio(
        val version: String,
        val baseDeDatos: String,
        val milisegundos: Long,
    ) : PruebaDeConexion

    /**
     * No contestó. [motivo] se muestra tal cual: aquí sí conviene, porque quien
     * abre esta pantalla vino justamente a diagnosticar.
     */
    data class NoRespondio(val motivo: String) : PruebaDeConexion
}

/**
 * Lo que la app tiene guardado de una cadena.
 *
 * [medidoEn] es cuándo el servicio midió esos precios en el supermercado;
 * [guardadoEn] es cuándo el teléfono los trajo. Entre las dos fechas puede haber
 * días, y son dos preguntas distintas: la primera dice qué tan viejo es el
 * precio, la segunda qué tan vieja es la copia.
 */
data class PreciosGuardados(
    val supermercado: Supermercado,
    val total: Int,
    val medidos: Int,
    val medidoEn: String?,
    val guardadoEn: Long?,
)
