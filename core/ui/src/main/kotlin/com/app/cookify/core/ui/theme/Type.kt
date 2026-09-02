package com.app.cookify.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * Jerarquia deliberadamente corta: cuatro tamanos de contenido y dos pesos.
 * Mas variantes que eso no crean jerarquia, la disuelven.
 *
 * displayLarge queda reservado para numeros grandes (presupuesto, comensales) y va
 * en monospace: los digitos de ancho fijo evitan que el numero "salte" al cambiar
 * de 9 a 10 en un stepper.
 */
private val Titulo = FontWeight.SemiBold
private val Cuerpo = FontWeight.Normal

internal val TipografiaCookify = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = Titulo,
        fontSize = 48.sp,
        lineHeight = 52.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = Titulo,
        fontSize = 32.sp,
        lineHeight = 36.sp,
    ),
    headlineLarge = TextStyle(fontWeight = Titulo, fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontWeight = Titulo, fontSize = 24.sp, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontWeight = Titulo, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontWeight = Titulo, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = Titulo, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontWeight = Titulo, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontWeight = Cuerpo, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = Cuerpo, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = Cuerpo, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = Titulo, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = Titulo, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = Titulo, fontSize = 11.sp, lineHeight = 16.sp),
)
