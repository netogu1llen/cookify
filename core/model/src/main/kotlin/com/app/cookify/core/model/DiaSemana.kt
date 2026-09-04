package com.app.cookify.core.model

/** Días de la semana en orden L-D, con el nombre completo que se muestra en la UI. */
enum class DiaSemana(val nombre: String, val abreviatura: String) {
    LUNES("Lunes", "Lun"),
    MARTES("Martes", "Mar"),
    MIERCOLES("Miércoles", "Mié"),
    JUEVES("Jueves", "Jue"),
    VIERNES("Viernes", "Vie"),
    SABADO("Sábado", "Sáb"),
    DOMINGO("Domingo", "Dom"),
    ;

    companion object {

        /**
         * Los días como se dirían en voz alta: "de lunes a viernes", "lunes y jueves",
         * "el domingo".
         *
         * Se usa en la pantalla de armado y en las tarjetas de semana guardada. Un
         * rango corrido se dice como rango porque es como lo piensa el usuario; leer
         * "lunes, martes, miércoles, jueves y viernes" es correcto pero suena a lista
         * de asistencia.
         */
        fun frase(dias: List<DiaSemana>): String {
            val orden = dias.distinct().sortedBy { it.ordinal }
            val nombres = orden.map { it.nombre.lowercase() }
            return when {
                nombres.isEmpty() -> "ningún día"
                nombres.size == 1 -> "el ${nombres.first()}"
                esCorrido(orden) && nombres.size >= MINIMO_PARA_RANGO ->
                    "de ${nombres.first()} a ${nombres.last()}"
                else -> nombres.dropLast(1).joinToString(", ") + " y " + nombres.last()
            }
        }

        private fun esCorrido(orden: List<DiaSemana>): Boolean =
            orden.zipWithNext().all { (a, b) -> b.ordinal == a.ordinal + 1 }

        /** Con dos días "lunes y martes" se lee mejor que "de lunes a martes". */
        private const val MINIMO_PARA_RANGO = 3
    }
}
