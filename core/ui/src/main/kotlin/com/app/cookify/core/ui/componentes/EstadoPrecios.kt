package com.app.cookify.core.ui.componentes

/**
 * Lo que la pantalla necesita saber sobre la procedencia de los precios.
 *
 * Es un modelo de interfaz y no el del dominio: aquí solo están las tres cosas
 * que cambian lo que se dibuja. Quien lo arma traduce desde `PreciosDeCadena`,
 * que sabe mucho más y nada de eso se usa para pintar.
 */
data class EstadoPrecios(
    /** Si la mayoría de los precios se midieron en la cadena. */
    val medidos: Boolean,
    /** Cuándo se midieron, en ISO 8601. Nulo si nunca. */
    val medidosEn: String? = null,
    /** Si el último intento de alcanzar el servicio falló. */
    val sinConexion: Boolean = false,
)
