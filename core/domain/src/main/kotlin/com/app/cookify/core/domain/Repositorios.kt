package com.app.cookify.core.domain

import com.app.cookify.core.model.PlanSemanal
import com.app.cookify.core.model.PreciosDeCadena
import com.app.cookify.core.model.PreciosGuardados
import com.app.cookify.core.model.PruebaDeConexion
import com.app.cookify.core.model.Receta
import com.app.cookify.core.model.SemanaGuardada
import com.app.cookify.core.model.Supermercado
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
 * interfaz, cambiar de "JSON empaquetado" a "servicio remoto con cache" es
 * escribir una implementacion nueva, no refactorizar la app.
 *
 * Los precios que devuelve ya estan expresados **en la cadena pedida**, y cada
 * uno viene diciendo si se midio o se estimo. Quien calcula no aplica ningun
 * multiplicador: eso es ahora asunto de la implementacion que estima.
 */
interface PrecioRepository {
    /**
     * Los precios de [supermercado], con su procedencia.
     *
     * Nunca falla por falta de red: la implementacion remota cae a su cache y,
     * si tampoco la hay, al catalogo empaquetado. Sin ese respaldo no se podria
     * armar una semana sin señal.
     */
    suspend fun precios(supermercado: Supermercado): PreciosDeCadena
}

/**
 * El estado del servicio de precios, para poder diagnosticarlo desde la app.
 *
 * Es una interfaz aparte de [PrecioRepository] a proposito: aquella responde
 * *cuanto cuesta*, que es lo que la app necesita para funcionar. Esta responde
 * *de donde salio ese numero*, que es lo que una persona necesita para confiar
 * en el. Son dos preguntas distintas y solo la segunda justifica salir a la red
 * sin que nadie lo haya pedido.
 */
interface ServicioRepository {

    /** La direccion del servicio con que se compilo esta version de la app. */
    val direccion: String

    /** Pregunta al servicio si esta arriba, midiendo cuanto tarda el viaje. */
    suspend fun probarConexion(): PruebaDeConexion

    /** Lo que la app tiene guardado, cadena por cadena. */
    suspend fun preciosGuardados(): List<PreciosGuardados>

    /**
     * Borra los precios guardados.
     *
     * Sirve para forzar que la proxima semana vuelva a pedirlos al servicio, que
     * es como se comprueba de verdad que la conexion funciona. No borra nada del
     * usuario: las semanas guardadas no se tocan.
     */
    suspend fun olvidarPreciosGuardados()
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
