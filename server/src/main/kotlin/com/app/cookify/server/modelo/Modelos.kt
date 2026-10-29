package com.app.cookify.server.modelo

import java.time.OffsetDateTime

/**
 * Los modelos del dominio del servicio.
 *
 * Son Kotlin puro: **no** estan anotados para serializar ni saben nada de HTTP.
 * Esa separacion es la que permite cambiar la forma de la respuesta de la API
 * sin tocar la capa de datos, y cambiar una columna sin cambiar el contrato.
 * La traduccion vive en un solo lugar, en el paquete dto.
 */

enum class Unidad(val simbolo: String) {
    GRAMO("g"),
    MILILITRO("ml"),
    UNIDAD("un");

    companion object {
        fun desdeSimbolo(valor: String?): Unidad? =
            entries.firstOrNull { it.simbolo == valor }
    }
}

enum class Pasillo {
    VERDULERIA, CARNICERIA, PESCADERIA, LACTEOS, ABARROTES, PANADERIA, CONGELADOS;

    companion object {
        fun desdeNombre(valor: String?): Pasillo? =
            entries.firstOrNull { it.name == valor?.uppercase() }
    }
}

/** Como se obtuvo un precio. La app lo muestra: no promete lo que no midio. */
enum class Origen {
    /** Leido del sitio de la cadena. */
    MEDIDO,

    /** Derivado del catalogo base por el multiplicador de la cadena. */
    ESTIMADO;

    companion object {
        fun desdeNombre(valor: String?): Origen? =
            entries.firstOrNull { it.name == valor?.uppercase() }
    }
}

enum class EstadoRecoleccion {
    EN_CURSO, COMPLETADA, FALLIDA
}

data class Supermercado(
    val codigo: String,
    val etiqueta: String,
    val multiplicadorEstimado: Double,
    val soportaMedicion: Boolean,
    val sitio: String?,
)

data class Ingrediente(
    val id: String,
    val nombre: String,
    val pasillo: Pasillo,
    val unidad: Unidad,
    val precioBaseClp: Double,
    val creadoEn: OffsetDateTime,
    val actualizadoEn: OffsetDateTime,
)

/**
 * De donde se lee el precio de un ingrediente en una cadena.
 *
 * [unidadesBasePorMedida] es el numero por el que se divide el precio publicado
 * una vez llevado a su unidad canonica. Vale 1 cuando el catalogo y la cadena
 * miden lo mismo, y deja de valer 1 cuando no: el catalogo cuenta dientes de
 * ajo y la cadena vende cabezas.
 */
data class Fuente(
    val id: Long,
    val supermercado: String,
    val ingredienteId: String,
    val categoriaRuta: String,
    val terminos: List<String>,
    val excluir: List<String>,
    val medidaEsperada: Unidad,
    val unidadesBasePorMedida: Double,
    val activa: Boolean,
)

data class Precio(
    val id: Long,
    val supermercado: String,
    val ingredienteId: String,
    val precioClpPorUnidad: Double,
    val unidad: Unidad,
    val origen: Origen,
    val candidatos: Int,
    val recoleccionId: Long?,
    val medidoEn: OffsetDateTime,
)

data class Recoleccion(
    val id: Long,
    val supermercado: String,
    val estado: EstadoRecoleccion,
    val iniciadaEn: OffsetDateTime,
    val terminadaEn: OffsetDateTime?,
    val categoriasPedidas: Int,
    val categoriasLeidas: Int,
    val productosLeidos: Int,
    val preciosEscritos: Int,
    val detalle: String?,
)

/** Una pagina de resultados, con lo necesario para pedir la siguiente. */
data class Pagina<T>(
    val datos: List<T>,
    val numero: Int,
    val tamano: Int,
    val total: Int,
) {
    val paginas: Int get() = if (tamano <= 0) 0 else (total + tamano - 1) / tamano
}
