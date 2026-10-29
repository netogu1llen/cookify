package com.app.cookify.server.negocio

import com.app.cookify.server.config.ErrorDelServicio
import com.app.cookify.server.config.validar
import com.app.cookify.server.datos.FuenteDao
import com.app.cookify.server.datos.FuenteDeDatos
import com.app.cookify.server.datos.IngredienteDao
import com.app.cookify.server.datos.PrecioDao
import com.app.cookify.server.datos.SupermercadoDao
import com.app.cookify.server.datos.ViolacionDeUnicidad
import com.app.cookify.server.dto.FuentePeticion
import com.app.cookify.server.dto.IngredientePeticion
import com.app.cookify.server.dto.PrecioPeticion
import com.app.cookify.server.modelo.Fuente
import com.app.cookify.server.modelo.Ingrediente
import com.app.cookify.server.modelo.Origen
import com.app.cookify.server.modelo.Pagina
import com.app.cookify.server.modelo.Pasillo
import com.app.cookify.server.modelo.Precio
import com.app.cookify.server.modelo.Supermercado
import com.app.cookify.server.modelo.Unidad
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * Las reglas del servicio.
 *
 * Esta capa decide **si** una operacion vale y **que** significa; la de datos
 * decide como se guarda y la de ruteo como se habla por HTTP. En concreto: aqui
 * no hay una sola sentencia SQL ni una sola referencia a codigos de estado, y
 * en el ruteo no hay una sola validacion de negocio.
 */

/** Tope de tamano de pagina. Sin el, un solo GET puede arrastrar toda la tabla. */
private const val TAMANO_MAXIMO = 200

internal const val TAMANO_POR_OMISION = 50

/** Valida y normaliza los parametros de paginacion de cualquier coleccion. */
internal fun paginacion(pagina: Int?, tamano: Int?, porOmision: Int = TAMANO_POR_OMISION):
    Pair<Int, Int> {
    validar {
        exigir(pagina == null || pagina >= 1, "pagina", "debe ser 1 o mayor")
        exigir(
            tamano == null || tamano in 1..TAMANO_MAXIMO,
            "tamano",
            "debe estar entre 1 y $TAMANO_MAXIMO",
        )
    }
    return (pagina ?: 1) to (tamano ?: porOmision)
}

// ---------------------------------------------------------------------------

/**
 * Si el servicio puede atender.
 *
 * Existe para que la ruta /salud no tenga que alcanzar la capa de datos por
 * encima del negocio. Sin el, ruteo importaria FuenteDeDatos y la regla de que
 * la presentacion no toca la base dejaria de ser verificable de un vistazo.
 */
class SaludServicio(private val fuenteDeDatos: FuenteDeDatos) {
    suspend fun baseResponde(): Boolean = fuenteDeDatos.responde()
}

class SupermercadoServicio(
    private val fuenteDeDatos: FuenteDeDatos,
    private val dao: SupermercadoDao,
) {
    suspend fun todos(): List<Supermercado> = fuenteDeDatos.transaccion { dao.todos(it) }

    suspend fun porCodigo(codigo: String): Supermercado = fuenteDeDatos.transaccion {
        dao.porCodigo(it, codigo.uppercase())
            ?: throw ErrorDelServicio.NoEncontrado("un supermercado", codigo)
    }
}

// ---------------------------------------------------------------------------

