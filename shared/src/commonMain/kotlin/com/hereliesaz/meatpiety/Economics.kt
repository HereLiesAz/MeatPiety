package com.hereliesaz.meatpiety

import kotlin.math.exp
import kotlin.math.ln

/**
 * One layer of abstention (you, the people you've influenced, the world) and
 * what it does to one species' market and population, as a chain in time:
 *
 * 1. You stop buying. The animals for that demand were already bred, so for
 *    the first [Biology.supplyLagDays] they still come to market ([bredAnyway]).
 * 2. With supply fixed, the surplus clears by price: the short-run drop
 *    [shortRunPriceChange] uses demand elasticity alone, and other buyers
 *    take those animals ([reboundAnimals]).
 * 3. The lower price tells producers to shrink the breeding herd. They cull
 *    breeders early ([cullWave]: extra slaughter now, offset as those animals'
 *    scheduled retirements never come), and births fall after one gestation
 *    ([neverBred], [birthingsNotNeeded], [malesNeverBorn]).
 * 4. Slaughter falls after the supply lag ([netAnimals], fewer slaughtered),
 *    and the price settles at the long-run equilibrium [priceChange].
 *
 * Model: one global constant-elasticity market per species. Long run,
 * ln P' = ln(1 − shift) / (es + ed), ln Q' = es · ln P'; short run, es = 0.
 *
 * Baselines: today's market already excludes today's abstainers. A marginal
 * layer (you, your reach) shifts today's market; the world layer is measured
 * against the counterfactual market they would have bought into, so its
 * price change compares today with a world without them.
 */
data class Impact(
    val species: Species,
    val grossUnits: Double,
    val grossAnimals: Double,
    val demandShift: Double,
    val bredAnyway: Double,
    val shortRunPriceChange: Double,
    val priceChange: Double,
    val neverBred: Double,
    val netUnits: Double,
    /** Fewer slaughtered so far, net of the early cull wave. Can be negative early on. */
    val netAnimals: Double,
    val reboundUnits: Double,
    val breedersNotKept: Double?,
    /** Breeders slaughtered early whose scheduled retirement hasn't yet come round. */
    val cullWave: Double?,
    // Replacement breeders that would have been born into the smaller herd and
    // weren't, and how many of the never-bred would have been their offspring.
    val replacementsNeverBred: Double?,
    val offspringOfUnbornBreeders: Double?,
    /** Male chicks or calves that are never born alongside the females not bred. */
    val malesNeverBorn: Double?,
    /** Animals that would be standing in barns right now but were never born. */
    val fewerInBarns: Double?,
    /** Days of farm life not lived, summed over every animal never born. */
    val animalDaysNotLived: Double?,
    /** Wild forage fish not caught to feed farmed fish and shrimp. */
    val forageFishNotCaught: Double,
    val birthingsNotNeeded: Double?,
    /** Retail spending that leaves the market (not farm income). */
    val retailSalesLostUsd: Double,
    val usdPrice: Double,
    val years: Double,
    /** World-average yields and global factory shares apply (influence, world). */
    val worldScope: Boolean,
) {
    val reboundAnimals get() = grossAnimals - netAnimals.coerceAtLeast(0.0)
    /** Every animal not born: the market animals, the breeders that would have replaced the herd, their brothers. */
    val totalNeverBorn get() = neverBred + (replacementsNeverBred ?: 0.0) + (malesNeverBorn ?: 0.0)
    /** Today's price is this much lower than without the layer; newUsdPrice reads it the other way for marginal layers. */
    val newUsdPrice get() = usdPrice * (1 + priceChange)
    /** World layer: what the price would be if its abstainers still ate meat. */
    val counterfactualUsdPrice get() = usdPrice / (1 + priceChange)
}

