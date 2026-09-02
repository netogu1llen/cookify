package com.app.cookify.core.data

import com.app.cookify.core.domain.CatalogoRepository
import com.app.cookify.core.domain.PrecioRepository
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

    /** Unico punto a cambiar si algun dia los precios dejan de ser locales. */
    @Binds
    @Singleton
    abstract fun precioRepository(impl: PrecioRepositoryLocal): PrecioRepository
}
