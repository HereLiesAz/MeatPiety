package com.hereliesaz.meatpiety

import kotlin.math.exp
import kotlin.math.ln

/**
 * One layer of abstention (you, the people you've influenced, the world) and
 * what it does to one species' market and population, as a chain in time:
 *
 * 1. You stop buying. The animals for that demand were already bred, so for
 *    the first [Biology.supplyLagDays] they still come to market ([bredAnyway]).
 * 2. With supply fixed, the surplus can only clear by price: the short-run
 *    drop [shortRunPriceChange] uses demand elasticity alone, and other
 *    buyers take those animals ([reboundAnimals]).
 * 3. The lower price tells producers to cut breeding. Births fall after one
 *    gestation ([neverBred], [birthingsNotNeeded]); the breeding herd shrinks
 *    at once ([breedersNotKept]).
 * 4. Slaughter falls after gestation + time to slaughter ([netAnimals],
 *    fewer slaughtered), and the price settles at the long-run equilibrium
 *    [priceChange] with supply responding too.
 *
 * Model: one global constant-elasticity market per species. Long run,
 * ln P' = ln(1 − shift) / (es + ed), ln Q' = es · ln P'; short run, es = 0.
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
    /** Fewer slaughtered so far. */
    val netAnimals: Double,
    val reboundUnits: Double,
    val breedersNotKept: Double?,
    // Replacement breeders that would have been born into the herd over the
    // span and weren't, and how many of the never-bred came from them.
    val replacementsNeverBred: Double?,
    val offspringOfUnbornBreeders: Double?,
    // Animals already born for your share when your demand left, whose
    // slaughter is pushed back while the breeding cut works through: a day's
    // share per elapsed day, up to slaughter age.
    val stillAlive: Double?,
    val birthingsNotNeeded: Double?,
    val producerRevenueLostUsd: Double,
    val belowBreakeven: Boolean,
    val usdPrice: Double,
) {
    val reboundAnimals get() = grossAnimals - netAnimals
    /** Every animal not born: market animals plus the breeders that would have replaced the herd. */
    val totalNeverBorn get() = neverBred + (replacementsNeverBred ?: 0.0)
    val newUsdPrice get() = usdPrice * (1 + priceChange)
}

data class Layer(val impacts: List<Impact>) {
    /** Fewer animals slaughtered so far, excluding the backward (bred-for-demand) lines. */
    val netSpared get() = impacts.filterNot { it.species.existsBecauseOfDemand }.sumOf { it.netAnimals }
    val grossSpared get() = impacts.filterNot { it.species.existsBecauseOfDemand }.sumOf { it.grossAnimals }
}

/** Global market for a species, in its retail unit, absent anyone abstaining. */
internal fun Species.globalMarketUnitsPerYear() = WORLD_POPULATION * usPerCapita * worldToUsRatio

// Fish and shellfish have no breeding model; catch and aquaculture stocking
// respond within about a season.
private const val SEAFOOD_LAG_DAYS = 90
private const val SEAFOOD_GESTATION_DAYS = 0

/**
 * Removes [unitsPerYear] of annual demand for [species] over [years] and works
 * out what the market and the animals do about it.
 */
