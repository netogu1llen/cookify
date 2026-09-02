package com.app.cookify.core.model

import kotlinx.serialization.Serializable

/**
 * Proteina o base principal del plato. El motor la usa para no repetir la misma
 * base varios dias seguidos: una semana de siete pollos calza el presupuesto pero
 * es una mala semana.
 */
@Serializable
enum class BasePrincipal {
    POLLO,
    VACUNO,
    CERDO,
    PESCADO,
    MARISCO,
    HUEVO,
    LEGUMBRE,
    VERDURA,
    PASTA,
    ARROZ,
    QUESO,
    TOFU,
}

/**
 * Una receta del catalogo local (`core/data/src/main/assets/recetas.json`).
 *
 * [requisitosArtefactos] se lee como una conjuncion de disyunciones: cada Set es un
 * grupo "alguno de estos". `[[ESTUFA], [HORNO, AIRFRYER]]` significa "necesita estufa
 * Y ademas horno o airfryer". Un grupo vacio o una lista vacia significa que no
 * requiere artefacto alguno.
 */
@Serializable
data class Receta(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val minutosTotal: Int,
    val porcionesBase: Int,
    val basePrincipal: BasePrincipal,
    val antojos: Set<Antojo> = emptySet(),
    val aptaPara: Set<Restriccion> = emptySet(),
    val requisitosArtefactos: List<Set<Artefacto>> = emptyList(),
    val ingredientes: List<IngredienteReceta>,
    val pasos: List<String>,
    val kcalPorPorcion: Int,
    val proteinaGPorPorcion: Int,
) {
    /** True si el usuario tiene lo necesario para cocinarla. */
    fun puedeCocinarseCon(disponibles: Set<Artefacto>): Boolean =
        requisitosArtefactos.all { grupo -> grupo.isEmpty() || grupo.any(disponibles::contains) }

    /** True si la receta respeta la restriccion alimentaria elegida. */
    fun respeta(restriccion: Restriccion): Boolean =
        restriccion == Restriccion.NINGUNA || restriccion in aptaPara
}
