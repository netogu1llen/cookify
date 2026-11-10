package com.app.cookify.core.model

/**
 * Como se obtuvo el precio de un ingrediente.
 *
 * La app lo muestra en pantalla en lugar de disimularlo: prometer precisión que
 * no se tiene es peor que admitir una estimación. En las dos cadenas con
 * recolector la mayoría de los precios son [MEDIDO]; en las otras cuatro, todos
 * son [ESTIMADO].
 */
enum class OrigenPrecio {
    /** Leído del sitio de la cadena por el servicio de precios. */
    MEDIDO,

    /** Derivado del catálogo base por el multiplicador de la cadena. */
    ESTIMADO,
}

/**
 * De dónde salió el lote de precios.
 *
 * No es lo mismo que [OrigenPrecio], que habla de **un** precio. Esto habla del
 * lote entero y existe para que la pantalla pueda explicar por qué el precio es
 * el que es, en lugar de mostrar un número sin contexto.
 */
enum class Procedencia {
    /** Recién traído del servicio. */
    SERVICIO,

    /** De lo que la app guardó la última vez que pudo. */
    CACHE,

    /** Del catálogo empaquetado en la app: no hubo servicio ni nada guardado. */
    CATALOGO,
}

/**
 * Los precios de los ingredientes **en una cadena concreta**.
 *
 * El precio de cada [Ingrediente] de [ingredientes] ya está expresado en esa
 * cadena. Esa es la diferencia con el diseño anterior, donde el catálogo traía
 * el precio de Líder y quien calculaba le aplicaba un multiplicador: ese
 * multiplicador no tiene sentido sobre un precio que ya se midió en la cadena,
 * y aplicarlo lo contaría dos veces.
 *
 * Ahora el multiplicador es un detalle de la fuente que **estima**, no del
 * cálculo. Quien calcula ya no sabe de supermercados.
 */
data class PreciosDeCadena(
    val supermercado: Supermercado,
    val ingredientes: Map<String, Ingrediente>,
    val origen: Map<String, OrigenPrecio>,
    /**
     * Cuándo se midió el lote, en ISO 8601. Es nulo cuando ningún precio de
     * esta cadena viene medido, que es el caso de las cuatro sin recolector.
     */
    val medidoEn: String? = null,
    /** De dónde salió este lote. */
    val procedencia: Procedencia = Procedencia.CATALOGO,
    /**
     * Si el último intento de hablar con el servicio falló.
     *
     * Es distinto de no haberlo intentado: con la caché fresca ni se intenta, y
     * eso no es estar sin conexión. Solo es verdadero cuando se intentó y no se
     * pudo, que es lo único que justifica decírselo al usuario.
     */
    val sinConexion: Boolean = false,
) {
    /** Cuántos de los precios vienen medidos y no estimados. */
    val medidos: Int get() = origen.count { it.value == OrigenPrecio.MEDIDO }

    /**
     * Si la mayoría de los precios de esta cadena vienen medidos.
     *
     * Es lo que decide si la pantalla puede decir "precio medido" sin matizar.
     * Se usa la mayoría y no la totalidad porque trece de los 196 pares quedaron
     * sin fuente a propósito: son ingredientes que la cadena no vende en línea.
     */
    val mayoriaMedida: Boolean get() = ingredientes.isNotEmpty() && medidos * 2 > ingredientes.size

    fun origenDe(ingredienteId: String): OrigenPrecio =
        origen[ingredienteId] ?: OrigenPrecio.ESTIMADO

    companion object {
        /**
         * Estima los precios de una cadena a partir del catálogo base.
         *
         * [base] son los 98 ingredientes con su precio escrito a mano, que está
         * en la base de Líder. Esta es la única regla de estimación del
         * producto y vive aquí —y no dentro de una implementación de
         * repositorio— porque es conocimiento del dominio: dice qué significa
         * «precio estimado» y se puede probar sin levantar nada.
         */
        fun estimados(
            supermercado: Supermercado,
            base: Map<String, Ingrediente>,
        ): PreciosDeCadena {
            val enLaCadena = base.mapValues { (_, ingrediente) ->
                ingrediente.copy(
                    precioClpPorUnidad = ingrediente.precioClpPorUnidad *
                        supermercado.multiplicadorPrecio,
                )
            }
            return PreciosDeCadena(
                supermercado = supermercado,
                ingredientes = enLaCadena,
                origen = enLaCadena.mapValues { OrigenPrecio.ESTIMADO },
                // Nulo a propósito: nada de esto se midió. La fecha del catálogo
                // es cuándo se escribieron los valores a mano, que es otra cosa.
                medidoEn = null,
                procedencia = Procedencia.CATALOGO,
            )
        }
    }
}
