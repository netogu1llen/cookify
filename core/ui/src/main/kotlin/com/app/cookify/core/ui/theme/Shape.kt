package com.app.cookify.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Esquinas generosas: en un tema oscuro sin sombras, el radio es lo que separa una
 * tarjeta del fondo. Se sobrescriben los tokens del tema, no cada componente.
 */
internal val FormasCookify = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
