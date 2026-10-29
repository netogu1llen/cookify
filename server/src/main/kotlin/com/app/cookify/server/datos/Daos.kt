package com.app.cookify.server.datos

import com.app.cookify.server.modelo.EstadoRecoleccion
import com.app.cookify.server.modelo.Fuente
import com.app.cookify.server.modelo.Ingrediente
import com.app.cookify.server.modelo.Origen
import com.app.cookify.server.modelo.Pagina
import com.app.cookify.server.modelo.Pasillo
import com.app.cookify.server.modelo.Precio
import com.app.cookify.server.modelo.Recoleccion
import com.app.cookify.server.modelo.Supermercado
import com.app.cookify.server.modelo.Unidad
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.time.OffsetDateTime

/**
 * Las consultas del servicio. Es la unica capa que escribe SQL.
 *
 * La regla, que se puede verificar abriendo cualquier otro archivo: en el ruteo
 * no hay consultas y en el negocio tampoco. Si una consulta nueva hace falta,
 * aparece aqui.
 *
 * Los DAO reciben la [Connection] en lugar de pedirsela al pozo. Asi varias
 * operaciones caben en una sola transaccion: el recolector escribe noventa y
 * seis precios y cierra su recoleccion en un solo todo-o-nada.
 */

/** Choque con un indice unico. La traduce la capa de negocio a un 409. */
class ViolacionDeUnicidad(val restriccion: String?, causa: SQLException) :
    RuntimeException(causa.message, causa)

/** Choque con una llave foranea: se apunto a algo que no existe. */
class ViolacionDeReferencia(val restriccion: String?, causa: SQLException) :
    RuntimeException(causa.message, causa)

private const val ESTADO_UNICIDAD = "23505"
private const val ESTADO_REFERENCIA = "23503"

/**
 * Traduce los codigos de PostgreSQL a excepciones que el negocio entiende.
 *
 * Se verifica el choque en la base y no solo antes de insertar porque entre
 * "no existe" y "lo inserte" cabe otra peticion. Preguntar primero da un
 * mensaje mejor; esto garantiza que el dato no se duplique.
 */
private fun <T> traduciendoChoques(bloque: () -> T): T =
    try {
        bloque()
    } catch (e: SQLException) {
        when (e.sqlState) {
            ESTADO_UNICIDAD -> throw ViolacionDeUnicidad(e.message?.extraerRestriccion(), e)
            ESTADO_REFERENCIA -> throw ViolacionDeReferencia(e.message?.extraerRestriccion(), e)
            else -> throw e
        }
    }

