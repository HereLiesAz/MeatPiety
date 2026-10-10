package com.hereliesaz.meatpiety

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sin

/**
 * Static, deterministic print erosion. The cached paths are built when the
 * drawing area's size changes, not when animations advance. Apply after a
 * background and before content; never draw texture over text or figures.
 */
fun Modifier.inkPatina(ink: Color = Piety.Oat): Modifier = drawWithCache {
    val spots = Path()
    val scratches = Path()
    val step = max(18.dp.toPx(), max(size.width, size.height) / 160f)
    val columns = (size.width / step).toInt()
    val rows = (size.height / step).toInt()
    for (row in 0..rows) {
        for (column in 0..columns) {
            val hash = (row * 193 + column * 97 + row * column * 31) % 101
            val x = (column + 0.2f + hash % 5 * 0.12f) * step
            val y = (row + 0.15f + hash % 7 * 0.1f) * step
            if (hash % 5 == 0) {
                val radius = (0.3f + hash % 3 * 0.25f).dp.toPx()
                spots.addOval(Rect(x - radius, y - radius, x + radius, y + radius))
            }
            if (hash == 17 || hash == 89) {
                scratches.moveTo(x, y)
                scratches.lineTo(x + step * 0.23f, y - step * 0.16f)
            }
        }
    }
    onDrawBehind {
        drawPath(spots, ink.copy(alpha = 0.085f))
        drawPath(scratches, ink.copy(alpha = 0.12f), style = Stroke(0.65.dp.toPx()))
    }
}

/**
 * Interactive variable-type title. Each glyph's weight, width and slab serif
 * respond to the pointer: drag across the word to sculpt it; release and it
 * breathes back. Axis values are quantized so only a few dozen Font instances exist.
 */
@Composable
fun KineticTitle(text: String, fontSize: TextUnit, color: Color, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    var pointerX by remember { mutableFloatStateOf(Float.NaN) }
    val centres = remember(text) { mutableStateListOf<Float>().apply { repeat(text.length) { add(0f) } } }
    val breath = if (reduced) 0f else rememberInfiniteTransition().animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Restart),
    ).value
    Row(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { pointerX = it.x },
                onDragEnd = { pointerX = Float.NaN },
                onDragCancel = { pointerX = Float.NaN },
            ) { change, _ -> pointerX = change.position.x }
        }.pointerInput(Unit) {
            detectTapGestures(onPress = { pointerX = it.x; tryAwaitRelease(); pointerX = Float.NaN })
        },
    ) {
        text.forEachIndexed { index, ch ->
            val influence = if (pointerX.isNaN()) {
                // Idle: a slow wave of weight travels through the word.
                0.5f + 0.5f * sin(2 * PI * (breath - index / text.length.toFloat())).toFloat()
            } else {
                val d = abs(centres[index] - pointerX) / 90f
                exp(-d * d)
            }
            val target by animateFloatAsState(influence, if (reduced) snap() else tween(220))
            val axes = Axes(
                wght = quantize(200f + 700f * target, 50),
                wdth = quantize(100f - 25f * target, 5),
                serf = quantize(100f * target, 20),
            )
            BasicText(
                text = ch.toString(),
                style = TextStyle(fontFamily = azrienoch(axes), fontSize = fontSize, color = color),
                modifier = Modifier.onGloballyPositioned { centres[index] = it.positionInParent().x + it.size.width / 2f },
            )
        }
    }
}

/** Number that counts toward its target. Tabular figures come from the style. */
@Composable
fun Counter(value: Double, style: TextStyle, color: Color, modifier: Modifier = Modifier, render: (Double) -> String) {
    val reduced = LocalReducedMotion.current
    val shown by animateFloatAsState(value.toFloat(), if (reduced) snap() else tween(1400))
    // Float carries ~7 significant digits; snap to the exact value once the tween lands.
    val display = if (abs(shown - value.toFloat()) < 1e-3f * max(1.0, abs(value)).toFloat()) value else shown.toDouble()
    Text(render(display), style = style, color = color, modifier = modifier)
}

/** Log-scaled bar: counts here span ten orders of magnitude, so linear bars would hide all but one. */
@Composable
fun LogBar(value: Double, max: Double, color: Color, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val reduced = LocalReducedMotion.current
    val fraction = if (max <= 0) 0f else (log10(1 + abs(value)) / log10(1 + max)).toFloat().coerceIn(0f, 1f)
    val grow = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, Gentle) }
    val track = MaterialTheme.colorScheme.outline
    Canvas(modifier.fillMaxWidth().height(height)) {
        val r = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = r)
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0.55f), color)),
            size = Size(size.width * fraction * grow.value, size.height),
            cornerRadius = r,
        )
    }
}

