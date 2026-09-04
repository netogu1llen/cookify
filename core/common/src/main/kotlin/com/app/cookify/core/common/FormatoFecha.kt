package com.app.cookify.core.common

import java.time.Instant
import java.time.ZoneId

/**
 * Fechas en español chileno, con los meses escritos a mano.
 *
 * Igual que el formato de pesos: DateTimeFormatter usaría el locale del teléfono, y
 * un usuario con el sistema en inglés vería "Week of September 4" dentro de una app
 * que está entera en español.
 */
private val MESES = arrayOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

/** "4 de septiembre". */
fun Long.aFechaLegible(): String {
    val fecha = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
    return "${fecha.dayOfMonth} de ${MESES[fecha.monthValue - 1]}"
}

/**
 * Nombre por defecto de una semana guardada: "Semana del 4 de septiembre".
 *
 * Es editable, pero tiene que servir tal cual: nadie quiere ponerle nombre a una
 * semana de almuerzos, y un campo vacío obligatorio convertiría el momento de guardar
 * en una tarea.
 */
fun nombrePorDefectoSemana(instante: Long): String = "Semana del ${instante.aFechaLegible()}"
