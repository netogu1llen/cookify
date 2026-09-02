package com.app.cookify.core.domain

import com.app.cookify.core.model.Ingrediente
import com.app.cookify.core.model.Receta

/** Fuente de las recetas. Hoy es el JSON empaquetado en assets. */
interface CatalogoRepository {
    suspend fun recetas(): List<Receta>
    suspend fun receta(id: String): Receta?
}

/**
 * Fuente de los precios, deliberadamente separada del catalogo de recetas.
 *
 * Las recetas son estables; los precios no. Teniendolos detras de su propia
 * interfaz, cambiar de "JSON empaquetado" a "JSON remoto con cache" o a una
 * fuente oficial es escribir una implementacion nueva, no refactorizar la app.
 *
 * Los precios devueltos son la base de referencia (Lider). El multiplicador por
 * supermercado lo aplica CalculadoraCostos, no esta capa.
 */
interface PrecioRepository {
    /** Ingredientes indexados por id. */
    suspend fun ingredientes(): Map<String, Ingrediente>

    /** Cuando se actualizaron los precios, para poder mostrarlo en Ajustes. */
    suspend fun actualizadoEn(): String
}
