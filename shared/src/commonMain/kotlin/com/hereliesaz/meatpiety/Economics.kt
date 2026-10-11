package com.hereliesaz.meatpiety

import kotlin.math.exp
import kotlin.math.ln

/**
 * One layer of abstention (you, the people you've influenced, the world) and
 * what it does to one species' market and population.
 *
 * Model: a single global market per species with constant-elasticity supply
 * and demand. Removing [grossUnits] of demand shifts the demand curve by the
 * share [demandShift]; the price falls until other buyers pick up part of the
 * slack ([reboundUnits]); producers cut the rest ([netUnits]). Fewer animals
 * raised means fewer breeders kept and fewer births, and it takes
 * [Biology.supplyLagDays] for a breeding cut to reach market.
 */
data class Impact(
    val species: Species,
    val grossUnits: Double,
    val grossAnimals: Double,
    val demandShift: Double,
    val priceChange: Double,
    val netUnits: Double,
    val netAnimals: Double,
    val reboundUnits: Double,
    val breedersNotKept: Double?,
    // Animals already born for your share when your demand left. Producers
    // can only cut breeding, so these are slaughtered later than scheduled
    // while the cut works through; at any moment that is a day's worth of
    // your share for each day of the pipeline elapsed, up to slaughter age.
    val stillAlive: Double?,
    val birthingsNotNeeded: Double?,
    val producerRevenueLostUsd: Double,
    val belowBreakeven: Boolean,
    val usdPrice: Double,
) {
    val reboundAnimals get() = grossAnimals - netAnimals
    val newUsdPrice get() = usdPrice * (1 + priceChange)
}

data class Layer(val impacts: List<Impact>) {
    /** Net fewer animals slaughtered, excluding the backward (bred-for-demand) lines. */
    val netSpared get() = impacts.filterNot { it.species.existsBecauseOfDemand }.sumOf { it.netAnimals }
    val grossSpared get() = impacts.filterNot { it.species.existsBecauseOfDemand }.sumOf { it.grossAnimals }
}

/** Global market for a species, in its retail unit, absent anyone abstaining. */
internal fun Species.globalMarketUnitsPerYear() = WORLD_POPULATION * usPerCapita * worldToUsRatio

/**
 * Removes [unitsPerYear] of annual demand for [species] over [years] and works
 * out what the market and the animals do about it.
 */
internal fun impact(species: Species, unitsPerYear: Double, years: Double, usdPrice: Double = species.usdPrice): Impact {
    val market = species.globalMarketUnitsPerYear()
    val shift = (unitsPerYear / market).coerceIn(0.0, 0.99)
    val es = species.supplyElasticity
    val ed = species.demandElasticity
    // Constant-elasticity equilibrium after demand shrinks by `shift`:
    // ln P' = ln(1 - shift) / (es + ed), ln Q' = es · ln P'.
    val lnPrice = ln(1 - shift) / (es + ed)
    val priceChange = exp(lnPrice) - 1
    val quantityChange = exp(es * lnPrice) - 1
    val grossUnits = unitsPerYear * years
    val netUnits = -quantityChange * market * years
    val netAnimals = netUnits / species.unitsPerAnimal
    val bio = species.biology
    val revenueBefore = market * usdPrice
    val revenueAfter = market * (1 + quantityChange) * usdPrice * (1 + priceChange)
    return Impact(
        species = species,
        grossUnits = grossUnits,
        grossAnimals = grossUnits / species.unitsPerAnimal,
        demandShift = shift,
        priceChange = priceChange,
        netUnits = netUnits,
        netAnimals = netAnimals,
        reboundUnits = grossUnits - netUnits,
        // Breeders are a standing herd: an annual rate of animals not raised,
        // divided by what one breeder produces a year.
        breedersNotKept = bio?.let { if (years > 0) netAnimals / years / it.offspringPerBreederYear else 0.0 },
        stillAlive = bio?.let { if (years > 0) netAnimals / (years * 365.0) * minOf(years * 365.0, it.slaughterAgeDays.toDouble()) else 0.0 },
        birthingsNotNeeded = bio?.let { netAnimals / it.perBirthing },
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
fun worldLayer(days: Int, prices: Map<String, Double> = emptyMap()): Layer {
    val years = days / 365.0
    return Layer(SPECIES.map {
        val abstainers = if (it.requiresVeganism) VEGANS_WORLDWIDE else VEGANS_WORLDWIDE + VEGETARIANS_WORLDWIDE
        impact(it, abstainers * it.usPerCapita * it.worldToUsRatio, years, prices[it.name] ?: it.usdPrice)
    })
}
