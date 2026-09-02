package com.app.cookify.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Tema de Cookify.
 *
 * Sin color dinamico (Material You) a proposito: la identidad oscura con acento
 * ambar es parte del producto, y dejar que el sistema la reemplace por el fondo de
 * pantalla del usuario haria que la app se viera distinta en cada telefono.
 *
 * [oscuro] queda expuesto para previews y para un eventual selector de tema; por
 * defecto la app siempre es oscura, sin importar el ajuste del sistema.
 */
@Composable
fun CookifyTheme(
    oscuro: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
        typography = TipografiaCookify,
        shapes = FormasCookify,
        content = content,
    )
}