class IngredienteServicio(
    private val fuenteDeDatos: FuenteDeDatos,
    private val dao: IngredienteDao,
) {

    suspend fun listar(
        pasillo: String?,
        busqueda: String?,
        pagina: Int?,
        tamano: Int?,
    ): Pagina<Ingrediente> {
        val (numero, largo) = paginacion(pagina, tamano)
        val filtro = pasillo?.let {
            Pasillo.desdeNombre(it) ?: throw ErrorDelServicio.Validacion(
                "El pasillo '$it' no existe. Valores: ${Pasillo.entries.joinToString()}"
            )
        }
        return fuenteDeDatos.transaccion { dao.listar(it, filtro, busqueda, numero, largo) }
    }

    suspend fun porId(id: String): Ingrediente = fuenteDeDatos.transaccion {
        dao.porId(it, id) ?: throw ErrorDelServicio.NoEncontrado("un ingrediente", id)
    }

    suspend fun crear(peticion: IngredientePeticion): Ingrediente {
        val id = peticion.id?.trim().orEmpty()
        validar {
            exigir(id.isNotEmpty(), "id", "es obligatorio")
            exigir(
                id.isEmpty() || FORMA_ID.matches(id),
                "id",
                "solo minusculas, numeros y guion bajo, entre 2 y 48 caracteres",
            )
        }
        val datos = validarCuerpo(peticion)

        return try {
            fuenteDeDatos.transaccion {
                dao.crear(it, id, datos.nombre, datos.pasillo, datos.unidad, datos.precio)
            }
        } catch (e: ViolacionDeUnicidad) {
            // Quien manda el segundo POST cree que esta creando algo. Decirle
            // 200 con el recurso que ya existia le oculta que no lo creo el.
            throw ErrorDelServicio.Conflicto("Ya existe un ingrediente con id '$id'")
                .also { it.initCause(e) }
        }
    }

    suspend fun actualizar(id: String, peticion: IngredientePeticion): Ingrediente {
        val datos = validarCuerpo(peticion)
        return fuenteDeDatos.transaccion {
            dao.actualizar(it, id, datos.nombre, datos.pasillo, datos.unidad, datos.precio)
                ?: throw ErrorDelServicio.NoEncontrado("un ingrediente", id)
        }
    }

    suspend fun borrar(id: String) {
        val borrado = fuenteDeDatos.transaccion { dao.borrar(it, id) }
        if (!borrado) throw ErrorDelServicio.NoEncontrado("un ingrediente", id)
    }

    private class Validado(
        val nombre: String,
        val pasillo: Pasillo,
        val unidad: Unidad,
        val precio: Double,
    )

    private fun validarCuerpo(peticion: IngredientePeticion): Validado {
        val nombre = peticion.nombre?.trim().orEmpty()
        val pasillo = Pasillo.desdeNombre(peticion.pasillo)
        val unidad = Unidad.desdeSimbolo(peticion.unidad)
        val precio = peticion.precioBaseClp

        validar {
            exigir(nombre.isNotEmpty(), "nombre", "es obligatorio")
            exigir(
                pasillo != null,
                "pasillo",
                "debe ser uno de: ${Pasillo.entries.joinToString { it.name }}",
            )
            exigir(
                unidad != null,
                "unidad",
                "debe ser uno de: ${Unidad.entries.joinToString { it.simbolo }}",
            )
            exigir(precio != null, "precioBaseClp", "es obligatorio")
            exigir(precio == null || precio > 0, "precioBaseClp", "debe ser mayor que 0")
        }
        return Validado(nombre, pasillo!!, unidad!!, precio!!)
    }

    private companion object {
        val FORMA_ID = Regex("^[a-z0-9_]{2,48}$")
    }
}

// ---------------------------------------------------------------------------

