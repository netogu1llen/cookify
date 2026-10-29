package com.app.cookify.server.datos.externo

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Un producto tal como lo publica la cadena, sin interpretar.
 *
 * [ppum] es el precio por unidad de medida que la propia cadena calcula, y
 * [medida] la unidad en que lo expresa. Que la cadena publique ese valor es lo
 * que hace viable todo el servicio: sin el habria que deducir el tamano del
 * envase desde el nombre del producto, que es exactamente donde se rompen los
 * raspadores de precios.
 */
data class ProductoCrudo(
    val sku: String,
    val nombre: String,
    val precioLista: Double?,
    val ppum: Double?,
    val medida: String?,
    val enStock: Boolean,
)

/**
 * Saca los productos de una pagina de categoria.
 *
 * Las dos cadenas son del mismo grupo y corren la misma plataforma, asi que un
 * solo parser sirve para las dos. La pagina trae el catalogo embebido en el
 * payload del framework, escapado dentro de etiquetas script.
 *
 * No se usa una expresion regular para cortar cada objeto: los objetos vienen
 * anidados y una regex no cierra bien la llave. Se cuenta la llave a mano,
 * respetando lo que esta dentro de una cadena de texto.
 */
object ParserCatalogo {

    private val json = Json { ignoreUnknownKeys = true }

    /** La llave por la que empieza cada objeto de producto en el payload. */
    private const val AGUJA = "{\"skuId\":"

    /** Tope de tamano de un objeto, para no recorrer la pagina entera si falta la llave de cierre. */
    private const val LARGO_MAXIMO = 20_000

    fun productos(html: String): List<ProductoCrudo> {
        val plano = html.replace("\\\"", "\"")
        val salida = LinkedHashMap<String, ProductoCrudo>()

        var desde = plano.indexOf(AGUJA)
        while (desde >= 0) {
            val cierre = buscarCierre(plano, desde)
            if (cierre > 0) {
                leerProducto(plano.substring(desde, cierre + 1))?.let { salida[it.sku] = it }
            }
            desde = plano.indexOf(AGUJA, desde + 1)
        }
        return salida.values.toList()
    }

    /** El indice de la llave que cierra el objeto que empieza en [inicio], o -1. */
    private fun buscarCierre(texto: String, inicio: Int): Int {
        var profundidad = 0
        var enCadena = false
        var escapa = false
        val hasta = minOf(texto.length, inicio + LARGO_MAXIMO)
        for (i in inicio until hasta) {
            val c = texto[i]
            if (enCadena) {
                when {
                    escapa -> escapa = false
                    c == '\\' -> escapa = true
                    c == '"' -> enCadena = false
                }
                continue
            }
            when (c) {
                '"' -> enCadena = true
                '{' -> profundidad++
                '}' -> {
                    profundidad--
                    if (profundidad == 0) return i
                }
            }
        }
        return -1
    }

    private fun leerProducto(crudo: String): ProductoCrudo? = runCatching {
        val objeto = json.parseToJsonElement(crudo) as? JsonObject ?: return null
        val sku = objeto["skuId"]?.jsonPrimitive?.content ?: return null
        val nombre = objeto["name"]?.jsonPrimitive?.content ?: return null
        ProductoCrudo(
            sku = sku,
            nombre = nombre,
            precioLista = objeto["listPrice"]?.jsonPrimitive?.doubleOrNull
                ?: objeto["price"]?.jsonPrimitive?.doubleOrNull,
            ppum = objeto["ppumListPrice"]?.jsonPrimitive?.doubleOrNull
                ?: objeto["ppumPrice"]?.jsonPrimitive?.doubleOrNull,
            medida = objeto["ppumMeasurementUnit"]?.jsonPrimitive?.content,
            // Si la cadena no lo dice, se supone disponible: descartar por una
            // ausencia dejaria la categoria vacia.
            enStock = objeto["stock"]?.jsonPrimitive?.booleanOrNull ?: true,
        )
    }.getOrNull()
}
