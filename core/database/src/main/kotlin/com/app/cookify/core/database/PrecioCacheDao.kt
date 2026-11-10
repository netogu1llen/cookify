package com.app.cookify.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction

@Dao
interface PrecioCacheDao {

    @Query("select * from precios_cache where supermercado = :supermercado")
    suspend fun deSupermercado(supermercado: String): List<PrecioCacheEntity>

    /**
     * Cuándo se guardó el lote más viejo de esa cadena.
     *
     * El más viejo y no el más nuevo: si un lote se guardó a medias, lo que
     * decide si la caché sirve es la fila que lleva más tiempo ahí.
     */
    @Query("select min(guardado_en) from precios_cache where supermercado = :supermercado")
    suspend fun guardadoEn(supermercado: String): Long?

    /**
     * Reemplaza el lote completo de una cadena, todo o nada.
     *
     * Borrar e insertar dentro de una transacción, y no insertar encima, porque
     * un ingrediente que el servicio dejó de responder tiene que desaparecer de
     * la caché en lugar de quedarse con su precio viejo para siempre.
     */
    @Transaction
    suspend fun reemplazarLote(supermercado: String, precios: List<PrecioCacheEntity>) {
        borrarDe(supermercado)
        insertar(precios)
    }

    @Query("delete from precios_cache where supermercado = :supermercado")
    suspend fun borrarDe(supermercado: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(precios: List<PrecioCacheEntity>)
}
