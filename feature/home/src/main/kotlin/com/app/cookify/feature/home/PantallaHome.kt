package com.app.cookify.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.cookify.core.common.aFechaLegible
import com.app.cookify.core.common.aPesos
import com.app.cookify.core.model.DiaSemana
import com.app.cookify.core.model.SemanaGuardada
import com.app.cookify.core.ui.componentes.BotonPrimario
import com.app.cookify.core.ui.componentes.Etiqueta
import com.app.cookify.core.ui.componentes.EstadoVacio
import com.app.cookify.core.ui.componentes.IconoCookify
import com.app.cookify.core.ui.componentes.IconosCookify
import com.app.cookify.core.ui.theme.Espaciado

/**
 * La primera pantalla al abrir la app.
 *
 * Sin semanas guardadas es un estado vacío con una sola acción; con semanas es la
 * lista. Es deliberado que no haya nada más: la app hace una cosa, y la home tiene que
 * dejar clarísimo cuál es tanto la primera vez como la número veinte.
 */
@Composable
fun PantallaHome(
    onNuevaSemana: () -> Unit,
    onAbrirSemana: (id: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    ContenidoHome(
        estado = estado,
        onNuevaSemana = onNuevaSemana,
        onAbrirSemana = onAbrirSemana,
        modifier = modifier,
    )
}

/** Versión sin estado, para previews y tests de UI. */
@Composable
internal fun ContenidoHome(
    estado: HomeUiState,
    onNuevaSemana: () -> Unit,
    onAbrirSemana: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { relleno ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(relleno),
        ) {
            when {
                // Sin nada mientras carga: un spinner que aparece y desaparece en
                // 50 ms se ve como un defecto, no como progreso.
                estado.cargando -> Unit

                estado.semanas.isEmpty() -> EstadoVacio(
                    titulo = "Todavía no tienes semanas",
                    mensaje = "Arma tu primera semana de almuerzos y te decimos cuánto " +
                        "te va a costar en el súper.",
                    textoAccion = "Armar mi primera semana",
                    onAccion = onNuevaSemana,
                )

                else -> ListaSemanas(
                    semanas = estado.semanas,
                    onNuevaSemana = onNuevaSemana,
                    onAbrirSemana = onAbrirSemana,
                )
            }
        }
    }
}

@Composable
private fun ListaSemanas(
    semanas: List<SemanaGuardada>,
    onNuevaSemana: () -> Unit,
    onAbrirSemana: (Long) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = Espaciado.margenPantalla,
                end = Espaciado.margenPantalla,
                bottom = Espaciado.m,
            ),
            verticalArrangement = Arrangement.spacedBy(Espaciado.s),
        ) {
            item(key = "titulo") {
                Text(
                    text = "Tus semanas",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = Espaciado.m, bottom = Espaciado.xs),
                )
            }

            items(semanas, key = { it.id }) { semana ->
                TarjetaSemana(semana = semana, onClick = { onAbrirSemana(semana.id) })
            }
        }

        BotonPrimario(
            texto = "Armar otra semana",
            onClick = onNuevaSemana,
            modifier = Modifier.padding(
                start = Espaciado.margenPantalla,
                end = Espaciado.margenPantalla,
                bottom = Espaciado.m,
            ),
        )
    }
}

@Composable
private fun TarjetaSemana(semana: SemanaGuardada, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val plan = semana.plan

    Surface(
        modifier = Modifier
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
            ContenidoTarjeta(semana, Modifier.weight(1f))

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
private fun ContenidoTarjeta(semana: SemanaGuardada, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    val plan = semana.plan

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Espaciado.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = semana.nombre,
                style = MaterialTheme.typography.titleMedium,
                color = colores.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(
                text = plan.costoTotalClp.aPesos(),
                style = MaterialTheme.typography.titleMedium,
                color = colores.primary,
            )
        }

        Text(
            text = DiaSemana.frase(plan.almuerzos.map { it.dia })
                .replaceFirstChar(Char::uppercase),
            style = MaterialTheme.typography.bodySmall,
            color = colores.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(Espaciado.xxs)) {
            Etiqueta(texto = plan.solicitud.supermercado.etiqueta, icono = IconosCookify.tienda)
            Etiqueta(texto = "${plan.solicitud.personas}", icono = IconosCookify.personas)
            Etiqueta(texto = semana.creadaEn.aFechaLegible(), icono = IconosCookify.calendario)
        }
    }
}