class FuenteServicio(
    private val fuenteDeDatos: FuenteDeDatos,
    private val dao: FuenteDao,
    private val supermercados: SupermercadoDao,
    private val ingredientes: IngredienteDao,
) {

    suspend fun listar(
        supermercado: String?,
        ingredienteId: String?,
        activa: Boolean?,
        pagina: Int?,
        tamano: Int?,
    ): Pagina<Fuente> {
        val (numero, largo) = paginacion(pagina, tamano, TAMANO_MAXIMO)
        return fuenteDeDatos.transaccion {
            dao.listar(it, supermercado?.uppercase(), ingredienteId, activa, numero, largo)
        }
    }

    suspend fun porId(id: Long): Fuente = fuenteDeDatos.transaccion {
        dao.porId(it, id) ?: throw ErrorDelServicio.NoEncontrado("una fuente", id.toString())
    }

    suspend fun crear(peticion: FuentePeticion): Fuente {
        val datos = validarCuerpo(peticion)
        return try {
            fuenteDeDatos.transaccion { conexion ->
                exigirQueExistan(conexion, datos.supermercado, datos.ingredienteId)
                dao.crear(conexion, datos.aFuente(0))
            }
        } catch (e: ViolacionDeUnicidad) {
            throw ErrorDelServicio.Conflicto(
                "Ya hay una fuente para ${datos.supermercado} y ${datos.ingredienteId}. " +
                    "Un par cadena-ingrediente tiene a lo mas una fuente: con dos, el precio " +
                    "dependeria de cual se leyo primero."
            ).also { it.initCause(e) }
        }
    }

    suspend fun actualizar(id: Long, peticion: FuentePeticion): Fuente {
        val datos = validarCuerpo(peticion)
        return fuenteDeDatos.transaccion { conexion ->
            exigirQueExistan(conexion, datos.supermercado, datos.ingredienteId)
            dao.actualizar(conexion, id, datos.aFuente(id))
                ?: throw ErrorDelServicio.NoEncontrado("una fuente", id.toString())
        }
    }

    suspend fun borrar(id: Long) {
        val borrado = fuenteDeDatos.transaccion { dao.borrar(it, id) }
        if (!borrado) throw ErrorDelServicio.NoEncontrado("una fuente", id.toString())
    }

    private fun exigirQueExistan(
        conexion: java.sql.Connection,
        supermercado: String,
        ingredienteId: String,
    ) {
        if (supermercados.porCodigo(conexion, supermercado) == null) {
            throw ErrorDelServicio.NoEncontrado("un supermercado", supermercado)
        }
        if (!ingredientes.existe(conexion, ingredienteId)) {
            throw ErrorDelServicio.NoEncontrado("un ingrediente", ingredienteId)
        }
    }

    private class Validado(
        val supermercado: String,
        val ingredienteId: String,
        val categoriaRuta: String,
        val terminos: List<String>,
        val excluir: List<String>,
        val medida: Unidad,
        val factor: Double,
        val activa: Boolean,
    ) {
        fun aFuente(id: Long) = Fuente(
            id = id,
            supermercado = supermercado,
            ingredienteId = ingredienteId,
            categoriaRuta = categoriaRuta,
            terminos = terminos,
            excluir = excluir,
            medidaEsperada = medida,
            unidadesBasePorMedida = factor,
            activa = activa,
        )
    }

    private fun validarCuerpo(peticion: FuentePeticion): Validado {
        val supermercado = peticion.supermercado?.trim()?.uppercase().orEmpty()
        val ingredienteId = peticion.ingredienteId?.trim().orEmpty()
        val ruta = peticion.categoriaRuta?.trim().orEmpty()
        val medida = Unidad.desdeSimbolo(peticion.medidaEsperada)
        val factor = peticion.unidadesBasePorMedida

        validar {
            exigir(supermercado.isNotEmpty(), "supermercado", "es obligatorio")
            exigir(ingredienteId.isNotEmpty(), "ingredienteId", "es obligatorio")
            exigir(ruta.isNotEmpty(), "categoriaRuta", "es obligatoria")
            exigir(ruta.isEmpty() || ruta.startsWith("/"), "categoriaRuta", "debe empezar con /")
            exigir(
                medida != null,
                "medidaEsperada",
                "debe ser uno de: ${Unidad.entries.joinToString { it.simbolo }}",
            )
            exigir(factor != null, "unidadesBasePorMedida", "es obligatorio")
            exigir(
                factor == null || factor > 0,
                "unidadesBasePorMedida",
                "debe ser mayor que 0",
            )
        }
        return Validado(
            supermercado = supermercado,
            ingredienteId = ingredienteId,
            categoriaRuta = ruta,
            terminos = peticion.terminos.map { it.trim() }.filter { it.isNotEmpty() },
            excluir = peticion.excluir.map { it.trim() }.filter { it.isNotEmpty() },
            medida = medida!!,
            factor = factor!!,
            activa = peticion.activa,
        )
    }
}

// ---------------------------------------------------------------------------

