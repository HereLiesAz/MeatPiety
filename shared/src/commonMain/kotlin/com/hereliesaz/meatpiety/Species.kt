package com.hereliesaz.meatpiety

enum class Diet { VEGAN, VEGETARIAN }

/**
 * Everything the ledger knows about one animal: how much of it ends up sold,
 * what that sells for, how fast its kind replaces itself, and how its market
 * bends when demand moves.
 *
 * Units: [unit] is what the retail price is quoted in ("lb", "dozen eggs",
 * "gallon milk"). [unitsPerAnimal] converts animals to that unit; for hens and
 * dairy cows it is a year of output from one animal, not a carcass.
 */
data class Species(
    val name: String,
    val unit: String,
    // U.S. retail consumption per person per year, in [unit]. USDA ERS food
    // availability (boneless retail weight), NOAA Fisheries for seafood,
    // USDA egg/dairy supply-and-use tables. Chosen so that
    // usPerCapita / unitsPerAnimal matches Harish Sethu's Counting Animals
    // per-capita animal counts.
    val usPerCapita: Double,
    // Retail units yielded per animal: live weight × dressing % × retail cut
    // yield (USDA AMS / extension carcass data). Fish and shellfish are averaged
    // across species eaten, so they are rough.
    val unitsPerAnimal: Double,
    // World per-capita supply ÷ U.S. per-capita supply (FAO food balance
    // sheets via Our World in Data). Used to size the global market and the
    // world's abstainers, who would mostly not have eaten like Americans.
    val worldToUsRatio: Double,
    // U.S. retail price in USD per [unit]; replaced by live BLS averages when
    // the price service answers.
    val usdPrice: Double,
    // Own-price demand elasticity magnitude and short-run supply elasticity
    // (USDA ERS elasticity database; Norwood & Lusk, Compassion by the Pound).
    val demandElasticity: Double,
    val supplyElasticity: Double,
    // Share of retail price that is producer margin. A price drop past it
    // means breeding stock is culled, not held. Illustrative.
    val breakevenDrop: Double,
    val requiresVeganism: Boolean,
    // Bison exist mostly because people buy them. Less demand means fewer bred.
    val existsBecauseOfDemand: Boolean = false,
    val biology: Biology? = null,
)

/** Reproduction, used to turn animals-not-raised into breeders and births not needed. */
data class Biology(
    // What one birthing is called and how many it produces.
    val birthing: String,
    val perBirthing: Double,
    // Market animals produced per breeding female per year (after mortality).
    val offspringPerBreederYear: Double,
    val gestationDays: Int,
    // Age at slaughter, or at culling for hens and dairy cows.
    val slaughterAgeDays: Int,
    // Natural lifespan, for the "you've outlived one" notices.
    val naturalLifespanDays: Int,
) {
    /** How long a breeding decision takes to show up as fewer animals at market. */
    val supplyLagDays: Int get() = gestationDays + slaughterAgeDays
}

