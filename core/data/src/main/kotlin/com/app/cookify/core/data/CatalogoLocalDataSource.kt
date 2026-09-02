package com.app.cookify.core.data

import android.content.Context
import com.app.cookify.core.common.Dispatcher
import com.app.cookify.core.common.CookifyDispatcher
import com.app.cookify.core.model.Ingrediente
import com.app.cookify.core.model.Receta
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Lee el catalogo empaquetado en assets y lo deja en memoria.
 *
 * Se parsea una sola vez por proceso: son ~200 KB de JSON y la app los consulta
 * en cada planificacion. El Mutex evita que dos pantallas que arrancan a la vez
 * lo parseen dos veces.
 */
@Singleton
internal class CatalogoLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(CookifyDispatcher.IO) private val io: CoroutineDispatcher,
) {
    private val json = Json {
        ignoreUnknownKeys = true // la cabecera trae campos informativos como "nota"
    }

    private val mutex = Mutex()
    private var cache: Catalogo? = null

    suspend fun catalogo(): Catalogo = cache ?: mutex.withLock {
        cache ?: cargar().also { cache = it }
    }

    private suspend fun cargar(): Catalogo = withContext(io) {
        val ingredientes = json
            .decodeFromString<IngredientesDto>(leerAsset(ARCHIVO_INGREDIENTES))
        val recetas = json
            .decodeFromString<RecetasDto>(leerAsset(ARCHIVO_RECETAS))

        Catalogo(
            ingredientes = ingredientes.ingredientes.associateBy { it.id },
            recetas = recetas.recetas,
            preciosActualizadosEn = ingredientes.actualizadoEn,
        )
    }

    private fun leerAsset(nombre: String): String =
        context.assets.open(nombre).bufferedReader().use { it.readText() }

    internal data class Catalogo(
        val ingredientes: Map<String, Ingrediente>,
        val recetas: List<Receta>,
        val preciosActualizadosEn: String,
    )

    private companion object {
        const val ARCHIVO_INGREDIENTES = "ingredientes.json"
        const val ARCHIVO_RECETAS = "recetas.json"
    }
}
