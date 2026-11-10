package com.app.cookify.core.data

import com.app.cookify.core.data.red.PrecioRepositoryRemoto
import com.app.cookify.core.data.red.Respaldo
import com.app.cookify.core.data.red.ServicioRepositoryRed
import com.app.cookify.core.domain.CatalogoRepository
import com.app.cookify.core.domain.PrecioRepository
import com.app.cookify.core.domain.SemanaRepository
import com.app.cookify.core.domain.ServicioRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DatosModule {

    @Binds
    @Singleton
    abstract fun catalogoRepository(impl: CatalogoRepositoryLocal): CatalogoRepository

    /**
     * El unico punto que hubo que cambiar cuando los precios dejaron de ser
     * locales. PrecioRepositoryLocal sigue existiendo y sigue siendo el ultimo
     * escalon de la cascada, pero ya no es quien responde.
     */
    @Binds
    @Singleton
    abstract fun precioRepository(impl: PrecioRepositoryRemoto): PrecioRepository

    /**
     * El repositorio local sigue existiendo, pero ya no es quien responde: es el
     * ultimo escalon de la cascada, al que el remoto cae cuando no hay red ni
     * cache. Con el calificador queda explicito que son dos cosas distintas.
     */
    @Binds
    @Singleton
    @Respaldo
    abstract fun precioRepositoryDeRespaldo(impl: PrecioRepositoryLocal): PrecioRepository

    @Binds
    @Singleton
    abstract fun semanaRepository(impl: SemanaRepositoryLocal): SemanaRepository

    @Binds
    @Singleton
    abstract fun servicioRepository(impl: ServicioRepositoryRed): ServicioRepository
}
