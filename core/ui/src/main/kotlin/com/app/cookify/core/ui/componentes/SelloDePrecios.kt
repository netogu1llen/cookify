package com.app.cookify.core.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.cookify.core.common.aFechaHoraCorta
import com.app.cookify.core.ui.theme.CookifyTheme
import com.app.cookify.core.ui.theme.Espaciado

/**
 * Dice de dónde salió el precio que la pantalla muestra.
 *
 * Existe porque un número sin contexto obliga a quien lo lee a confiar a ciegas.
 * La app estima en cuatro de las seis cadenas, y puede estar mostrando un precio
 * guardado hace días si no hay red: decirlo cuesta una línea y es la diferencia
 * entre una cifra y una cifra que se puede juzgar.
 *
 * Nunca es un error ni una alarma. Incluso sin conexión el mensaje es
 * informativo, porque la semana se armó igual: no hay nada que el usuario deba
 * hacer al respecto.
 */
@Composable
fun SelloDePrecios(
    estado: EstadoPrecios,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    val mensaje = mensajeDe(estado)

    // Un solo texto para el lector de pantalla: leer el título y el detalle por
    // separado los convierte en dos anuncios sin relación.
    val descripcion = listOfNotNull(mensaje.titulo, mensaje.detalle).joinToString(". ")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = descripcion },
        shape = MaterialTheme.shapes.medium,
        color = if (estado.sinConexion) {
            colores.surfaceContainerHigh
        } else {
            colores.surfaceContainer
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Espaciado.m, vertical = Espaciado.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espaciado.s),
        ) {
            IconoCookify(
                icono = mensaje.icono,
                descripcion = null,
                modifier = Modifier.size(TAMANO_ICONO.dp),
                tinte = colores.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(Espaciado.xxs)) {
                Text(
                    text = mensaje.titulo,
                    style = MaterialTheme.typography.labelLarge,
                    color = colores.onSurface,
                )
                if (mensaje.detalle != null) {
                    Text(
                        text = mensaje.detalle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colores.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private data class Mensaje(val titulo: String, val detalle: String?, val icono: Int)

/**
 * Qué decir, según de dónde salió el precio.
 *
 * Separado de lo que dibuja porque es la decisión, no la pintura: así se lee de
 * corrido y se puede cambiar una frase sin tocar el diseño.
 */
private fun mensajeDe(estado: EstadoPrecios): Mensaje {
    val fecha = estado.medidosEn.aFechaHoraCorta()
    return when {
        estado.sinConexion && estado.medidos && fecha != null -> Mensaje(
            titulo = "Sin conexión",
            detalle = "Última actualización de precios $fecha",
            icono = IconosCookify.sinConexion,
        )

        estado.sinConexion -> Mensaje(
            titulo = "Sin conexión",
            detalle = "Precios estimados: no se pudo consultar el supermercado",
            icono = IconosCookify.sinConexion,
        )

        estado.medidos && fecha != null -> Mensaje(
            titulo = "Precios medidos en el supermercado",
            detalle = "Última actualización $fecha",
            icono = IconosCookify.etiquetaPrecio,
        )

        estado.medidos -> Mensaje(
            titulo = "Precios medidos en el supermercado",
            detalle = null,
            icono = IconosCookify.etiquetaPrecio,
        )

        else -> Mensaje(
            titulo = "Precios estimados",
            detalle = "Esta cadena no publica su catálogo: el costo es una referencia",
            icono = IconosCookify.etiquetaPrecio,
        )
    }
}

private const val TAMANO_ICONO = 18

@Preview
@Composable
private fun SelloMedidoPreview() {
    CookifyTheme {
        SelloDePrecios(
            EstadoPrecios(medidos = true, medidosEn = "2026-11-09T22:50:09Z"),
            modifier = Modifier.padding(Espaciado.m),
        )
    }
}

@Preview
@Composable
private fun SelloSinConexionPreview() {
    CookifyTheme {
        SelloDePrecios(
            EstadoPrecios(medidos = true, medidosEn = "2026-11-02T12:00:00Z", sinConexion = true),
            modifier = Modifier.padding(Espaciado.m),
        )
    }
}

@Preview
@Composable
private fun SelloEstimadoPreview() {
    CookifyTheme {
        SelloDePrecios(
            EstadoPrecios(medidos = false),
            modifier = Modifier.padding(Espaciado.m),
        )
    }
}
