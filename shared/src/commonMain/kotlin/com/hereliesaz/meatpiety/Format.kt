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

/** Signed value in plain or scientific notation; one person's share of a world market is tiny. */
internal fun formatTiny(x: Double): String {
    if (x == 0.0) return "0"
    val sign = if (x < 0) "-" else "+"
    val n = abs(x)
    if (n >= 0.01) return sign + formatOneDecimal(n)
    var exp = 0
    var m = n
    while (m < 1) { m *= 10; exp-- }
    return "$sign${formatOneDecimal(m)}×10^$exp"
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

/**
 * Animals come whole. [net] is an expected count: each purchase skipped
 * shifts the odds that a producer's next order is cut by a whole batch, so
 * a fraction is a share of one animal's probability, not part of a body.
 * Reported as whole animals so far plus the wait for the next one.
 */
internal fun wholeAnimals(net: Double, days: Int): String {
    if (net <= 0.0 || days <= 0) return "none yet"
    val whole = kotlin.math.floor(net).toLong()
    val perDay = net / days
    val toNext = kotlin.math.ceil((whole + 1 - net) / perDay).toLong()
    val wait = when {
        toNext <= 1 -> "the next any day now"
        toNext < 730 -> "the next in $toNext days"
        else -> "the next in ${toNext / 365} years"
    }
    return if (whole == 0L) "${(net * 100).toInt()}% of the first · $wait" else "$whole · $wait"
}
