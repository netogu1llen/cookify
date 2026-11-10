package com.app.cookify.core.data.red

import com.app.cookify.core.database.PrecioCacheDao
import com.app.cookify.core.database.PrecioCacheEntity
import com.app.cookify.core.domain.PrecioRepository
import com.app.cookify.core.model.OrigenPrecio
import com.app.cookify.core.model.PreciosDeCadena
import com.app.cookify.core.model.Procedencia
import com.app.cookify.core.model.Supermercado
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days

/** La fuente que estima, que es el último escalón de la cascada. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class Respaldo

/**
 * Los precios del servicio, con caché y con respaldo.
 *
 * Decora al repositorio local en lugar de reemplazarlo: parte de lo que aquel
 * estima y le superpone lo que el servicio midió. Así un ingrediente que el
 * servicio no conoce no desaparece de las recetas, se queda con su estimación.
 *
 * Nunca falla por falta de red. Resuelve en cascada y siempre llega a algo:
 *
 * ```
 * caché fresca (< 7 días)  →  servicio  →  caché vencida  →  catálogo empaquetado
 * ```
 *
 * El último escalón no es opcional: sin él no se podría armar una semana sin
 * señal, y los 98 valores escritos a mano ya están dentro del APK. Lo que se
 * pierde al caer hasta ahí no es la app, es la precisión, y el resultado viene
 * marcado [OrigenPrecio.ESTIMADO] para que la pantalla lo diga.
 *
 * Por qué la caché dura siete días: un precio de supermercado no cambia de un
 * día para otro lo suficiente como para justificar una descarga diaria, y la app
 * sirve para planificar la semana. Es el mismo número con que el servicio decide
 * cuándo volver a medir.
 */
@Singleton
internal class PrecioRepositoryRemoto @Inject constructor(
    private val remoto: PreciosRemotos,
    private val cache: PrecioCacheDao,
    @Respaldo private val respaldo: PrecioRepository,
) : PrecioRepository {

    override suspend fun precios(supermercado: Supermercado): PreciosDeCadena {
        val estimados = respaldo.precios(supermercado)

        val guardadoEn = cache.guardadoEn(supermercado.name)
        if (guardadoEn != null && estaFresca(guardadoEn)) {
            // Con la caché fresca ni se intenta la red, asi que no se sabe si
            // hay conexion ni hace falta: sinConexion queda en falso.
            desdeCache(supermercado, estimados, sinConexion = false)?.let { return it }
        }

        // Si la red falla no se le cuenta al usuario: quien está armando su
        // semana no puede hacer nada con un error de red, y lo que necesita es
        // que la semana se arme igual.
        val respuesta = remoto.deCadena(supermercado)
        if (respuesta is PreciosRemotos.Resultado.Hay) {
            guardarEnCache(supermercado, respuesta.precios)
            desdeServicio(respuesta.precios, estimados)?.let { return it }
        }

        // Se llega aquí con el servicio caído, o habiendo respondido una lista
        // vacía, que para la app es lo mismo: no hay precios nuevos. La caché
        // vencida todavía sirve —un precio medido hace un mes vale más que uno
        // estimado hoy— y solo si tampoco la hay se estima.
        desdeCache(supermercado, estimados, sinConexion = true)?.let { return it }
        return estimados.copy(sinConexion = true)
    }

    private fun estaFresca(guardadoEn: Long): Boolean =
        System.currentTimeMillis() - guardadoEn < VIGENCIA.inWholeMilliseconds

    private suspend fun desdeCache(
        supermercado: Supermercado,
        estimados: PreciosDeCadena,
        sinConexion: Boolean,
    ): PreciosDeCadena? {
        val filas = cache.deSupermercado(supermercado.name)
        if (filas.isEmpty()) return null
        return superponer(
            base = estimados,
            medidos = filas.associate { it.ingredienteId to it.precioClpPorUnidad },
            origenes = filas.associate { it.ingredienteId to origenDe(it.origen) },
            medidoEn = filas.mapNotNull { it.medidoEn }.maxOrNull(),
            procedencia = Procedencia.CACHE,
            sinConexion = sinConexion,
        )
    }

    private fun desdeServicio(
        dto: PreciosVigentesDto,
        estimados: PreciosDeCadena,
    ): PreciosDeCadena? {
        if (dto.datos.isEmpty()) return null
        return superponer(
            base = estimados,
            medidos = dto.datos.associate { it.ingredienteId to it.precioClpPorUnidad },
            origenes = dto.datos.associate { it.ingredienteId to origenDe(it.origen) },
            medidoEn = dto.actualizadoEn,
            procedencia = Procedencia.SERVICIO,
            sinConexion = false,
        )
    }

    /**
     * Pone los precios medidos encima de los estimados.
     *
     * El catálogo empaquetado manda en qué ingredientes existen y en qué unidad
     * se miden; el servicio solo aporta el precio. Un precio que no venga, o que
     * venga en cero, deja al ingrediente con su estimación en lugar de sacarlo
     * del mapa: el motor fallaría al costear una receta que lo use.
     */
    private fun superponer(
        base: PreciosDeCadena,
        medidos: Map<String, Double>,
        origenes: Map<String, OrigenPrecio>,
        medidoEn: String?,
        procedencia: Procedencia,
        sinConexion: Boolean,
    ): PreciosDeCadena {
        val usables = medidos.filterValues { it > 0 }
        return base.copy(
            ingredientes = base.ingredientes.mapValues { (id, ingrediente) ->
                usables[id]?.let { ingrediente.copy(precioClpPorUnidad = it) } ?: ingrediente
            },
            origen = base.ingredientes.mapValues { (id, _) ->
                if (usables.containsKey(id)) {
                    origenes[id] ?: OrigenPrecio.MEDIDO
                } else {
                    OrigenPrecio.ESTIMADO
                }
            },
            medidoEn = medidoEn?.takeIf { it.isNotBlank() },
            procedencia = procedencia,
            sinConexion = sinConexion,
        )
    }

    private suspend fun guardarEnCache(supermercado: Supermercado, dto: PreciosVigentesDto) {
        // Una respuesta vacía no borra lo que había: un servicio recién
        // desplegado, sin recolecciones todavía, responde una lista vacía, y
        // tomarla al pie de la letra dejaría a la app sin precios.
        if (dto.datos.isEmpty()) return
        val ahora = System.currentTimeMillis()
        cache.reemplazarLote(
            supermercado.name,
            dto.datos.map { precio ->
                PrecioCacheEntity(
                    supermercado = supermercado.name,
                    ingredienteId = precio.ingredienteId,
                    precioClpPorUnidad = precio.precioClpPorUnidad,
                    unidad = precio.unidad,
                    origen = precio.origen,
                    medidoEn = precio.medidoEn,
                    guardadoEn = ahora,
                )
            },
        )
    }

    private fun origenDe(valor: String): OrigenPrecio =
        OrigenPrecio.entries.firstOrNull { it.name == valor } ?: OrigenPrecio.ESTIMADO

    private companion object {
        val VIGENCIA = 7.days
    }
}
