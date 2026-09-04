package com.app.cookify.feature.week

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.cookify.core.model.PlanSemanal
import com.app.cookify.core.model.SolicitudPlan
import com.app.cookify.core.model.Supermercado
import com.app.cookify.core.ui.componentes.BarraPresupuesto
import com.app.cookify.core.ui.componentes.BotonPrimario
import com.app.cookify.core.ui.componentes.BotonSecundario
import com.app.cookify.core.ui.componentes.Etiqueta
import com.app.cookify.core.ui.componentes.IconoCookify
import com.app.cookify.core.ui.componentes.IconosCookify
import com.app.cookify.core.ui.theme.Espaciado

/**
 * La semana lista: un almuerzo por día, en orden L-D.
 *
 * Es la pantalla que justifica la app, así que lo primero que se ve no es la lista
 * sino el total contra el presupuesto: la pregunta que el usuario trajo es "¿cuánto me
 * va a costar?", y la respuesta no puede estar al final del scroll.
 */
@Composable
fun PantallaSemana(
    solicitud: SolicitudPlan,
    semilla: Long,
    onAbrirReceta: (recetaId: String, personas: Int, supermercado: Supermercado) -> Unit,
    onGuardada: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SemanaViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val guardada by rememberUpdatedState(onGuardada)

    LaunchedEffect(solicitud, semilla) { viewModel.cargar(solicitud, semilla) }

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                EventoSemana.Guardada -> guardada()
                // Solo llega en modo lectura, que usa la otra entrada de la pantalla.
                EventoSemana.Borrada -> Unit
            }
        }
    }

    ContenidoSemana(
        estado = estado,
        acciones = AccionesSemana(
            abrirReceta = onAbrirReceta,
            regenerar = viewModel::regenerar,
            pedirNombre = viewModel::pedirNombre,
            cambiarNombre = viewModel::cambiarNombre,
            confirmarGuardado = viewModel::guardar,
            cancelarGuardado = viewModel::cancelarGuardado,
            volver = onVolver,
        ),
        modifier = modifier,
    )
}

/**
 * Una semana guardada, reabierta desde la home.
 *
 * Misma pantalla, mismo estado: cambia solo que no se regenera ni se vuelve a guardar,
 * y que la accion del fondo pasa a ser borrarla.
 */
@Composable
fun PantallaSemanaGuardada(
    id: Long,
    onAbrirReceta: (recetaId: String, personas: Int, supermercado: Supermercado) -> Unit,
    onBorrada: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SemanaViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val borrada by rememberUpdatedState(onBorrada)

    LaunchedEffect(id) { viewModel.cargarGuardada(id) }

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            if (evento is EventoSemana.Borrada) borrada()
        }
    }

    ContenidoSemana(
        estado = estado,
        acciones = AccionesSemana(
            abrirReceta = onAbrirReceta,
            regenerar = {},
            pedirNombre = {},
            cambiarNombre = {},
            confirmarGuardado = {},
            cancelarGuardado = {},
            borrar = viewModel::borrar,
            volver = onVolver,
        ),
        modifier = modifier,
    )
}

/**
 * Las acciones agrupadas, para no pasar el ViewModel hacia abajo: el contenido sin
 * estado se puede previsualizar y testear sin Hilt.
 */
@Immutable
internal data class AccionesSemana(
    val abrirReceta: (recetaId: String, personas: Int, supermercado: Supermercado) -> Unit,
    val regenerar: () -> Unit,
    val pedirNombre: () -> Unit,
    val cambiarNombre: (String) -> Unit,
    val confirmarGuardado: () -> Unit,
    val cancelarGuardado: () -> Unit,
    val volver: () -> Unit,
    val borrar: () -> Unit = {},
)

/** Versión sin estado, para previews y tests de UI. */
@Composable
internal fun ContenidoSemana(
    estado: SemanaUiState,
    acciones: AccionesSemana,
    modifier: Modifier = Modifier,
) {
    // La celebracion reemplaza la pantalla entera en vez de superponerse: es el cierre
    // del recorrido, y dejar la lista asomando por detras lo convertiria en un aviso.
    if (estado.guardada) {
        Celebracion(modifier)
        return
    }

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
            BarraVolver(acciones.volver)

            val plan = estado.plan
            when {
                estado.cargando && plan == null -> Cargando(Modifier.weight(1f))
                plan == null -> Error(estado.error, Modifier.weight(1f))
                else -> {
                    ListaSemana(
                        plan = plan,
                        titulo = estado.nombre ?: "Tu semana",
                        onAbrirReceta = acciones.abrirReceta,
                        modifier = Modifier.weight(1f),
                    )
                    Acciones(
                        soloLectura = estado.soloLectura,
                        onGuardar = acciones.pedirNombre,
                        onRegenerar = acciones.regenerar,
                        onBorrar = acciones.borrar,
                        ocupado = estado.cargando || estado.guardando,
                    )
                }
            }
        }
    }

    val plan = estado.plan
    if (estado.pidiendoNombre && plan != null) {
        DialogoNombre(
            nombre = estado.nombrePropuesto,
            plan = plan,
            onCambiarNombre = acciones.cambiarNombre,
            onConfirmar = acciones.confirmarGuardado,
            onCancelar = acciones.cancelarGuardado,
        )
    }
}

