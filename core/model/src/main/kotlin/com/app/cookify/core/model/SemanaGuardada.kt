package com.app.cookify.core.model

/**
 * Una semana que el usuario guardó, ya resuelta contra el catálogo.
 *
 * [creadaEn] es epoch en milisegundos. Se guarda el instante y no un texto formateado
 * para que la home pueda ordenar por fecha y mostrar el formato que quiera.
 */
data class SemanaGuardada(
    val id: Long,
    val nombre: String,
    val creadaEn: Long,
    val plan: PlanSemanal,
) {
    val cantidadAlmuerzos: Int get() = plan.almuerzos.size
}
