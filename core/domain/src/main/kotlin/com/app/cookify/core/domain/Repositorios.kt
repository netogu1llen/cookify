package com.app.cookify.core.domain

import com.app.cookify.core.model.Ingrediente
import com.app.cookify.core.model.PlanSemanal
import com.app.cookify.core.model.Receta
import com.app.cookify.core.model.SemanaGuardada
import kotlinx.coroutines.flow.Flow

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

/**
 * Las semanas que el usuario guardó.
 *
 * Devuelve Flow y no una lista: la home se suscribe una vez y se actualiza sola
 * cuando se guarda o se borra una semana, sin refrescos manuales ni callbacks.
 */
interface SemanaRepository {
    fun observar(): Flow<List<SemanaGuardada>>

    /** @return el id de la semana recién guardada. */
    suspend fun guardar(nombre: String, plan: PlanSemanal): Long

    /** Una semana concreta, para reabrirla desde la home. */
    suspend fun porId(id: Long): SemanaGuardada?

    suspend fun borrar(id: Long)

    /** Para decidir en el arranque si mostrar la home con semanas o el estado vacío. */
    suspend fun hayAlguna(): Boolean
}
