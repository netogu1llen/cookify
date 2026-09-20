package com.app.cookify.core.ui.componentes

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.cookify.core.model.BasePrincipal
import com.app.cookify.core.ui.R

/**
 * La imagen de un plato, por categoria.
 *
 * No son fotos. Sesenta y nueve fotos con licencia para un catalogo que crece es un
 * problema que no se termina nunca, y una foto generica de internet se nota mas que
 * un dibujo honesto. Cada receta declara su [BasePrincipal], asi que doce glifos
 * cubren el catalogo completo y cualquier receta nueva ya nace con imagen.
 *
 * El color tambien informa: la parrilla de la semana se puede recorrer de un vistazo
 * y ver que no hay tres platos de pollo seguidos, sin leer los nombres.
 */
@Composable
fun IlustracionPlato(
    base: BasePrincipal,
    modifier: Modifier = Modifier,
    tamano: Dp = TAMANO_NORMAL.dp,
) {
    val color = colorDe(base)

    Surface(
        modifier = modifier.size(tamano),
        shape = MaterialTheme.shapes.medium,
        // El fondo es el mismo color del glifo casi transparente: se tine del tono de la
        // categoria sin competir con el nombre del plato, que es lo que hay que leer.
        color = color.copy(alpha = ALFA_FONDO),
    ) {
        Box(contentAlignment = Alignment.Center) {
            IconoCookify(
                icono = glifoDe(base),
                // La categoria ya se deduce del nombre del plato que va al lado.
                descripcion = null,
                modifier = Modifier.size(tamano * PROPORCION_GLIFO),
                tinte = color,
            )
        }
    }
}

@DrawableRes
private fun glifoDe(base: BasePrincipal): Int = when (base) {
    BasePrincipal.POLLO -> R.drawable.core_ui_ic_plato_pollo
    BasePrincipal.VACUNO -> R.drawable.core_ui_ic_plato_vacuno
    BasePrincipal.CERDO -> R.drawable.core_ui_ic_plato_cerdo
    // Se reusa el mismo pescado que marca la restriccion "Solo pescado": dos pescados
    // distintos en la misma app se leerian como dos cosas distintas.
    BasePrincipal.PESCADO -> R.drawable.core_ui_ic_pescado
    BasePrincipal.MARISCO -> R.drawable.core_ui_ic_plato_marisco
    BasePrincipal.HUEVO -> R.drawable.core_ui_ic_plato_huevo
    BasePrincipal.LEGUMBRE -> R.drawable.core_ui_ic_plato_legumbre
    BasePrincipal.VERDURA -> R.drawable.core_ui_ic_plato_verdura
    BasePrincipal.PASTA -> R.drawable.core_ui_ic_plato_pasta
    BasePrincipal.ARROZ -> R.drawable.core_ui_ic_plato_arroz
    BasePrincipal.QUESO -> R.drawable.core_ui_ic_plato_queso
    BasePrincipal.TOFU -> R.drawable.core_ui_ic_plato_tofu
}

/*
 * Un tono por categoria, todos claros porque van sobre fondo oscuro. Los tres amarillos
 * -pollo, huevo y queso- estan separados a proposito hacia el naranjo, el limon y el
 * oro: la diferencia real la hace el dibujo, pero puestos uno sobre otro en la lista de
 * la semana tres amarillos iguales se leen como el mismo plato repetido.
 */
private fun colorDe(base: BasePrincipal): Color = when (base) {
    BasePrincipal.POLLO -> NARANJO_POLLO
    BasePrincipal.VACUNO -> ROJO_VACUNO
    BasePrincipal.CERDO -> ROSA_CERDO
    BasePrincipal.PESCADO -> AZUL_PESCADO
    BasePrincipal.MARISCO -> CORAL_MARISCO
    BasePrincipal.HUEVO -> AMARILLO_HUEVO
    BasePrincipal.LEGUMBRE -> CAFE_LEGUMBRE
    BasePrincipal.VERDURA -> VERDE_VERDURA
    BasePrincipal.PASTA -> TRIGO_PASTA
    BasePrincipal.ARROZ -> ARENA_ARROZ
    BasePrincipal.QUESO -> ORO_QUESO
    BasePrincipal.TOFU -> SALVIA_TOFU
}

private val NARANJO_POLLO = Color(0xFFD98F45)
private val ROJO_VACUNO = Color(0xFFD2685E)
private val ROSA_CERDO = Color(0xFFE09BAE)
private val AZUL_PESCADO = Color(0xFF63B9D8)
private val CORAL_MARISCO = Color(0xFFE8896B)
private val AMARILLO_HUEVO = Color(0xFFF0CE5C)
private val CAFE_LEGUMBRE = Color(0xFFC08A5E)
private val VERDE_VERDURA = Color(0xFF7FC26A)
private val TRIGO_PASTA = Color(0xFFDFB579)
private val ARENA_ARROZ = Color(0xFFC9C3B6)
private val ORO_QUESO = Color(0xFFE0A82E)
private val SALVIA_TOFU = Color(0xFFA9BFAE)

private const val TAMANO_NORMAL = 56
private const val ALFA_FONDO = 0.16f
private const val PROPORCION_GLIFO = 0.58f
