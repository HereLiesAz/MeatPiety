package com.hereliesaz.meatpiety

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

/*
 * Two long views built on the same market model as the ledger.
 *
 * Extinction outlook: abstainers grow at [ABSTAINER_GROWTH] a year until the
 * whole world abstains. Each year's demand shift sets the herd through the
 * long-run equilibrium (herd ∝ (1 − shift)^(es / (es + ed))). Commercial
 * breeding ends when the price has fallen past the producer margin
 * (Species.breakevenDrop) relative to today. After that the species lives on
 * only in its non-commercial reservoir: wild ancestors, feral, heritage and
 * sanctuary animals. Farmed breeds go; the species only goes if that
 * reservoir is too small to persist (under ~5,000, Traill et al. 2007).
 *
 * Soy and tofu: most soy feeds animals; the animals you didn't eat free their
 * feed, which is shown as tofu it could have made.
 */

/** Standing global populations (FAOSTAT live animals, 2021–2023, rounded). */
internal val GLOBAL_HERD = mapOf(
    "Chickens" to 18.8e9, // 27.2 bn chickens minus 8.4 bn laying hens
    "Egg-Laying Hens" to 8.4e9,
    "Turkeys" to 0.45e9,
    "Ducks" to 1.2e9,
    "Pigs" to 0.78e9,
    "Cows" to 1.28e9, // 1.55 bn cattle minus ~0.27 bn dairy cows
    "Dairy Cows" to 0.27e9,
    "Sheep & Lambs" to 1.29e9,
    "Buffalo" to 0.4e6, // North American commercial bison
)

/** What survives without the market, and roughly how many (order of magnitude). */
internal val RESERVOIRS = mapOf(
    "Chickens" to ("red junglefowl, backyard and sanctuary flocks" to 1e8),
    "Egg-Laying Hens" to ("red junglefowl, backyard and sanctuary flocks" to 1e8),
    "Turkeys" to ("wild turkeys (North America)" to 7e6),
    "Ducks" to ("wild mallards" to 2e7),
    "Pigs" to ("wild boar and feral pigs" to 2e7),
    "Cows" to ("heritage herds and sanctuaries; the wild aurochs is already extinct" to 1e5),
    "Dairy Cows" to ("heritage herds and sanctuaries; the wild aurochs is already extinct" to 1e5),
    "Sheep & Lambs" to ("mouflon and feral flocks" to 1e6),
    "Buffalo" to ("conservation herds (plains and wood bison)" to 3.1e4),
)

internal const val MINIMUM_VIABLE = 5_000.0

data class ExtinctionOutlook(
    val species: Species,
    val herdNow: Double,
    /** Years from now until commercial breeding stops, or null within [horizon]. */
    val yearsToCommercialEnd: Int?,
    val herdAtEnd: Double?,
    val reservoir: String,
    val reservoirSize: Double,
) {
    val speciesSurvives get() = reservoirSize >= MINIMUM_VIABLE
}

private fun Species.shiftIn(years: Int, growth: Double): Double {
    val abstainers = (if (requiresVeganism) VEGANS_WORLDWIDE else VEGANS_WORLDWIDE + VEGETARIANS_WORLDWIDE) * (1 + growth).pow(years)
    return (abstainers / WORLD_POPULATION).coerceIn(0.0, 0.99)
}

private fun Species.lnPrice(shift: Double) = ln(1 - shift) / (supplyElasticity + demandElasticity)

fun extinctionOutlook(growth: Double = ABSTAINER_GROWTH, horizon: Int = 200): List<ExtinctionOutlook> =
    SPECIES.mapNotNull { s ->
        val herd = GLOBAL_HERD[s.name] ?: return@mapNotNull null
        val (reservoir, size) = RESERVOIRS.getValue(s.name)
        val lnP0 = s.lnPrice(s.shiftIn(0, growth))
        val end = (1..horizon).firstOrNull { t -> 1 - exp(s.lnPrice(s.shiftIn(t, growth)) - lnP0) >= s.breakevenDrop }
        val herdAt = { t: Int -> herd * exp(s.supplyElasticity * (s.lnPrice(s.shiftIn(t, growth)) - lnP0)) }
        ExtinctionOutlook(s, herd, end, end?.let(herdAt), reservoir, size)
    }

/** Soy fed per kg of product (approximate; WWF soy-footprint estimates). */
internal val SOY_KG_PER_KG = mapOf(
    "Chickens" to 0.58, "Turkeys" to 0.58, "Ducks" to 0.58,
    "Egg-Laying Hens" to 0.35, "Pigs" to 0.26, "Cows" to 0.2, "Buffalo" to 0.2,
    "Dairy Cows" to 0.03, "Sheep & Lambs" to 0.05, "Fish" to 0.3, "Shellfish" to 0.3,
)

// About 77% of the world's soy feeds animals (Our World in Data, 2021).
internal const val SOY_FED_TO_ANIMALS = 0.77
// Firm tofu yield per kg of dry soybeans.
internal const val TOFU_KG_PER_SOY_KG = 2.0
// Global tofu output estimates disagree widely (about 2–5 Mt); 3 Mt is a midpoint.
internal const val GLOBAL_TOFU_KG = 3e9

data class SoyLedger(val soyFreedKg: Double) {
    val tofuKg get() = soyFreedKg * TOFU_KG_PER_SOY_KG
    /** Share of a year's world tofu that freed feed could make. */
    val shareOfWorldTofu get() = tofuKg / GLOBAL_TOFU_KG
}

/** Feed soy no longer grown for the animals this layer stopped producing. */
val Layer.soy: SoyLedger
    get() = SoyLedger(impacts.filterNot { it.species.existsBecauseOfDemand }.sumOf { i ->
        val kg = FOOTPRINTS[i.species.name]?.let { i.netUnits * it.kgPerUnit } ?: 0.0
        kg * (SOY_KG_PER_KG[i.species.name] ?: 0.0)
    })
