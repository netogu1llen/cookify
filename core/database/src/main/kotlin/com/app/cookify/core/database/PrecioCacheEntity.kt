package com.app.cookify.core.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index

/**
 * El último lote de precios que el servicio respondió para una cadena.
 *
 * Es caché y no verdad: la verdad vive en el servidor. Si esta tabla se borra
 * entera, la app sigue armando semanas con el catálogo empaquetado, solo que con
 * precios estimados.
 *
 * La clave es el par cadena-ingrediente porque eso es lo que identifica un
 * precio. [guardadoEn] es cuándo lo trajo la app, que no es lo mismo que
 * [medidoEn], cuándo el servicio lo leyó del supermercado: entre esas dos fechas
 * puede haber días, y la que decide si la caché venció es la primera.
 */
@Entity(
    tableName = "precios_cache",
    primaryKeys = ["supermercado", "ingrediente_id"],
    indices = [Index("supermercado")],
)
data class PrecioCacheEntity(
    val supermercado: String,
    @ColumnInfo(name = "ingrediente_id") val ingredienteId: String,
    @ColumnInfo(name = "precio_clp_por_unidad") val precioClpPorUnidad: Double,
    val unidad: String,
    /** MEDIDO o ESTIMADO, tal como lo dijo el servicio. */
    val origen: String,
    /** Cuándo lo midió el servicio, en ISO 8601. Nulo si nunca se midió. */
    @ColumnInfo(name = "medido_en") val medidoEn: String?,
    /** Cuándo lo trajo la app, en milisegundos desde la época. */
    @ColumnInfo(name = "guardado_en") val guardadoEn: Long,
)
