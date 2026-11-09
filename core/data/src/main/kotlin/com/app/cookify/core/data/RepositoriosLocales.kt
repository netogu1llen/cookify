package com.app.cookify.core.data

import com.app.cookify.core.domain.CatalogoRepository
import com.app.cookify.core.domain.PrecioRepository
import com.app.cookify.core.model.Ingrediente
import com.app.cookify.core.model.PreciosDeCadena
import com.app.cookify.core.model.Receta
import com.app.cookify.core.model.Supermercado
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
 * Precios estimados desde el JSON empaquetado.
 *
 * Es la fuente que **estima**: toma el precio de referencia del catalogo, que esta
 * en la base de Lider, y le aplica el multiplicador de la cadena. Por eso todo lo
 * que devuelve viene marcado [OrigenPrecio.ESTIMADO], y por eso el multiplicador
 * vive aqui y ya no en CalculadoraCostos: sobre un precio medido no habria nada que
 * multiplicar.
 *
 * Sigue siendo el respaldo del que nunca se prescinde. Sin ella no se puede armar
 * una semana sin señal, y los 98 valores escritos a mano ya estan empaquetados.
 */
@Singleton
internal class PrecioRepositoryLocal @Inject constructor(
    private val fuente: CatalogoLocalDataSource,
) : PrecioRepository {

    override suspend fun precios(supermercado: Supermercado): PreciosDeCadena =
        PreciosDeCadena.estimados(supermercado, fuente.catalogo().ingredientes)
}
