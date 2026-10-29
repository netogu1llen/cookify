package com.app.cookify.server.negocio

import com.app.cookify.server.config.ErrorDelServicio
import com.app.cookify.server.config.validar
import com.app.cookify.server.datos.FuenteDao
import com.app.cookify.server.datos.FuenteDeDatos
import com.app.cookify.server.datos.IngredienteDao
import com.app.cookify.server.datos.PrecioDao
import com.app.cookify.server.datos.RecoleccionDao
import com.app.cookify.server.datos.SupermercadoDao
import com.app.cookify.server.datos.externo.CatalogoDeCadena
import com.app.cookify.server.dto.RecoleccionPeticion
import com.app.cookify.server.modelo.EstadoRecoleccion
import com.app.cookify.server.modelo.Fuente
import com.app.cookify.server.modelo.Origen
import com.app.cookify.server.modelo.Pagina
import com.app.cookify.server.modelo.Recoleccion
import com.app.cookify.server.modelo.Unidad
import java.time.OffsetDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

/**
 * Lee los precios del sitio de una cadena y los deja medidos en la base.
 *
 * Es la razon de existir del servicio. El argumento esta medido: comparando los
 * productos presentes el mismo dia en Jumbo y en Santa Isabel, la diferencia
 * entre cadenas va de -9% a +57% **segun el producto, no segun la tienda**. No
 * existe un multiplicador por supermercado, asi que el precio hay que medirlo.
 *
 * Y medir cuesta: un refresco completo son unas 29 categorias, varios miles de
 * productos y entre diez y veinte segundos. Eso no cabe en el telefono —la
 * pantalla de armado de la app dura 3,5 segundos— ni conviene repetirlo en cada
 * telefono: una descarga aqui sirve a todos los clientes.
 */
