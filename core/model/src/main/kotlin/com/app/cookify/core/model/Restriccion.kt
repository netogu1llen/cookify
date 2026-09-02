package com.app.cookify.core.model

/** Restriccion alimentaria. Seleccion unica. */
enum class Restriccion(val etiqueta: String) {
    NINGUNA("Ninguna"),
    VEGETARIANO("Vegetariano"),
    VEGANO("Vegano"),
    SOLO_PESCADO("Solo pescado"),
}
