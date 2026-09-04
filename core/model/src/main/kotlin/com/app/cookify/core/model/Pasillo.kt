package com.app.cookify.core.model

/** Pasillo del supermercado. Agrupa la lista de compras en el orden en que se recorre la tienda. */
enum class Pasillo(val etiqueta: String) {
    VERDULERIA("Frutas y verduras"),
    CARNICERIA("Carnicería"),
    PESCADERIA("Pescadería"),
    LACTEOS("Lácteos y huevos"),
    ABARROTES("Abarrotes"),
    PANADERIA("Panadería"),
    CONGELADOS("Congelados"),
}
