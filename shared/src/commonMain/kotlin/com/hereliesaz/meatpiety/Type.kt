package com.hereliesaz.meatpiety

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.meatpiety.resources.Res
import com.hereliesaz.meatpiety.resources.azrienoch
import org.jetbrains.compose.resources.Font

/**
 * Azrienoch axes: wght 100–900, wdth 75–100, SERF 0–100 (slab foot), GRAD -50–50.
 * Each distinct axis combination is its own Font instance, so callers quantize
 * animated values (see [quantize]) to keep the instance count small.
 */
data class Axes(val wght: Int = 400, val wdth: Int = 100, val serf: Int = 0, val grad: Int = 0)

internal fun quantize(value: Float, step: Int): Int = (kotlin.math.round(value / step) * step).toInt()

@Composable
fun azrienoch(axes: Axes): FontFamily {
    val font = Font(
        resource = Res.font.azrienoch,
        weight = FontWeight(axes.wght.coerceIn(100, 900)),
        variationSettings = FontVariation.Settings(
            FontVariation.weight(axes.wght.coerceIn(100, 900)),
            FontVariation.width(axes.wdth.coerceIn(75, 100).toFloat()),
            FontVariation.Setting("SERF", axes.serf.coerceIn(0, 100).toFloat()),
            FontVariation.grade(axes.grad.coerceIn(-50, 50)),
        ),
    )
    return remember(font) { FontFamily(font) }
}

@Composable
internal fun pietyTypography(): Typography {
    val light = azrienoch(Axes(wght = 300))
    val book = azrienoch(Axes(wght = 400))
    val medium = azrienoch(Axes(wght = 560))
    val heavy = azrienoch(Axes(wght = 800, wdth = 88))
    // Tabular figures keep animated counters from jittering.
    val tnum = "tnum"
    return Typography(
        displayLarge = TextStyle(fontFamily = heavy, fontSize = 64.sp, lineHeight = 0.95.em, letterSpacing = (-0.03).em, fontFeatureSettings = tnum),
        displayMedium = TextStyle(fontFamily = heavy, fontSize = 44.sp, lineHeight = 1.0.em, letterSpacing = (-0.02).em, fontFeatureSettings = tnum),
        headlineMedium = TextStyle(fontFamily = medium, fontSize = 28.sp, lineHeight = 1.1.em, letterSpacing = (-0.01).em),
        titleLarge = TextStyle(fontFamily = medium, fontSize = 22.sp, lineHeight = 1.2.em),
        titleMedium = TextStyle(fontFamily = medium, fontSize = 16.sp, lineHeight = 1.3.em, letterSpacing = 0.01.em),
        bodyLarge = TextStyle(fontFamily = book, fontSize = 17.sp, lineHeight = 1.5.em, fontFeatureSettings = tnum),
        bodyMedium = TextStyle(fontFamily = book, fontSize = 15.sp, lineHeight = 1.5.em),
        bodySmall = TextStyle(fontFamily = light, fontSize = 13.sp, lineHeight = 1.5.em),
        labelLarge = TextStyle(fontFamily = medium, fontSize = 14.sp, letterSpacing = 0.04.em),
        labelSmall = TextStyle(fontFamily = medium, fontSize = 11.sp, letterSpacing = 0.18.em),
    )
}
