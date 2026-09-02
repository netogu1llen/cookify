package com.app.cookify.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Paleta de Cookify. Este es el UNICO archivo donde se escriben colores literales;
 * en los composables siempre se usan los roles de MaterialTheme.colorScheme.
 *
 * Identidad: oscuro premium. Fondo casi negro, profundidad por tono de contenedor
 * (no por sombras), un acento ambar que se reserva para acciones y precios, y un
 * verde lima que marca lo saludable. Los acentos aparecen poco a proposito: si todo
 * es naranjo, nada destaca.
 */

// Ambar: acciones primarias, precios, dia seleccionado.
private val Ambar80 = Color(0xFFFFB68E)
private val Ambar70 = Color(0xFFFF8A4C)
private val Ambar40 = Color(0xFF9C4415)
private val Ambar30 = Color(0xFF7A3208)
private val Ambar20 = Color(0xFF551F00)
private val Ambar10 = Color(0xFF351200)
private val Ambar90 = Color(0xFFFFDBC9)

// Lima: nutricion, tags saludables, confirmaciones.
private val Lima80 = Color(0xFFC6E86B)
private val Lima90 = Color(0xFFDDF98A)
private val Lima40 = Color(0xFF556B00)
private val Lima30 = Color(0xFF3F5000)
private val Lima20 = Color(0xFF2A3600)
private val Lima10 = Color(0xFF171F00)

// Verde agua: tiempo de preparacion y metadatos neutros.
private val Agua80 = Color(0xFF7FD7C4)
private val Agua90 = Color(0xFF9CF3E0)
private val Agua40 = Color(0xFF00695C)
private val Agua30 = Color(0xFF005046)
private val Agua20 = Color(0xFF003730)
private val Agua10 = Color(0xFF00201B)

// Neutros. La escala de contenedores es la que da profundidad en el tema oscuro.
private val NegroFondo = Color(0xFF0B0B0E)
private val NegroMasBajo = Color(0xFF060608)
private val GrisContenedorBajo = Color(0xFF131318)
private val GrisContenedor = Color(0xFF17171D)
private val GrisContenedorAlto = Color(0xFF212128)
private val GrisContenedorMax = Color(0xFF2C2C34)
private val GrisBrillante = Color(0xFF35353E)
private val BlancoTexto = Color(0xFFE9E4E9)
private val GrisTextoSuave = Color(0xFFC9C3CE)
private val GrisBorde = Color(0xFF8F8A97)
private val GrisBordeSuave = Color(0xFF3D3A44)

private val Rojo80 = Color(0xFFFFB4AB)
private val Rojo90 = Color(0xFFFFDAD6)
private val Rojo40 = Color(0xFFBA1A1A)
private val Rojo30 = Color(0xFF93000A)
private val Rojo20 = Color(0xFF690005)
private val Rojo10 = Color(0xFF410002)

/** Tema principal de la app. */
internal val EsquemaOscuro = darkColorScheme(
    primary = Ambar70,
    onPrimary = Ambar10,
    primaryContainer = Ambar30,
    onPrimaryContainer = Ambar90,
    inversePrimary = Ambar40,

    secondary = Lima80,
    onSecondary = Lima10,
    secondaryContainer = Lima20,
    onSecondaryContainer = Lima90,

    tertiary = Agua80,
    onTertiary = Agua10,
    tertiaryContainer = Agua20,
    onTertiaryContainer = Agua90,

    error = Rojo80,
    onError = Rojo20,
    errorContainer = Rojo30,
    onErrorContainer = Rojo90,

    background = NegroFondo,
    onBackground = BlancoTexto,

    surface = NegroFondo,
    onSurface = BlancoTexto,
    surfaceVariant = GrisContenedorAlto,
    onSurfaceVariant = GrisTextoSuave,
    surfaceTint = Ambar70,

    surfaceDim = NegroFondo,
    surfaceBright = GrisBrillante,
    surfaceContainerLowest = NegroMasBajo,
    surfaceContainerLow = GrisContenedorBajo,
    surfaceContainer = GrisContenedor,
    surfaceContainerHigh = GrisContenedorAlto,
    surfaceContainerHighest = GrisContenedorMax,

    inverseSurface = BlancoTexto,
    inverseOnSurface = GrisContenedorBajo,

    outline = GrisBorde,
    outlineVariant = GrisBordeSuave,
    scrim = Color(0xFF000000),

    primaryFixed = Ambar90,
    primaryFixedDim = Ambar80,
    onPrimaryFixed = Ambar10,
    onPrimaryFixedVariant = Ambar30,
    secondaryFixed = Lima90,
    secondaryFixedDim = Lima80,
    onSecondaryFixed = Lima10,
    onSecondaryFixedVariant = Lima30,
    tertiaryFixed = Agua90,
    tertiaryFixedDim = Agua80,
    onTertiaryFixed = Agua10,
    onTertiaryFixedVariant = Agua30,
)

/**
 * Cookify se ve oscuro por decision de marca, pero el esquema claro queda completo:
 * evita que un composable herede colores por defecto si alguna vista se fuerza a
 * claro, y deja la puerta abierta a un selector de tema sin rehacer la paleta.
 */
internal val EsquemaClaro = lightColorScheme(
    primary = Ambar40,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Ambar90,
    onPrimaryContainer = Ambar10,
    inversePrimary = Ambar80,

    secondary = Lima40,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Lima90,
    onSecondaryContainer = Lima10,

    tertiary = Agua40,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Agua90,
    onTertiaryContainer = Agua10,

    error = Rojo40,
    onError = Color(0xFFFFFFFF),
    errorContainer = Rojo90,
    onErrorContainer = Rojo10,

    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF201A17),

    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF201A17),
    surfaceVariant = Color(0xFFF3DFD4),
    onSurfaceVariant = Color(0xFF52443C),
    surfaceTint = Ambar40,

    surfaceDim = Color(0xFFE6D8D1),
    surfaceBright = Color(0xFFFFF8F5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF1EA),
    surfaceContainer = Color(0xFFFAECE4),
    surfaceContainerHigh = Color(0xFFF4E6DF),
    surfaceContainerHighest = Color(0xFFEEE0D9),

    inverseSurface = Color(0xFF362F2B),
    inverseOnSurface = Color(0xFFFBEEE8),

    outline = Color(0xFF85736B),
    outlineVariant = Color(0xFFD7C2B8),
    scrim = Color(0xFF000000),

    primaryFixed = Ambar90,
    primaryFixedDim = Ambar80,
    onPrimaryFixed = Ambar10,
    onPrimaryFixedVariant = Ambar30,
    secondaryFixed = Lima90,
    secondaryFixedDim = Lima80,
    onSecondaryFixed = Lima10,
    onSecondaryFixedVariant = Lima30,
    tertiaryFixed = Agua90,
    tertiaryFixedDim = Agua80,
    onTertiaryFixed = Agua10,
    onTertiaryFixedVariant = Agua30,
)
