package com.app.cookify.core.model

/**
 * Todo lo que el usuario respondio en el onboarding. Es la entrada del motor de
 * planificacion y se guarda junto a la semana para poder regenerarla despues.
 */
data class SolicitudPlan(
    val supermercado: Supermercado,
    val personas: Int,
    val dias: List<DiaSemana>,
    val presupuestoClp: Int,
    val antojos: Set<Antojo>,
    val restriccion: Restriccion,
    val artefactos: Set<Artefacto>,
) {
    init {
        require(personas in RANGO_PERSONAS) { "personas fuera de rango: $personas" }
        require(dias.isNotEmpty()) { "hay que elegir al menos un dia" }
        require(antojos.size <= Antojo.MAXIMO_SELECCION) {
            "maximo ${Antojo.MAXIMO_SELECCION} antojos, llegaron ${antojos.size}"
        }
        require(artefactos.isNotEmpty()) { "hay que tener al menos un artefacto" }
        require(presupuestoClp > 0) { "el presupuesto debe ser positivo" }
    }

    /** Los dias siempre se recorren en orden L-D, sin importar en que orden se tocaron. */
    val diasOrdenados: List<DiaSemana> get() = dias.distinct().sortedBy { it.ordinal }

    companion object {
        val RANGO_PERSONAS = 1..12
    }
}
