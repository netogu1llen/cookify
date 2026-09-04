package com.app.cookify.core.database

import androidx.room3.Dao
import androidx.room3.Embedded
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Relation
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

/** Una semana con sus días, tal como se lee de la base. */
data class SemanaConDias(
    @Embedded val semana: SemanaEntity,
    // Room 3 cambio estos parametros a arrays: son parentColumns/entityColumns,
    // no los singulares de Room 2.
    @Relation(parentColumns = ["id"], entityColumns = ["semana_id"])
    val dias: List<DiaGuardadoEntity>,
)

@Dao
interface SemanaDao {

    /**
     * Flow y no suspend: la home se suscribe una vez y Room le avisa sola cada vez que
     * se guarda o se borra una semana, sin que nadie tenga que refrescar.
     */
    @Transaction
    @Query("SELECT * FROM semanas_guardadas ORDER BY creada_en DESC")
    fun observarTodas(): Flow<List<SemanaConDias>>

    @Transaction
    @Query("SELECT * FROM semanas_guardadas WHERE id = :id")
    suspend fun porId(id: Long): SemanaConDias?

    @Insert
    suspend fun insertarSemana(semana: SemanaEntity): Long

    @Insert
    suspend fun insertarDias(dias: List<DiaGuardadoEntity>)

    /**
     * Los días se insertan con el id que devuelve la semana, así que las dos escrituras
     * tienen que ser una sola: a medias quedaría una semana sin platos.
     */
    @Transaction
    suspend fun guardar(semana: SemanaEntity, dias: (Long) -> List<DiaGuardadoEntity>): Long {
        val id = insertarSemana(semana)
        insertarDias(dias(id))
        return id
    }

    /** Los días se van por la FK en cascada. */
    @Query("DELETE FROM semanas_guardadas WHERE id = :id")
    suspend fun borrar(id: Long)

    @Query("SELECT COUNT(*) FROM semanas_guardadas")
    suspend fun cuantas(): Int
}
