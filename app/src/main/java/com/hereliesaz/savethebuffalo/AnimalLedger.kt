package com.hereliesaz.savethebuffalo

enum class Diet { VEGAN, VEGETARIAN }

data class SparedAnimal(val name: String, val count: Double)

private data class AnimalRate(
    val name: String,
    val perYearOnStandardDiet: Double,
    val requiresVeganism: Boolean,
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
)

fun animalsSpared(days: Int, diet: Diet): List<SparedAnimal> {
    val years = days / 365.0
    return RATES
        .filter { diet == Diet.VEGAN || !it.requiresVeganism }
        .map { SparedAnimal(it.name, it.perYearOnStandardDiet * years) }
}
