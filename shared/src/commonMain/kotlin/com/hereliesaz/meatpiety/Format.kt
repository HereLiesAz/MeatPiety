package com.hereliesaz.meatpiety

import kotlin.math.abs

// Nine digits always parse as Int, so the field never shows a number the math ignores.
internal const val MAX_INPUT_DIGITS = 9

internal fun formatPercent(fraction: Double): String {
    val tenths = kotlin.math.round(fraction * 1000).toLong()
    val sign = if (tenths < 0) "-" else if (tenths > 0) "+" else ""
    val magnitude = abs(tenths)
    return "$sign${magnitude / 10}.${magnitude % 10}%"
}

internal fun formatPrice(price: Double): String {
    val cents = kotlin.math.round(price * 100).toLong()
    val wholePart = cents / 100
    val centPart = abs(cents % 100)
    val centStr = if (centPart < 10) "0$centPart" else "$centPart"
    return "$$wholePart.$centStr"
}

internal fun format(count: Double): String {
    val n = abs(count)
    return if (n >= 1000) formatWithThousands(n) else formatOneDecimal(n)
}

internal fun formatOneDecimal(n: Double): String {
    val tenths = kotlin.math.round(n * 10).toLong()
    return "${tenths / 10}.${tenths % 10}"
}

internal fun formatWithThousands(n: Double): String {
    val digits = kotlin.math.round(n).toLong().toString()
    val grouped = StringBuilder()
    for ((index, digit) in digits.withIndex()) {
        val remaining = digits.length - index
        if (index != 0 && remaining % 3 == 0) grouped.append(',')
        grouped.append(digit)
    }
    return grouped.toString()
}
