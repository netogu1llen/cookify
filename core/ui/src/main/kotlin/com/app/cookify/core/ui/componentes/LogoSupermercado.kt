package com.app.cookify.core.ui.componentes

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.app.cookify.core.model.Supermercado
import com.app.cookify.core.ui.R

/**
 * Logo de una cadena, dentro de una baldosa de tamano fijo.
 *
 * La baldosa no es decoracion. Los logos reales no forman un set: el de Jumbo es una
 * letra cuadrada, el de Tottus una grilla de puntos, y los de Lider y aCuenta son
 * palabras casi tres veces mas anchas que altas. Sueltos en una lista se ven de
 * tamanos distintos y desalineados; encajados en la misma caja se leen como opciones
 * comparables, que es lo que son.
 *
 * El fondo va por cadena y no uniforme porque algunos logos no existen sin el suyo:
 * el de Lider es amarillo y blanco, pensado para ir sobre azul, y sobre una baldosa
 * blanca se perderia la mitad del dibujo.
 */
@Composable
fun LogoSupermercado(
    supermercado: Supermercado,
    modifier: Modifier = Modifier,
) {
    val marca = marcaDe(supermercado)

    Surface(
        modifier = modifier.size(width = ANCHO_BALDOSA.dp, height = ALTO_BALDOSA.dp),
        shape = MaterialTheme.shapes.small,
        color = marca.fondo,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (marca.logo != null) {
                Image(
                    painter = painterResource(marca.logo),
                    // El nombre de la cadena ya va escrito al lado en la misma fila.
                    contentDescription = null,
                    modifier = Modifier.padding(RESPIRO.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                IconoCookify(
                    icono = IconosCookify.tienda,
                    descripcion = null,
                    modifier = Modifier.size(TAMANO_RESPALDO.dp),
                    tinte = marca.tinteRespaldo,
                )
            }
        }
    }
}

/**
 * [logo] es null cuando no tenemos el archivo de esa cadena y se cae al icono generico.
 * Es el caso de Unimarc: su sitio responde 403 a cualquier descarga automatica.
 */
private data class MarcaSupermercado(
    @DrawableRes val logo: Int?,
    val fondo: Color,
    val tinteRespaldo: Color = Color.White,
)

private fun marcaDe(supermercado: Supermercado): MarcaSupermercado = when (supermercado) {
    Supermercado.LIDER -> MarcaSupermercado(
        logo = R.drawable.core_ui_logo_lider,
        fondo = AZUL_LIDER,
    )

    Supermercado.ACUENTA -> MarcaSupermercado(
        // El archivo ya trae su propio bloque rojo, asi que la baldosa va del mismo rojo
        // para que no se vea un recuadro dentro de otro.
        logo = R.drawable.core_ui_logo_acuenta,
        fondo = ROJO_ACUENTA,
    )

    Supermercado.TOTTUS -> MarcaSupermercado(
        logo = R.drawable.core_ui_logo_tottus,
        fondo = Color.White,
    )

    Supermercado.UNIMARC -> MarcaSupermercado(
        logo = null,
        fondo = ROJO_UNIMARC,
    )

    Supermercado.SANTA_ISABEL -> MarcaSupermercado(
        logo = R.drawable.core_ui_logo_santa_isabel,
        fondo = Color.White,
    )

    Supermercado.JUMBO -> MarcaSupermercado(
        logo = R.drawable.core_ui_logo_jumbo,
        fondo = Color.White,
    )
}

/*
 * Colores de marca de cada cadena, tomados de sus propios sitios. Solo se usan como
 * fondo de la baldosa del logo: fuera de ahi manda la paleta de la app.
 */
private val AZUL_LIDER = Color(0xFF0071CE)
private val ROJO_ACUENTA = Color(0xFFDB2729)
private val ROJO_UNIMARC = Color(0xFFE4002B)

private const val ANCHO_BALDOSA = 56
private const val ALTO_BALDOSA = 36
private const val RESPIRO = 5
private const val TAMANO_RESPALDO = 20
