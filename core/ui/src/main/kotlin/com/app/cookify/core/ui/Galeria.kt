package com.app.cookify.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.app.cookify.core.ui.componentes.BarraPasos
import com.app.cookify.core.ui.componentes.BotonPrimario
import com.app.cookify.core.ui.componentes.BotonSecundario
import com.app.cookify.core.ui.componentes.BotonTexto
import com.app.cookify.core.ui.componentes.ChipAntojo
import com.app.cookify.core.ui.componentes.DatoResumen
import com.app.cookify.core.ui.componentes.EncabezadoPaso
import com.app.cookify.core.ui.componentes.EstadoCheck
import com.app.cookify.core.ui.componentes.EstadoVacio
import com.app.cookify.core.ui.componentes.Etiqueta
import com.app.cookify.core.ui.componentes.FilaCheck
import com.app.cookify.core.ui.componentes.IconosCookify
import com.app.cookify.core.ui.componentes.TarjetaOpcion
import com.app.cookify.core.ui.theme.CookifyTheme
import com.app.cookify.core.ui.theme.Espaciado

/*
 * Galeria del design system. Sirve para revisar todos los componentes juntos en el
 * panel de previews de Android Studio y notar de inmediato si alguno se sale de la
 * paleta o del espaciado.
 */

@Preview(name = "Botones", showBackground = true, backgroundColor = 0xFF0B0B0E)
@Composable
private fun PreviewBotones() = Marco {
    BotonPrimario(texto = "Armar mi semana", onClick = {})
    BotonPrimario(texto = "Cargando", onClick = {}, cargando = true)
    BotonPrimario(texto = "Deshabilitado", onClick = {}, habilitado = false)
    BotonSecundario(texto = "Regenerar", onClick = {}, iconoInicial = IconosCookify.regenerar)
    BotonTexto(texto = "Ahora no", onClick = {})
}

@Preview(name = "Seleccion", showBackground = true, backgroundColor = 0xFF0B0B0E)
@Composable
private fun PreviewSeleccion() = Marco {
    var elegido by remember { mutableStateOf("Jumbo") }
    listOf("Líder", "Jumbo", "Santa Isabel").forEach { nombre ->
        TarjetaOpcion(
            titulo = nombre,
            seleccionada = elegido == nombre,
            onClick = { elegido = nombre },
            icono = IconosCookify.tienda,
        )
    }
    TarjetaOpcion(
        titulo = "Airfryer",
        subtitulo = "Rapido y sin aceite",
        seleccionada = true,
        onClick = {},
        icono = IconosCookify.horno,
        multiple = true,
    )
}

@Preview(name = "Chips de antojo", showBackground = true, backgroundColor = 0xFF0B0B0E)
@Composable
private fun PreviewChips() = Marco {
    Text("2 de 3 elegidos", style = MaterialTheme.typography.bodySmall)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Espaciado.xs),
        verticalArrangement = Arrangement.spacedBy(Espaciado.xs),
    ) {
        ChipAntojo("Rapido", true, {}, icono = IconosCookify.rayo)
        ChipAntojo("Alto en proteína", true, {}, icono = IconosCookify.balanza)
        ChipAntojo("Bajo en calorías", false, {}, icono = IconosCookify.hoja)
        ChipAntojo("Para llevar", false, {}, habilitado = false)
        ChipAntojo("Cae bien al estómago", false, {}, habilitado = false)
    }
}

@Preview(name = "Progreso y checks", showBackground = true, backgroundColor = 0xFF0B0B0E)
@Composable
private fun PreviewProgreso() = Marco {
    BarraPasos(pasoActual = 4, totalPasos = 7)
    FilaCheck("Revisando el catalogo de Jumbo", EstadoCheck.LISTO)
    FilaCheck("Calzando platos con tus \$45.000", EstadoCheck.LISTO)
    FilaCheck("Ordenando los almuerzos de la semana", EstadoCheck.EN_CURSO)
    FilaCheck("Armando tu lista de compras", EstadoCheck.PENDIENTE)
}

@Preview(name = "Etiquetas y datos", showBackground = true, backgroundColor = 0xFF0B0B0E)
@Composable
private fun PreviewEtiquetas() = Marco {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
        Etiqueta(
            texto = "Alto en proteína",
            icono = IconosCookify.balanza,
            colorContenedor = MaterialTheme.colorScheme.secondaryContainer,
            colorContenido = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Etiqueta(
            texto = "35 min",
            icono = IconosCookify.reloj,
            colorContenedor = MaterialTheme.colorScheme.tertiaryContainer,
            colorContenido = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Etiqueta(texto = "Para llevar")
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        DatoResumen("35 min", "Tiempo", icono = IconosCookify.reloj)
        DatoResumen("4", "Personas", icono = IconosCookify.personas)
        DatoResumen("\$6.700", "Costo", icono = IconosCookify.billetera)
    }
}

@Preview(name = "Encabezado de paso", showBackground = true, backgroundColor = 0xFF0B0B0E)
@Composable
private fun PreviewEncabezado() = Marco {
    EncabezadoPaso(
        titulo = "¿Qué se te antoja?",
        ayuda = "Elige hasta 3. Mientras mas especifico, mejor calza la semana.",
    )
}

@Preview(name = "Estado vacio", showBackground = true, backgroundColor = 0xFF0B0B0E, heightDp = 640)
@Composable
private fun PreviewEstadoVacio() = CookifyTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        EstadoVacio(
            titulo = "Todavía no tienes semanas",
            mensaje = "Arma tu primera semana de almuerzos y te decimos cuánto te va a costar en el súper.",
            textoAccion = "Armar mi primera semana",
            onAccion = {},
        )
    }
}

@Composable
private fun Marco(contenido: @Composable () -> Unit) = CookifyTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(Espaciado.margenPantalla),
            verticalArrangement = Arrangement.spacedBy(Espaciado.s),
        ) {
            contenido()
        }
    }
}
