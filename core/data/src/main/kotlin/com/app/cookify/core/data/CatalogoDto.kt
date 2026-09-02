package com.app.cookify.core.data

import com.app.cookify.core.model.Ingrediente
import com.app.cookify.core.model.Receta
import kotlinx.serialization.Serializable

/**
 * Envoltorios de los archivos de assets. Existen solo para llevar la cabecera
 * (version, fecha) por separado del contenido; Ingrediente y Receta ya son
 * @Serializable en :core:model y se deserializan directo.
 */
@Serializable
internal data class IngredientesDto(
    val version: Int,
    val actualizadoEn: String,
    val ingredientes: List<Ingrediente>,
)

@Serializable
internal data class RecetasDto(
    val version: Int,
    val actualizadoEn: String,
    val recetas: List<Receta>,
)
