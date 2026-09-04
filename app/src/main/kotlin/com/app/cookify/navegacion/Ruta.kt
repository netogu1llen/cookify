package com.app.cookify.navegacion

import androidx.navigation3.runtime.NavKey
import com.app.cookify.core.model.SolicitudPlan
import com.app.cookify.core.model.Supermercado
import kotlinx.serialization.Serializable

/**
 * Destinos de la app.
 *
 * Son @Serializable porque Navigation3 guarda el back stack completo en el estado
 * de la instancia: si Android mata el proceso, el usuario vuelve donde estaba.
 *
 * Las rutas cargan la solicitud y la semilla, no el plan armado. El motor es
 * determinista, así que con ese par se reconstruye la semana idéntica en cualquier
 * momento; meter el PlanSemanal entero en el back stack significaría serializar
 * setenta recetas con sus pasos cada vez que la app pasa a segundo plano.
 */
sealed interface Ruta : NavKey {

    @Serializable
    data object Home : Ruta

    @Serializable
    data object Onboarding : Ruta

    @Serializable
    data class Armado(val solicitud: SolicitudPlan) : Ruta

    @Serializable
    data class Semana(val solicitud: SolicitudPlan, val semilla: Long) : Ruta

    /** Una semana ya guardada, que se lee de la base tal como quedo. */
    @Serializable
    data class SemanaGuardada(val id: Long) : Ruta

    /**
     * El detalle no necesita la semana entera: con el id de la receta y las dos cosas
     * que cambian los números (comensales y supermercado) se resuelve todo contra el
     * catálogo.
     */
    @Serializable
    data class Detalle(
        val recetaId: String,
        val personas: Int,
        val supermercado: Supermercado,
    ) : Ruta
}
