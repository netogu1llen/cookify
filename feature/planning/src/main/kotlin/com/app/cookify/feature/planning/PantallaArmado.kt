package com.app.cookify.feature.planning

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.cookify.core.model.SolicitudPlan
import com.app.cookify.core.ui.componentes.EstadoCheck
import com.app.cookify.core.ui.componentes.FilaCheck
import com.app.cookify.core.ui.componentes.IconoCookify
import com.app.cookify.core.ui.componentes.IconosCookify
import com.app.cookify.core.ui.theme.CookifyTheme
import com.app.cookify.core.ui.theme.Espaciado

/**
 * Pantalla de armado: las cuatro etapas con sus checks.
 *
 * No tiene botón de salida ni back mientras trabaja: interrumpir a la mitad dejaría
 * al usuario en el cuestionario que acaba de terminar. La única forma de irse es que
 * el armado falle, y entonces la hoja de error ofrece la salida explícita.
 */
@Composable
fun PantallaArmado(
    solicitud: SolicitudPlan,
    onListo: (SolicitudPlan, Long) -> Unit,
    onCambiarRespuestas: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArmadoViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val listo by rememberUpdatedState(onListo)
    val cambiarRespuestas by rememberUpdatedState(onCambiarRespuestas)

    LaunchedEffect(solicitud) { viewModel.armar(solicitud) }

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                is EventoArmado.Listo -> listo(evento.solicitud, evento.semilla)
            }
        }
    }

    val hayFallo = estado.fallo != null

    // Con la hoja de error abierta el back equivale a "cambiar mis respuestas".
    // Mientras arma se traga, para no dejar el armado a medio camino.
    BackHandler(enabled = true) {
        if (hayFallo) cambiarRespuestas()
    }

    ContenidoArmado(
        estado = estado,
        onContinuarConMinimo = viewModel::continuarConMinimo,
        onCambiarRespuestas = cambiarRespuestas,
        modifier = modifier,
    )
}

/** Versión sin estado, para previews y tests de UI. */
@Composable
internal fun ContenidoArmado(
    estado: ArmadoUiState,
    onContinuarConMinimo: () -> Unit,
    onCambiarRespuestas: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { relleno ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(relleno),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Espaciado.margenPantalla),
                verticalArrangement = Arrangement.Center,
            ) {
                OllaPulsante(detenida = estado.fallo != null)

                Text(
                    text = "Armando tu semana",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = Espaciado.xl, bottom = Espaciado.l),
                )

                Column(verticalArrangement = Arrangement.spacedBy(Espaciado.m)) {
                    estado.etapas.forEach { etapa ->
                        FilaCheck(texto = etapa.texto, estado = etapa.estado)
                    }
                }
            }

            val fallo = estado.fallo
            if (fallo != null) {
                HojaFallo(
                    fallo = fallo,
                    onContinuarConMinimo = onContinuarConMinimo,
                    onCambiarRespuestas = onCambiarRespuestas,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(Espaciado.m),
                )
            }
        }
    }
}

/**
 * Marca de agua animada mientras se arma.
 *
 * Late suave en vez de girar: un spinner grande sobre cuatro spinners chicos satura
 * la pantalla de movimiento. Al fallar se detiene y se apaga, que es la señal de que
 * algo pasó incluso antes de leer la hoja.
 */
@Composable
private fun OllaPulsante(detenida: Boolean) {
    val transicion = rememberInfiniteTransition(label = "olla")
    val pulso by transicion.animateFloat(
        initialValue = 1f,
        targetValue = ESCALA_PULSO,
        animationSpec = infiniteRepeatable(
            animation = tween(DURACION_PULSO_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulsoOlla",
    )

    Surface(
        modifier = Modifier
            .size(TAMANO_OLLA.dp)
            .scale(if (detenida) 1f else pulso)
            .alpha(if (detenida) ALFA_DETENIDA else 1f),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            IconoCookify(
                icono = IconosCookify.plato,
                descripcion = null,
                modifier = Modifier.size(ICONO_OLLA.dp),
                tinte = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private const val ESCALA_PULSO = 1.08f
private const val DURACION_PULSO_MS = 900
private const val ALFA_DETENIDA = 0.4f
private const val TAMANO_OLLA = 96
private const val ICONO_OLLA = 44

@Preview
@Composable
private fun ArmadoEnCursoPreview() {
    CookifyTheme {
        ContenidoArmado(
            estado = ArmadoUiState(
                etapas = listOf(
                    EtapaArmado("Revisando el catálogo de Jumbo", EstadoCheck.LISTO),
                    EtapaArmado("Calzando platos con tus \$45.000", EstadoCheck.EN_CURSO),
                    EtapaArmado(
                        "Ordenando los almuerzos de lunes a viernes",
                        EstadoCheck.PENDIENTE,
                    ),
                    EtapaArmado("Armando tu lista de compras", EstadoCheck.PENDIENTE),
                ),
            ),
            onContinuarConMinimo = {},
            onCambiarRespuestas = {},
        )
    }
}
