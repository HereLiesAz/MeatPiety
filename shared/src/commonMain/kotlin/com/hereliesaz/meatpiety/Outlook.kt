package com.hereliesaz.meatpiety

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

/*
 * Two long views built on the same market model as the ledger.
 *
 * Herd outlook: the potential market grows with population and income
 * ([MARKET_GROWTH]); abstainers grow at a chosen scenario rate and take a
 * share s(t) of it. In the long-run equilibrium the herd scales as
 * [(1 + g)^t · (1 − s(t)) / (1 − s0)]^(es / (es + ed)) relative to today, and
 * the price as the same base to 1 / (es + ed). A constant-elasticity market
 * shrinks but never reaches zero, so the model gives a trajectory (when the
 * herd halves, what is left in 50 and 100 years), not an extinction date. What
 * would outlast farming is the non-commercial reservoir: wild ancestors,
 * feral, heritage and sanctuary animals, judged against the Traill et al.
 * (2007) minimum viable population where anyone has counted.
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

// Global meat consumption growth from population and income: OECD-FAO
// Agricultural Outlook 2023–2032 projects ~12% over the decade (~1.1%/yr).
internal const val MARKET_GROWTH = 0.011
// World population growth, ~0.8%/yr (UN World Population Prospects 2024).
internal const val POPULATION_GROWTH = 0.008

data class HerdOutlook(
    val species: Species,
    val herdNow: Double,
    /** Herd relative to today at 25, 50 and 100 years. */
    val herdIn25: Double,
    val herdIn50: Double,
    val herdIn100: Double,
    /** Years until the herd halves, or null within [horizon]. */
    val yearsToHalf: Int?,
    val reservoir: String,
    val reservoirSize: Double?,
    val factoryShareNow: Double?,
    /** Factory share in 100 years, if prices have fallen by then. */
    val factoryShareIn100: Double?,
) {
    /** Null when the reservoir is uncounted. */
    val reservoirViable get() = reservoirSize?.let { it >= MINIMUM_VIABLE }
}

private fun Species.abstainerShare(years: Int, growth: Double): Double {
    val a0 = (if (requiresVeganism) VEGANS_WORLDWIDE else VEGANS_WORLDWIDE + VEGETARIANS_WORLDWIDE).toDouble() / WORLD_POPULATION
    return (a0 * ((1 + growth) / (1 + POPULATION_GROWTH)).pow(years)).coerceIn(0.0, 0.99)
}

/** ln of demand at year t relative to today. */
private fun Species.lnDemand(t: Int, growth: Double) =
    t * ln(1 + MARKET_GROWTH) + ln(1 - abstainerShare(t, growth)) - ln(1 - abstainerShare(0, growth))

fun herdOutlook(growth: Double = DEFAULT_GROWTH, horizon: Int = 300): List<HerdOutlook> =
    SPECIES.mapNotNull { s ->
        val herd = GLOBAL_HERD[s.name] ?: return@mapNotNull null
        val (reservoir, size) = RESERVOIRS.getValue(s.name)
        val es = s.supplyElasticity * s.farmedShare
        val cef = es / (es + s.demandElasticity)
        val ratio = { t: Int -> exp(cef * s.lnDemand(t, growth)) }
        val priceDrop100 = 1 - exp(s.lnDemand(100, growth) / (es + s.demandElasticity))
        HerdOutlook(
            species = s,
            herdNow = herd,
            herdIn25 = herd * ratio(25),
            herdIn50 = herd * ratio(50),
            herdIn100 = herd * ratio(100),
            yearsToHalf = (1..horizon).firstOrNull { ratio(it) <= 0.5 },
            reservoir = reservoir,
            reservoirSize = size,
            factoryShareNow = FACTORY_SHARE[s.name],
            factoryShareIn100 = if (priceDrop100 > 0) s.factoryShareAfter(priceDrop100) else FACTORY_SHARE[s.name],
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
    // Farmed fish only; wild-caught fish eat no soy.
    "Fish" to 0.951 * 0.51,
)

// 77% of the world's soy feeds animals (Ritchie & Roser, Our World in Data, 2021).
internal const val SOY_FED_TO_ANIMALS = 0.77
// Firm tofu yield: ~1.9–2.4 kg per kg dry soybeans in food-science trials.
internal const val TOFU_KG_PER_SOY_KG = 2.0
// No authoritative world tofu figure exists (FAO doesn't track it); industry
// estimates run 2–5 Mt. 3.5 Mt is the midpoint, labelled as such in the UI.
// Feed soy is mostly crushed meal, so "tofu it could have made" assumes the
// beans, not the meal, are freed.
internal const val GLOBAL_TOFU_KG = 3.5e9

data class SoyLedger(val soyFreedKg: Double, val years: Double) {
    val tofuKg get() = soyFreedKg * TOFU_KG_PER_SOY_KG
    /** Share of a year's world tofu that one year of freed feed could make. */
    val shareOfWorldTofu get() = if (years > 0) tofuKg / years / GLOBAL_TOFU_KG else 0.0
}

/** Feed soy no longer grown for the animals this layer stopped producing. */
val Layer.soy: SoyLedger
    get() = SoyLedger(impacts.filterNot { it.species.existsBecauseOfDemand }.sumOf { i ->
        val kg = FOOTPRINTS[i.species.name]?.let { i.netUnits * it.kgPerUnit } ?: 0.0
        kg * (SOY_KG_PER_KG[i.species.name] ?: 0.0)
    }, years)
