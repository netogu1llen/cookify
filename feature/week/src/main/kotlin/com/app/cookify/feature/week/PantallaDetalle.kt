package com.app.cookify.feature.week

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.cookify.core.common.aPesos
import com.app.cookify.core.model.IngredienteCalculado
import com.app.cookify.core.model.Receta
import com.app.cookify.core.model.Supermercado
import com.app.cookify.core.ui.componentes.DatoResumen
import com.app.cookify.core.ui.componentes.Etiqueta
import com.app.cookify.core.ui.componentes.IconoCookify
import com.app.cookify.core.ui.componentes.IconosCookify
import com.app.cookify.core.ui.theme.Espaciado

/**
 * Detalle del plato: tiempo, clasificación, para cuántos, ingredientes e instrucciones.
 *
 * Los ingredientes van antes que los pasos porque son lo que se consulta en el súper,
 * que es cuando la pantalla se abre de verdad; los pasos se leen después, en la cocina,
 * y ahí el scroll ya no molesta.
 */
@Composable
fun PantallaDetalle(
    recetaId: String,
    personas: Int,
    supermercado: Supermercado,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetalleViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(recetaId) { viewModel.cargar(recetaId, personas, supermercado) }

    ContenidoDetalle(estado = estado, onVolver = onVolver, modifier = modifier)
}

/** Versión sin estado, para previews y tests de UI. */
@Composable
internal fun ContenidoDetalle(
    estado: DetalleUiState,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { relleno ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(relleno),
        ) {
            BarraVolver(onVolver)

            val receta = estado.receta
            when {
                estado.cargando -> Box(
                    Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                receta == null -> Box(
                    Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No encontramos esta receta.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> CuerpoDetalle(receta = receta, estado = estado)
            }
        }
    }
}

@Composable
private fun CuerpoDetalle(receta: Receta, estado: DetalleUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = Espaciado.margenPantalla,
            end = Espaciado.margenPantalla,
            bottom = Espaciado.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(Espaciado.m),
    ) {
        item(key = "titulo") { Titulo(receta) }
        item(key = "datos") { FilaDatos(receta, estado) }
        item(key = "etiquetas") { Clasificacion(receta) }

        item(key = "tituloIngredientes") {
            Seccion("Ingredientes", "Para ${personasEnTexto(estado.personas)}")
        }
        estado.ingredientesPorPasillo.forEach { grupo ->
            item(key = "pasillo-${grupo.pasillo.name}") {
                GrupoDeIngredientes(grupo)
            }
        }

        item(key = "tituloPasos") { Seccion("Instrucciones", null) }
        receta.pasos.forEachIndexed { indice, paso ->
            item(key = "paso-$indice") { Paso(numero = indice + 1, texto = paso) }
        }
    }
}

@Composable
private fun BarraVolver(onVolver: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Espaciado.xs, vertical = Espaciado.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onVolver) {
            IconoCookify(
                icono = IconosCookify.flechaAtras,
                descripcion = "Volver a la semana",
                tinte = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun Titulo(receta: Receta) {
    Column(verticalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
        Text(
            text = receta.nombre,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = receta.descripcion,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FilaDatos(receta: Receta, estado: DetalleUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(vertical = Espaciado.m),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            DatoResumen(
                valor = "${receta.minutosTotal} min",
                etiqueta = "Tiempo",
                icono = IconosCookify.reloj,
                modifier = Modifier.weight(1f),
            )
            DatoResumen(
                valor = estado.personas.toString(),
                etiqueta = if (estado.personas == 1) "Persona" else "Personas",
                icono = IconosCookify.personas,
                modifier = Modifier.weight(1f),
            )
            DatoResumen(
                valor = estado.costoClp.aPesos(),
                etiqueta = "En total",
                icono = IconosCookify.billetera,
                modifier = Modifier.weight(1f),
            )
            DatoResumen(
                valor = "${receta.kcalPorPorcion}",
                etiqueta = "kcal c/u",
                icono = IconosCookify.balanza,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Clasificacion(receta: Receta) {
    val colores = MaterialTheme.colorScheme
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Espaciado.xxs),
        verticalArrangement = Arrangement.spacedBy(Espaciado.xxs),
    ) {
        receta.antojos.forEach { antojo ->
            Etiqueta(
                texto = antojo.etiqueta,
                colorContenedor = colores.secondaryContainer,
                colorContenido = colores.onSecondaryContainer,
            )
        }
        Etiqueta(
            texto = "${receta.proteinaGPorPorcion} g de proteína",
            colorContenedor = colores.tertiaryContainer,
            colorContenido = colores.onTertiaryContainer,
        )
    }
}

@Composable
private fun Seccion(titulo: String, ayuda: String?) {
    Column(
        modifier = Modifier.padding(top = Espaciado.xs),
        verticalArrangement = Arrangement.spacedBy(Espaciado.xxs),
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (ayuda != null) {
            Text(
                text = ayuda,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = Espaciado.xs),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun GrupoDeIngredientes(grupo: GrupoPasillo) {
    Column(verticalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
        Text(
            text = grupo.pasillo.etiqueta,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        grupo.ingredientes.forEach { FilaIngrediente(it) }
    }
}

@Composable
private fun FilaIngrediente(ingrediente: IngredienteCalculado) {
    val colores = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Espaciado.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = ingrediente.ingrediente.nombre +
                if (ingrediente.opcional) " (opcional)" else "",
            style = MaterialTheme.typography.bodyLarge,
            color = if (ingrediente.opcional) colores.onSurfaceVariant else colores.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = ingrediente.cantidadLegible,
            style = MaterialTheme.typography.bodyLarge,
            color = colores.onSurfaceVariant,
        )
    }
}

@Composable
private fun Paso(numero: Int, texto: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Espaciado.s),
    ) {
        Surface(
            modifier = Modifier.size(TAMANO_NUMERO.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = numero.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun personasEnTexto(personas: Int): String =
    if (personas == 1) "1 persona" else "$personas personas"

private const val TAMANO_NUMERO = 28
