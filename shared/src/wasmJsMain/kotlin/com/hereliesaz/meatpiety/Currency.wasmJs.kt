package com.hereliesaz.meatpiety

// Browsers don't expose a locale's currency; the worker infers it from the
// visitor's country instead.
actual fun deviceCurrencyCode(): String? = null
