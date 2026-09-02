package com.app.cookify.navegacion

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Destinos de la app.
 *
 * Son @Serializable porque Navigation3 guarda el back stack completo en el estado
 * de la instancia: si Android mata el proceso, el usuario vuelve donde estaba.
 */
sealed interface Ruta : NavKey {

    @Serializable
    data object Home : Ruta

    @Serializable
    data object Onboarding : Ruta
}
