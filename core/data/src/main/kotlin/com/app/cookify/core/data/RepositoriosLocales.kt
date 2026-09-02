package com.app.cookify.core.data

import com.app.cookify.core.domain.CatalogoRepository
import com.app.cookify.core.domain.PrecioRepository
import com.app.cookify.core.model.Ingrediente
import com.app.cookify.core.model.Receta
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class CatalogoRepositoryLocal @Inject constructor(
    private val fuente: CatalogoLocalDataSource,
) : CatalogoRepository {

    override suspend fun recetas(): List<Receta> = fuente.catalogo().recetas

    override suspend fun receta(id: String): Receta? =
        fuente.catalogo().recetas.firstOrNull { it.id == id }
}

/**
 * Precios desde el JSON empaquetado.
 *
 * Cuando se quiera pasar a precios remotos o a una fuente oficial, se escribe otra
 * implementacion de [PrecioRepository] y se cambia el @Binds en [DatosModule]. Nada
 * mas de la app se entera.
 */
@Singleton
internal class PrecioRepositoryLocal @Inject constructor(
    private val fuente: CatalogoLocalDataSource,
) : PrecioRepository {

    override suspend fun ingredientes(): Map<String, Ingrediente> = fuente.catalogo().ingredientes

    override suspend fun actualizadoEn(): String = fuente.catalogo().preciosActualizadosEn
}