data class Layer(
    val impacts: List<Impact>,
    /** Extra beef cattle raised because less dairy beef reaches market. */
    val dairyBeefOffset: Double = 0.0,
) {
    /** Fewer animals slaughtered so far, excluding the backward (bred-for-demand) lines, net of the dairy-beef offset. */
    val netSpared get() = impacts.filterNot { it.species.existsBecauseOfDemand }.sumOf { it.netAnimals } - dairyBeefOffset
    val grossSpared get() = impacts.filterNot { it.species.existsBecauseOfDemand }.sumOf { it.grossAnimals }
    val neverBorn get() = impacts.filterNot { it.species.existsBecauseOfDemand }.sumOf { it.totalNeverBorn }
    val years get() = impacts.firstOrNull()?.years ?: 0.0
}

/** Today's global market for a species, in its retail unit (already net of today's abstainers). */
internal fun Species.globalMarketUnitsPerYear() = worldPopulation(currentYear()) * usPerCapita * worldToUsRatio

// Fish and shellfish have no breeding model; aquaculture stocking responds
// within about a season.
private const val SEAFOOD_LAG_DAYS = 90
private const val SEAFOOD_GESTATION_DAYS = 0

/**
 * Share of U.S. beef from the dairy herd (cull cows and dairy-bred steers):
 * USDA ERS puts it around a fifth. Less milk means less of that beef, and the
 * beef herd grows to fill some of the gap.
 */
internal const val DAIRY_BEEF_SHARE = 0.2

internal enum class Baseline { MARGINAL, COUNTERFACTUAL }

/** Long-run supply elasticity with the Norwood & Lusk factor nudged by [cefDelta] (for ranges). */
internal fun Species.supplyElasticity(cefDelta: Double): Double {
    if (cefDelta == 0.0) return supplyElasticity
    val cef = (supplyElasticity / (supplyElasticity + demandElasticity) + cefDelta).coerceIn(0.05, 0.97)
    return cef * demandElasticity / (1 - cef)
}

/**
 * Removes [unitsPerYear] of annual demand for [species] over [years] and works
 * out what the market and the animals do about it.
 */
