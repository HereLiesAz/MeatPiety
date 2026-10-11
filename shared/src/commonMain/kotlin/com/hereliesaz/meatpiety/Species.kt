package com.hereliesaz.meatpiety

enum class Diet { VEGAN, VEGETARIAN }

/**
 * Everything the ledger knows about one animal: how much of it ends up sold,
 * what that sells for, how fast its kind replaces itself, and how its market
 * bends when demand moves.
 *
 * Units: [unit] is what the retail price is quoted in ("lb", "dozen eggs",
 * "gallon milk"). [unitsPerAnimal] converts animals to that unit: a carcass for
 * meat, a productive lifetime of output for hens and dairy cows, so every
 * count is whole animals, never animal-years.
 */
data class Species(
    val name: String,
    val unit: String,
    // U.S. retail consumption per person per year, in [unit]. USDA ERS food
    // availability (retail weight), NOAA Fisheries for seafood,
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
    // Own-price demand elasticity magnitude: means of USDA ERS meta-analysis
    // (Chen & Abler 2012): beef 0.80, pork 0.67, poultry 0.71, eggs 0.82,
    // dairy 0.78. Long-run supply elasticity is backed out of Norwood & Lusk's
    // cumulative elasticity factors (Compassion by the Pound, 2011, fig. 8.2:
    // share of a demand cut that becomes a production cut — beef 0.68, pork
    // 0.74, chicken 0.76, eggs 0.91, milk 0.56) as es = CEF·ed / (1 − CEF).
    // Fish and shellfish have no such sources; their values are illustrative.
    val demandElasticity: Double,
    val supplyElasticity: Double,
    // Producer margin as a share of price. Only used to set the small-farm
    // margin in Consolidation.kt (half of this). Illustrative.
    val breakevenDrop: Double,
    val requiresVeganism: Boolean,
    // Bison exist mostly because people buy them. Less demand means fewer bred.
    val existsBecauseOfDemand: Boolean = false,
    val biology: Biology? = null,
    // World-average yield per animal where it differs sharply from the U.S.
    // (FAOSTAT: world cow milk ÷ dairy cows ≈ 655 gal/yr vs ≈ 2,790 in the
    // U.S.; world eggs ÷ laying hens ≈ 17.9 dozen/yr vs 25). Used for the
    // influence and world layers. Null = same as U.S.
    val worldUnitsPerAnimal: Double? = null,
    // Seafood only: share farmed (FAO SOFIA 2024: aquaculture is ~51% of
    // aquatic animal production). Wild catch is quota-bound, so demand cuts
    // there move price, not catch; only the farmed share changes production.
    val farmedShare: Double = 1.0,
    // Seafood only: wild forage fish caught for feed per [unit] farmed
    // (fish-in:fish-out ≈ 0.3 kg/kg, IFFO/FAO) ÷ ~20 g per anchovy-class fish.
    val feedFishPerUnit: Double = 0.0,
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
    // Share of the breeding herd replaced each year by animals born into it
    // (USDA / extension herd-management figures: broiler, turkey, duck and
    // layer breeder flocks are replaced about yearly; sows ~45%; beef cows
    // ~15%; ewes ~20%; dairy cows ~35%; bison ~12%).
    val replacementRate: Double,
    // Age at which a replacement first breeds.
    val firstBreedingDays: Int,
    // Producers of eggs and milk cut output by culling early, not by waiting
    // out a slaughter age. Null for meat animals.
    val outputLagDays: Int? = null,
    // Males born per female kept who never enter the product: male layer
    // chicks killed at hatch (about one per pullet), male dairy calves sent
    // to veal or beef (~0.45 a cow-year). Null where males are raised for meat.
    val malesPerAnimal: Double? = null,
) {
    /** How long a breeding decision takes to show up at market. */
    val supplyLagDays: Int get() = outputLagDays ?: (gestationDays + slaughterAgeDays)
}