private fun String.extraerRestriccion(): String? =
    Regex("""«(.+?)»|"(.+?)"""").find(this)?.let { it.groupValues[1].ifEmpty { it.groupValues[2] } }

// ---------------------------------------------------------------------------
// supermercados
// ---------------------------------------------------------------------------

class SupermercadoDao {

    private fun leer(f: ResultSet) = Supermercado(
        codigo = f.getString("codigo"),
        etiqueta = f.getString("etiqueta"),
        multiplicadorEstimado = f.getDouble("multiplicador_estimado"),
        soportaMedicion = f.getBoolean("soporta_medicion"),
        sitio = f.getString("sitio"),
    )

    fun todos(c: Connection): List<Supermercado> =
        c.prepareStatement(
            "select * from supermercado order by soporta_medicion desc, etiqueta"
        ).use { it.consultar(::leer) }

    fun porCodigo(c: Connection, codigo: String): Supermercado? =
        c.prepareStatement("select * from supermercado where codigo = ?")
            .use { it.parametros(codigo).consultarUno(::leer) }
}

// ---------------------------------------------------------------------------
// ingredientes
// ---------------------------------------------------------------------------

class IngredienteDao {

    private fun leer(f: ResultSet) = Ingrediente(
        id = f.getString("id"),
        nombre = f.getString("nombre"),
        pasillo = Pasillo.valueOf(f.getString("pasillo")),
        unidad = checkNotNull(Unidad.desdeSimbolo(f.getString("unidad"))),
        precioBaseClp = f.getDouble("precio_base_clp"),
        creadoEn = f.fecha("creado_en"),
        actualizadoEn = f.fecha("actualizado_en"),
    )

    /**
     * Pagina filtrando por pasillo y por nombre.
     *
     * El filtro de nombre usa unaccent a mano con translate: "brocoli" tiene
     * que encontrar "Brócoli", porque nadie escribe las tildes al buscar.
     */
    fun listar(
        c: Connection,
        pasillo: Pasillo?,
        busqueda: String?,
        pagina: Int,
        tamano: Int,
    ): Pagina<Ingrediente> {
        val condiciones = buildList {
            if (pasillo != null) add("pasillo = ?")
            if (!busqueda.isNullOrBlank()) {
                add(
                    "translate(lower(nombre), 'áéíóúñü', 'aeiounu') like " +
                        "translate(lower(?), 'áéíóúñü', 'aeiounu')"
                )
            }
        }
        val donde = if (condiciones.isEmpty()) "" else "where " + condiciones.joinToString(" and ")
        val valores = buildList<Any?> {
            if (pasillo != null) add(pasillo.name)
            if (!busqueda.isNullOrBlank()) add("%$busqueda%")
        }

        val total = c.prepareStatement("select count(*) from ingrediente $donde").use { sentencia ->
            sentencia.parametros(*valores.toTypedArray()).consultarUno { it.getInt(1) } ?: 0
        }
        val datos = c.prepareStatement(
            "select * from ingrediente $donde order by pasillo, nombre limit ? offset ?"
        ).use { sentencia ->
            sentencia
                .parametros(*(valores + listOf(tamano, (pagina - 1) * tamano)).toTypedArray())
                .consultar(::leer)
        }
        return Pagina(datos, pagina, tamano, total)
    }

    fun porId(c: Connection, id: String): Ingrediente? =
        c.prepareStatement("select * from ingrediente where id = ?")
            .use { it.parametros(id).consultarUno(::leer) }

    fun existe(c: Connection, id: String): Boolean =
        c.prepareStatement("select 1 from ingrediente where id = ?")
            .use { it.parametros(id).consultarUno { true } ?: false }

    /**
     * La unidad de cada ingrediente, indexada por id.
     *
     * La necesita el recolector: el precio que calcula queda expresado en la
     * unidad del **catalogo**, que no siempre es la que usa la cadena. La
     * albahaca se publica por kilo y el catalogo la cuenta por atado.
     */
    fun unidades(c: Connection): Map<String, Unidad> =
        c.prepareStatement("select id, unidad from ingrediente").use { sentencia ->
            sentencia.consultar { f ->
                f.getString("id") to checkNotNull(Unidad.desdeSimbolo(f.getString("unidad")))
            }.toMap()
        }

    fun crear(
        c: Connection,
        id: String,
        nombre: String,
        pasillo: Pasillo,
        unidad: Unidad,
        precioBaseClp: Double,
    ): Ingrediente = traduciendoChoques {
        c.prepareStatement(
            """
            insert into ingrediente (id, nombre, pasillo, unidad, precio_base_clp)
            values (?, ?, ?, ?, ?)
            returning *
            """.trimIndent()
        ).use {
            it.parametros(id, nombre, pasillo.name, unidad.simbolo, precioBaseClp)
                .consultarUno(::leer)!!
        }
    }

    fun actualizar(
        c: Connection,
        id: String,
        nombre: String,
        pasillo: Pasillo,
        unidad: Unidad,
        precioBaseClp: Double,
    ): Ingrediente? = traduciendoChoques {
        c.prepareStatement(
            """
            update ingrediente
               set nombre = ?, pasillo = ?, unidad = ?, precio_base_clp = ?, actualizado_en = now()
             where id = ?
            returning *
            """.trimIndent()
        ).use {
            it.parametros(nombre, pasillo.name, unidad.simbolo, precioBaseClp, id)
                .consultarUno(::leer)
        }
    }

    fun borrar(c: Connection, id: String): Boolean =
        c.prepareStatement("delete from ingrediente where id = ?")
            .use { it.parametros(id).executeUpdate() > 0 }
}

// ---------------------------------------------------------------------------
// fuentes
// ---------------------------------------------------------------------------

class FuenteDao {

    private fun leer(f: ResultSet) = Fuente(
        id = f.getLong("id"),
        supermercado = f.getString("supermercado_codigo"),
        ingredienteId = f.getString("ingrediente_id"),
        categoriaRuta = f.getString("categoria_ruta"),
        terminos = f.textos("terminos"),
        excluir = f.textos("excluir"),
        medidaEsperada = checkNotNull(Unidad.desdeSimbolo(f.getString("medida_esperada"))),
        unidadesBasePorMedida = f.getDouble("unidades_base_por_medida"),
        activa = f.getBoolean("activa"),
    )

    fun listar(
        c: Connection,
        supermercado: String?,
        ingredienteId: String?,
        activa: Boolean?,
        pagina: Int,
        tamano: Int,
    ): Pagina<Fuente> {
        val condiciones = buildList {
            if (supermercado != null) add("supermercado_codigo = ?")
            if (ingredienteId != null) add("ingrediente_id = ?")
            if (activa != null) add("activa = ?")
        }
        val donde = if (condiciones.isEmpty()) "" else "where " + condiciones.joinToString(" and ")
        val valores = listOfNotNull<Any>(supermercado, ingredienteId, activa)

        val total = c.prepareStatement("select count(*) from fuente_precio $donde").use {
            it.parametros(*valores.toTypedArray()).consultarUno { f -> f.getInt(1) } ?: 0
        }
        val datos = c.prepareStatement(
            "select * from fuente_precio $donde order by supermercado_codigo, ingrediente_id " +
                "limit ? offset ?"
        ).use {
            it.parametros(*(valores + listOf(tamano, (pagina - 1) * tamano)).toTypedArray())
                .consultar(::leer)
        }
        return Pagina(datos, pagina, tamano, total)
    }

    /** Las fuentes que el recolector tiene que recorrer en una corrida. */
    fun activasDe(c: Connection, supermercado: String, ingredientes: List<String>): List<Fuente> {
        val filtro = if (ingredientes.isEmpty()) "" else
            "and ingrediente_id in (${ingredientes.joinToString(",") { "?" }})"
        return c.prepareStatement(
            "select * from fuente_precio where supermercado_codigo = ? and activa $filtro " +
                "order by categoria_ruta, ingrediente_id"
        ).use {
            it.parametros(*(listOf(supermercado) + ingredientes).toTypedArray()).consultar(::leer)
        }
    }

    fun porId(c: Connection, id: Long): Fuente? =
        c.prepareStatement("select * from fuente_precio where id = ?")
            .use { it.parametros(id).consultarUno(::leer) }

    fun crear(c: Connection, fuente: Fuente): Fuente = traduciendoChoques {
        c.prepareStatement(
            """
            insert into fuente_precio (supermercado_codigo, ingrediente_id, categoria_ruta,
                                       terminos, excluir, medida_esperada,
                                       unidades_base_por_medida, activa)
            values (?, ?, ?, ?, ?, ?, ?, ?)
            returning *
            """.trimIndent()
        ).use { sentencia ->
            sentencia.setString(1, fuente.supermercado)
            sentencia.setString(2, fuente.ingredienteId)
            sentencia.setString(3, fuente.categoriaRuta)
            sentencia.setArray(4, c.createArrayOf("text", fuente.terminos.toTypedArray()))
            sentencia.setArray(5, c.createArrayOf("text", fuente.excluir.toTypedArray()))
            sentencia.setString(6, fuente.medidaEsperada.simbolo)
            sentencia.setDouble(7, fuente.unidadesBasePorMedida)
            sentencia.setBoolean(8, fuente.activa)
            sentencia.consultarUno(::leer)!!
        }
    }

    fun actualizar(c: Connection, id: Long, fuente: Fuente): Fuente? = traduciendoChoques {
        c.prepareStatement(
            """
            update fuente_precio
               set supermercado_codigo = ?, ingrediente_id = ?, categoria_ruta = ?,
                   terminos = ?, excluir = ?, medida_esperada = ?,
                   unidades_base_por_medida = ?, activa = ?, actualizado_en = now()
             where id = ?
            returning *
            """.trimIndent()
        ).use { sentencia ->
            sentencia.setString(1, fuente.supermercado)
            sentencia.setString(2, fuente.ingredienteId)
            sentencia.setString(3, fuente.categoriaRuta)
            sentencia.setArray(4, c.createArrayOf("text", fuente.terminos.toTypedArray()))
            sentencia.setArray(5, c.createArrayOf("text", fuente.excluir.toTypedArray()))
            sentencia.setString(6, fuente.medidaEsperada.simbolo)
            sentencia.setDouble(7, fuente.unidadesBasePorMedida)
            sentencia.setBoolean(8, fuente.activa)
            sentencia.setLong(9, id)
            sentencia.consultarUno(::leer)
        }
    }

    fun borrar(c: Connection, id: Long): Boolean =
        c.prepareStatement("delete from fuente_precio where id = ?")
            .use { it.parametros(id).executeUpdate() > 0 }
}

// ---------------------------------------------------------------------------
// precios
// ---------------------------------------------------------------------------

class PrecioDao {

    private fun leer(f: ResultSet) = Precio(
        id = f.getLong("id"),
        supermercado = f.getString("supermercado_codigo"),
        ingredienteId = f.getString("ingrediente_id"),
        precioClpPorUnidad = f.getDouble("precio_clp_por_unidad"),
        unidad = checkNotNull(Unidad.desdeSimbolo(f.getString("unidad"))),
        origen = Origen.valueOf(f.getString("origen")),
        candidatos = f.getInt("candidatos"),
        recoleccionId = f.enteroOpcional("recoleccion_id"),
        medidoEn = f.fecha("medido_en"),
    )

    /**
     * El precio vigente de cada ingrediente en una cadena.
     *
     * Sale de la vista precio_vigente, que resuelve "la ultima medicion de cada
     * par" con DISTINCT ON en la base. Hacerlo aqui obligaria a traer la
     * historia completa por la red para descartar casi todo.
     */
    fun vigentes(
        c: Connection,
        supermercado: String,
        origen: Origen?,
        ingredienteId: String?,
    ): List<Precio> {
        val condiciones = buildList {
            add("supermercado_codigo = ?")
            if (origen != null) add("origen = ?")
            if (ingredienteId != null) add("ingrediente_id = ?")
        }
        val valores = listOfNotNull<Any>(supermercado, origen?.name, ingredienteId)
        return c.prepareStatement(
            "select * from precio_vigente where ${condiciones.joinToString(" and ")} " +
                "order by ingrediente_id"
        ).use { it.parametros(*valores.toTypedArray()).consultar(::leer) }
    }

    fun historia(
        c: Connection,
        supermercado: String?,
        ingredienteId: String?,
        origen: Origen?,
        pagina: Int,
        tamano: Int,
    ): Pagina<Precio> {
        val condiciones = buildList {
            if (supermercado != null) add("supermercado_codigo = ?")
            if (ingredienteId != null) add("ingrediente_id = ?")
            if (origen != null) add("origen = ?")
        }
        val donde = if (condiciones.isEmpty()) "" else "where " + condiciones.joinToString(" and ")
        val valores = listOfNotNull<Any>(supermercado, ingredienteId, origen?.name)

        val total = c.prepareStatement("select count(*) from precio $donde").use {
            it.parametros(*valores.toTypedArray()).consultarUno { f -> f.getInt(1) } ?: 0
        }
        val datos = c.prepareStatement(
            "select * from precio $donde order by medido_en desc, ingrediente_id limit ? offset ?"
        ).use {
            it.parametros(*(valores + listOf(tamano, (pagina - 1) * tamano)).toTypedArray())
                .consultar(::leer)
        }
        return Pagina(datos, pagina, tamano, total)
    }

    fun porId(c: Connection, id: Long): Precio? =
        c.prepareStatement("select * from precio where id = ?")
            .use { it.parametros(id).consultarUno(::leer) }

    fun crear(
        c: Connection,
        supermercado: String,
        ingredienteId: String,
        precio: Double,
        unidad: Unidad,
        origen: Origen,
        candidatos: Int,
        recoleccionId: Long?,
        medidoEn: OffsetDateTime,
    ): Precio = traduciendoChoques {
        c.prepareStatement(
            """
            insert into precio (supermercado_codigo, ingrediente_id, precio_clp_por_unidad,
                                unidad, origen, candidatos, recoleccion_id, medido_en)
            values (?, ?, ?, ?, ?, ?, ?, ?)
            returning *
            """.trimIndent()
        ).use {
            it.parametros(
                supermercado, ingredienteId, precio, unidad.simbolo, origen.name,
                candidatos, recoleccionId, medidoEn,
            ).consultarUno(::leer)!!
        }
    }

    fun actualizar(
        c: Connection,
        id: Long,
        precio: Double,
        unidad: Unidad,
        origen: Origen,
        candidatos: Int,
    ): Precio? = traduciendoChoques {
        c.prepareStatement(
            """
            update precio
               set precio_clp_por_unidad = ?, unidad = ?, origen = ?, candidatos = ?
             where id = ?
            returning *
            """.trimIndent()
        ).use {
            it.parametros(precio, unidad.simbolo, origen.name, candidatos, id).consultarUno(::leer)
        }
    }

    fun borrar(c: Connection, id: Long): Boolean =
        c.prepareStatement("delete from precio where id = ?")
            .use { it.parametros(id).executeUpdate() > 0 }
}

// ---------------------------------------------------------------------------
// recolecciones
// ---------------------------------------------------------------------------

class RecoleccionDao {

    private fun leer(f: ResultSet) = Recoleccion(
        id = f.getLong("id"),
        supermercado = f.getString("supermercado_codigo"),
        estado = EstadoRecoleccion.valueOf(f.getString("estado")),
        iniciadaEn = f.fecha("iniciada_en"),
        terminadaEn = f.fechaOpcional("terminada_en"),
        categoriasPedidas = f.getInt("categorias_pedidas"),
        categoriasLeidas = f.getInt("categorias_leidas"),
        productosLeidos = f.getInt("productos_leidos"),
        preciosEscritos = f.getInt("precios_escritos"),
        detalle = f.getString("detalle"),
    )

    fun listar(c: Connection, supermercado: String?, pagina: Int, tamano: Int): Pagina<Recoleccion> {
        val donde = if (supermercado == null) "" else "where supermercado_codigo = ?"
        val valores = listOfNotNull<Any>(supermercado)
        val total = c.prepareStatement("select count(*) from recoleccion $donde").use {
            it.parametros(*valores.toTypedArray()).consultarUno { f -> f.getInt(1) } ?: 0
        }
        val datos = c.prepareStatement(
            "select * from recoleccion $donde order by iniciada_en desc limit ? offset ?"
        ).use {
            it.parametros(*(valores + listOf(tamano, (pagina - 1) * tamano)).toTypedArray())
                .consultar(::leer)
        }
        return Pagina(datos, pagina, tamano, total)
    }

    fun porId(c: Connection, id: Long): Recoleccion? =
        c.prepareStatement("select * from recoleccion where id = ?")
            .use { it.parametros(id).consultarUno(::leer) }

    fun hayEnCurso(c: Connection, supermercado: String): Boolean =
        c.prepareStatement(
            "select 1 from recoleccion where supermercado_codigo = ? and estado = 'EN_CURSO'"
        ).use { it.parametros(supermercado).consultarUno { true } ?: false }

    fun abrir(c: Connection, supermercado: String, categoriasPedidas: Int): Recoleccion =
        traduciendoChoques {
            c.prepareStatement(
                """
                insert into recoleccion (supermercado_codigo, estado, categorias_pedidas)
                values (?, 'EN_CURSO', ?)
                returning *
                """.trimIndent()
            ).use { it.parametros(supermercado, categoriasPedidas).consultarUno(::leer)!! }
        }

    fun cerrar(
        c: Connection,
        id: Long,
        estado: EstadoRecoleccion,
        categoriasLeidas: Int,
        productosLeidos: Int,
        preciosEscritos: Int,
        detalle: String?,
    ): Recoleccion? =
        c.prepareStatement(
            """
            update recoleccion
               set estado = ?, terminada_en = now(), categorias_leidas = ?,
                   productos_leidos = ?, precios_escritos = ?, detalle = ?
             where id = ?
            returning *
            """.trimIndent()
        ).use {
            it.parametros(estado.name, categoriasLeidas, productosLeidos, preciosEscritos, detalle, id)
                .consultarUno(::leer)
        }

    fun borrar(c: Connection, id: Long): Boolean =
        c.prepareStatement("delete from recoleccion where id = ?")
            .use { it.parametros(id).executeUpdate() > 0 }

    /**
     * Cierra las corridas que quedaron abiertas de un arranque anterior.
     *
     * Una recoleccion vive en la memoria del proceso: si el proceso se cae o se
     * reinicia a mitad de camino, la fila queda EN_CURSO para siempre y bloquea
     * toda recoleccion futura de esa cadena, porque el servicio se niega a
     * correr dos a la vez. Se marcan FALLIDA al arrancar, diciendo por que.
     *
     * @return cuantas se cerraron.
     */
    fun cerrarHuerfanas(c: Connection): Int =
        c.prepareStatement(
            """
            update recoleccion
               set estado = 'FALLIDA', terminada_en = now(),
                   detalle = coalesce(detalle || ' · ', '') ||
                             'quedo abierta: el servicio se reinicio durante la corrida'
             where estado = 'EN_CURSO'
            """.trimIndent()
        ).use { it.executeUpdate() }
}
