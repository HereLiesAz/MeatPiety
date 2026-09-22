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

// Annual per-capita figures for a standard U.S. omnivorous diet. Land- and
// sea-animal direct-consumption counts follow Harish Sethu's Counting Animals
// analysis of USDA/FAO data (chickens, turkeys, pigs, cows, ducks, fish,
// shellfish); egg-laying hens and dairy cows are derived from USDA/UEP
// inventory versus U.S. population; buffalo from the National Bison
// Association's 2023 U.S. harvest count versus U.S. population. Sheep and
// lambs are derived from USDA per-capita lamb/mutton consumption. All are
// best available real-world estimates, not lab-grade measurements.
private val RATES = listOf(
    AnimalRate("Chickens", 23.2, requiresVeganism = false),
    AnimalRate("Fish", 15.5, requiresVeganism = false),
    AnimalRate("Shellfish", 131.1, requiresVeganism = false),
    AnimalRate("Turkeys", 0.7, requiresVeganism = false),
    AnimalRate("Ducks", 0.1, requiresVeganism = false),
    AnimalRate("Pigs", 0.4, requiresVeganism = false),
    AnimalRate("Cows", 0.1, requiresVeganism = false),
    AnimalRate("Sheep & Lambs", 0.02, requiresVeganism = false),
    AnimalRate("Egg-Laying Hens", 0.9, requiresVeganism = true),
    AnimalRate("Dairy Cows", 0.028, requiresVeganism = true),
    // Bison survive today mostly as livestock; ranchers breed them because people eat
    // them. Less demand means fewer bred, not fewer killed, so this one runs backward.
    AnimalRate("Buffalo", 0.000239, requiresVeganism = false, existsBecauseOfDemand = true),
)

// Global vegan/vegetarian population, order-of-magnitude estimates from Ipsos,
// the Vegan Society, and aggregated national vegetarianism surveys (dominated
// by India's ~500 million vegetarians). Nobody is actually counting precisely.
private const val VEGANS_WORLDWIDE = 100_000_000L
private const val VEGETARIANS_WORLDWIDE = 800_000_000L

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
