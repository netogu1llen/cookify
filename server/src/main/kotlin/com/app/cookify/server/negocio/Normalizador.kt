package com.app.cookify.server.negocio

import com.app.cookify.server.datos.externo.ProductoCrudo
import com.app.cookify.server.modelo.Fuente
import com.app.cookify.server.modelo.Unidad
import java.text.Normalizer

/**
 * Convierte lo que publica la cadena en el precio que usa el planificador.
 *
 * Es el centro del servicio y por eso es Kotlin puro: no sabe de HTTP ni de
 * SQL, recibe productos y una fuente y devuelve un precio. Asi se prueba con
 * JUnit en milisegundos y sin levantar nada, igual que el motor de
 * planificacion de la app.
 */
object Normalizador {

    /**
     * De la unidad en que la cadena publica su precio por unidad de medida, a
     * la unidad canonica del catalogo, con el divisor que las separa.
     *
     * El divisor de 'g' es 10 y no 1. Verificado contra precio y tamano en los
     * 176 productos de la categoria de condimentos de Jumbo: el oregano de 50 g
     * cuesta $1.150, o sea $23 por gramo, y la cadena publica 230. La paprika de
     * 100 g cuesta $1.950, o sea $19,5 por gramo, y publica 195. Cuando la
     * unidad es 'g' el valor viene por cada diez gramos; leerlo como precio por
     * gramo multiplicaba por diez el costo de todos los condimentos de una
     * receta, y nada fallaba al hacerlo.
     */
    private val equivalencias: Map<String, Pair<Unidad, Double>> = mapOf(
        "kg" to (Unidad.GRAMO to 1000.0),
        "g" to (Unidad.GRAMO to 10.0),
        "gr" to (Unidad.GRAMO to 10.0),
        "lt" to (Unidad.MILILITRO to 1000.0),
        "l" to (Unidad.MILILITRO to 1000.0),
        "ml" to (Unidad.MILILITRO to 10.0),
        "cc" to (Unidad.MILILITRO to 10.0),
        "un" to (Unidad.UNIDAD to 1.0),
        "unidad" to (Unidad.UNIDAD to 1.0),
    )

    data class Canonico(val unidad: Unidad, val precio: Double)

    /** El precio publicado llevado a pesos por gramo, por mililitro o por unidad. */
    fun canonizar(medida: String?, ppum: Double): Canonico? {
        val (unidad, divisor) = equivalencias[medida?.lowercase()?.trim()] ?: return null
        return Canonico(unidad, ppum / divisor)
    }

    /** Un producto que pasa los filtros de la fuente, ya convertido. */
    data class Candidato(
        val nombre: String,
        val precioBase: Double,
        val ppum: Double,
    )

    /**
     * Los productos de la categoria que son el ingrediente de la fuente.
     *
     * El orden de los filtros no es casual. Primero la categoria —que ya viene
     * dada—, porque es la que hace el trabajo: dentro de
     * frutas-y-verduras/verduras no hay mermelada de cebolla. Despues los
     * terminos, que eligen el ingrediente dentro de la categoria. Al final las
     * exclusiones, que sacan la variedad que no corresponde: cebolla frita,
     * betarraga cocida, zapallo en cubos.
     *
     * Buscar por nombre sin la categoria da precios absurdos. Medido: "leche"
     * devuelve una mediana de $8,74 por mililitro contra $1,06 real, porque
     * entra leche condensada y en polvo.
     */
    fun candidatos(productos: List<ProductoCrudo>, fuente: Fuente): List<Candidato> {
        val terminos = fuente.terminos.map(::sinTildes)
        val excluir = fuente.excluir.map(::sinTildes)

        return productos.mapNotNull { producto ->
            val nombre = sinTildes(producto.nombre)
            if (terminos.isNotEmpty() && terminos.none { it in nombre }) return@mapNotNull null
            if (excluir.any { it in nombre }) return@mapNotNull null
            if (!producto.enStock) return@mapNotNull null

            val ppum = producto.ppum?.takeIf { it > 0 } ?: return@mapNotNull null
            val canonico = canonizar(producto.medida, ppum) ?: return@mapNotNull null
            // Una medida distinta de la esperada no se convierte a la fuerza:
            // esa conversion seria una invencion. El producto se descarta.
            if (canonico.unidad != fuente.medidaEsperada) return@mapNotNull null

            Candidato(
                nombre = producto.nombre,
                precioBase = canonico.precio / fuente.unidadesBasePorMedida,
                ppum = ppum,
            )
        }
    }

    /**
     * Percentil 25 por interpolacion lineal.
     *
     * No la mediana, y la razon es del producto, no estadistica: quien cocina
     * con presupuesto contado compra la marca barata, no la del medio. Con
     * pocos candidatos cae naturalmente hacia el minimo, que es el
     * comportamiento deseado.
     */
    fun percentil25(valores: List<Double>): Double? {
        if (valores.isEmpty()) return null
        val orden = valores.sorted()
        if (orden.size == 1) return orden.first()
        val posicion = 0.25 * (orden.size - 1)
        val bajo = posicion.toInt()
        if (bajo + 1 >= orden.size) return orden[bajo]
        val resto = posicion - bajo
        return orden[bajo] + resto * (orden[bajo + 1] - orden[bajo])
    }

    /**
     * Minusculas y sin tildes, para comparar nombres.
     *
     * Hace falta en las dos direcciones: el mapeo escribe "brocoli" y la cadena
     * publica "Brócoli", y nadie escribe las tildes al configurar una fuente.
     */
    fun sinTildes(texto: String): String =
        Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD)
            .replace(MARCAS_DIACRITICAS, "")

    private val MARCAS_DIACRITICAS = Regex("\\p{Mn}+")
}
