package com.app.cookify.core.ui.componentes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.app.cookify.core.ui.theme.Espaciado

/**
 * Barra de pasos del onboarding.
 *
 * Segmentos separados en vez de una barra continua: el usuario puede contar cuanto
 * le falta de un vistazo, que es justo lo que uno quiere saber en un cuestionario.
 */
@Composable
fun BarraPasos(
    pasoActual: Int,
    totalPasos: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Paso $pasoActual de $totalPasos"
            },
        horizontalArrangement = Arrangement.spacedBy(Espaciado.xxs),
    ) {
        repeat(totalPasos) { indice ->
            val completado = indice < pasoActual
            val progreso by animateFloatAsState(
                targetValue = if (completado) 1f else 0f,
                label = "progresoPaso$indice",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(ALTO_SEGMENTO.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progreso)
                        .height(ALTO_SEGMENTO.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

/** Estado de cada verificacion de la pantalla de carga. */
enum class EstadoCheck { PENDIENTE, EN_CURSO, LISTO }

/**
 * Una linea de la pantalla de armado: spinner mientras trabaja, check cuando termina.
 *
 * El texto es live region para que TalkBack lea cada paso a medida que avanza; sin
 * eso la pantalla seria un silencio de varios segundos para quien no la ve.
 */
@Composable
fun FilaCheck(
    texto: String,
    estado: EstadoCheck,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    val alfa by animateFloatAsState(
        targetValue = if (estado == EstadoCheck.PENDIENTE) ALFA_PENDIENTE else 1f,
        label = "alfaFilaCheck",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alfa),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espaciado.s),
    ) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            when (estado) {
                EstadoCheck.PENDIENTE -> Box(
                    Modifier
                        .size(8.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(colores.outlineVariant),
                )

                EstadoCheck.EN_CURSO -> CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = colores.primary,
                    strokeWidth = 2.dp,
                )

                // El check entra con rebote: es el unico momento de recompensa de
                // esta pantalla y conviene que se note.
                EstadoCheck.LISTO -> {
                    val escala = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
                        escala.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        )
                    }
                    IconoCookify(
                        icono = IconosCookify.checkCirculo,
                        descripcion = null,
                        modifier = Modifier.scale(escala.value),
                        tinte = colores.secondary,
                    )
                }
            }
        }

        Text(
            text = texto,
            style = MaterialTheme.typography.bodyLarge,
            color = if (estado == EstadoCheck.LISTO) colores.onSurface else colores.onSurfaceVariant,
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
            },
        )
    }
}

private const val ALTO_SEGMENTO = 6
private const val ALFA_PENDIENTE = 0.45f