internal fun impact(
    species: Species,
    unitsPerYear: Double,
    years: Double,
    usdPrice: Double = species.usdPrice,
    baseline: Baseline = Baseline.MARGINAL,
    worldScope: Boolean = false,
    cefDelta: Double = 0.0,
    marketScale: Double = 1.0,
): Impact {
    // Population and per-capita intake move the market over time (Population.kt).
    val today = species.globalMarketUnitsPerYear() * marketScale
    val market = if (baseline == Baseline.COUNTERFACTUAL) today + unitsPerYear else today
    val shift = (unitsPerYear / market).coerceIn(0.0, 0.99)
    // Wild catch is quota-bound: only the farmed share answers demand.
    val es = species.supplyElasticity(cefDelta) * species.farmedShare
    val ed = species.demandElasticity
    val lnPrice = ln(1 - shift) / (es + ed)
    val priceChange = exp(lnPrice) - 1
    val quantityChange = exp(es * lnPrice) - 1
    val bio = species.biology
    val perAnimal = if (worldScope) species.worldUnitsPerAnimal ?: species.unitsPerAnimal else species.unitsPerAnimal
    val days = years * 365.0
    val lag = (bio?.supplyLagDays ?: SEAFOOD_LAG_DAYS).toDouble()
    val gestation = (bio?.gestationDays ?: SEAFOOD_GESTATION_DAYS).toDouble()
    val grossUnits = unitsPerYear * years
    val grossAnimals = grossUnits / perAnimal
    // Long-run cut in animals per day, once breeding has responded.
    val cutPerDay = -quantityChange * market / 365.0 / perAnimal
    val sinceBirths = (days - gestation).coerceAtLeast(0.0)
    val neverBred = cutPerDay * sinceBirths
    val breeders = bio?.let { if (days > 0) cutPerDay * 365.0 / it.offspringPerBreederYear else 0.0 }
    // Breeders culled at once; each would have retired within 1 / replacementRate
    // years anyway, so the extra slaughter unwinds linearly over that time.
    val cullWave = if (bio != null && breeders != null) breeders * (1 - bio.replacementRate * days / 365.0).coerceAtLeast(0.0) else null
    val fewerSlaughtered = cutPerDay * (days - lag).coerceAtLeast(0.0) - (cullWave ?: 0.0)
    val replacements = if (bio != null && breeders != null) breeders * bio.replacementRate * sinceBirths / 365.0 else null
    // The smaller herd is at first the culled breeders, then — as they would
    // have retired — the replacements never born. Offspring the missing herd
    // doesn't have are split between the two; this is the share from unborn
    // replacements, which breed from firstBreedingDays. A breakdown of
    // neverBred, not an addition.
    val fromUnborn = if (bio != null && breeders != null) {
        val x = (sinceBirths - bio.firstBreedingDays).coerceAtLeast(0.0)
        val full = 365.0 / bio.replacementRate
        val breederDays = if (x <= full) breeders * x * x / (2 * full) else breeders * (full / 2 + (x - full))
        minOf(neverBred, breederDays * bio.offspringPerBreederYear / 365.0)
    } else null
    // Standing population missing: births missed in the last slaughter-age window.
    val residence = bio?.slaughterAgeDays?.toDouble()
    val fewerInBarns = residence?.let { cutPerDay * minOf(sinceBirths, it) }
    val animalDays = residence?.let { l ->
        if (sinceBirths <= l) cutPerDay * sinceBirths * sinceBirths / 2 else cutPerDay * (l * l / 2 + l * (sinceBirths - l))
    }
    val netUnits = (cutPerDay * (days - lag).coerceAtLeast(0.0)) * perAnimal
    return Impact(
        species = species,
        grossUnits = grossUnits,
        grossAnimals = grossAnimals,
        demandShift = shift,
        bredAnyway = if (days > 0) grossAnimals / days * minOf(days, lag) else 0.0,
        shortRunPriceChange = exp(ln(1 - shift) / ed) - 1,
        priceChange = priceChange,
        neverBred = neverBred,
        netUnits = netUnits,
        netAnimals = fewerSlaughtered,
        reboundUnits = grossUnits - netUnits,
        breedersNotKept = breeders,
        cullWave = cullWave,
        replacementsNeverBred = replacements,
        offspringOfUnbornBreeders = fromUnborn,
        malesNeverBorn = bio?.malesPerAnimal?.let { neverBred * it },
        fewerInBarns = fewerInBarns,
        animalDaysNotLived = animalDays,
        forageFishNotCaught = netUnits * species.feedFishPerUnit,
        birthingsNotNeeded = bio?.let { neverBred / it.perBirthing },
        retailSalesLostUsd = market * usdPrice * (1 - (1 + quantityChange) * (1 + priceChange)) * years,
        usdPrice = usdPrice,
        years = years,
        worldScope = worldScope,
    )
}

/**
 * Builds a layer and adds the dairy–beef link: the dairy impact's lost cull
 * cows and steers shift beef supply left by DAIRY_BEEF_SHARE × the dairy
 * quantity cut, raising the beef price; the beef herd expands by es × Δln P.
 */
internal fun layerOf(impacts: List<Impact>): Layer {
    val dairy = impacts.firstOrNull { it.species.name == "Dairy Cows" } ?: return Layer(impacts)
    val beef = SPECIES.first { it.name == "Cows" }
    val dairyCut = dairy.netUnits / (dairy.species.globalMarketUnitsPerYear() * dairy.years.coerceAtLeast(1e-9))
    val lnP = DAIRY_BEEF_SHARE * dairyCut / (beef.supplyElasticity + beef.demandElasticity)
    val perAnimal = beef.unitsPerAnimal
    val extraBeefCattle = beef.supplyElasticity * lnP * beef.globalMarketUnitsPerYear() * (1 - DAIRY_BEEF_SHARE) / perAnimal * dairy.years
    return Layer(impacts, extraBeefCattle)
}

