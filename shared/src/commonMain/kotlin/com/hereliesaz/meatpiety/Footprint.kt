package com.hereliesaz.meatpiety

/**
 * Environmental cost of producing one kilogram of a product, global medians
 * from Poore & Nemecek (2018), Science 360:987 (via Our World in Data).
 * Applied to what producers actually stop making ([Impact.netUnits]), so it
 * follows the same chain as the animals: nothing until breeding responds.
 */
data class Footprint(
    val kgPerUnit: Double,
    val co2eKgPerKg: Double,
    val landM2PerKg: Double,
    val waterLPerKg: Double,
)

private const val LB = 0.4536

// Turkey and duck use poultry; bison uses the beef herd; fish and shellfish
// use farmed fish and farmed crustaceans.
internal val FOOTPRINTS = mapOf(
    "Chickens" to Footprint(LB, 9.87, 12.22, 660.0),
    "Turkeys" to Footprint(LB, 9.87, 12.22, 660.0),
    "Ducks" to Footprint(LB, 9.87, 12.22, 660.0),
    "Pigs" to Footprint(LB, 12.31, 17.36, 1796.0),
    "Cows" to Footprint(LB, 99.48, 326.21, 1451.0),
    "Buffalo" to Footprint(LB, 99.48, 326.21, 1451.0),
    "Sheep & Lambs" to Footprint(LB, 39.72, 369.81, 1803.0),
    "Fish" to Footprint(LB, 13.63, 8.41, 3691.0),
    "Shellfish" to Footprint(LB, 26.87, 2.97, 3515.0),
    "Egg-Laying Hens" to Footprint(0.6, 4.67, 6.27, 578.0),
    "Dairy Cows" to Footprint(3.9, 3.15, 8.95, 628.0),
)

data class Environment(val co2eKg: Double, val landM2: Double, val waterL: Double) {
    operator fun plus(o: Environment) = Environment(co2eKg + o.co2eKg, landM2 + o.landM2, waterL + o.waterL)

    companion object { val ZERO = Environment(0.0, 0.0, 0.0) }
}

val Impact.environment: Environment
    get() = FOOTPRINTS[species.name]?.let {
        val kg = netUnits * it.kgPerUnit
        Environment(kg * it.co2eKgPerKg, kg * it.landM2PerKg, kg * it.waterLPerKg)
    } ?: Environment.ZERO

val Layer.environment: Environment
    get() = impacts.filterNot { it.species.existsBecauseOfDemand }.fold(Environment.ZERO) { a, i -> a + i.environment }

/** Plain-language scale: tonnes, hectares, Olympic pools, car-km. */
internal fun Environment.describe(): String {
    val co2 = if (co2eKg >= 1000) "${format(co2eKg / 1000)} t" else "${format(co2eKg)} kg"
    val land = if (landM2 >= 10_000) "${format(landM2 / 10_000)} ha" else "${format(landM2)} m²"
    val water = if (waterL >= 2_500_000) "${format(waterL / 2_500_000)} Olympic pools" else "${format(waterL)} L"
    // ~0.17 kg CO2e per km for an average petrol car.
    return "$co2 CO₂e (${format(co2eKg / 0.17)} km of driving) · $land of land · $water of water"
}
