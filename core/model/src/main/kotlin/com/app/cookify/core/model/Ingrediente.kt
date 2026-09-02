package com.app.cookify.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Unidad base en la que se miden las cantidades y los precios del catalogo. */
@Serializable
enum class Unidad(val simbolo: String) {
    @SerialName("g")
    GRAMO("g"),

    @SerialName("ml")
    MILILITRO("ml"),

    @SerialName("un")
    UNIDAD("un"),
}

/**
 * Un ingrediente del catalogo.
 *
 * [precioClpPorUnidad] es el precio de referencia en pesos por UNA unidad base
 * (1 g, 1 ml o 1 unidad) en Lider. Ejemplo: pollo a $7.500/kg se guarda como 7.5.
 * Se usa Double a proposito: en gramos los precios son fracciones de peso y
 * redondear aca acumularia un error grande al escalar a 4 porciones por 7 dias.
 */
@Serializable
data class Ingrediente(
    val id: String,
    val nombre: String,
    val pasillo: Pasillo,
    val unidad: Unidad,
    val precioClpPorUnidad: Double,
)

/** Cantidad de un ingrediente que lleva una receta, expresada POR PORCION. */
@Serializable
data class IngredienteReceta(
    val ingredienteId: String,
    val cantidadPorPorcion: Double,
    val opcional: Boolean = false,
)

/**
 * Un ingrediente ya resuelto contra el catalogo y escalado al numero de comensales.
 * Es lo que se muestra en el detalle de la receta y en la lista de compras.
 */
data class IngredienteCalculado(
    val ingrediente: Ingrediente,
    val cantidadTotal: Double,
    val costoClp: Int,
    val opcional: Boolean,
) {
    /** "1,2 kg", "350 g", "3 un". Convierte a kilo/litro cuando la cantidad lo amerita. */
    val cantidadLegible: String
        get() = when {
            ingrediente.unidad == Unidad.UNIDAD -> "${formatearNumero(cantidadTotal)} un"
            cantidadTotal >= MIL -> "${formatearNumero(cantidadTotal / MIL)} " +
                if (ingrediente.unidad == Unidad.GRAMO) "kg" else "L"
            else -> "${formatearNumero(cantidadTotal)} ${ingrediente.unidad.simbolo}"
        }

    private fun formatearNumero(valor: Double): String {
        val redondeado = Math.round(valor * DECIMAL) / DECIMAL
        return if (redondeado % 1.0 == 0.0) {
            redondeado.toLong().toString()
        } else {
            redondeado.toString().replace('.', ',')
        }
    }

    private companion object {
        const val MIL = 1000.0
        const val DECIMAL = 10.0
    }
}
