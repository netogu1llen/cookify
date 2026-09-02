package com.app.cookify.core.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Grilla de 8 puntos. Todo margen y padding sale de aca; no hay valores sueltos en
 * los composables.
 *
 * La regla de agrupamiento: lo relacionado va a [xs] o [s], lo no relacionado salta
 * a [l] o [xl]. La distancia comunica la jerarquia mejor que las lineas divisorias.
 */
object Espaciado {
    val xxs = 4.dp
    val xs = 8.dp
    val s = 12.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
    val xxl = 48.dp

    /** Margen lateral de todas las pantallas. */
    val margenPantalla = 20.dp

    /** Padding interno de las tarjetas. */
    val paddingTarjeta = 20.dp

    /** Alto minimo de cualquier cosa tocable. Accesibilidad, no estetica. */
    val objetivoTactil = 48.dp

    /** Alto del boton primario que vive en la zona del pulgar. */
    val altoBotonPrimario = 56.dp
}
