package com.app.cookify.core.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.cookify.core.ui.theme.Espaciado

/**
 * Etiqueta de clasificacion: "Rapido", "Alto en proteina", "Cae bien al estomago".
 *
 * El color lo decide quien la usa segun el tipo de dato (nutricion en lima, tiempo
 * en verde agua), pero siempre sobre un contenedor tonal, nunca sobre el acento
 * plano: son informacion, no acciones.
 */
@Composable
fun Etiqueta(
    texto: String,
    modifier: Modifier = Modifier,
    icono: Int? = null,
    colorContenedor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    colorContenido: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = colorContenedor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Espaciado.s, vertical = Espaciado.xxs + 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espaciado.xxs),
        ) {
            if (icono != null) {
                IconoCookify(
                    icono = icono,
                    descripcion = null,
                    modifier = Modifier.size(14.dp),
                    tinte = colorContenido,
                )
            }
            Text(
                text = texto,
                style = MaterialTheme.typography.labelMedium,
                color = colorContenido,
            )
        }
    }
}

/**
 * Dato con su etiqueta: "35 min", "Para 4", "$6.700".
 *
 * El valor va arriba y grande, la etiqueta abajo y tenue. Al reves -etiqueta grande,
 * valor chico- es el error mas comun en tarjetas de datos: se enfatiza el nombre del
 * campo en vez del dato que la persona vino a leer.
 */
@Composable
fun DatoResumen(
    valor: String,
    etiqueta: String,
    modifier: Modifier = Modifier,
    icono: Int? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Espaciado.xxs),
    ) {
        if (icono != null) {
            IconoCookify(
                icono = icono,
                descripcion = null,
                modifier = Modifier.size(18.dp),
                tinte = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = valor,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Estado vacio.
 *
 * Nunca es solo "no hay nada": explica en una linea que gana el usuario y ofrece la
 * accion. Una pantalla vacia es la primera impresion de la app, no un error.
 */
@Composable
fun EstadoVacio(
    titulo: String,
    mensaje: String,
    textoAccion: String,
    onAccion: () -> Unit,
    modifier: Modifier = Modifier,
    icono: Int = IconosCookify.plato,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Espaciado.margenPantalla),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Box(Modifier.padding(Espaciado.l)) {
                IconoCookify(
                    icono = icono,
                    descripcion = null,
                    modifier = Modifier.size(40.dp),
                    tinte = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Espaciado.l),
        )
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = Espaciado.xs)
                .widthIn(max = ANCHO_MAXIMO_TEXTO.dp),
        )

        BotonPrimario(
            texto = textoAccion,
            onClick = onAccion,
            modifier = Modifier.padding(top = Espaciado.xl),
        )
    }
}

/** Encabezado de paso del onboarding: pregunta grande, ayuda breve. */
@Composable
fun EncabezadoPaso(
    titulo: String,
    modifier: Modifier = Modifier,
    ayuda: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (ayuda != null) {
            Text(
                text = ayuda,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Espaciado.xs),
            )
        }
    }
}

private const val ANCHO_MAXIMO_TEXTO = 300
