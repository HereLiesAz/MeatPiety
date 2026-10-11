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

/**
 * Standing global populations: FAOSTAT QCL, World, 2024 (bulk file
 * Production_Crops_Livestock_E_All_Data). Chickens = stocks minus laying hens
 * (hen eggs, "Laying"); cows = cattle stocks minus dairy cows (raw cattle milk,
 * "Milk Animals"). Bison: ~500,000 commercial in North America (IUCN 2017:
 * ~97% of the continental population is commercial; Canada 149,539 in the
 * 2021 Census of Agriculture).
 */
internal val GLOBAL_HERD = mapOf(
    "Chickens" to 27.68e9 - 8.646e9,
    "Egg-Laying Hens" to 8.646e9,
    "Turkeys" to 0.2435e9,
    "Ducks" to 1.275e9,
    "Pigs" to 0.9624e9,
    "Cows" to 1.5786e9 - 0.2946e9,
    "Dairy Cows" to 0.2946e9,
    "Sheep & Lambs" to 1.3626e9,
    "Buffalo" to 0.5e6,
)

/**
 * What survives without the market and how many, where anyone has counted.
 * Null counts are uncounted, not zero.
 */
internal val RESERVOIRS: Map<String, Pair<String, Double?>> = mapOf(
    // BirdLife/IUCN: Least Concern, decreasing, population not quantified.
    "Chickens" to ("red junglefowl (Least Concern, uncounted), backyard and sanctuary flocks" to null),
    "Egg-Laying Hens" to ("red junglefowl (Least Concern, uncounted), backyard and sanctuary flocks" to null),
    // NWTF, 2016.
    "Turkeys" to ("wild turkeys in North America" to 6.0e6),
    // USFWS Waterfowl Population Status 2025, survey area only.
    "Ducks" to ("wild mallards (surveyed North American breeding population alone)" to 6.6e6),
    // ENETWILD 2025 (Europe, 13.5–19.6 M, low end) + USDA APHIS (US feral swine, >6 M).
    "Pigs" to ("wild boar in Europe and feral swine in the US" to 19.5e6),
    "Cows" to ("heritage herds and sanctuaries (uncounted); the wild aurochs is already extinct" to null),
    "Dairy Cows" to ("heritage herds and sanctuaries (uncounted); the wild aurochs is already extinct" to null),
    // IUCN 2020: Near Threatened; no global count.
    "Sheep & Lambs" to ("mouflon (Near Threatened, uncounted) and feral flocks" to null),
    // IUCN 2017: ~31,000 in 68 conservation herds.
    "Buffalo" to ("conservation herds of plains and wood bison" to 3.1e4),
)

/** Traill, Bradshaw & Brook 2007: median minimum viable population, 4,169. */
internal const val MINIMUM_VIABLE = 4_169.0

data class ExtinctionOutlook(
    val species: Species,
    val herdNow: Double,
    /** Years from now until commercial breeding stops, or null within [horizon]. */
    val yearsToCommercialEnd: Int?,
    val herdAtEnd: Double?,
    val reservoir: String,
    val reservoirSize: Double?,
    /** Factory-farmed share today, and when commercial breeding ends. */
    val factoryShareNow: Double?,
    val factoryShareAtEnd: Double?,
) {
    /** Null when the reservoir is uncounted. */
    val speciesSurvives get() = reservoirSize?.let { it >= MINIMUM_VIABLE }
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
        val dropAt = { t: Int -> 1 - exp(s.lnPrice(s.shiftIn(t, growth)) - lnP0) }
        ExtinctionOutlook(
            s, herd, end, end?.let(herdAt), reservoir, size,
            FACTORY_SHARE[s.name], end?.let { s.factoryShareAfter(dropAt(it)) },
        )
    }

/**
 * Soy embedded per kg of product, retail weight: WWF-NL / Profundo, "European
 * soy supply" (2021, 2019/20 data, EU27+UK): broiler meat 0.956, farmed fish
 * 0.951, pork 0.415, beef 0.206; eggs 29 g each (≈0.58 per kg). Turkey and duck
 * use the broiler factor. No source gives fresh milk, lamb or shellfish, so
 * they are left out rather than guessed.
 */
internal val SOY_KG_PER_KG = mapOf(
    "Chickens" to 0.956, "Turkeys" to 0.956, "Ducks" to 0.956,
    "Egg-Laying Hens" to 0.58, "Pigs" to 0.415, "Cows" to 0.206, "Buffalo" to 0.206,
    "Fish" to 0.951,
)

// 77% of the world's soy feeds animals (Ritchie & Roser, Our World in Data, 2021).
internal const val SOY_FED_TO_ANIMALS = 0.77
// Firm tofu yield: ~1.9–2.4 kg per kg dry soybeans in food-science trials.
internal const val TOFU_KG_PER_SOY_KG = 2.0
// No authoritative world tofu figure exists (FAO doesn't track it); industry
// estimates run 2–5 Mt. 3 Mt is a midpoint, labelled as such in the UI.
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
