package com.app.cookify.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.cookify.core.model.SolicitudPlan
import com.app.cookify.core.ui.componentes.BarraPasos
import com.app.cookify.core.ui.componentes.BotonPrimario
import com.app.cookify.core.ui.componentes.IconoCookify
import com.app.cookify.core.ui.componentes.IconosCookify
import com.app.cookify.core.ui.theme.Espaciado
import com.app.cookify.feature.onboarding.pasos.PasoAntojos
import com.app.cookify.feature.onboarding.pasos.PasoArtefactos
import com.app.cookify.feature.onboarding.pasos.PasoDias
import com.app.cookify.feature.onboarding.pasos.PasoPersonas
import com.app.cookify.feature.onboarding.pasos.PasoPresupuesto
import com.app.cookify.feature.onboarding.pasos.PasoRestriccion
import com.app.cookify.feature.onboarding.pasos.PasoSupermercado

/**
 * Wizard de siete pasos.
 *
 * Los pasos viven dentro de una sola ruta de navegacion, no como destinos separados:
 * asi el estado es uno solo, retroceder no reconstruye nada y la transicion entre
 * preguntas se siente como avanzar en un formulario y no como cambiar de pantalla.
 */
@Composable
fun PantallaOnboarding(
    onTerminado: (SolicitudPlan) -> Unit,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    // rememberUpdatedState y no la lambda directa: el efecto se lanza una sola vez y
    // se quedaria con la version original de onTerminado si el padre la recompone.
    val terminado by rememberUpdatedState(onTerminado)
    val salir by rememberUpdatedState(onSalir)

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                is EventoOnboarding.Terminado -> terminado(evento.solicitud)
            }
        }
    }

    val retroceder = { if (!viewModel.retroceder()) salir() }

    // El back del sistema retrocede de a un paso; solo sale del wizard en el primero.
    BackHandler(onBack = retroceder)

    val acciones = remember(viewModel) {
        AccionesOnboarding(
            elegirSupermercado = viewModel::elegirSupermercado,
            cambiarPersonas = viewModel::cambiarPersonas,
            alternarDia = viewModel::alternarDia,
            cambiarPresupuesto = viewModel::cambiarPresupuesto,
            alternarAntojo = viewModel::alternarAntojo,
            elegirRestriccion = viewModel::elegirRestriccion,
            alternarArtefacto = viewModel::alternarArtefacto,
            avanzar = viewModel::avanzar,
            retroceder = retroceder,
        )
    }

    ContenidoOnboarding(estado = estado, acciones = acciones, modifier = modifier)
}

/** Version sin estado, para previews y tests de UI. */
@Composable
internal fun ContenidoOnboarding(
    estado: OnboardingUiState,
    acciones: AccionesOnboarding,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .padding(horizontal = Espaciado.margenPantalla),
        ) {
            EncabezadoNavegacion(
                pasoActual = estado.indicePaso + 1,
                onAtras = acciones.retroceder,
            )

            AnimatedContent(
                targetState = estado.paso,
                transitionSpec = { transicionPasos(initialState, targetState) },
                label = "pasoOnboarding",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { paso ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        // El padding inferior extra deja que la ultima opcion suba por
                        // sobre el borde del scroll; sin el queda cortada contra el boton.
                        .padding(top = Espaciado.m, bottom = Espaciado.xl),
                ) {
                    ContenidoPaso(paso = paso, estado = estado, acciones = acciones)
                }
            }

            BotonPrimario(
                texto = if (estado.esUltimoPaso) "Armar mi semana" else "Continuar",
                onClick = acciones.avanzar,
                habilitado = estado.puedeAvanzar,
                modifier = Modifier.padding(bottom = Espaciado.m),
            )
        }
    }
}

@Composable
private fun EncabezadoNavegacion(pasoActual: Int, onAtras: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Espaciado.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espaciado.xs),
    ) {
        IconButton(onClick = onAtras) {
            IconoCookify(
                icono = IconosCookify.flechaAtras,
                descripcion = "Volver al paso anterior",
                tinte = MaterialTheme.colorScheme.onSurface,
            )
        }
        BarraPasos(
            pasoActual = pasoActual,
            totalPasos = PasoOnboarding.total,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ContenidoPaso(
    paso: PasoOnboarding,
    estado: OnboardingUiState,
    acciones: AccionesOnboarding,
) {
    when (paso) {
        PasoOnboarding.SUPERMERCADO -> PasoSupermercado(
            seleccionado = estado.supermercado,
            onElegir = acciones.elegirSupermercado,
        )

        PasoOnboarding.PERSONAS -> PasoPersonas(
            personas = estado.personas,
            onCambiar = acciones.cambiarPersonas,
        )

        PasoOnboarding.DIAS -> PasoDias(
            elegidos = estado.dias,
            onAlternar = acciones.alternarDia,
        )

        PasoOnboarding.PRESUPUESTO -> PasoPresupuesto(
            presupuesto = estado.presupuestoClp,
            onCambiar = acciones.cambiarPresupuesto,
        )

        PasoOnboarding.ANTOJOS -> PasoAntojos(
            elegidos = estado.antojos,
            topeAlcanzado = estado.topeAntojosAlcanzado,
            onAlternar = acciones.alternarAntojo,
        )

        PasoOnboarding.RESTRICCION -> PasoRestriccion(
            seleccionada = estado.restriccion,
            onElegir = acciones.elegirRestriccion,
        )

        PasoOnboarding.ARTEFACTOS -> PasoArtefactos(
            elegidos = estado.artefactos,
            onAlternar = acciones.alternarArtefacto,
        )
    }
}

/**
 * Avanzar entra desde la derecha, retroceder desde la izquierda. La direccion es lo
 * que le dice al usuario si esta yendo o volviendo, mas que cualquier texto.
 */
private fun transicionPasos(inicial: PasoOnboarding, objetivo: PasoOnboarding) =
    if (objetivo.ordinal > inicial.ordinal) {
        slideInHorizontally(tween(DURACION_MS)) { it / DIVISOR_DESLIZAMIENTO } +
            fadeIn(tween(DURACION_MS)) togetherWith
            slideOutHorizontally(tween(DURACION_MS)) { -it / DIVISOR_DESLIZAMIENTO } +
            fadeOut(tween(DURACION_MS))
    } else {
        slideInHorizontally(tween(DURACION_MS)) { -it / DIVISOR_DESLIZAMIENTO } +
            fadeIn(tween(DURACION_MS)) togetherWith
            slideOutHorizontally(tween(DURACION_MS)) { it / DIVISOR_DESLIZAMIENTO } +
            fadeOut(tween(DURACION_MS))
    }

private const val DURACION_MS = 250

/** El paso entra desplazado un tercio del ancho: sugiere movimiento sin marear. */
private const val DIVISOR_DESLIZAMIENTO = 3
