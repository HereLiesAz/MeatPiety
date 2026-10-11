package com.hereliesaz.meatpiety

import kotlin.math.abs
import kotlin.math.round

/**
 * The user's national currency. All prices are U.S. figures in USD converted
 * at [perUsd]; they are not local shelf prices.
 */
data class Money(val code: String, val perUsd: Double) {
    fun format(usd: Double): String {
        val v = usd * perUsd
        val decimals = if (code in ZERO_DECIMAL) 0 else 2
        val symbol = SYMBOLS[code]
        val body = if (decimals == 0) formatWithThousands(abs(v)) else twoDecimals(abs(v))
        val sign = if (v < 0) "-" else ""
        return if (symbol != null) "$sign$symbol$body" else "$sign$body $code"
    }

    companion object {
        val USD = Money("USD", 1.0)
    }
}

private fun twoDecimals(n: Double): String {
    val cents = round(n * 100).toLong()
    val whole = if (cents / 100 >= 1000) formatWithThousands((cents / 100).toDouble()) else "${cents / 100}"
    val c = cents % 100
    return "$whole.${if (c < 10) "0$c" else "$c"}"
}

private val ZERO_DECIMAL = setOf("JPY", "KRW", "VND", "CLP", "ISK", "HUF", "IDR", "COP", "PYG", "UGX")

private val SYMBOLS = mapOf(
    "USD" to "$", "EUR" to "€", "GBP" to "£", "JPY" to "¥", "CNY" to "¥", "INR" to "₹",
    "KRW" to "₩", "BRL" to "R$", "CAD" to "CA$", "AUD" to "A$", "NZD" to "NZ$", "MXN" to "MX$",
    "RUB" to "₽", "TRY" to "₺", "ILS" to "₪", "NGN" to "₦", "PHP" to "₱", "VND" to "₫",
    "UAH" to "₴", "THB" to "฿", "PLN" to "zł", "ZAR" to "R", "CHF" to "CHF ",
)

/** Device locale's currency when the platform knows it; null defers to the worker's guess. */
expect fun deviceCurrencyCode(): String?