/** Your own abstention, eating as a standard U.S. diet otherwise would. */
fun personalLayer(
    days: Int,
    diet: Diet,
    prices: Map<String, Double> = emptyMap(),
    cefDelta: Double = 0.0,
    marketScale: Double = pastMarketFactor(days),
): Layer {
    val years = days / 365.0
    return layerOf(SPECIES.filter { it.eatenBy(diet) }.map {
        impact(it, it.usPerCapita, years, prices[it.name] ?: it.usdPrice, cefDelta = cefDelta, marketScale = marketScale)
    })
}

/** Low and high estimates of [Layer.netSpared], moving each supply-response factor by ±0.1. */
fun personalRange(days: Int, diet: Diet, prices: Map<String, Double> = emptyMap()): Pair<Double, Double> =
    personalLayer(days, diet, prices, -0.1).netSpared to personalLayer(days, diet, prices, 0.1).netSpared

/**
 * People you've persuaded. They count at the world-average diet, since your
 * audience isn't all American, and for half your span on average, since they
 * came round gradually.
 */
fun influenceLayer(people: Double, days: Int, diet: Diet, prices: Map<String, Double> = emptyMap()): Layer {
    val years = days / 365.0 / 2
    return layerOf(SPECIES.filter { it.eatenBy(diet) }.map {
        impact(
            it, people * it.usPerCapita * it.worldToUsRatio, years, prices[it.name] ?: it.usdPrice,
            worldScope = true, marketScale = pastMarketFactor(days / 2),
        )
    })
}

/**
 * Every vegan and vegetarian on earth, at the world-average diet, measured
 * against the market they would have bought into: the price change says how
 * much cheaper today is than a world without them.
 */
fun worldLayer(
    days: Int,
    prices: Map<String, Double> = emptyMap(),
    growth: Double = 1.0,
    marketScale: Double = pastMarketFactor(days),
): Layer {
    val years = days / 365.0
    return layerOf(SPECIES.map {
        val abstainers = (if (it.requiresVeganism) VEGANS_WORLDWIDE else VEGANS_WORLDWIDE + VEGETARIANS_WORLDWIDE) * growth
        impact(
            it, abstainers * it.usPerCapita * it.worldToUsRatio, years, prices[it.name] ?: it.usdPrice,
            baseline = Baseline.COUNTERFACTUAL, worldScope = true, marketScale = marketScale,
        )
    })
}

/**
 * Growth scenarios for the world's vegans and vegetarians. There is no
 * measured global trend: Gallup shows the U.S. share flat since 1999, so the
 * app offers flat, slow and fast scenarios rather than a forecast.
 */
val GROWTH_SCENARIOS = listOf(0.0, 0.01, 0.03)
const val DEFAULT_GROWTH = 0.01

/** One future horizon: your ledger if you keep going, and the world's if abstention keeps growing. */
data class Projection(val yearsAhead: Int, val personal: Layer, val world: Layer, val abstainers: Double)

fun projections(days: Int, diet: Diet, prices: Map<String, Double> = emptyMap(), growthRate: Double = DEFAULT_GROWTH, horizons: List<Int> = listOf(1, 5, 10, 25)) =
    horizons.map { h ->
        val growth = kotlin.math.exp(h * kotlin.math.ln(1 + growthRate))
        Projection(
            yearsAhead = h,
            // Your span runs from your start date to h years out.
            personal = personalLayer(days + h * 365, diet, prices, marketScale = (pastMarketFactor(days) * days + (1 + marketFactor(h.toDouble())) / 2 * h * 365) / (days + h * 365).coerceAtLeast(1)),
            // World snapshot h years out: one year of abstention at that year's
            // headcount, in that year's market (UN WPP 2024 population).
            world = worldLayer(365, prices, growth, marketScale = marketFactor(h.toDouble())),
            abstainers = (VEGANS_WORLDWIDE + VEGETARIANS_WORLDWIDE) * growth,
        )
    }
