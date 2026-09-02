package com.app.cookify.feature.onboarding

import androidx.compose.runtime.Immutable
import com.app.cookify.core.model.Antojo
import com.app.cookify.core.model.Artefacto
import com.app.cookify.core.model.DiaSemana
import com.app.cookify.core.model.Restriccion
import com.app.cookify.core.model.Supermercado

/**
 * Las acciones del wizard agrupadas.
 *
 * Existe para no pasar el ViewModel hacia abajo por varios composables: los pasos
 * solo reciben datos y lambdas, lo que los hace previsualizables y testeables sin
 * Hilt ni ViewModel.
 */
@Immutable
data class AccionesOnboarding(
    val elegirSupermercado: (Supermercado) -> Unit,
    val cambiarPersonas: (Int) -> Unit,
    val alternarDia: (DiaSemana) -> Unit,
    val cambiarPresupuesto: (Int?) -> Unit,
    val alternarAntojo: (Antojo) -> Unit,
    val elegirRestriccion: (Restriccion) -> Unit,
    val alternarArtefacto: (Artefacto) -> Unit,
    val avanzar: () -> Unit,
    val retroceder: () -> Unit,
)
