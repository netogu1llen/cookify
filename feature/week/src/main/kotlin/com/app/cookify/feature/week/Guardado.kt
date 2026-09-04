package com.app.cookify.feature.week

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.app.cookify.core.common.aPesos
import com.app.cookify.core.model.PlanSemanal
import com.app.cookify.core.ui.componentes.BotonPrimario
import com.app.cookify.core.ui.componentes.BotonTexto
import com.app.cookify.core.ui.componentes.IconoCookify
import com.app.cookify.core.ui.componentes.IconosCookify
import com.app.cookify.core.ui.theme.Espaciado

/**
 * Pide el nombre antes de guardar.
 *
 * Viene con un nombre por defecto ya escrito y utilizable: guardar tiene que poder
 * ser dos toques. El campo está para quien quiera distinguir "Semana liviana" de
 * "Semana del asado", no para obligar a nadie a inventar un título.
 */
@Composable
internal fun DialogoNombre(
    nombre: String,
    plan: PlanSemanal,
    onCambiarNombre: (String) -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    Dialog(onDismissRequest = onCancelar) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.padding(Espaciado.paddingTarjeta),
                verticalArrangement = Arrangement.spacedBy(Espaciado.s),
            ) {
                Text(
                    text = "Guardar semana",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${plan.almuerzos.size} almuerzos por ${plan.costoTotalClp.aPesos()} " +
                        "en ${plan.solicitud.supermercado.etiqueta}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = nombre,
                    onValueChange = onCambiarNombre,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    shape = MaterialTheme.shapes.medium,
                )

                BotonPrimario(texto = "Guardar", onClick = onConfirmar)
                BotonTexto(texto = "Ahora no", onClick = onCancelar)
            }
        }
    }
}

/**
 * La confirmación de que quedó guardada.
 *
 * El check crece con rebote y ocupa la pantalla entera por un segundo. Es el cierre
 * del recorrido completo -siete preguntas, el armado, la semana- y es el único lugar
 * de la app donde vale la pena gastar un segundo en celebrar.
 */
@Composable
internal fun Celebracion(modifier: Modifier = Modifier) {
    val escala = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        escala.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Espaciado.l),
        ) {
            Surface(
                modifier = Modifier
                    .size(TAMANO_CIRCULO.dp)
                    .scale(escala.value),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    IconoCookify(
                        icono = IconosCookify.check,
                        descripcion = null,
                        modifier = Modifier.size(TAMANO_CHECK.dp),
                        tinte = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            Text(
                text = "Semana guardada",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "La vas a encontrar al abrir la app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val TAMANO_CIRCULO = 112
private const val TAMANO_CHECK = 56
