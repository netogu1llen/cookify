package com.app.cookify.core.ui.componentes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.cookify.core.ui.theme.Espaciado

/**
 * Tarjeta seleccionable del onboarding (supermercado, restriccion, artefacto).
 *
 * El estado seleccionado se comunica por tres canales a la vez -borde de acento,
 * contenedor mas claro y check-, no solo por color: quien no distingue el ambar del
 * gris igual ve cual eligio.
 *
 * [multiple] cambia el rol de accesibilidad: TalkBack anuncia "casilla" en vez de
 * "opcion" cuando se pueden elegir varias.
 */
@Composable
fun TarjetaOpcion(
    titulo: String,
    seleccionada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    icono: Int? = null,
    habilitada: Boolean = true,
    multiple: Boolean = false,
) {
    val colores = MaterialTheme.colorScheme

    val contenedor by animateColorAsState(
        targetValue = when {
            !habilitada -> colores.surfaceContainerLow
            seleccionada -> colores.surfaceContainerHigh
            else -> colores.surfaceContainer
        },
        label = "contenedorOpcion",
    )
    val borde by animateColorAsState(
        targetValue = if (seleccionada) colores.primary else colores.outlineVariant,
        label = "bordeOpcion",
    )
    val grosorBorde by animateDpAsState(
        targetValue = if (seleccionada) 2.dp else 1.dp,
        label = "grosorBordeOpcion",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ALTO_MINIMO_TARJETA.dp)
            .then(modificadorSeleccion(seleccionada, habilitada, multiple, onClick)),
        shape = MaterialTheme.shapes.medium,
        color = contenedor,
        border = BorderStroke(grosorBorde, borde),
    ) {
        ContenidoOpcion(
            titulo = titulo,
            subtitulo = subtitulo,
            icono = icono,
            seleccionada = seleccionada,
            habilitada = habilitada,
        )
    }
}

/**
 * El rol de accesibilidad cambia segun si la seleccion es unica o multiple: TalkBack
 * anuncia "casilla" para varias y "opcion" para una sola.
 */
private fun modificadorSeleccion(
    seleccionada: Boolean,
    habilitada: Boolean,
    multiple: Boolean,
    onClick: () -> Unit,
): Modifier = if (multiple) {
    Modifier.toggleable(
        value = seleccionada,
        enabled = habilitada,
        role = Role.Checkbox,
        onValueChange = { onClick() },
    )
} else {
    Modifier.selectable(
        selected = seleccionada,
        enabled = habilitada,
        role = Role.RadioButton,
        onClick = onClick,
    )
}

@Composable
private fun ContenidoOpcion(
    titulo: String,
    subtitulo: String?,
    icono: Int?,
    seleccionada: Boolean,
    habilitada: Boolean,
) {
    val colores = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.padding(Espaciado.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espaciado.s),
    ) {
        if (icono != null) {
            IconoCookify(
                icono = icono,
                descripcion = null,
                modifier = Modifier.size(24.dp),
                tinte = if (seleccionada) colores.primary else colores.onSurfaceVariant,
            )
        }

        Column(Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                color = if (habilitada) colores.onSurface else colores.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitulo != null) {
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // El check ocupa su lugar siempre, visible o no, para que el texto no se
        // reacomode al seleccionar.
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            if (seleccionada) {
                IconoCookify(
                    icono = IconosCookify.checkCirculo,
                    descripcion = null,
                    tinte = colores.primary,
                )
            }
        }
    }
}

private const val ALTO_MINIMO_TARJETA = 72

/**
 * Chip de antojo, con tope de seleccion.
 *
 * Cuando ya hay tres elegidos los demas se atenuan pero siguen visibles: esconderlos
 * o quitarlos de la lista haria que la pantalla saltara y el usuario perderia de
 * vista lo que descarto.
 */
@Composable
fun ChipAntojo(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    icono: Int? = null,
) {
    val colores = MaterialTheme.colorScheme

    val contenedor by animateColorAsState(
        targetValue = if (seleccionado) colores.primaryContainer else colores.surfaceContainer,
        label = "contenedorChip",
    )
    val contenido = when {
        seleccionado -> colores.onPrimaryContainer
        habilitado -> colores.onSurface
        else -> colores.onSurfaceVariant.copy(alpha = ALFA_DESHABILITADO)
    }

    Surface(
        modifier = modifier
            .heightIn(min = Espaciado.objetivoTactil)
            .toggleable(
                value = seleccionado,
                enabled = habilitado || seleccionado,
                role = Role.Checkbox,
                onValueChange = { onClick() },
            ),
        shape = MaterialTheme.shapes.extraLarge,
        color = contenedor,
        border = BorderStroke(
            width = 1.dp,
            color = if (seleccionado) colores.primary else colores.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Espaciado.m, vertical = Espaciado.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espaciado.xs),
        ) {
            if (icono != null) {
                IconoCookify(
                    icono = icono,
                    descripcion = null,
                    modifier = Modifier.size(18.dp),
                    tinte = contenido,
                )
            }
            Text(text = texto, style = MaterialTheme.typography.labelLarge, color = contenido)
        }
    }
}

private const val ALFA_DESHABILITADO = 0.38f
