package com.app.cookify.core.model

/** El almuerzo de un dia concreto, con su costo ya escalado a los comensales. */
data class AlmuerzoDelDia(
    val dia: DiaSemana,
    val receta: Receta,
    val costoClp: Int,
)

/**
 * Una semana armada. [semilla] permite reproducir exactamente el mismo plan, que es
 * lo que hace testeable al motor y lo que permite que "regenerar" de un resultado
 * distinto sin volverse aleatorio de verdad.
 */
data class PlanSemanal(
    val almuerzos: List<AlmuerzoDelDia>,
    val solicitud: SolicitudPlan,
    val semilla: Long,
) {
    val costoTotalClp: Int get() = almuerzos.sumOf { it.costoClp }

    /** Cuanto sobra del presupuesto. Negativo si se paso. */
    val holguraClp: Int get() = solicitud.presupuestoClp - costoTotalClp

    val dentroDePresupuesto: Boolean get() = costoTotalClp <= solicitud.presupuestoClp
}

/** Resultado del motor de planificacion. */
sealed interface ResultadoPlan {

    data class Exito(val plan: PlanSemanal) : ResultadoPlan

    /**
     * No hay combinacion que quepa en el presupuesto. [minimoNecesarioClp] es lo que
     * costaria la semana mas barata posible con los filtros elegidos, para que la UI
     * pueda ofrecer "subir el presupuesto a X".
     */
    data class PresupuestoInsuficiente(
        val minimoNecesarioClp: Int,
        val planMasBarato: PlanSemanal,
    ) : ResultadoPlan

    /** Los filtros dejaron el catalogo sin recetas suficientes. */
    data class SinRecetasSuficientes(
        val motivo: String,
        val recetasDisponibles: Int,
        val diasPedidos: Int,
    ) : ResultadoPlan
}
