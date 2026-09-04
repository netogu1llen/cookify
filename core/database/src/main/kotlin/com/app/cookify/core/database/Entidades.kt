package com.app.cookify.core.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Una semana que el usuario decidió guardar.
 *
 * Se guardan las respuestas del cuestionario además del resultado: sirven para volver
 * a mostrar el contexto ("4 personas en Jumbo") y para poder regenerar una semana
 * parecida más adelante sin volver a preguntar todo.
 */
@Entity(tableName = "semanas_guardadas")
data class SemanaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    @ColumnInfo(name = "creada_en") val creadaEn: Long,
    val supermercado: String,
    val personas: Int,
    @ColumnInfo(name = "presupuesto_clp") val presupuestoClp: Int,
    @ColumnInfo(name = "costo_total_clp") val costoTotalClp: Int,
    val antojos: String,
    val restriccion: String,
    val artefactos: String,
    val semilla: Long,
)

/**
 * Un día de una semana guardada.
 *
 * Se guarda el id de la receta y el costo, no la receta entera: la receta vive en el
 * catálogo. Pero sí se guardan los días uno por uno en vez de regenerar el plan desde
 * la semilla al leerlo, porque una actualización de la app que agregue recetas cambiaría
 * el resultado del motor y la semana guardada del usuario mutaría sola.
 */
@Entity(
    tableName = "dias_semana_guardada",
    foreignKeys = [
        ForeignKey(
            entity = SemanaEntity::class,
            parentColumns = ["id"],
            childColumns = ["semana_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("semana_id")],
)
data class DiaGuardadoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "semana_id") val semanaId: Long,
    val dia: String,
    @ColumnInfo(name = "receta_id") val recetaId: String,
    @ColumnInfo(name = "costo_clp") val costoClp: Int,
)
