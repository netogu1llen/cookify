package com.app.cookify.core.data.red

import kotlinx.serialization.Serializable

/**
 * Lo que el servicio de precios responde, tal cual.
 *
 * Es el contrato HTTP y no el modelo del dominio: por eso vive aquí y no en
 * `:core:model`, y por eso los campos se llaman como los nombró el servidor.
 * Si mañana el servicio renombra un campo, cambia esta clase y el mapeo; el
 * resto de la app no se entera.
 *
 * Todos los campos opcionales tienen valor por omisión para que la app no se
 * caiga si el servicio agrega o quita algo. Esa tolerancia es lo que permite
 * que agregar un campo no obligue a subir la versión de la interfaz, y está
 * declarada así en el contrato (`docs/api-cookify.md`).
 */
@Serializable
internal data class PreciosVigentesDto(
    val supermercado: String = "",
    val actualizadoEn: String? = null,
    val cantidad: Int = 0,
    val datos: List<PrecioDto> = emptyList(),
)

@Serializable
internal data class PrecioDto(
    val ingredienteId: String,
    val precioClpPorUnidad: Double,
    val unidad: String,
    val origen: String,
    val candidatos: Int = 0,
    val medidoEn: String? = null,
)
