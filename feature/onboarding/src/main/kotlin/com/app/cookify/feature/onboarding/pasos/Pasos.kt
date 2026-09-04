package com.app.cookify.feature.onboarding.pasos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.cookify.core.common.aPesos
import com.app.cookify.core.model.Antojo
import com.app.cookify.core.model.Artefacto
import com.app.cookify.core.model.DiaSemana
import com.app.cookify.core.model.Restriccion
import com.app.cookify.core.model.SolicitudPlan
import com.app.cookify.core.model.Supermercado
import com.app.cookify.core.ui.componentes.ChipAntojo
import com.app.cookify.core.ui.componentes.EncabezadoPaso
import com.app.cookify.core.ui.componentes.IconoCookify
import com.app.cookify.core.ui.componentes.IconosCookify
import com.app.cookify.core.ui.componentes.TarjetaOpcion
import com.app.cookify.core.ui.theme.Espaciado

@Composable
internal fun PasoSupermercado(
    seleccionado: Supermercado?,
    onElegir: (Supermercado) -> Unit,
    modifier: Modifier = Modifier,
) {
    ColumnaPaso(modifier) {
        EncabezadoPaso(
            titulo = "¿En qué súper compras?",
            ayuda = "Los precios cambian bastante entre cadenas.",
        )
        Supermercado.entries.forEach { super_ ->
            TarjetaOpcion(
                titulo = super_.etiqueta,
                seleccionada = seleccionado == super_,
                onClick = { onElegir(super_) },
                icono = IconosCookify.tienda,
            )
        }
    }
}

