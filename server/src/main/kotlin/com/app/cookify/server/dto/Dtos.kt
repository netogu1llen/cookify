package com.app.cookify.server.dto

import com.app.cookify.server.modelo.Fuente
import com.app.cookify.server.modelo.Ingrediente
import com.app.cookify.server.modelo.Pagina
import com.app.cookify.server.modelo.Precio
import com.app.cookify.server.modelo.Recoleccion
import com.app.cookify.server.modelo.Supermercado
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.Serializable

/**
 * El contrato HTTP del servicio: lo unico que el cliente puede suponer.
 *
 * Es deliberadamente distinto del modelo del dominio. Asi una columna se puede
 * renombrar sin romper a la app, y la respuesta puede cambiar de forma sin
 * tocar el SQL. Donde los dos coinciden hoy, la traduccion es trivial; el valor
 * esta en que exista el lugar donde hacerla cuando dejen de coincidir.
 *
 * Las fechas viajan como texto ISO 8601 con desplazamiento explicito. No como
 * marca de tiempo en segundos: un numero no dice en que zona fue medido, y el
 * precio de un supermercado chileno se mide en hora de Chile.
 */

private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

internal fun OffsetDateTime.aTexto(): String = format(ISO)

// --- supermercados ---------------------------------------------------------

@Serializable
data class SupermercadoDto(
    val codigo: String,
    val etiqueta: String,
    val multiplicadorEstimado: Double,
    val soportaMedicion: Boolean,
    val sitio: String? = null,
)

fun Supermercado.aDto() = SupermercadoDto(
    codigo = codigo,
    etiqueta = etiqueta,
    multiplicadorEstimado = multiplicadorEstimado,
    soportaMedicion = soportaMedicion,
    sitio = sitio,
)

// --- ingredientes ----------------------------------------------------------

@Serializable
data class IngredienteDto(
    val id: String,
    val nombre: String,
    val pasillo: String,
    val unidad: String,
    val precioBaseClp: Double,
    val creadoEn: String,
    val actualizadoEn: String,
)

fun Ingrediente.aDto() = IngredienteDto(
    id = id,
    nombre = nombre,
    pasillo = pasillo.name,
    unidad = unidad.simbolo,
    precioBaseClp = precioBaseClp,
    creadoEn = creadoEn.aTexto(),
    actualizadoEn = actualizadoEn.aTexto(),
)

/**
 * Cuerpo de POST y PUT de ingredientes.
 *
 * Todos los campos son nullable aunque sean obligatorios, y eso es a proposito:
 * si fueran no nulos, un cuerpo incompleto fallaria dentro del deserializador
 * con un mensaje de la libreria. Asi el campo llega vacio al validador y el
 * cliente recibe el nombre exacto de lo que falta.
 */
@Serializable
data class IngredientePeticion(
    val id: String? = null,
    val nombre: String? = null,
    val pasillo: String? = null,
    val unidad: String? = null,
    val precioBaseClp: Double? = null,
)

// --- fuentes ---------------------------------------------------------------

@Serializable
data class FuenteDto(
    val id: Long,
    val supermercado: String,
    val ingredienteId: String,
    val categoriaRuta: String,
    val terminos: List<String>,
    val excluir: List<String>,
    val medidaEsperada: String,
    val unidadesBasePorMedida: Double,
    val activa: Boolean,
)

fun Fuente.aDto() = FuenteDto(
    id = id,
    supermercado = supermercado,
    ingredienteId = ingredienteId,
    categoriaRuta = categoriaRuta,
    terminos = terminos,
    excluir = excluir,
    medidaEsperada = medidaEsperada.simbolo,
    unidadesBasePorMedida = unidadesBasePorMedida,
    activa = activa,
)

@Serializable
data class FuentePeticion(
    val supermercado: String? = null,
    val ingredienteId: String? = null,
    val categoriaRuta: String? = null,
    val terminos: List<String> = emptyList(),
    val excluir: List<String> = emptyList(),
    val medidaEsperada: String? = null,
    val unidadesBasePorMedida: Double? = null,
    val activa: Boolean = true,
)

// --- precios ---------------------------------------------------------------

@Serializable
data class PrecioDto(
    val id: Long,
    val ingredienteId: String,
    val precioClpPorUnidad: Double,
    val unidad: String,
    val origen: String,
    val candidatos: Int,
    val recoleccionId: Long? = null,
    val medidoEn: String,
)

fun Precio.aDto() = PrecioDto(
    id = id,
    ingredienteId = ingredienteId,
    precioClpPorUnidad = precioClpPorUnidad,
    unidad = unidad.simbolo,
    origen = origen.name,
    candidatos = candidatos,
    recoleccionId = recoleccionId,
    medidoEn = medidoEn.aTexto(),
)

/**
 * La respuesta que la app guarda en su cache.
 *
 * Lleva [actualizadoEn] al nivel de la lista y no solo en cada precio: la app
 * necesita una sola fecha para decidir si su cache vencio, y es la de la
 * medicion mas nueva del lote.
 */
@Serializable
data class PreciosVigentesDto(
    val supermercado: String,
    val actualizadoEn: String? = null,
    val cantidad: Int,
    val datos: List<PrecioDto>,
)

@Serializable
data class PrecioPeticion(
    val supermercado: String? = null,
    val ingredienteId: String? = null,
    val precioClpPorUnidad: Double? = null,
    val unidad: String? = null,
    val origen: String? = null,
    val candidatos: Int = 0,
    val medidoEn: String? = null,
)

// --- recolecciones ---------------------------------------------------------

@Serializable
data class RecoleccionDto(
    val id: Long,
    val supermercado: String,
    val estado: String,
    val iniciadaEn: String,
    val terminadaEn: String? = null,
    val categoriasPedidas: Int,
    val categoriasLeidas: Int,
    val productosLeidos: Int,
    val preciosEscritos: Int,
    val detalle: String? = null,
)

fun Recoleccion.aDto() = RecoleccionDto(
    id = id,
    supermercado = supermercado,
    estado = estado.name,
    iniciadaEn = iniciadaEn.aTexto(),
    terminadaEn = terminadaEn?.aTexto(),
    categoriasPedidas = categoriasPedidas,
    categoriasLeidas = categoriasLeidas,
    productosLeidos = productosLeidos,
    preciosEscritos = preciosEscritos,
    detalle = detalle,
)

@Serializable
data class RecoleccionPeticion(
    val supermercado: String? = null,
    /** Vacio significa todas las fuentes activas de la cadena. */
    val ingredientes: List<String> = emptyList(),
)

// --- paginacion ------------------------------------------------------------

@Serializable
data class PaginaDto<T>(
    val datos: List<T>,
    val pagina: Meta,
) {
    @Serializable
    data class Meta(val numero: Int, val tamano: Int, val total: Int, val paginas: Int)
}

fun <T, R> Pagina<T>.aDto(mapear: (T) -> R) = PaginaDto(
    datos = datos.map(mapear),
    pagina = PaginaDto.Meta(numero = numero, tamano = tamano, total = total, paginas = paginas),
)

// --- salud -----------------------------------------------------------------

@Serializable
data class SaludDto(
    val estado: String,
    val version: String,
    val baseDeDatos: String,
)
