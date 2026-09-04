package com.app.cookify.feature.week

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.cookify.core.common.aPesos
import com.app.cookify.core.model.AlmuerzoDelDia
import com.app.cookify.core.ui.componentes.Etiqueta
import com.app.cookify.core.ui.componentes.IconoCookify
import com.app.cookify.core.ui.componentes.IconosCookify
import com.app.cookify.core.ui.theme.Espaciado

/**
 * Un día de la semana.
 *
 * El día va arriba en mayúsculas y chico, y el plato grande: la persona está
 * recorriendo su semana, así que busca por día pero lee el plato. El precio se alinea
 * a la derecha en la misma línea del día para que la columna de precios se pueda
 * escanear de arriba abajo sin leer nada más.
 */
@Composable
internal fun TarjetaAlmuerzo(
    almuerzo: AlmuerzoDelDia,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = colores.surfaceContainer,
        border = BorderStroke(1.dp, colores.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(Espaciado.paddingTarjeta),
            horizontalArrangement = Arrangement.spacedBy(Espaciado.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ContenidoAlmuerzo(almuerzo, Modifier.weight(1f))

            IconoCookify(
                icono = IconosCookify.chevron,
                descripcion = null,
                modifier = Modifier.size(20.dp),
                tinte = colores.outline,
            )
        }
    }
}

@Composable
private fun ContenidoAlmuerzo(almuerzo: AlmuerzoDelDia, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Espaciado.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = almuerzo.dia.nombre.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = colores.primary,
            )
            Text(
                text = almuerzo.costoClp.aPesos(),
                style = MaterialTheme.typography.labelLarge,
                color = colores.onSurfaceVariant,
            )
        }

        Text(
            text = almuerzo.receta.nombre,
            style = MaterialTheme.typography.titleMedium,
            color = colores.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Espaciado.xxs),
            verticalArrangement = Arrangement.spacedBy(Espaciado.xxs),
        ) {
            Etiqueta(
                texto = "${almuerzo.receta.minutosTotal} min",
                icono = IconosCookify.reloj,
            )
            almuerzo.receta.antojos.take(MAX_ETIQUETAS).forEach { antojo ->
                Etiqueta(
                    texto = antojo.etiqueta,
                    colorContenedor = colores.secondaryContainer,
                    colorContenido = colores.onSecondaryContainer,
                )
            }
        }
    }
}

/**
 * Dos etiquetas de antojo y no todas: una receta puede declarar cinco, y una tarjeta
 * tapizada de etiquetas deja de comunicar cual es la gracia del plato.
 */
private const val MAX_ETIQUETAS = 2
