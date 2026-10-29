package com.app.cookify.server.datos

import com.app.cookify.server.config.Configuracion
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * El acceso a PostgreSQL, con un pozo de conexiones y SQL escrito a mano.
 *
 * Sin ORM, y es una decision con costo asumido. A favor: el esquema vive en
 * db/001_esquema.sql y es la unica verdad, la consulta que se ejecuta es la que
 * esta escrita, y cualquiera puede leerla sin aprender un lenguaje intermedio.
 * En contra: mapear cada fila a mano es codigo repetitivo que un ORM daria
 * gratis. Se acepta porque el servicio tiene cuatro tablas, no cuarenta.
 *
 * Todas las operaciones son suspend y saltan a [Dispatchers.IO]: JDBC bloquea
 * el hilo, y bloquear un hilo del pozo de Netty deja de atender peticiones que
 * no tienen nada que ver.
 */
class FuenteDeDatos(configuracion: Configuracion.BaseDeDatos) : AutoCloseable {

    private val pozo = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = configuracion.url
            username = configuracion.usuario
            password = configuracion.clave
            // Diez conexiones para un servicio cuyo trabajo pesado es la red
            // hacia el supermercado, no la base.
            maximumPoolSize = 10
            minimumIdle = 2
            connectionTimeout = 10_000
            poolName = "cookify"
            // El recolector escribe lotes de 98 filas en una transaccion.
            isAutoCommit = false
        }
    )

    /**
     * Ejecuta [bloque] en una transaccion y la confirma, o la revierte completa
     * si algo falla.
     *
     * Que sea todo o nada importa aqui: una recoleccion a medio escribir
     * dejaria la mitad de los precios de una cadena nuevos y la otra mitad
     * viejos, y nadie podria saber cual es cual.
     */
    suspend fun <T> transaccion(bloque: (Connection) -> T): T = withContext(Dispatchers.IO) {
        pozo.connection.use { conexion ->
            try {
                val resultado = bloque(conexion)
                conexion.commit()
                resultado
            } catch (e: Throwable) {
                conexion.rollback()
                throw e
            }
        }
    }

    /** Si la base responde. Lo usa /salud, que existe para el despliegue. */
    suspend fun responde(): Boolean = runCatching {
        transaccion { conexion ->
            conexion.prepareStatement("select 1").use { it.executeQuery().next() }
        }
    }.getOrDefault(false)

    override fun close() = pozo.close()
}

// --- ayudas de mapeo -------------------------------------------------------
//
// Pequenas y sin magia. Existen para que los DAO digan lo que hacen y no se
// pierdan entre getString e indices numericos.

internal fun <T> PreparedStatement.consultar(mapear: (ResultSet) -> T): List<T> =
    executeQuery().use { filas ->
        buildList { while (filas.next()) add(mapear(filas)) }
    }

internal fun <T> PreparedStatement.consultarUno(mapear: (ResultSet) -> T): T? =
    executeQuery().use { filas -> if (filas.next()) mapear(filas) else null }

internal fun PreparedStatement.parametros(vararg valores: Any?): PreparedStatement = apply {
    valores.forEachIndexed { indice, valor ->
        when (valor) {
            null -> setObject(indice + 1, null)
            is String -> setString(indice + 1, valor)
            is Int -> setInt(indice + 1, valor)
            is Long -> setLong(indice + 1, valor)
            is Double -> setDouble(indice + 1, valor)
            is Boolean -> setBoolean(indice + 1, valor)
            is OffsetDateTime -> setObject(indice + 1, valor)
            else -> setObject(indice + 1, valor)
        }
    }
}

internal fun ResultSet.fecha(columna: String): OffsetDateTime =
    getObject(columna, OffsetDateTime::class.java)

internal fun ResultSet.fechaOpcional(columna: String): OffsetDateTime? =
    getObject(columna, OffsetDateTime::class.java)

internal fun ResultSet.textos(columna: String): List<String> {
    val arreglo = getArray(columna) ?: return emptyList()
    @Suppress("UNCHECKED_CAST")
    val crudo = arreglo.array as Array<String?>
    return crudo.filterNotNull()
}

internal fun ResultSet.enteroOpcional(columna: String): Long? {
    val valor = getLong(columna)
    return if (wasNull()) null else valor
}
