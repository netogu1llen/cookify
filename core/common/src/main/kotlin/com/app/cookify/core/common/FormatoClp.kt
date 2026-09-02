package com.app.cookify.core.common

/**
 * Formatea pesos chilenos como "$45.000": punto de miles, sin decimales.
 *
 * Se hace a mano en vez de con NumberFormat porque el separador depende del locale
 * del telefono y aca el formato tiene que ser siempre el chileno.
 */
fun Int.aPesos(): String {
    val signo = if (this < 0) "-" else ""
    val digitos = kotlin.math.abs(this).toString()
    val conPuntos = digitos
        .reversed()
        .chunked(DIGITOS_POR_GRUPO)
        .joinToString(".")
        .reversed()
    return "$signo$$conPuntos"
}

private const val DIGITOS_POR_GRUPO = 3