@Composable
private fun ListaSemana(
    plan: PlanSemanal,
    titulo: String,
    onAbrirReceta: (String, Int, Supermercado) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = Espaciado.margenPantalla,
            end = Espaciado.margenPantalla,
            bottom = Espaciado.m,
        ),
        verticalArrangement = Arrangement.spacedBy(Espaciado.s),
    ) {
        item(key = "encabezado") {
            Encabezado(plan = plan, titulo = titulo)
        }

        items(plan.almuerzos, key = { it.dia.name }) { almuerzo ->
            TarjetaAlmuerzo(
                almuerzo = almuerzo,
                onClick = {
                    onAbrirReceta(
                        almuerzo.receta.id,
                        plan.solicitud.personas,
                        plan.solicitud.supermercado,
                    )
                },
            )
        }
    }
}

@Composable
private fun Encabezado(plan: PlanSemanal, titulo: String) {
    val solicitud = plan.solicitud
    val platos = plan.almuerzos.size

    Column(
        modifier = Modifier.padding(top = Espaciado.m, bottom = Espaciado.xs),
        verticalArrangement = Arrangement.spacedBy(Espaciado.s),
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(Espaciado.xxs)) {
            Etiqueta(texto = solicitud.supermercado.etiqueta, icono = IconosCookify.tienda)
            Etiqueta(
                texto = if (solicitud.personas == 1) "1 persona" else "${solicitud.personas} personas",
                icono = IconosCookify.personas,
            )
            Etiqueta(
                texto = if (platos == 1) "1 almuerzo" else "$platos almuerzos",
                icono = IconosCookify.plato,
            )
        }

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Box(Modifier.padding(Espaciado.paddingTarjeta)) {
                BarraPresupuesto(
                    gastadoClp = plan.costoTotalClp,
                    presupuestoClp = solicitud.presupuestoClp,
                )
            }
        }
    }
}

@Composable
private fun Acciones(
    soloLectura: Boolean,
    onGuardar: () -> Unit,
    onRegenerar: () -> Unit,
    onBorrar: () -> Unit,
    ocupado: Boolean,
) {
    var confirmandoBorrado by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(
            start = Espaciado.margenPantalla,
            end = Espaciado.margenPantalla,
            bottom = Espaciado.m,
        ),
        verticalArrangement = Arrangement.spacedBy(Espaciado.xs),
    ) {
        if (soloLectura) {
            BotonSecundario(
                texto = "Borrar esta semana",
                onClick = { confirmandoBorrado = true },
                iconoInicial = IconosCookify.borrar,
            )
        } else {
            BotonPrimario(texto = "Guardar semana", onClick = onGuardar, habilitado = !ocupado)
            BotonSecundario(
                texto = "Regenerar",
                onClick = onRegenerar,
                habilitado = !ocupado,
                iconoInicial = IconosCookify.regenerar,
            )
        }
    }

    if (confirmandoBorrado) {
        // Borrar es lo unico irreversible de la app, asi que es lo unico que pregunta.
        AlertDialog(
            onDismissRequest = { confirmandoBorrado = false },
            title = { Text("¿Borrar la semana?") },
            text = { Text("No se puede deshacer, pero siempre puedes armar otra.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmandoBorrado = false
                        onBorrar()
                    },
                ) {
                    Text("Borrar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoBorrado = false }) { Text("Cancelar") }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }
}

@Composable
private fun Cargando(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun Error(mensaje: String?, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .padding(Espaciado.margenPantalla),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = mensaje ?: "No pudimos armar la semana.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Barra con la flecha de volver.
 *
 * Existe porque una semana guardada no tenia como salir mas que borrandola: el gesto
 * de atras funcionaba, pero la unica salida visible era el boton destructivo.
 */
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
                descripcion = "Volver",
                tinte = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