internal fun impact(species: Species, unitsPerYear: Double, years: Double, usdPrice: Double = species.usdPrice): Impact {
    val market = species.globalMarketUnitsPerYear()
    val shift = (unitsPerYear / market).coerceIn(0.0, 0.99)
    val es = species.supplyElasticity
    val ed = species.demandElasticity
    val lnPrice = ln(1 - shift) / (es + ed)
    val priceChange = exp(lnPrice) - 1
    val quantityChange = exp(es * lnPrice) - 1
    val bio = species.biology
    val days = years * 365.0
    val lag = (bio?.supplyLagDays ?: SEAFOOD_LAG_DAYS).toDouble()
    val gestation = (bio?.gestationDays ?: SEAFOOD_GESTATION_DAYS).toDouble()
    val grossUnits = unitsPerYear * years
    val grossAnimals = grossUnits / species.unitsPerAnimal
    // Long-run cut in animals per day, once breeding has responded.
    val cutPerDay = -quantityChange * market / 365.0 / species.unitsPerAnimal
    val netAnimals = cutPerDay * (days - lag).coerceAtLeast(0.0)
    val neverBred = cutPerDay * (days - gestation).coerceAtLeast(0.0)
    val breeders = bio?.let { cutPerDay * 365.0 / it.offspringPerBreederYear }
    // Days since the first missing replacement could have been born.
    val sinceBirths = (days - gestation).coerceAtLeast(0.0)
    val replacements = if (bio != null && breeders != null) breeders * bio.replacementRate * sinceBirths / 365.0 else null
    // Litters those unborn replacements would have had: they join the herd
    // continuously and breed from firstBreedingDays, until they make up the
    // whole herd cut (after 1 / replacementRate years). Integrated in closed
    // form. This is a breakdown of neverBred, not an addition: in steady
    // state the smaller herd *is* the missing replacements.
    val fromUnborn = if (bio != null && breeders != null) {
        val x = (sinceBirths - bio.firstBreedingDays).coerceAtLeast(0.0)
        val full = 365.0 / bio.replacementRate
        val breederDays = if (x <= full) breeders * x * x / (2 * full) else breeders * (full / 2 + (x - full))
        minOf(neverBred, breederDays * bio.offspringPerBreederYear / 365.0)
    } else null
    val revenueBefore = market * usdPrice
    val revenueAfter = market * (1 + quantityChange) * usdPrice * (1 + priceChange)
    return Impact(
        species = species,
        grossUnits = grossUnits,
        grossAnimals = grossAnimals,
        demandShift = shift,
        bredAnyway = if (days > 0) grossAnimals / days * minOf(days, lag) else 0.0,
        shortRunPriceChange = exp(ln(1 - shift) / ed) - 1,
        priceChange = priceChange,
        neverBred = neverBred,
        netUnits = netAnimals * species.unitsPerAnimal,
        netAnimals = netAnimals,
        reboundUnits = grossUnits - netAnimals * species.unitsPerAnimal,
        // The breeding herd is cut as soon as the price signal arrives.
        breedersNotKept = if (days > 0) breeders else breeders?.let { 0.0 },
        replacementsNeverBred = replacements,
        offspringOfUnbornBreeders = fromUnborn,
        stillAlive = bio?.let { cutPerDay * minOf(days, it.slaughterAgeDays.toDouble()) },
        birthingsNotNeeded = bio?.let { neverBred / it.perBirthing },
        producerRevenueLostUsd = (revenueBefore - revenueAfter) * years,
        belowBreakeven = -priceChange >= species.breakevenDrop,
        usdPrice = usdPrice,
    )
}

/** Your own abstention, eating as a standard U.S. diet otherwise would. */
fun personalLayer(days: Int, diet: Diet, prices: Map<String, Double> = emptyMap()): Layer {
    val years = days / 365.0
    return Layer(SPECIES.filter { it.eatenBy(diet) }.map {
        impact(it, it.usPerCapita, years, prices[it.name] ?: it.usdPrice)
    })
}

/**
 * People you've persuaded. They count at the world-average diet, since your
 * audience isn't all American, and for half your span on average, since they
 * came round gradually.
 */
fun influenceLayer(people: Double, days: Int, diet: Diet, prices: Map<String, Double> = emptyMap()): Layer {
    val years = days / 365.0 / 2
    return Layer(SPECIES.filter { it.eatenBy(diet) }.map {
        impact(it, people * it.usPerCapita * it.worldToUsRatio, years, prices[it.name] ?: it.usdPrice)
    })
}

/** Every vegan and vegetarian on earth, at the world-average diet. */
fun worldLayer(days: Int, prices: Map<String, Double> = emptyMap(), growth: Double = 1.0): Layer {
    val years = days / 365.0
    return Layer(SPECIES.map {
        val abstainers = (if (it.requiresVeganism) VEGANS_WORLDWIDE else VEGANS_WORLDWIDE + VEGETARIANS_WORLDWIDE) * growth
        impact(it, abstainers * it.usPerCapita * it.worldToUsRatio, years, prices[it.name] ?: it.usdPrice)
    })
}

// Assumed yearly growth of the world's vegans and vegetarians. Survey trends
// (Ipsos, Google Trends, plant-based sales) put it in low single digits; 3%
// is a round, stated guess, not a forecast.
internal const val ABSTAINER_GROWTH = 0.03

/** One future horizon: your ledger if you keep going, and the world's if abstention keeps growing. */
data class Projection(val yearsAhead: Int, val personal: Layer, val world: Layer, val abstainers: Double)

fun projections(days: Int, diet: Diet, prices: Map<String, Double> = emptyMap(), horizons: List<Int> = listOf(1, 5, 10, 25)) =
    horizons.map { h ->
        val growth = kotlin.math.exp(h * kotlin.math.ln(1 + ABSTAINER_GROWTH))
        Projection(
            yearsAhead = h,
            personal = personalLayer(days + h * 365, diet, prices),
            // World snapshot h years out: one year of abstention at that year's headcount.
            world = worldLayer(365, prices, growth),
            abstainers = (VEGANS_WORLDWIDE + VEGETARIANS_WORLDWIDE) * growth,
        )
    }