@Composable
internal fun PasoPersonas(
    personas: Int,
    onCambiar: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    ColumnaPaso(modifier) {
        EncabezadoPaso(
            titulo = "¿Para cuántas personas cocinas?",
            ayuda = "Con esto escalamos las cantidades de cada receta.",
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BotonPaso(
                    icono = IconosCookify.menos,
                    descripcion = "Quitar una persona",
                    habilitado = personas > SolicitudPlan.RANGO_PERSONAS.first,
                    onClick = { onCambiar(-1) },
                )
                // Monospace para que el numero no salte de ancho al pasar de 9 a 10.
                Text(
                    text = personas.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                BotonPaso(
                    icono = IconosCookify.mas,
                    descripcion = "Agregar una persona",
                    habilitado = personas < SolicitudPlan.RANGO_PERSONAS.last,
                    onClick = { onCambiar(1) },
                )
            }
        }
        Text(
            text = if (personas == 1) "Un plato por día" else "$personas platos por día",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun PasoDias(
    elegidos: Set<DiaSemana>,
    onAlternar: (DiaSemana) -> Unit,
    modifier: Modifier = Modifier,
) {
    ColumnaPaso(modifier) {
        EncabezadoPaso(
            titulo = "¿Qué días vas a cocinar?",
            ayuda = "Elige los días que quieras; el resto los dejamos libres.",
        )
        DiaSemana.entries.forEach { dia ->
            TarjetaOpcion(
                titulo = dia.nombre,
                seleccionada = dia in elegidos,
                onClick = { onAlternar(dia) },
                icono = IconosCookify.calendario,
                multiple = true,
            )
        }
    }
}

@Composable
internal fun PasoPresupuesto(
    presupuesto: Int?,
    onCambiar: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    ColumnaPaso(modifier) {
        EncabezadoPaso(
            titulo = "¿Cuánto quieres gastar?",
            ayuda = "Es el total de la semana, para todas las personas.",
        )

        Text(
            text = presupuesto?.aPesos() ?: "$0",
            style = MaterialTheme.typography.displayMedium,
            color = if (presupuesto == null) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
        )

        OutlinedTextField(
            value = presupuesto?.toString().orEmpty(),
            onValueChange = { texto ->
                val digitos = texto.filter(Char::isDigit).take(MAX_DIGITOS_PRESUPUESTO)
                onCambiar(digitos.toIntOrNull())
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Presupuesto en pesos") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = MaterialTheme.shapes.medium,
        )

        Text(
            text = "Sugerencias",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
            SUGERENCIAS_PRESUPUESTO.forEach { monto ->
                ChipAntojo(
                    texto = monto.aPesos(),
                    seleccionado = presupuesto == monto,
                    onClick = { onCambiar(monto) },
                )
            }
        }
    }
}

@Composable
internal fun PasoAntojos(
    elegidos: Set<Antojo>,
    topeAlcanzado: Boolean,
    onAlternar: (Antojo) -> Unit,
    modifier: Modifier = Modifier,
) {
    ColumnaPaso(modifier) {
        EncabezadoPaso(
            titulo = "¿Qué se te antoja?",
            ayuda = "Elige hasta ${Antojo.MAXIMO_SELECCION}. También puedes seguir sin elegir ninguno.",
        )
        Text(
            text = "${elegidos.size} de ${Antojo.MAXIMO_SELECCION}",
            style = MaterialTheme.typography.labelLarge,
            color = if (topeAlcanzado) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Espaciado.xs),
            verticalArrangement = Arrangement.spacedBy(Espaciado.xs),
        ) {
            Antojo.entries.forEach { antojo ->
                val seleccionado = antojo in elegidos
                ChipAntojo(
                    texto = antojo.etiqueta,
                    seleccionado = seleccionado,
                    onClick = { onAlternar(antojo) },
                    habilitado = seleccionado || !topeAlcanzado,
                    icono = iconoDe(antojo),
                )
            }
        }
    }
}

@Composable
internal fun PasoRestriccion(
    seleccionada: Restriccion?,
    onElegir: (Restriccion) -> Unit,
    modifier: Modifier = Modifier,
) {
    ColumnaPaso(modifier) {
        EncabezadoPaso(
            titulo = "¿Alguna restricción?",
            ayuda = "Filtramos el recetario completo según lo que elijas.",
        )
        Restriccion.entries.forEach { restriccion ->
            TarjetaOpcion(
                titulo = restriccion.etiqueta,
                seleccionada = seleccionada == restriccion,
                onClick = { onElegir(restriccion) },
                icono = if (restriccion == Restriccion.NINGUNA) {
                    IconosCookify.plato
                } else {
                    IconosCookify.hoja
                },
            )
        }
    }
}

@Composable
internal fun PasoArtefactos(
    elegidos: Set<Artefacto>,
    onAlternar: (Artefacto) -> Unit,
    modifier: Modifier = Modifier,
) {
    ColumnaPaso(modifier) {
        EncabezadoPaso(
            titulo = "¿Con qué cocinas?",
            ayuda = "Solo te proponemos recetas que puedas hacer de verdad.",
        )
        Artefacto.entries.forEach { artefacto ->
            TarjetaOpcion(
                titulo = artefacto.etiqueta,
                seleccionada = artefacto in elegidos,
                onClick = { onAlternar(artefacto) },
                icono = IconosCookify.horno,
                multiple = true,
            )
        }
        if (elegidos.isEmpty()) {
            Text(
                text = "Elige al menos uno para poder armar la semana.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ColumnaPaso(
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Espaciado.s),
    ) {
        contenido()
    }
}

@Composable
private fun BotonPaso(
    icono: Int,
    descripcion: String,
    habilitado: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = habilitado,
        modifier = Modifier.size(Espaciado.objetivoTactil + 8.dp),
    ) {
        IconoCookify(
            icono = icono,
            descripcion = descripcion,
            tinte = if (habilitado) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        )
    }
}

private fun iconoDe(antojo: Antojo): Int = when (antojo) {
    Antojo.RAPIDO -> IconosCookify.rayo
    Antojo.BAJO_CALORIAS -> IconosCookify.balanza
    Antojo.PARA_TODOS -> IconosCookify.personas
    Antojo.SANO -> IconosCookify.hoja
    Antojo.TAKEAWAY -> IconosCookify.plato
    Antojo.GUT_FRIENDLY -> IconosCookify.corazon
    Antojo.ALTO_PROTEINA -> IconosCookify.balanza
}

private const val MAX_DIGITOS_PRESUPUESTO = 7

private val SUGERENCIAS_PRESUPUESTO = listOf(20_000, 35_000, 50_000, 70_000)
