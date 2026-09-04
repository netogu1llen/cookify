package com.app.cookify.core.model

/**
 * "Que se te antoja": el usuario elige hasta [MAXIMO_SELECCION].
 * Cada receta declara los antojos que satisface y el motor de planificacion puntua
 * las coincidencias.
 */
enum class Antojo(
    val etiqueta: String,
    val descripcion: String,
) {
    RAPIDO("Rápido y sin esfuerzo", "Listo en 25 minutos o menos"),
    BAJO_CALORIAS("Bajo en calorías", "Menos de 500 kcal por porción"),
    PARA_TODOS("Amigable para todos", "Sabores que le gustan a grandes y chicos"),
    SANO("Algo sano", "Verduras frescas y poca fritura"),
    TAKEAWAY("Para envasar y llevar", "Aguanta bien recalentado al día siguiente"),
    GUT_FRIENDLY("Cae bien al estómago", "Suave, sin frituras ni exceso de condimento"),
    ALTO_PROTEINA("Alto en proteína", "30 g de proteína o más por porción"),
    ;

    companion object {
        const val MAXIMO_SELECCION = 3
    }
}
