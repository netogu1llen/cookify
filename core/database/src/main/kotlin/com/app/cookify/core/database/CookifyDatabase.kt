package com.app.cookify.core.database

import android.content.Context
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

@Database(
    entities = [SemanaEntity::class, DiaGuardadoEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class CookifyDatabase : RoomDatabase() {
    abstract fun semanaDao(): SemanaDao
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

    private const val NOMBRE_ARCHIVO = "cookify.db"
}
