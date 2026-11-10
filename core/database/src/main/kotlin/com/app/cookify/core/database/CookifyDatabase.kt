package com.app.cookify.core.database

import android.content.Context
import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers

/**
 * La version 2 agrega la tabla de cache de precios.
 *
 * Con migracion automatica y no destructiva: la version 1 ya esta en telefonos
 * con semanas guardadas, y borrarlas para agregar una tabla de cache seria
 * cambiar datos del usuario por comodidad del desarrollador. Room la deduce
 * sola porque la unica diferencia es una tabla nueva y el esquema de la 1 esta
 * exportado en schemas/.
 */
@Database(
    entities = [SemanaEntity::class, DiaGuardadoEntity::class, PrecioCacheEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class CookifyDatabase : RoomDatabase() {
    abstract fun semanaDao(): SemanaDao
    abstract fun precioCacheDao(): PrecioCacheDao
}

@Module
@InstallIn(SingletonComponent::class)
object BaseDeDatosModule {

    @Provides
    @Singleton
    fun base(@ApplicationContext contexto: Context): CookifyDatabase =
        Room.databaseBuilder(
            context = contexto,
            klass = CookifyDatabase::class.java,
            name = NOMBRE_ARCHIVO,
        )
            // Driver empaquetado y no el de la plataforma: la version de SQLite del
            // sistema varia entre fabricantes y versiones de Android, y con este el
            // comportamiento es el mismo en todos los telefonos.
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

    @Provides
    fun semanaDao(base: CookifyDatabase): SemanaDao = base.semanaDao()

    @Provides
    fun precioCacheDao(base: CookifyDatabase): PrecioCacheDao = base.precioCacheDao()

    private const val NOMBRE_ARCHIVO = "cookify.db"
}
