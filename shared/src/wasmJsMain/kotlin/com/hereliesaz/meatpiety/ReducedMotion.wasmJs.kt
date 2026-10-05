package com.hereliesaz.meatpiety

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window

@Composable
actual fun rememberReducedMotion(): Boolean =
    remember { window.matchMedia("(prefers-reduced-motion: reduce)").matches }
