package com.app.cookify.core.ui.componentes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.app.cookify.core.common.aPesos
import com.app.cookify.core.ui.theme.Espaciado

/**
 * Cuánto de la plata del usuario se lleva la semana.
 *
 * La barra se llena hasta el tope y ahí cambia a rojo en vez de desbordarse: una
 * barra que se sale del riel no dice cuánto se pasó, y el número de al lado sí. El
 * texto siempre dice el monto exacto porque es el dato que la persona vino a leer;
 * la barra solo da la proporción de un vistazo.
 */
@Composable
fun BarraPresupuesto(
    gastadoClp: Int,
    presupuestoClp: Int,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    val excedido = gastadoClp > presupuestoClp
    val proporcion = if (presupuestoClp <= 0) {
        1f
    } else {
        (gastadoClp.toFloat() / presupuestoClp).coerceIn(0f, 1f)
    }

    val relleno by animateFloatAsState(targetValue = proporcion, label = "rellenoPresupuesto")
    val color by animateColorAsState(
        targetValue = if (excedido) colores.error else colores.primary,
        label = "colorPresupuesto",
    )

    val holgura = presupuestoClp - gastadoClp
    val resumen = if (excedido) {
        "Te pasas por ${(-holgura).aPesos()}"
    } else {
        "Te sobran ${holgura.aPesos()}"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription =
                    "Gastas ${gastadoClp.aPesos()} de ${presupuestoClp.aPesos()}. $resumen"
            },
        verticalArrangement = Arrangement.spacedBy(Espaciado.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = gastadoClp.aPesos(),
                style = MaterialTheme.typography.headlineMedium,
                color = if (excedido) colores.error else colores.onSurface,
            )
            Text(
                text = "de ${presupuestoClp.aPesos()}",
                style = MaterialTheme.typography.bodyMedium,
                color = colores.onSurfaceVariant,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ALTO_BARRA.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(colores.surfaceContainerHigh),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(relleno)
                    .height(ALTO_BARRA.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(color),
            )
        }

        Text(
            text = resumen,
            style = MaterialTheme.typography.bodySmall,
            color = if (excedido) colores.error else colores.onSurfaceVariant,
        )
    }
}

private const val ALTO_BARRA = 8
