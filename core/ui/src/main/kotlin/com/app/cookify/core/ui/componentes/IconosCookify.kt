package com.app.cookify.core.ui.componentes

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.app.cookify.core.ui.R

/**
 * Catalogo de iconos de la app.
 *
 * Se usan drawables propios en vez de androidx.compose.material.icons: esa libreria
 * arrastra el set completo al APK y el proyecto la tiene vetada. Cualquier icono
 * nuevo se agrega como vector en core/ui/src/main/res/drawable con el prefijo
 * core_ui_ que exige el convention plugin, y se declara aca.
 */
object IconosCookify {
    @DrawableRes val check = R.drawable.core_ui_ic_check
    @DrawableRes val checkCirculo = R.drawable.core_ui_ic_check_circle
    @DrawableRes val flechaAtras = R.drawable.core_ui_ic_flecha_atras
    @DrawableRes val chevron = R.drawable.core_ui_ic_chevron
    @DrawableRes val cerrar = R.drawable.core_ui_ic_cerrar
    @DrawableRes val mas = R.drawable.core_ui_ic_mas
    @DrawableRes val menos = R.drawable.core_ui_ic_menos
    @DrawableRes val regenerar = R.drawable.core_ui_ic_regenerar
    @DrawableRes val borrar = R.drawable.core_ui_ic_borrar
    @DrawableRes val reloj = R.drawable.core_ui_ic_reloj
    @DrawableRes val personas = R.drawable.core_ui_ic_personas
    @DrawableRes val tienda = R.drawable.core_ui_ic_tienda
    @DrawableRes val plato = R.drawable.core_ui_ic_plato
    @DrawableRes val rayo = R.drawable.core_ui_ic_rayo
    @DrawableRes val corazon = R.drawable.core_ui_ic_corazon
    @DrawableRes val billetera = R.drawable.core_ui_ic_billetera
    @DrawableRes val calendario = R.drawable.core_ui_ic_calendario
    @DrawableRes val horno = R.drawable.core_ui_ic_horno
    @DrawableRes val hoja = R.drawable.core_ui_ic_hoja
    @DrawableRes val balanza = R.drawable.core_ui_ic_balanza
}

/**
 * Icono de la app.
 *
 * [descripcion] es null solo cuando el icono es decorativo y el texto de al lado ya
 * dice lo mismo; en cualquier otro caso hay que describirlo para TalkBack.
 */
@Composable
fun IconoCookify(
    @DrawableRes icono: Int,
    descripcion: String?,
    modifier: Modifier = Modifier,
    tinte: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Icon(
        painter = painterResource(icono),
        contentDescription = descripcion,
        modifier = modifier,
        tint = tinte,
    )
}
