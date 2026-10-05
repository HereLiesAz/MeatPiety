package com.hereliesaz.meatpiety

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// Earth palette: forest-floor dark, sage and moss greens, oat-paper text.
// Terracotta is reserved for the one line that runs backward (buffalo) and
// for loss; everything that was spared reads green.
object Piety {
    val Soil = Color(0xFF0D120E)
    val Loam = Color(0xFF141B16)
    val Bark = Color(0xFF1D2620)
    val Moss = Color(0xFF3E5A3A)
    val Fern = Color(0xFF6E9A5E)
    val Sage = Color(0xFFA8C69A)
    val Sprout = Color(0xFFD6E8B8)
    val Oat = Color(0xFFEDE6D6)
    val Lichen = Color(0xFF9AA595)
    val Clay = Color(0xFFC8734F)
    val Rule = Color(0xFF2C372F)
}

internal val PietyColors = darkColorScheme(
    primary = Piety.Sage,
    onPrimary = Piety.Soil,
    secondary = Piety.Fern,
    tertiary = Piety.Clay,
    background = Piety.Soil,
    onBackground = Piety.Oat,
    surface = Piety.Loam,
    onSurface = Piety.Oat,
    surfaceVariant = Piety.Bark,
    onSurfaceVariant = Piety.Lichen,
    outline = Piety.Rule,
    outlineVariant = Piety.Moss,
)