internal val SPECIES = listOf(
    Species(
        "Chickens", "lb", usPerCapita = 100.0, unitsPerAnimal = 4.31, worldToUsRatio = 0.29,
        usdPrice = 2.01, demandElasticity = 0.4, supplyElasticity = 0.5, breakevenDrop = 0.25,
        requiresVeganism = false,
        biology = Biology("clutch", 12.0, 130.0, 21, 47, 8 * 365),
    ),
    Species(
        "Fish", "lb", usPerCapita = 12.5, unitsPerAnimal = 0.81, worldToUsRatio = 0.9,
        usdPrice = 12.00, demandElasticity = 0.5, supplyElasticity = 0.3, breakevenDrop = 0.20,
        requiresVeganism = false,
    ),
    Species(
        "Shellfish", "lb", usPerCapita = 8.0, unitsPerAnimal = 0.061, worldToUsRatio = 0.9,
        usdPrice = 11.00, demandElasticity = 0.6, supplyElasticity = 0.3, breakevenDrop = 0.20,
        requiresVeganism = false,
    ),
    Species(
        "Turkeys", "lb", usPerCapita = 15.0, unitsPerAnimal = 21.4, worldToUsRatio = 0.15,
        usdPrice = 1.60, demandElasticity = 0.4, supplyElasticity = 0.4, breakevenDrop = 0.20,
        requiresVeganism = false,
        biology = Biology("clutch", 12.0, 80.0, 28, 140, 10 * 365),
    ),
    Species(
        "Ducks", "lb", usPerCapita = 0.33, unitsPerAnimal = 3.3, worldToUsRatio = 6.0,
        usdPrice = 5.50, demandElasticity = 0.4, supplyElasticity = 0.4, breakevenDrop = 0.20,
        requiresVeganism = false,
        biology = Biology("clutch", 12.0, 120.0, 28, 49, 10 * 365),
    ),
    Species(
        "Pigs", "lb", usPerCapita = 50.0, unitsPerAnimal = 140.0, worldToUsRatio = 0.52,
        usdPrice = 4.00, demandElasticity = 0.5, supplyElasticity = 0.3, breakevenDrop = 0.20,
        requiresVeganism = false,
        biology = Biology("litter", 11.5, 25.0, 114, 180, 15 * 365),
    ),
    Species(
        "Cows", "lb", usPerCapita = 59.0, unitsPerAnimal = 490.0, worldToUsRatio = 0.24,
        usdPrice = 5.50, demandElasticity = 0.5, supplyElasticity = 0.15, breakevenDrop = 0.15,
        requiresVeganism = false,
        biology = Biology("calving", 1.0, 0.9, 283, 540, 20 * 365),
    ),
    Species(
        "Sheep & Lambs", "lb", usPerCapita = 1.1, unitsPerAnimal = 47.0, worldToUsRatio = 4.0,
        usdPrice = 9.50, demandElasticity = 0.5, supplyElasticity = 0.2, breakevenDrop = 0.15,
        requiresVeganism = false,
        biology = Biology("lambing", 1.5, 1.3, 150, 210, 12 * 365),
    ),
    Species(
        "Egg-Laying Hens", "dozen eggs", usPerCapita = 23.3, unitsPerAnimal = 25.0, worldToUsRatio = 0.64,
        usdPrice = 3.78, demandElasticity = 0.2, supplyElasticity = 0.3, breakevenDrop = 0.20,
        requiresVeganism = true,
        // About half of hatched layer chicks are male and are culled at hatch;
        // 60 is pullets kept per breeder hen per year.
        biology = Biology("clutch", 12.0, 60.0, 21, 540, 8 * 365),
    ),
    Species(
        "Dairy Cows", "gallon milk", usPerCapita = 76.0, unitsPerAnimal = 2_790.0, worldToUsRatio = 0.43,
        usdPrice = 4.00, demandElasticity = 0.3, supplyElasticity = 0.15, breakevenDrop = 0.15,
        requiresVeganism = true,
        biology = Biology("calving", 1.0, 0.9, 283, 1_800, 20 * 365),
    ),
    Species(
        "Buffalo", "lb", usPerCapita = 0.096, unitsPerAnimal = 400.0, worldToUsRatio = 0.01,
        usdPrice = 9.00, demandElasticity = 0.3, supplyElasticity = 0.1, breakevenDrop = 0.20,
        requiresVeganism = false, existsBecauseOfDemand = true,
        biology = Biology("calving", 1.0, 0.8, 285, 820, 20 * 365),
    ),
)

internal fun Species.eatenBy(diet: Diet) = diet == Diet.VEGAN || !requiresVeganism

// Global vegan/vegetarian population, order-of-magnitude estimates from Ipsos,
// the Vegan Society, and national vegetarianism surveys (dominated by India's
// ~500 million vegetarians). Nobody is counting precisely.
internal const val VEGANS_WORLDWIDE = 100_000_000L
internal const val VEGETARIANS_WORLDWIDE = 800_000_000L
internal const val WORLD_POPULATION = 8_100_000_000L
