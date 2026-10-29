package com.app.cookify.server.datos.externo

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

/**
 * El unico lugar del servicio que habla con el sitio de un supermercado.
 *
 * Esta en la capa de datos y no en la de negocio: desde el punto de vista del
 * servicio, jumbo.cl es una fuente de datos mas, como la base. Que este detras
 * de esta clase es lo que permite probar el recolector con paginas guardadas en
 * disco en lugar de salir a la red.
 */
interface CatalogoDeCadena {
    /** Los productos de una categoria, recorriendo sus paginas. */
    suspend fun productosDeCategoria(sitio: String, ruta: String): Resultado

    sealed interface Resultado {
        data class Leido(val productos: List<ProductoCrudo>, val paginas: Int) : Resultado
        data class NoDisponible(val motivo: String) : Resultado
    }
}

class ClienteCadena(
    private val paginasMaximas: Int,
    private val pausaEntrePeticionesMs: Long,
) : CatalogoDeCadena, AutoCloseable {

    private val registro = LoggerFactory.getLogger(ClienteCadena::class.java)

    private val http = HttpClient(CIO) {
        expectSuccess = false
        engine {
            requestTimeout = 45_000
        }
    }

    override suspend fun productosDeCategoria(
        sitio: String,
        ruta: String,
    ): CatalogoDeCadena.Resultado {
        val acumulado = LinkedHashMap<String, ProductoCrudo>()
        var paginasLeidas = 0

        for (pagina in 1..paginasMaximas) {
            val url = sitio + ruta + if (pagina == 1) "" else "?page=$pagina"
            val respuesta = runCatching {
                http.get(url) {
                    // La cadena responde el catalogo renderizado solo a un
                    // navegador. Sin estas cabeceras devuelve el cascaron.
                    header("User-Agent", AGENTE)
                    header("Accept-Language", "es-CL,es;q=0.9")
                }
            }.getOrElse { error ->
                return si(paginasLeidas, acumulado, "no se pudo conectar: ${error.message}")
            }

            if (!respuesta.status.isSuccess()) {
                // Una categoria que se acaba responde 404 a la pagina
                // siguiente. Eso es el fin del recorrido, no una falla; solo
                // falla cuando no se pudo leer ni la primera pagina.
                val motivo = "la cadena respondio ${respuesta.status.value} en $ruta"
                if (respuesta.status == HttpStatusCode.NotFound && pagina > 1) break
                return si(paginasLeidas, acumulado, motivo)
            }

            val productos = ParserCatalogo.productos(respuesta.bodyAsText())
            paginasLeidas++
            productos.forEach { acumulado.putIfAbsent(it.sku, it) }

            if (productos.size < PRODUCTOS_POR_PAGINA) break
            if (pausaEntrePeticionesMs > 0) delay(pausaEntrePeticionesMs)
        }

        registro.debug("{} -> {} productos en {} paginas", ruta, acumulado.size, paginasLeidas)
        return CatalogoDeCadena.Resultado.Leido(acumulado.values.toList(), paginasLeidas)
    }

    /**
     * Devuelve lo leido si alcanzo a leer algo, y falla solo si no leyo nada.
     *
     * Media categoria sirve: el percentil se calcula igual sobre los candidatos
     * que haya. Cero categoria no sirve, y ahi hay que decirlo.
     */
    private fun si(
        paginasLeidas: Int,
        acumulado: Map<String, ProductoCrudo>,
        motivo: String,
    ): CatalogoDeCadena.Resultado =
        if (paginasLeidas > 0) {
            CatalogoDeCadena.Resultado.Leido(acumulado.values.toList(), paginasLeidas)
        } else {
            CatalogoDeCadena.Resultado.NoDisponible(motivo)
        }

    override fun close() = http.close()

    private companion object {
        /**
         * Tamano de pagina del sitio. Si una pagina trae menos, es la ultima y
         * no hace falta pedir la siguiente para descubrirlo.
         */
        const val PRODUCTOS_POR_PAGINA = 40

        const val AGENTE =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }
}
