package com.app.cookify.feature.planning

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.cookify.core.common.aPesos
import com.app.cookify.core.model.Artefacto
import com.app.cookify.core.model.DiaSemana
import com.app.cookify.core.model.Restriccion
import com.app.cookify.core.model.SolicitudPlan
import com.app.cookify.core.model.Supermercado
import com.app.cookify.core.ui.componentes.BotonPrimario
import com.app.cookify.core.ui.componentes.BotonSecundario
import com.app.cookify.core.ui.componentes.IconoCookify
import com.app.cookify.core.ui.componentes.IconosCookify
import com.app.cookify.core.ui.theme.CookifyTheme
import com.app.cookify.core.ui.theme.Espaciado

/**
 * Lo que aparece cuando la semana no se pudo armar.
 *
 * Nunca es solo el motivo: cada caso ofrece la acción que resuelve el problema sin
 * volver a empezar. En presupuesto insuficiente esa acción trae el número exacto que
 * sí alcanza, porque "sube el presupuesto" sin decir cuánto obliga al usuario a
 * adivinar y reintentar.
 */
@Composable
internal fun HojaFallo(
    fallo: FalloArmado,
    onContinuarConMinimo: () -> Unit,
    onCambiarRespuestas: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // MutableTransitionState arranca en false y se pone en true en la primera
    // composición: es lo que hace que la hoja entre deslizándose la primera vez que
    // se muestra, en vez de aparecer ya puesta.
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }

    AnimatedVisibility(
        visibleState = visible,
        enter = slideInVertically(
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
            initialOffsetY = { it },
        ) + fadeIn(),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.padding(Espaciado.paddingTarjeta),
                verticalArrangement = Arrangement.spacedBy(Espaciado.s),
            ) {
                when (fallo) {
                    is FalloArmado.Presupuesto -> ContenidoPresupuesto(fallo, onContinuarConMinimo)
                    is FalloArmado.SinRecetas -> ContenidoSinRecetas(fallo)
                }

                BotonSecundario(
                    texto = "Cambiar mis respuestas",
                    onClick = onCambiarRespuestas,
                    iconoInicial = IconosCookify.flechaAtras,
                )
            }
        }
    }
}

@Composable
private fun ContenidoPresupuesto(
    fallo: FalloArmado.Presupuesto,
    onContinuarConMinimo: () -> Unit,
) {
    val faltante = fallo.minimoClp - fallo.presupuestoClp

    Encabezado(
        icono = IconosCookify.billetera,
        titulo = "Te faltan ${faltante.aPesos()}",
        mensaje = "Con ${fallo.presupuestoClp.aPesos()} no alcanza para esos almuerzos. " +
            "La semana más barata que podemos armarte cuesta ${fallo.minimoClp.aPesos()}.",
    )

    BotonPrimario(
        texto = "Armar igual por ${fallo.minimoClp.aPesos()}",
        onClick = onContinuarConMinimo,
    )
}

@Composable
private fun ContenidoSinRecetas(fallo: FalloArmado.SinRecetas) {
    val disponibles = if (fallo.disponibles == 1) "1 receta" else "${fallo.disponibles} recetas"

    Encabezado(
        icono = IconosCookify.plato,
        titulo = "Nos faltan recetas",
        mensaje = "${fallo.motivo} Solo tenemos $disponibles que te sirvan y necesitamos " +
            "${fallo.diasPedidos}. Prueba con menos días o soltando alguna restricción.",
    )
}

@Composable
private fun Encabezado(icono: Int, titulo: String, mensaje: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Espaciado.s)) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.errorContainer,
        ) {
            Box(Modifier.padding(Espaciado.s)) {
                IconoCookify(
                    icono = icono,
                    descripcion = null,
                    modifier = Modifier.size(TAMANO_ICONO_FALLO.dp),
                    tinte = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val TAMANO_ICONO_FALLO = 24

/** Solo para el preview: cuatro personas de lunes a viernes, el caso tipico. */
private const val PERSONAS_PREVIEW = 4
private const val DIAS_PREVIEW = 5

@Preview
@Composable
private fun HojaPresupuestoPreview() {
    val solicitud = SolicitudPlan(
        supermercado = Supermercado.JUMBO,
        personas = PERSONAS_PREVIEW,
        dias = DiaSemana.entries.take(DIAS_PREVIEW),
        presupuestoClp = 30_000,
        antojos = emptySet(),
        restriccion = Restriccion.NINGUNA,
        artefactos = setOf(Artefacto.ESTUFA),
    )
    CookifyTheme {
        Box(Modifier.padding(Espaciado.m), contentAlignment = Alignment.BottomCenter) {
            HojaFallo(
                fallo = FalloArmado.Presupuesto(
                    presupuestoClp = 30_000,
                    minimoClp = 42_300,
                    solicitudMinima = solicitud.copy(presupuestoClp = 42_300),
                    semilla = 1L,
                ),
                onContinuarConMinimo = {},
                onCambiarRespuestas = {},
            )
        }
    }
}