/** One mark per animal (capped), filling in sequence: the count made countable. */
@Composable
fun Pictogram(count: Double, cap: Int, color: Color, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val marks = minOf(cap, kotlin.math.ceil(abs(count)).toInt())
    val fill = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(marks) { if (!reduced) { fill.snapTo(0f); fill.animateTo(1f, tween(1600)) } }
    val empty = MaterialTheme.colorScheme.outline
    Canvas(modifier) {
        val columns = 20
        val cell = size.width / columns
        for (i in 0 until cap) {
            val c = Offset(cell * (i % columns) + cell / 2, cell * (i / columns) + cell / 2)
            val lit = i < marks * fill.value
            drawCircle(if (lit) color else empty, radius = cell * if (lit) 0.32f else 0.18f, center = c)
        }
    }
}

/** Concentric ripples: one person's choice propagating through a social graph. */
@Composable
fun Ripples(rings: Int, color: Color, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val phase = if (reduced) 0f else rememberInfiniteTransition().animateFloat(
        0f, 1f, infiniteRepeatable(tween(3600, easing = LinearEasing)),
    ).value
    Canvas(modifier) {
        val centre = Offset(size.width / 2, size.height / 2)
        val maxR = size.minDimension / 2
        val n = rings.coerceIn(1, 7)
        for (i in 0 until n) {
            val t = ((i + phase) / n) % 1f
            drawCircle(color.copy(alpha = (1f - t) * 0.8f), radius = maxR * t, center = centre, style = Stroke(1.5.dp.toPx()))
        }
        drawCircle(color, radius = 5.dp.toPx(), center = centre)
    }
}

/**
 * Hero backdrop: layered hills and grass blades at different depths. [scroll]
 * is the hero's scroll offset in px; each layer moves at its own rate.
 */
@Composable
fun FieldBackdrop(scroll: () -> Float, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val sway = if (reduced) 0f else rememberInfiniteTransition().animateFloat(
        0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing)),
    ).value
    Canvas(modifier) {
        val s = if (reduced) 0f else scroll()
        drawRect(Brush.verticalGradient(listOf(Piety.Bark, Piety.Soil)))
        // The existing sun remains the slowest layer of the landscape.
        drawCircle(Piety.Sprout.copy(alpha = 0.10f), radius = size.width * 0.28f, center = Offset(size.width * 0.72f, size.height * 0.32f + s * 0.15f))
        // Engraved halo / registration marks: an icon motif, not a data signal.
        val seal = Offset(size.width * 0.58f, size.height * 0.36f + s * 0.15f)
        val radius = size.minDimension * 0.46f
        drawCircle(Piety.Gold.copy(alpha = 0.31f), radius, seal, style = Stroke(1.7.dp.toPx()))
        drawCircle(Piety.Gold.copy(alpha = 0.14f), radius * 0.78f, seal, style = Stroke(0.8.dp.toPx()))
        drawLine(
            Piety.Gold.copy(alpha = 0.17f),
            Offset(seal.x, seal.y - radius * 1.16f), Offset(seal.x, seal.y + radius * 1.16f),
            strokeWidth = 0.8.dp.toPx(),
        )
        drawLine(
            Piety.Gold.copy(alpha = 0.14f),
            Offset(seal.x - radius * 1.16f, seal.y), Offset(seal.x + radius * 1.16f, seal.y),
            strokeWidth = 0.8.dp.toPx(),
        )
        val layers = listOf(0.25f to Piety.Moss.copy(alpha = 0.35f), 0.45f to Piety.Moss.copy(alpha = 0.6f), 0.7f to Piety.Loam)
        layers.forEachIndexed { i, (rate, colour) ->
            val base = size.height * (0.62f + i * 0.1f) + s * rate
            val path = Path().apply {
                moveTo(0f, size.height)
                lineTo(0f, base)
                val steps = 24
                for (k in 0..steps) {
                    val x = size.width * k / steps
                    val y = base - size.height * 0.05f * sin((k / steps.toFloat()) * 2 * PI * (1.2 + i * 0.4) + i).toFloat()
                    lineTo(x, y)
                }
                lineTo(size.width, size.height)
                close()
            }
            drawPath(path, colour)
        }
        // Foreground grass blades sway and move fastest.
        val blades = 60
        for (b in 0 until blades) {
            val x = size.width * (b + 0.5f) / blades
            val h = size.height * (0.08f + 0.06f * ((b * 37) % 11) / 10f)
            val lean = 10.dp.toPx() * sin(2 * PI * (sway + b / 13.0)).toFloat()
            val baseY = size.height + s * 0.9f
            drawLine(Piety.Fern.copy(alpha = 0.7f), Offset(x, baseY), Offset(x + lean, baseY - h), strokeWidth = 2.dp.toPx())
        }
    }
}


/** Linear 0..1 bar for values that share one order of magnitude. */
@Composable
fun LinearBar(fraction: Double, color: Color, modifier: Modifier = Modifier, height: Dp = 4.dp) {
    val reduced = LocalReducedMotion.current
    val grow = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, Gentle) }
    val track = MaterialTheme.colorScheme.outline
    Canvas(modifier.fillMaxWidth().height(height)) {
        val r = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = r)
        drawRoundRect(color, size = Size(size.width * fraction.toFloat().coerceIn(0f, 1f) * grow.value, size.height), cornerRadius = r)
    }
}
