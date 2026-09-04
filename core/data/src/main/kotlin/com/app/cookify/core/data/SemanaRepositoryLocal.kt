package com.app.cookify.core.data

import com.app.cookify.core.database.DiaGuardadoEntity
import com.app.cookify.core.database.SemanaConDias
import com.app.cookify.core.database.SemanaDao
import com.app.cookify.core.database.SemanaEntity
import com.app.cookify.core.domain.CatalogoRepository
import com.app.cookify.core.domain.SemanaRepository
import com.app.cookify.core.model.AlmuerzoDelDia
import com.app.cookify.core.model.Antojo
import com.app.cookify.core.model.Artefacto
import com.app.cookify.core.model.DiaSemana
import com.app.cookify.core.model.PlanSemanal
import com.app.cookify.core.model.Receta
import com.app.cookify.core.model.Restriccion
import com.app.cookify.core.model.SemanaGuardada
import com.app.cookify.core.model.SolicitudPlan
import com.app.cookify.core.model.Supermercado
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Semanas guardadas en Room, resueltas contra el catálogo al leerlas.
 *
 * Los enums se guardan por su `name` y no por su `ordinal`: reordenar un enum es
 * algo que uno hace sin pensarlo, y con ordinales eso convertiría en silencio todas
 * las semanas guardadas de "Jumbo" en semanas de "Tottus".
 */
@Singleton
internal class SemanaRepositoryLocal @Inject constructor(
    private val dao: SemanaDao,
    private val catalogo: CatalogoRepository,
) : SemanaRepository {

    override fun observar(): Flow<List<SemanaGuardada>> = dao.observarTodas().map { filas ->
        val recetas = catalogo.recetas().associateBy { it.id }
        filas.mapNotNull { it.aDominio(recetas) }
    }

    override suspend fun guardar(nombre: String, plan: PlanSemanal): Long {
        val solicitud = plan.solicitud
        val semana = SemanaEntity(
            nombre = nombre,
            creadaEn = System.currentTimeMillis(),
            supermercado = solicitud.supermercado.name,
            personas = solicitud.personas,
            presupuestoClp = solicitud.presupuestoClp,
            costoTotalClp = plan.costoTotalClp,
            antojos = solicitud.antojos.joinToString(SEPARADOR) { it.name },
            restriccion = solicitud.restriccion.name,
            artefactos = solicitud.artefactos.joinToString(SEPARADOR) { it.name },
            semilla = plan.semilla,
        )

        return dao.guardar(semana) { id ->
            plan.almuerzos.map { almuerzo ->
                DiaGuardadoEntity(
                    semanaId = id,
                    dia = almuerzo.dia.name,
                    recetaId = almuerzo.receta.id,
                    costoClp = almuerzo.costoClp,
                )
            }
        }
    }

    override suspend fun porId(id: Long): SemanaGuardada? {
        val fila = dao.porId(id) ?: return null
        return fila.aDominio(catalogo.recetas().associateBy { it.id })
    }

    override suspend fun borrar(id: Long) = dao.borrar(id)

    override suspend fun hayAlguna(): Boolean = dao.cuantas() > 0

    /**
     * @return null si el catálogo ya no tiene ninguna de las recetas guardadas. Es el
     * único caso en que se descarta una semana: si sobrevive aunque sea un plato, se
     * muestra lo que quedó en vez de hacer desaparecer la semana entera.
     */
    private fun SemanaConDias.aDominio(recetas: Map<String, Receta>): SemanaGuardada? {
        val almuerzos = dias
            .sortedBy { DiaSemana.valueOf(it.dia).ordinal }
            .mapNotNull { fila ->
                val receta = recetas[fila.recetaId] ?: return@mapNotNull null
                AlmuerzoDelDia(
                    dia = DiaSemana.valueOf(fila.dia),
                    receta = receta,
                    costoClp = fila.costoClp,
                )
            }
        if (almuerzos.isEmpty()) return null

        return SemanaGuardada(
            id = semana.id,
            nombre = semana.nombre,
            creadaEn = semana.creadaEn,
            plan = PlanSemanal(
                almuerzos = almuerzos,
                solicitud = SolicitudPlan(
                    supermercado = Supermercado.valueOf(semana.supermercado),
                    personas = semana.personas,
                    dias = almuerzos.map { it.dia },
                    presupuestoClp = semana.presupuestoClp,
                    antojos = semana.antojos.aEnums(Antojo::valueOf),
                    restriccion = Restriccion.valueOf(semana.restriccion),
                    artefactos = semana.artefactos.aEnums(Artefacto::valueOf),
                ),
                semilla = semana.semilla,
            ),
        )
    }

    private companion object {
        const val SEPARADOR = ","

        /** split de un string vacío devuelve [""], no una lista vacía. */
        fun <T> String.aEnums(convertir: (String) -> T): Set<T> =
            if (isEmpty()) emptySet() else split(SEPARADOR).map(convertir).toSet()
    }
}