internal val SPECIES = listOf(
    Species(
        "Chickens", "lb", usPerCapita = 100.0, unitsPerAnimal = 4.31, worldToUsRatio = 0.29,
        usdPrice = 2.01, demandElasticity = 0.71, supplyElasticity = 2.25, breakevenDrop = 0.25,
        requiresVeganism = false,
        biology = Biology("clutch", 12.0, 130.0, 21, 47, 8 * 365, 1.0, 168),
    ),
    Species(
        "Fish", "lb", usPerCapita = 12.5, unitsPerAnimal = 0.81, worldToUsRatio = 0.9,
        usdPrice = 12.00, demandElasticity = 0.5, supplyElasticity = 0.3, breakevenDrop = 0.20,
        requiresVeganism = false, farmedShare = 0.51, feedFishPerUnit = 0.3 * 0.4536 / 0.02,
    ),
    Species(
        "Shellfish", "lb", usPerCapita = 8.0, unitsPerAnimal = 0.061, worldToUsRatio = 0.9,
        usdPrice = 11.00, demandElasticity = 0.6, supplyElasticity = 0.3, breakevenDrop = 0.20,
        requiresVeganism = false, farmedShare = 0.51, feedFishPerUnit = 0.3 * 0.4536 / 0.02,
    ),
    Species(
        "Turkeys", "lb", usPerCapita = 15.0, unitsPerAnimal = 21.4, worldToUsRatio = 0.15,
        usdPrice = 1.60, demandElasticity = 0.71, supplyElasticity = 2.25, breakevenDrop = 0.20,
        requiresVeganism = false,
        biology = Biology("clutch", 12.0, 80.0, 28, 140, 10 * 365, 1.0, 210),
    ),
    Species(
        "Ducks", "lb", usPerCapita = 0.33, unitsPerAnimal = 3.3, worldToUsRatio = 6.0,
        usdPrice = 5.50, demandElasticity = 0.71, supplyElasticity = 2.25, breakevenDrop = 0.20,
        requiresVeganism = false,
        biology = Biology("clutch", 12.0, 120.0, 28, 49, 10 * 365, 1.0, 182),
    ),
    Species(
        "Pigs", "lb", usPerCapita = 50.0, unitsPerAnimal = 140.0, worldToUsRatio = 0.52,
        usdPrice = 4.00, demandElasticity = 0.67, supplyElasticity = 1.91, breakevenDrop = 0.20,
        requiresVeganism = false,
        biology = Biology("litter", 11.5, 25.0, 114, 180, 15 * 365, 0.45, 240),
    ),
    Species(
        "Cows", "lb", usPerCapita = 59.0, unitsPerAnimal = 490.0, worldToUsRatio = 0.24,
        usdPrice = 5.50, demandElasticity = 0.8, supplyElasticity = 1.7, breakevenDrop = 0.15,
        requiresVeganism = false,
        biology = Biology("calving", 1.0, 0.9, 283, 540, 20 * 365, 0.15, 450),
    ),
    Species(
        "Sheep & Lambs", "lb", usPerCapita = 1.1, unitsPerAnimal = 47.0, worldToUsRatio = 4.0,
        usdPrice = 9.50, demandElasticity = 0.8, supplyElasticity = 1.7, breakevenDrop = 0.15,
        requiresVeganism = false,
        biology = Biology("lambing", 1.5, 1.3, 150, 210, 12 * 365, 0.2, 240),
    ),
    Species(
        // A hen lays about one year before she is culled, so a hen-year is a hen.
        "Egg-Laying Hens", "dozen eggs", usPerCapita = 23.3, unitsPerAnimal = 25.0, worldToUsRatio = 0.64,
        usdPrice = 3.78, demandElasticity = 0.82, supplyElasticity = 8.29, breakevenDrop = 0.20,
        requiresVeganism = true, worldUnitsPerAnimal = 17.9,
        // 60 pullets kept per breeder hen per year; each pullet's brother is
        // killed at hatch.
        biology = Biology("clutch", 12.0, 60.0, 21, 540, 8 * 365, 1.0, 168, outputLagDays = 90, malesPerAnimal = 1.0),
    ),
    Species(
        // Lifetime output: ~2,790 gal a year over ~1 / 0.35 = 2.9 milking years.
        "Dairy Cows", "gallon milk", usPerCapita = 76.0, unitsPerAnimal = 2_790.0 / 0.35, worldToUsRatio = 0.43,
        usdPrice = 4.00, demandElasticity = 0.78, supplyElasticity = 0.99, breakevenDrop = 0.15,
        requiresVeganism = true, worldUnitsPerAnimal = 655.0 / 0.35,
        // ~0.9 calves a year for 2.9 years, half male.
        biology = Biology("calving", 1.0, 0.9, 283, 1_800, 20 * 365, 0.35, 450, outputLagDays = 90, malesPerAnimal = 0.45 / 0.35),
    ),
    Species(
        "Buffalo", "lb", usPerCapita = 0.096, unitsPerAnimal = 400.0, worldToUsRatio = 0.01,
        usdPrice = 9.00, demandElasticity = 0.8, supplyElasticity = 1.7, breakevenDrop = 0.20,
        requiresVeganism = false, existsBecauseOfDemand = true,
        biology = Biology("calving", 1.0, 0.8, 285, 820, 20 * 365, 0.12, 730),
    ),
)

internal fun Species.eatenBy(diet: Diet) = diet == Diet.VEGAN || !requiresVeganism

// Global vegan/vegetarian population. No authoritative count exists: Ipsos
// (2018, 28 countries) found 5% vegetarian and 3% vegan; most vegetarians are
// in India. Order-of-magnitude, and labelled so in the app.
internal const val VEGANS_WORLDWIDE = 100_000_000L
internal const val VEGETARIANS_WORLDWIDE = 800_000_000L

