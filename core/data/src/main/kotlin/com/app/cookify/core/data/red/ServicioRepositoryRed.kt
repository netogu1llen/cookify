package com.app.cookify.core.data.red

import com.app.cookify.core.database.PrecioCacheDao
import com.app.cookify.core.domain.ServicioRepository
import com.app.cookify.core.model.PreciosGuardados
import com.app.cookify.core.model.PruebaDeConexion
import com.app.cookify.core.model.Supermercado
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable

/** La respuesta de /salud, que vive fuera del contrato de datos del servicio. */
@Serializable
internal data class SaludDto(
    val estado: String = "",
    val version: String = "",
    val baseDeDatos: String = "",
)

/**
 * Diagnostico del servicio de precios.
 *
 * Pega a /salud y no a /api/v1/precios: esa ruta existe justamente para esto,
 * responde en bytes en lugar de en decenas de kilobytes, y dice además si el
 * servicio alcanza su propia base de datos. Un servicio que contesta pero no
 * llega a su base responde 503, y eso es exactamente lo que hay que saber.
 */
@Singleton
internal class ServicioRepositoryRed @Inject constructor(
    private val http: HttpClient,
    @UrlDelServicio private val urlBase: String,
    private val cache: PrecioCacheDao,
) : ServicioRepository {

    override val direccion: String get() = urlBase

    override suspend fun probarConexion(): PruebaDeConexion {
        val partida = System.currentTimeMillis()
        return runCatching {
            val respuesta = http.get("$urlBase/salud")
            val tardanza = System.currentTimeMillis() - partida
            if (!respuesta.status.isSuccess()) {
                // 503 es el caso interesante: el servicio está, pero no alcanza
                // su base. Decir solo "no responde" escondería la diferencia.
                return PruebaDeConexion.NoRespondio(
                    "el servicio respondió ${respuesta.status.value} en ${tardanza} ms"
                )
            }
            val salud = respuesta.body<SaludDto>()
            PruebaDeConexion.Respondio(
                version = salud.version,
                baseDeDatos = salud.baseDeDatos,
                milisegundos = tardanza,
            )
        }.getOrElse { error ->
            PruebaDeConexion.NoRespondio(
                error.message?.take(TOPE_MOTIVO) ?: error::class.simpleName.orEmpty()
            )
        }
    }

    override suspend fun preciosGuardados(): List<PreciosGuardados> =
        cache.resumen().mapNotNull { fila ->
            val cadena = Supermercado.entries.firstOrNull { it.name == fila.supermercado }
                ?: return@mapNotNull null
            PreciosGuardados(
                supermercado = cadena,
                total = fila.total,
                medidos = fila.medidos,
                medidoEn = fila.medidoEn,
                guardadoEn = fila.guardadoEn,
            )
        }

    override suspend fun olvidarPreciosGuardados() = cache.borrarTodo()

    private companion object {
        /** Un mensaje de error de red puede traer la traza entera. */
        const val TOPE_MOTIVO = 120
    }
}
