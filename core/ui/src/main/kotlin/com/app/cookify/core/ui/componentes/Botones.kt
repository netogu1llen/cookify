package com.app.cookify.core.ui.componentes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.app.cookify.core.ui.theme.Espaciado

/**
 * Accion principal de la pantalla.
 *
 * Va siempre al fondo, ancho completo y con 56dp de alto: es la zona del pulgar y
 * el usuario no deberia estirarse para avanzar. El pequeno hundimiento al presionar
 * es la unica confirmacion tactil que tenemos en pantalla.
 */
@Composable
fun BotonPrimario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    cargando: Boolean = false,
) {
    val interacciones = remember { MutableInteractionSource() }
    val presionado by interacciones.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionado) ESCALA_PRESIONADO else 1f,
        label = "escalaBotonPrimario",
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(Espaciado.altoBotonPrimario)
            .scale(escala),
        enabled = habilitado && !cargando,
        interactionSource = interacciones,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text(texto, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Accion secundaria: presente pero que no compita con la primaria. */
@Composable
fun BotonSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    iconoInicial: Int? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(Espaciado.altoBotonPrimario),
        enabled = habilitado,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Espaciado.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconoInicial != null) {
                IconoCookify(
                    icono = iconoInicial,
                    descripcion = null,
                    modifier = Modifier.size(20.dp),
                    tinte = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(texto, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Accion terciaria de bajo peso: "saltar", "ahora no". */
@Composable
fun BotonTexto(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val ESCALA_PRESIONADO = 0.97f
