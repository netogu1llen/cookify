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

    /**
     * Que hay guardado, cadena por cadena.
     *
     * Se agrega en la base y no trayendo las 588 filas para contarlas en
     * memoria: la pantalla de ajustes solo necesita seis numeros.
     */
    @Query(
        """
        select supermercado,
               count(*) as total,
               sum(case when origen = 'MEDIDO' then 1 else 0 end) as medidos,
               max(medido_en) as medidoEn,
               min(guardado_en) as guardadoEn
          from precios_cache
         group by supermercado
         order by supermercado
        """
    )
    suspend fun resumen(): List<ResumenCache>

    /** Vacia la cache entera. No toca las semanas guardadas. */
    @Query("delete from precios_cache")
    suspend fun borrarTodo()
}

/** Una fila del resumen de [PrecioCacheDao.resumen]. */
data class ResumenCache(
    val supermercado: String,
    val total: Int,
    val medidos: Int,
    val medidoEn: String?,
    val guardadoEn: Long?,
)
