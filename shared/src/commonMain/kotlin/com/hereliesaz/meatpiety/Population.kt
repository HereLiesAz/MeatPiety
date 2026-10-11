package com.hereliesaz.meatpiety

import kotlin.math.pow

/**
 * World population, UN World Population Prospects 2024, medium variant
 * (estimates to 2023, projections after; rounded to 0.01 bn). It peaks at
 * about 10.3 bn in the mid-2080s and then edges down.
 */
private val WPP_2024 = listOf(
    1990 to 5.33, 2000 to 6.17, 2010 to 6.99, 2020 to 7.89, 2024 to 8.16,
    2030 to 8.55, 2040 to 9.19, 2050 to 9.66, 2060 to 9.98, 2070 to 10.18,
    2080 to 10.29, 2085 to 10.30, 2090 to 10.27, 2100 to 10.18,
)

/** World population in people for a calendar [year], interpolated; flat beyond the table. */
fun worldPopulation(year: Double): Double {
    val first = WPP_2024.first()
    val last = WPP_2024.last()
    if (year <= first.first) return first.second * 1e9
    if (year >= last.first) return last.second * 1e9
    val (a, b) = WPP_2024.zipWithNext().first { (_, hi) -> year <= hi.first }
    val f = (year - a.first) / (b.first - a.first)
    return (a.second + f * (b.second - a.second)) * 1e9
}

/** Calendar year now, fractional, from the device clock. */
internal fun currentYear(): Double = 1970 + todayEpochDay() / 365.2425

/**
 * Meat eaten per person rises slowly with income: OECD-FAO Agricultural
 * Outlook 2023–2032 projects ~2% more per capita over the decade (~0.2%/yr).
 */
internal const val PER_CAPITA_GROWTH = 0.002

/** Size of the meat market [yearsFromNow] away, relative to today: population × per-capita intake. */
fun marketFactor(yearsFromNow: Double): Double {
    val now = currentYear()
    return worldPopulation(now + yearsFromNow) / worldPopulation(now) * (1 + PER_CAPITA_GROWTH).pow(yearsFromNow)
}

/** Average [marketFactor] over the [days] just past, for ledgers that started back then. */
fun pastMarketFactor(days: Int): Double {
    if (days <= 0) return 1.0
    val steps = 12
    return (0 until steps).sumOf { i -> marketFactor(-(i + 0.5) * days / 365.0 / steps) } / steps
}
