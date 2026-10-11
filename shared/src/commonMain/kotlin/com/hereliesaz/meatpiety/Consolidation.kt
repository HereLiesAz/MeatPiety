package com.hereliesaz.meatpiety

/*
 * Margin squeeze → consolidation. Lower prices hit smaller, higher-cost farms
 * first; they exit, and the animals still bred are raised by factory farms
 * that survive on volume. So abstention also changes how the remaining animals
 * live.
 *
 * Model: non-factory share n = n0 · max(0, 1 − drop / smallMargin), where
 * drop is the price fall relative to today and smallMargin is a small farm's
 * margin, taken as half the industry's (Species.breakevenDrop / 2).
 * Illustrative; the direction is well documented (USDA ERS on farm
 * consolidation), the magnitude is not.
 */

/** Factory-farmed share of animals today (Sentience Institute, U.S., 2019; lower elsewhere). */
internal val FACTORY_SHARE = mapOf(
    "Chickens" to 0.999, "Egg-Laying Hens" to 0.98, "Turkeys" to 0.998, "Ducks" to 0.9,
    "Pigs" to 0.983, "Cows" to 0.704, "Dairy Cows" to 0.75, "Sheep & Lambs" to 0.1, "Buffalo" to 0.0,
)

internal fun Species.factoryShareAfter(priceDrop: Double): Double? {
    val f0 = FACTORY_SHARE[name] ?: return null
    // No factory system to absorb the animals (bison): small ranches just shrink.
    if (f0 <= 0.0) return 0.0
    val small = breakevenDrop / 2
    val n = (1 - f0) * (1 - priceDrop / small).coerceIn(0.0, 1.0)
    return 1 - n
}

/**
 * Animals that, because of this layer's price drop, are raised in factory
 * farms instead of smaller farms over the span. Zero for species without
 * factory figures (fish) or with none to displace (bison).
 */
val Impact.shiftedToFactory: Double
    get() {
        val f0 = FACTORY_SHARE[species.name] ?: return 0.0
        val f1 = species.factoryShareAfter(-priceChange) ?: return 0.0
        val stillRaised = species.globalMarketUnitsPerYear() / species.unitsPerAnimal * years - netAnimals
        return (f1 - f0) * stillRaised.coerceAtLeast(0.0)
    }

val Layer.shiftedToFactory get() = impacts.sumOf { it.shiftedToFactory }
