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

/** El año se muestra con dos dígitos: "09/11/26", no "09/11/2026". */
private const val SIGLO = 100

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

/**
 * Una fecha ISO 8601 del servicio como "09/11/26 19:50", en hora local.
 *
 * En hora local y no en la que vino: el servicio responde en UTC, y decirle a
 * alguien en Chile que su precio se midió a las 22:50 cuando eran las 19:50 lo
 * hace dudar del dato entero.
 *
 * Devuelve nulo si el texto no se puede interpretar, para que quien la use
 * decida qué mostrar en vez de enseñar una fecha inventada.
 */
fun String?.aFechaHoraCorta(): String? {
    if (this.isNullOrBlank()) return null
    val instante = runCatching { Instant.parse(this) }.getOrNull() ?: return null
    val local = instante.atZone(ZoneId.systemDefault())
    return "%02d/%02d/%02d %02d:%02d".format(
        local.dayOfMonth,
        local.monthValue,
        local.year % SIGLO,
        local.hour,
        local.minute,
    )
}

/**
 * Lo mismo, pero desde un instante en milisegundos.
 *
 * El servicio responde texto ISO y la caché guarda milisegundos, así que hacen
 * falta las dos entradas. El formato de salida es el mismo para que en pantalla
 * "medido en el súper" y "traído al teléfono" se puedan comparar de un vistazo.
 */
fun Long.aFechaHoraCorta(): String {
    val local = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault())
    return "%02d/%02d/%02d %02d:%02d".format(
        local.dayOfMonth,
        local.monthValue,
        local.year % SIGLO,
        local.hour,
        local.minute,
    )
}
