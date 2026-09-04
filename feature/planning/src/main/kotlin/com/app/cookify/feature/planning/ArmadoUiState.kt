package com.app.cookify.feature.planning

import androidx.compose.runtime.Immutable
import com.app.cookify.core.model.SolicitudPlan
import com.app.cookify.core.ui.componentes.EstadoCheck

/**
 * Estado de la pantalla de armado.
 *
 * Los textos de las etapas viven en el estado, no en el composable, porque nombran
 * datos del usuario ("tus $45.000", "de lunes a viernes"): es esa concreción la que
 * hace que la espera se sienta como trabajo real y no como un spinner de relleno.
 */
@Immutable
data class ArmadoUiState(
    val etapas: List<EtapaArmado> = emptyList(),
    val fallo: FalloArmado? = null,
)

@Immutable
data class EtapaArmado(
    val texto: String,
    val estado: EstadoCheck,
)

/**
 * Por qué no se pudo armar la semana.
 *
 * Cada caso trae lo necesario para ofrecer una salida concreta, no solo para
 * disculparse: un callejón sin salida al final de siete preguntas es la forma más
 * rápida de perder al usuario.
 */
sealed interface FalloArmado {

    /** La etapa donde se detiene la animación, para que el error apunte a su causa. */
    val etapa: Int

    /**
     * Ni la combinación más barata cabe. [solicitudMinima] es la misma solicitud con
     * el presupuesto subido a lo que de verdad cuesta, lista para seguir con ella.
     */
    data class Presupuesto(
        val presupuestoClp: Int,
        val minimoClp: Int,
        val solicitudMinima: SolicitudPlan,
        val semilla: Long,
    ) : FalloArmado {
        override val etapa: Int = ETAPA_PRESUPUESTO
    }

    /** Los filtros dejaron el catálogo con menos recetas que días pedidos. */
    data class SinRecetas(
        val motivo: String,
        val disponibles: Int,
        val diasPedidos: Int,
    ) : FalloArmado {
        override val etapa: Int = ETAPA_CATALOGO
    }
}

internal const val ETAPA_CATALOGO = 0
internal const val ETAPA_PRESUPUESTO = 1

/** Evento de una sola vez: la semana quedó lista y hay que navegar a verla. */
sealed interface EventoArmado {
    data class Listo(val solicitud: SolicitudPlan, val semilla: Long) : EventoArmado
}
