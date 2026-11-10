package com.app.cookify.feature.ajustes

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.cookify.core.common.aFechaHoraCorta
import com.app.cookify.core.model.PreciosGuardados
import com.app.cookify.core.model.PruebaDeConexion
import com.app.cookify.core.ui.componentes.BotonPrimario
import com.app.cookify.core.ui.componentes.BotonSecundario
import com.app.cookify.core.ui.componentes.IconoCookify
import com.app.cookify.core.ui.componentes.IconosCookify
import com.app.cookify.core.ui.theme.Espaciado

/**
 * Estado del servicio de precios.
 *
 * Responde tres preguntas que antes solo se podían contestar borrando los datos
 * de la app y poniendo modo avión: a qué servidor habla esta versión, si lo
 * alcanza **ahora**, y qué tan viejo es lo que tiene guardado.
 */
@Composable
fun PantallaAjustes(
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AjustesViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    ContenidoAjustes(
        estado = estado,
        onProbar = viewModel::probarConexion,
        onOlvidar = viewModel::olvidarPrecios,
        onVolver = onVolver,
        modifier = modifier,
    )
}

/** Versión sin estado, para previews y pruebas de interfaz. */
@Composable
internal fun ContenidoAjustes(
    estado: AjustesUiState,
    onProbar: () -> Unit,
    onOlvidar: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { relleno ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno),
            contentPadding = PaddingValues(
                start = Espaciado.margenPantalla,
                end = Espaciado.margenPantalla,
                bottom = Espaciado.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(Espaciado.s),
        ) {
            item(key = "encabezado") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onVolver) {
                        IconoCookify(
                            icono = IconosCookify.flechaAtras,
                            descripcion = "Volver",
                            tinte = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = "Estado del servicio",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            item(key = "direccion") { Direccion(estado.direccion) }

            item(key = "prueba") {
                Prueba(
                    probando = estado.probando,
                    resultado = estado.prueba,
                    onProbar = onProbar,
                )
            }

            seccionDePreciosGuardados(estado, onOlvidar)
        }
    }
}

/** Que tiene la app guardado, y el boton para hacerla olvidarlo. */
private fun LazyListScope.seccionDePreciosGuardados(
    estado: AjustesUiState,
    onOlvidar: () -> Unit,
) {
    item(key = "tituloGuardados") {
        Seccion(
            "Precios guardados en este teléfono",
            "Lo que la app usa cuando no hay red",
        )
    }

    if (estado.guardados.isEmpty()) {
        item(key = "sinGuardados") {
            Nota(
                "No hay nada guardado. La próxima semana que armes va a pedirle " +
                    "los precios al servicio.",
            )
        }
    } else {
        filasDeCadenas(estado.guardados)
    }

    item(key = "olvidar") {
        Column(verticalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
            BotonSecundario(
                texto = if (estado.olvidando) "Borrando…" else "Olvidar precios guardados",
                onClick = onOlvidar,
                modifier = Modifier.fillMaxWidth(),
                habilitado = !estado.olvidando && estado.guardados.isNotEmpty(),
            )
            Nota(
                estado.aviso ?: "No borra tus semanas guardadas. Solo obliga a la " +
                    "app a volver a pedir los precios, que es como se comprueba " +
                    "que la conexión funciona de verdad.",
            )
        }
    }
}

private fun LazyListScope.filasDeCadenas(
    guardados: List<PreciosGuardados>,
) {
    guardados.forEach { guardado ->
        item(key = "cadena-${guardado.supermercado.name}") { FilaCadena(guardado) }
    }
}

@Composable
private fun Direccion(direccion: String) {
    Tarjeta {
        Column(verticalArrangement = Arrangement.spacedBy(Espaciado.xxs)) {
            Text(
                text = "Esta versión habla con",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = direccion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                // Se fija al compilar: no hay forma de apuntar la app a otro
                // servidor desde la interfaz, y eso es a propósito.
                text = "La dirección se fija al compilar la app",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Prueba(
    probando: Boolean,
    resultado: PruebaDeConexion?,
    onProbar: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Espaciado.s)) {
        BotonPrimario(
            texto = if (probando) "Probando…" else "Probar conexión",
            onClick = onProbar,
            modifier = Modifier.fillMaxWidth(),
            habilitado = !probando,
        )

        ResultadoDePrueba(probando, resultado)
    }
}

@Composable
private fun ResultadoDePrueba(probando: Boolean, resultado: PruebaDeConexion?) {
    Column {
        when {
            probando -> Tarjeta {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Espaciado.s),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(TAMANO_GIRO.dp),
                        strokeWidth = GROSOR_GIRO.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Preguntándole al servicio…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            resultado is PruebaDeConexion.Respondio -> Tarjeta {
                Column(verticalArrangement = Arrangement.spacedBy(Espaciado.xxs)) {
                    Text(
                        text = "Conectado",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Dato("Respondió en", "${resultado.milisegundos} ms")
                    Dato("Versión del servicio", resultado.version)
                    Dato("Base de datos", resultado.baseDeDatos)
                }
            }

            resultado is PruebaDeConexion.NoRespondio -> Tarjeta {
                Column(verticalArrangement = Arrangement.spacedBy(Espaciado.xxs)) {
                    Text(
                        text = "No se pudo conectar",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = resultado.motivo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        // Lo importante: esto no rompe la app.
                        text = "La app sigue funcionando: arma las semanas con los " +
                            "precios guardados, y si no hay, con los del catálogo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaCadena(guardado: PreciosGuardados) {
    Tarjeta {
        Column(verticalArrangement = Arrangement.spacedBy(Espaciado.xxs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = guardado.supermercado.etiqueta,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${guardado.medidos} de ${guardado.total} medidos",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (guardado.medidos > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            guardado.medidoEn.aFechaHoraCorta()?.let {
                Dato("Medidos en el súper", it)
            }
            guardado.guardadoEn?.let {
                Dato("Traídos al teléfono", it.aFechaHoraCorta())
            }
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun Seccion(titulo: String, bajada: String) {
    Column(
        modifier = Modifier.padding(top = Espaciado.m),
        verticalArrangement = Arrangement.spacedBy(Espaciado.xxs),
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = bajada,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Nota(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Tarjeta(contenido: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(Espaciado.paddingTarjeta)) { contenido() }
    }
}

private const val TAMANO_GIRO = 18
private const val GROSOR_GIRO = 2
