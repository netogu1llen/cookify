package com.app.cookify.core.domain

import com.app.cookify.core.model.ResultadoPlan
import com.app.cookify.core.model.SolicitudPlan
import javax.inject.Inject
import kotlin.random.Random

/**
 * Arma la semana de almuerzos a partir de lo que el usuario pidio.
 *
 * Solo orquesta: carga catalogo y precios y se los pasa a [PlanificadorSemanal],
 * que es Kotlin puro y donde vive toda la logica testeable.
 */
class GenerarPlanSemanalUseCase @Inject constructor(
    private val catalogo: CatalogoRepository,
    private val precios: PrecioRepository,
) {
    /**
     * @param semilla fijala para reproducir un plan; omitila para que "regenerar"
     * entregue una semana distinta.
     */
    suspend operator fun invoke(
        solicitud: SolicitudPlan,
        semilla: Long = Random.nextLong(),
    ): ResultadoPlan = PlanificadorSemanal.planificar(
        solicitud = solicitud,
        recetas = catalogo.recetas(),
        ingredientes = precios.ingredientes(),
        semilla = semilla,
    )
}
