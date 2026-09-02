package com.app.cookify.core.model

/** Dias de la semana en orden L-D, con el nombre completo que se muestra en la UI. */
enum class DiaSemana(val nombre: String, val abreviatura: String) {
    LUNES("Lunes", "Lun"),
    MARTES("Martes", "Mar"),
    MIERCOLES("Miercoles", "Mie"),
    JUEVES("Jueves", "Jue"),
    VIERNES("Viernes", "Vie"),
    SABADO("Sabado", "Sab"),
    DOMINGO("Domingo", "Dom"),
}
