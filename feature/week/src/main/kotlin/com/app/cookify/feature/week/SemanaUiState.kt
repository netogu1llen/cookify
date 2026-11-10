package com.app.cookify.feature.week

import androidx.compose.runtime.Immutable
import com.app.cookify.core.model.PlanSemanal
import com.app.cookify.core.ui.componentes.EstadoPrecios

/**
 * La semana armada, lista para mostrarse.
 *
 * El plan no viaja en la ruta de navegación: se reconstruye desde la solicitud y la
 * semilla, que es lo que hace que el motor determinista valga la pena. Por eso hay un
 * estado de carga aunque el cálculo dure milisegundos.
 */
@Immutable
data class SemanaUiState(
    val cargando: Boolean = true,
    val plan: PlanSemanal? = null,
    val error: String? = null,
    val nombrePropuesto: String = "",
    val pidiendoNombre: Boolean = false,
    val guardando: Boolean = false,
    val guardada: Boolean = false,
    /**
     * Una semana reabierta desde la home no se regenera ni se vuelve a guardar: ya es
     * la que el usuario eligio, y "regenerar" ahi seria destruir lo que guardo.
     */
    val soloLectura: Boolean = false,
    /**
     * El nombre que le puso el usuario, cuando la semana viene de la home. Una semana
     * recien armada todavia no tiene nombre y se anuncia como "Tu semana".
     */
    val nombre: String? = null,
    /**
     * De dónde salieron los precios con que se costeó esta semana.
     *
     * Solo tiene sentido en una semana recién armada. Una reabierta desde la
     * home guarda el costo con que se armó, y anunciar la procedencia de los
     * precios de hoy sobre un costo de hace dos semanas seria mentir.
     */
    val precios: EstadoPrecios? = null,
) {
    /**
     * Se muestra la semana igual cuando no cabe en el presupuesto, con la barra en
     * rojo. Esconderla seria peor: el usuario acepto seguir sabiendo que se pasaba, y
     * lo que necesita ver es cuanto.
     */
    val excedePresupuesto: Boolean get() = plan?.dentroDePresupuesto == false
}
