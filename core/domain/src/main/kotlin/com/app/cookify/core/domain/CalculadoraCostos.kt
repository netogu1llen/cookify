package com.app.cookify.core.domain

import com.app.cookify.core.model.Ingrediente
import com.app.cookify.core.model.IngredienteCalculado
import com.app.cookify.core.model.IngredienteReceta
import com.app.cookify.core.model.Receta
import com.app.cookify.core.model.Supermercado
import com.app.cookify.core.model.Unidad
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Traduce una receta a pesos chilenos.
 *
 * Los precios del catalogo estan en la base de Lider y por unidad base (1 g, 1 ml,
 * 1 unidad). El supermercado elegido solo aplica un multiplicador: la app estima, no
 * cotiza precios reales.
 *
 * Los ingredientes marcados como opcionales no suman al costo, porque el presupuesto
 * tiene que reflejar lo que el usuario si o si va a comprar.
 */
object CalculadoraCostos {

    /** Se redondea a la centena porque mostrar "$4.387" da una precision que no tenemos. */
    private const val REDONDEO_CLP = 100

    fun costoReceta(
        receta: Receta,
        ingredientes: Map<String, Ingrediente>,
        personas: Int,
        supermercado: Supermercado,
    ): Int {
        val bruto = receta.ingredientes
            .filterNot { it.opcional }
            .sumOf { linea ->
                val ingrediente = ingredientes[linea.ingredienteId]
                    ?: error("Ingrediente '${linea.ingredienteId}' de la receta '${receta.id}' no esta en el catalogo")
                ingrediente.precioClpPorUnidad * cantidadTotal(linea, ingrediente, personas)
            }
        return redondear(bruto * supermercado.multiplicadorPrecio)
    }

    /**
     * Ingredientes ya escalados a los comensales, para el detalle de la receta y la
     * lista de compras. Aca si se incluyen los opcionales, marcados como tales.
     */
    fun ingredientesCalculados(
        receta: Receta,
        ingredientes: Map<String, Ingrediente>,
        personas: Int,
        supermercado: Supermercado,
    ): List<IngredienteCalculado> = receta.ingredientes.map { linea ->
        val ingrediente = ingredientes.getValue(linea.ingredienteId)
        val cantidadTotal = cantidadTotal(linea, ingrediente, personas)
        IngredienteCalculado(
            ingrediente = ingrediente,
            cantidadTotal = cantidadTotal,
            costoClp = redondear(
                ingrediente.precioClpPorUnidad * cantidadTotal * supermercado.multiplicadorPrecio,
            ),
            opcional = linea.opcional,
        )
    }

    /**
     * Lo que hay que comprar de verdad, no lo que pide la receta.
     *
     * Lo que se vende por unidad se redondea hacia arriba: media palta y 1,6 limones
     * no existen en el supermercado. Se hace aca y no al mostrar, para que el
     * presupuesto cobre las dos paltas enteras que el usuario va a terminar pagando.
     */
    private fun cantidadTotal(
        linea: IngredienteReceta,
        ingrediente: Ingrediente,
        personas: Int,
    ): Double {
        val bruto = linea.cantidadPorPorcion * personas
        return if (ingrediente.unidad == Unidad.UNIDAD) ceil(bruto) else bruto
    }

    private fun redondear(valor: Double): Int =
        (valor / REDONDEO_CLP).roundToInt() * REDONDEO_CLP
}
