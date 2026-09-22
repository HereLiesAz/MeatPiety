package com.hereliesaz.savethebuffalo

enum class Diet { VEGAN, VEGETARIAN }

data class SparedAnimal(
    val name: String,
    val count: Double,
    val existsBecauseOfDemand: Boolean = false,
)

data class WorldTally(
    val veganPopulation: Long,
    val vegetarianPopulation: Long,
    val animals: List<SparedAnimal>,
)

private data class AnimalRate(
    val name: String,
    val perYearOnStandardDiet: Double,
    val requiresVeganism: Boolean,
    val existsBecauseOfDemand: Boolean = false,
)

// Annual per-capita figures for a standard U.S. omnivorous diet, rounded from
// commonly cited slaughter and dairy/egg industry averages (Counting Animals, USDA).
private val RATES = listOf(
    AnimalRate("Chickens", 28.5, requiresVeganism = false),
    AnimalRate("Fish", 15.0, requiresVeganism = false),
    AnimalRate("Shellfish", 130.0, requiresVeganism = false),
    AnimalRate("Turkeys", 0.66, requiresVeganism = false),
    AnimalRate("Ducks", 0.35, requiresVeganism = false),
    AnimalRate("Pigs", 0.35, requiresVeganism = false),
    AnimalRate("Cows", 0.08, requiresVeganism = false),
    AnimalRate("Sheep & Lambs", 0.03, requiresVeganism = false),
    AnimalRate("Egg-Laying Hens", 0.9, requiresVeganism = true),
    AnimalRate("Dairy Cows", 0.1, requiresVeganism = true),
    // Bison survive today mostly as livestock; ranchers breed them because people eat
    // them. Less demand means fewer bred, not fewer killed, so this one runs backward.
    AnimalRate("Buffalo", 0.0015, requiresVeganism = false, existsBecauseOfDemand = true),
)

// Rough, widely-cited orders of magnitude for world vegan/vegetarian populations.
// Nobody is actually counting; these exist to make a point, not to survey the planet.
private const val VEGANS_WORLDWIDE = 100_000_000L
private const val VEGETARIANS_WORLDWIDE = 700_000_000L

fun animalsSpared(days: Int, diet: Diet): List<SparedAnimal> {
    val years = days / 365.0
    return RATES
        .filter { diet == Diet.VEGAN || !it.requiresVeganism }
        .map {
            val raw = it.perYearOnStandardDiet * years
            SparedAnimal(it.name, if (it.existsBecauseOfDemand) -raw else raw, it.existsBecauseOfDemand)
        }
}

fun sphereOfInfluence(personal: List<SparedAnimal>, friendCount: Int): List<SparedAnimal> {
    val multiplier = (friendCount + 1).toDouble()
    return personal.map { it.copy(count = it.count * multiplier) }
}

fun worldTally(days: Int): WorldTally {
    val veganShare = animalsSpared(days, Diet.VEGAN).associateBy { it.name }
    val vegetarianShare = animalsSpared(days, Diet.VEGETARIAN).associateBy { it.name }
    val combined = RATES.map { rate ->
        val fromVegans = (veganShare[rate.name]?.count ?: 0.0) * VEGANS_WORLDWIDE
        val fromVegetarians = (vegetarianShare[rate.name]?.count ?: 0.0) * VEGETARIANS_WORLDWIDE
        SparedAnimal(rate.name, fromVegans + fromVegetarians, rate.existsBecauseOfDemand)
    }
    return WorldTally(VEGANS_WORLDWIDE, VEGETARIANS_WORLDWIDE, combined)
}
