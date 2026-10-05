package com.hereliesaz.meatpiety

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** True when the platform asks for reduced motion; parallax, loops and staggers then snap. */
val LocalReducedMotion = compositionLocalOf { false }

/** Android: animator duration scale 0. Web: prefers-reduced-motion. */
@Composable
expect fun rememberReducedMotion(): Boolean

internal val Gentle = spring<Float>(dampingRatio = 0.82f, stiffness = Spring.StiffnessVeryLow)

/**
 * Scroll-triggered reveal. LazyColumn composes an item as it enters the
 * viewport, so animating on first composition is the reveal trigger.
 */
@Composable
fun Modifier.reveal(delayMillis: Int = 0): Modifier {
    val reduced = LocalReducedMotion.current
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) {
            kotlinx.coroutines.delay(delayMillis.toLong())
            progress.animateTo(1f, Gentle)
        }
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 48.dp.toPx()
        val s = 0.96f + 0.04f * progress.value
        scaleX = s
        scaleY = s
    }
}

/**
 * Parallax for a keyed LazyColumn item: shifts content by [rate] × its distance
 * from the viewport centre. Read inside graphicsLayer so scrolling never recomposes.
 */
fun Modifier.parallax(state: LazyListState, key: Any, rate: Float, enabled: Boolean = true): Modifier =
    if (!enabled) this else graphicsLayer {
        val info = state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return@graphicsLayer
        val viewportCentre = (state.layoutInfo.viewportStartOffset + state.layoutInfo.viewportEndOffset) / 2f
        val itemCentre = info.offset + info.size / 2f
        translationY = (itemCentre - viewportCentre) * rate
    }