class RecolectorServicio(
    private val fuenteDeDatos: FuenteDeDatos,
    private val recolecciones: RecoleccionDao,
    private val fuentes: FuenteDao,
    private val precios: PrecioDao,
    private val supermercados: SupermercadoDao,
    private val ingredientes: IngredienteDao,
    private val catalogo: CatalogoDeCadena,
    private val alcance: CoroutineScope,
) {

    private val registro = LoggerFactory.getLogger(RecolectorServicio::class.java)

    /**
     * Cierra lo que quedo abierto de un arranque anterior. Se llama una vez, al
     * levantar el servicio, antes de atender nada.
     */
    suspend fun cerrarCorridasHuerfanas() {
        val cerradas = fuenteDeDatos.transaccion { recolecciones.cerrarHuerfanas(it) }
        if (cerradas > 0) {
            registro.warn(
                "{} recolecciones habian quedado EN_CURSO de un arranque anterior; " +
                    "se marcaron FALLIDA",
                cerradas,
            )
        }
    }

    suspend fun listar(supermercado: String?, pagina: Int?, tamano: Int?): Pagina<Recoleccion> {
        val (numero, largo) = paginacion(pagina, tamano)
        return fuenteDeDatos.transaccion {
            recolecciones.listar(it, supermercado?.uppercase(), numero, largo)
        }
    }

    suspend fun porId(id: Long): Recoleccion = fuenteDeDatos.transaccion {
        recolecciones.porId(it, id)
            ?: throw ErrorDelServicio.NoEncontrado("una recoleccion", id.toString())
    }

    suspend fun borrar(id: Long) {
        fuenteDeDatos.transaccion { conexion ->
            val recoleccion = recolecciones.porId(conexion, id)
                ?: throw ErrorDelServicio.NoEncontrado("una recoleccion", id.toString())
            if (recoleccion.estado == EstadoRecoleccion.EN_CURSO) {
                throw ErrorDelServicio.Conflicto(
                    "La recoleccion $id esta EN_CURSO: borrarla dejaria precios huerfanos " +
                        "apuntando a una corrida que ya no existe"
                )
            }
            recolecciones.borrar(conexion, id)
        }
    }

    /**
     * Abre una recoleccion y la deja corriendo en segundo plano.
     *
     * Devuelve la recoleccion recien abierta, no el resultado: por eso la ruta
     * responde 202 y no 201. Recorrer las categorias de una cadena toma
     * decenas de segundos y nadie deberia esperar colgado de una peticion HTTP
     * para saber que la corrida arranco.
     */
    suspend fun iniciar(peticion: RecoleccionPeticion): Recoleccion {
        val codigo = peticion.supermercado?.trim()?.uppercase().orEmpty()
        validar { exigir(codigo.isNotEmpty(), "supermercado", "es obligatorio") }

        val apertura = fuenteDeDatos.transaccion { conexion ->
            val cadena = supermercados.porCodigo(conexion, codigo)
                ?: throw ErrorDelServicio.NoEncontrado("un supermercado", codigo)

            if (!cadena.soportaMedicion) {
                throw ErrorDelServicio.Conflicto(
                    "${cadena.etiqueta} no tiene recolector: su sitio no entrega el catalogo " +
                        "renderizado en el servidor. Sus precios quedan ESTIMADO."
                )
            }
            val url = cadena.sitio ?: throw ErrorDelServicio.Conflicto(
                "${cadena.etiqueta} no tiene sitio configurado"
            )

            // Dos corridas simultaneas de la misma cadena escribirian la misma
            // fila con el mismo instante y una de las dos fallaria a medio
            // camino, dejando media cadena medida.
            if (recolecciones.hayEnCurso(conexion, codigo)) {
                throw ErrorDelServicio.Conflicto(
                    "Ya hay una recoleccion EN_CURSO para $codigo"
                )
            }

            val activas = fuentes.activasDe(conexion, codigo, peticion.ingredientes)
            if (activas.isEmpty()) {
                throw ErrorDelServicio.Conflicto(
                    "No hay fuentes activas para $codigo con ese filtro: no habria nada que leer"
                )
            }
            val categorias = activas.map { it.categoriaRuta }.distinct().size
            Apertura(
                recoleccion = recolecciones.abrir(conexion, codigo, categorias),
                fuentes = activas,
                sitio = url,
                // La unidad con la que se guarda el precio es la del catalogo,
                // no la que publica la cadena: la albahaca se vende por kilo y
                // el recetario la cuenta por atado.
                unidades = ingredientes.unidades(conexion),
            )
        }

        alcance.launch {
            recorrer(apertura.recoleccion, apertura.fuentes, apertura.sitio, apertura.unidades)
        }
        return apertura.recoleccion
    }

    private class Apertura(
        val recoleccion: Recoleccion,
        val fuentes: List<Fuente>,
        val sitio: String,
        val unidades: Map<String, Unidad>,
    )

    /**
     * El recorrido en si: una peticion por categoria, no una por ingrediente.
     *
     * Las fuentes se agrupan por categoria antes de salir a la red porque 96
     * ingredientes caben en 29 categorias: pedir una pagina por ingrediente
     * seria pedir la misma pagina tres veces para la cebolla, el tomate y la
     * papa.
     */
    private suspend fun recorrer(
        recoleccion: Recoleccion,
        fuentes: List<Fuente>,
        sitio: String,
        unidades: Map<String, Unidad>,
    ) {
        val medidoEn = OffsetDateTime.now()
        val porCategoria = fuentes.groupBy { it.categoriaRuta }

        var categoriasLeidas = 0
        var productosLeidos = 0
        val medidos = mutableListOf<Medicion>()
        val problemas = mutableListOf<String>()

        for ((ruta, fuentesDeLaRuta) in porCategoria) {
            when (val resultado = catalogo.productosDeCategoria(sitio, ruta)) {
                is CatalogoDeCadena.Resultado.NoDisponible -> {
                    problemas += "$ruta: ${resultado.motivo}"
                }

                is CatalogoDeCadena.Resultado.Leido -> {
                    categoriasLeidas++
                    productosLeidos += resultado.productos.size
                    for (fuente in fuentesDeLaRuta) {
                        val candidatos = Normalizador.candidatos(resultado.productos, fuente)
                        val precio = Normalizador.percentil25(candidatos.map { it.precioBase })
                        if (precio == null) {
                            problemas += "${fuente.ingredienteId}: sin candidatos en $ruta"
                        } else {
                            medidos += Medicion(fuente, precio, candidatos.size)
                        }
                    }
                }
            }
        }

        // Una recoleccion que no pudo leer ni una categoria es FALLIDA aunque
        // no haya lanzado ninguna excepcion. Decir COMPLETADA con cero precios
        // escritos seria mentir en el estado.
        val estado = if (categoriasLeidas == 0) {
            EstadoRecoleccion.FALLIDA
        } else {
            EstadoRecoleccion.COMPLETADA
        }

        runCatching {
            // Todo en una transaccion: media cadena medida y media vieja seria
            // imposible de distinguir despues.
            fuenteDeDatos.transaccion { conexion ->
                medidos.forEach { medicion ->
                    precios.crear(
                        conexion,
                        medicion.fuente.supermercado,
                        medicion.fuente.ingredienteId,
                        redondear(medicion.precio),
                        // La unidad del catalogo. medidaEsperada es la unidad en
                        // que publica la cadena, y el factor de la fuente ya
                        // llevo el valor de una a la otra: guardar aquella diria
                        // que la albahaca cuesta $1.320 el gramo.
                        unidades[medicion.fuente.ingredienteId] ?: medicion.fuente.medidaEsperada,
                        Origen.MEDIDO,
                        medicion.candidatos,
                        recoleccion.id,
                        medidoEn,
                    )
                }
                recolecciones.cerrar(
                    conexion,
                    recoleccion.id,
                    estado,
                    categoriasLeidas,
                    productosLeidos,
                    medidos.size,
                    problemas.take(TOPE_PROBLEMAS).joinToString("; ").ifEmpty { null },
                )
            }
            registro.info(
                "recoleccion {} de {}: {} categorias, {} productos, {} precios",
                recoleccion.id, recoleccion.supermercado, categoriasLeidas, productosLeidos,
                medidos.size,
            )
        }.onFailure { error ->
            registro.error("recoleccion {} no se pudo guardar", recoleccion.id, error)
            // Si falla el guardado, la corrida no puede quedar EN_CURSO para
            // siempre: eso bloquearia toda recoleccion futura de la cadena.
            runCatching {
                fuenteDeDatos.transaccion { conexion ->
                    recolecciones.cerrar(
                        conexion, recoleccion.id, EstadoRecoleccion.FALLIDA,
                        categoriasLeidas, productosLeidos, 0,
                        "no se pudo guardar: ${error.message}",
                    )
                }
            }
        }
    }

    /**
     * Cuatro decimales, los que guarda la columna.
     *
     * No se redondea a peso: en gramos los precios son fracciones, y redondear
     * aqui acumula un error grande al escalar a cuatro porciones por siete dias.
     */
    private fun redondear(valor: Double): Double = Math.round(valor * 10_000.0) / 10_000.0

    private data class Medicion(val fuente: Fuente, val precio: Double, val candidatos: Int)

    private companion object {
        /** Cuantos problemas se guardan en el detalle antes de cortar. */
        const val TOPE_PROBLEMAS = 25
    }
}
