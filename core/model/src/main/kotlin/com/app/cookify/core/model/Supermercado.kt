package com.app.cookify.core.model

/**
 * Cadenas de supermercado soportadas.
 *
 * [multiplicadorPrecio] escala los precios de referencia del catalogo, que estan
 * expresados en la base de Lider (1.0). Son valores aproximados de posicionamiento
 * de precio, no datos scrapeados: la app estima el costo, no lo cotiza.
 */
enum class Supermercado(
    val etiqueta: String,
    val multiplicadorPrecio: Double,
) {
    LIDER("Lider", 1.00),
    ACUENTA("aCuenta", 0.92),
    TOTTUS("Tottus", 0.98),
    UNIMARC("Unimarc", 1.05),
    SANTA_ISABEL("Santa Isabel", 1.08),
    JUMBO("Jumbo", 1.15),
}
