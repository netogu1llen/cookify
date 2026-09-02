package com.app.cookify.feature.onboarding

import com.app.cookify.core.model.Antojo
import com.app.cookify.core.model.Artefacto
import com.app.cookify.core.model.DiaSemana
import com.app.cookify.core.model.Restriccion
import com.app.cookify.core.model.SolicitudPlan
import com.app.cookify.core.model.Supermercado
import kotlinx.serialization.Serializable

/** Los siete pasos, en orden. El indice se usa para la barra de progreso. */
enum class PasoOnboarding {
    SUPERMERCADO,
    PERSONAS,
    DIAS,
    PRESUPUESTO,
    ANTOJOS,
    RESTRICCION,
    ARTEFACTOS,
    ;

    companion object {
        val total = entries.size
    }
}

/**
 * Estado del wizard.
 *
 * Es @Serializable para poder guardarlo entero en SavedStateHandle: si Android mata
 * el proceso a mitad del cuestionario, el usuario no pierde lo que ya respondio.
 *
 * Los valores por defecto son los mas probables (4 personas, lunes a viernes,
 * estufa) para que avanzar sin tocar nada ya sea una respuesta razonable.
 */
@Serializable
data class OnboardingUiState(
    val paso: PasoOnboarding = PasoOnboarding.SUPERMERCADO,
    val supermercado: Supermercado? = null,
    val personas: Int = PERSONAS_POR_DEFECTO,
    val dias: Set<DiaSemana> = DIAS_POR_DEFECTO,
    val presupuestoClp: Int? = null,
    val antojos: Set<Antojo> = emptySet(),
    val restriccion: Restriccion? = null,
    val artefactos: Set<Artefacto> = setOf(Artefacto.ESTUFA),
) {
    /** Un paso solo deja avanzar cuando la respuesta esta completa. */
    val puedeAvanzar: Boolean
        get() = when (paso) {
            PasoOnboarding.SUPERMERCADO -> supermercado != null
            PasoOnboarding.PERSONAS -> personas in SolicitudPlan.RANGO_PERSONAS
            PasoOnboarding.DIAS -> dias.isNotEmpty()
            PasoOnboarding.PRESUPUESTO -> (presupuestoClp ?: 0) >= PRESUPUESTO_MINIMO
            // Los antojos son opcionales: sin ninguno el motor simplemente no prioriza.
            PasoOnboarding.ANTOJOS -> true
            PasoOnboarding.RESTRICCION -> restriccion != null
            PasoOnboarding.ARTEFACTOS -> artefactos.isNotEmpty()
        }

    val esUltimoPaso: Boolean get() = paso == PasoOnboarding.ARTEFACTOS

    val indicePaso: Int get() = paso.ordinal

    val topeAntojosAlcanzado: Boolean get() = antojos.size >= Antojo.MAXIMO_SELECCION

    /**
     * Solo se puede llamar cuando todos los pasos estan completos; el boton final
     * esta deshabilitado hasta entonces.
     */
    fun aSolicitud(): SolicitudPlan = SolicitudPlan(
        supermercado = requireNotNull(supermercado) { "falta el supermercado" },
        personas = personas,
        dias = dias.sortedBy { it.ordinal },
        presupuestoClp = requireNotNull(presupuestoClp) { "falta el presupuesto" },
        antojos = antojos,
        restriccion = requireNotNull(restriccion) { "falta la restriccion" },
        artefactos = artefactos,
    )

    companion object {
        const val PERSONAS_POR_DEFECTO = 4
        const val PRESUPUESTO_MINIMO = 5_000
        val DIAS_POR_DEFECTO = DiaSemana.entries.take(5).toSet()
    }
}
