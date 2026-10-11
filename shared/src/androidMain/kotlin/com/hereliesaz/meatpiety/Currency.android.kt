package com.hereliesaz.meatpiety

import java.util.Currency
import java.util.Locale

actual fun deviceCurrencyCode(): String? =
    runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrNull()