class PrecioServicio(
    private val fuenteDeDatos: FuenteDeDatos,
    private val dao: PrecioDao,
    private val supermercados: SupermercadoDao,
    private val ingredientes: IngredienteDao,
) {

    /** Lo que la app pide: el precio vigente de cada ingrediente en una cadena. */
    suspend fun vigentes(
        supermercado: String?,
        origen: String?,
        ingredienteId: String?,
    ): List<Precio> {
        val codigo = supermercado?.trim()?.uppercase()
        validar {
            exigir(
                !codigo.isNullOrEmpty(),
                "supermercado",
                "es obligatorio al pedir los precios vigentes; " +
                    "para ver la historia completa usar historia=true",
            )
        }
        val filtroOrigen = origen?.let {
            Origen.desdeNombre(it)
                ?: throw ErrorDelServicio.Validacion("El origen '$it' no existe: MEDIDO o ESTIMADO")
        }
        return fuenteDeDatos.transaccion { conexion ->
            if (supermercados.porCodigo(conexion, codigo!!) == null) {
                throw ErrorDelServicio.NoEncontrado("un supermercado", codigo)
            }
            dao.vigentes(conexion, codigo, filtroOrigen, ingredienteId)
        }
    }

    suspend fun historia(
        supermercado: String?,
        ingredienteId: String?,
        origen: String?,
        pagina: Int?,
        tamano: Int?,
    ): Pagina<Precio> {
        val (numero, largo) = paginacion(pagina, tamano, TAMANO_MAXIMO)
        val filtroOrigen = origen?.let {
            Origen.desdeNombre(it)
                ?: throw ErrorDelServicio.Validacion("El origen '$it' no existe: MEDIDO o ESTIMADO")
        }
        return fuenteDeDatos.transaccion {
            dao.historia(it, supermercado?.uppercase(), ingredienteId, filtroOrigen, numero, largo)
        }
    }

    suspend fun porId(id: Long): Precio = fuenteDeDatos.transaccion {
        dao.porId(it, id) ?: throw ErrorDelServicio.NoEncontrado("un precio", id.toString())
    }

    suspend fun crear(peticion: PrecioPeticion): Precio {
        val datos = validarCuerpo(peticion)
        return try {
            fuenteDeDatos.transaccion { conexion ->
                if (supermercados.porCodigo(conexion, datos.supermercado) == null) {
                    throw ErrorDelServicio.NoEncontrado("un supermercado", datos.supermercado)
                }
                if (!ingredientes.existe(conexion, datos.ingredienteId)) {
                    throw ErrorDelServicio.NoEncontrado("un ingrediente", datos.ingredienteId)
                }
                dao.crear(
                    conexion, datos.supermercado, datos.ingredienteId, datos.precio,
                    datos.unidad, datos.origen, datos.candidatos, null, datos.medidoEn,
                )
            }
        } catch (e: ViolacionDeUnicidad) {
            throw ErrorDelServicio.Conflicto(
                "Ya hay un precio de ${datos.ingredienteId} en ${datos.supermercado} " +
                    "medido en ${datos.medidoEn}: seria la misma medicion cargada dos veces"
            ).also { it.initCause(e) }
        }
    }

    suspend fun actualizar(id: Long, peticion: PrecioPeticion): Precio {
        val datos = validarCuerpo(peticion)
        return fuenteDeDatos.transaccion {
            dao.actualizar(it, id, datos.precio, datos.unidad, datos.origen, datos.candidatos)
                ?: throw ErrorDelServicio.NoEncontrado("un precio", id.toString())
        }
    }

    suspend fun borrar(id: Long) {
        val borrado = fuenteDeDatos.transaccion { dao.borrar(it, id) }
        if (!borrado) throw ErrorDelServicio.NoEncontrado("un precio", id.toString())
    }

    private class Validado(
        val supermercado: String,
        val ingredienteId: String,
        val precio: Double,
        val unidad: Unidad,
        val origen: Origen,
        val candidatos: Int,
        val medidoEn: OffsetDateTime,
    )

    private fun validarCuerpo(peticion: PrecioPeticion): Validado {
        val supermercado = peticion.supermercado?.trim()?.uppercase().orEmpty()
        val ingredienteId = peticion.ingredienteId?.trim().orEmpty()
        val precio = peticion.precioClpPorUnidad
        val unidad = Unidad.desdeSimbolo(peticion.unidad)
        val origen = Origen.desdeNombre(peticion.origen)
        val medidoEn = peticion.medidoEn?.let { texto ->
            try {
                OffsetDateTime.parse(texto)
            } catch (e: DateTimeParseException) {
                throw ErrorDelServicio.Validacion(
                    "medidoEn no es una fecha ISO 8601 con desplazamiento, " +
                        "como 2026-10-29T14:03:11-03:00"
                ).also { it.initCause(e) }
            }
        } ?: OffsetDateTime.now()

        validar {
            exigir(supermercado.isNotEmpty(), "supermercado", "es obligatorio")
            exigir(ingredienteId.isNotEmpty(), "ingredienteId", "es obligatorio")
            exigir(precio != null, "precioClpPorUnidad", "es obligatorio")
            exigir(precio == null || precio > 0, "precioClpPorUnidad", "debe ser mayor que 0")
            exigir(
                unidad != null,
                "unidad",
                "debe ser uno de: ${Unidad.entries.joinToString { it.simbolo }}",
            )
            exigir(origen != null, "origen", "debe ser MEDIDO o ESTIMADO")
            exigir(peticion.candidatos >= 0, "candidatos", "no puede ser negativo")
        }
        return Validado(
            supermercado = supermercado,
            ingredienteId = ingredienteId,
            precio = precio!!,
            unidad = unidad!!,
            origen = origen!!,
            candidatos = peticion.candidatos,
            medidoEn = medidoEn,
        )
    }
}
