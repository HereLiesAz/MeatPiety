package com.hereliesaz.savethebuffalo

data class MarketEffect(
    val name: String,
    val unit: String,
    val currentPrice: Double,
    val percentPriceChange: Double,
    val newPrice: Double,
    val belowBreakeven: Boolean,
)

private data class ElasticityProfile(
    val name: String,
    val price: Double,
    val unit: String,
    // Magnitude of own-price demand elasticity and short-run supply elasticity,
    // from ag-econ meta-analyses (Tonsor & Olynk-style reviews, USDA ERS's
    // Commodity and Food Elasticities database). Supply elasticities track
    // each species' breeding cycle: cattle and bison are slow to expand or
    // shrink a herd, poultry turns over in weeks.
    val demandElasticity: Double,
    val supplyElasticity: Double,
    val requiresVeganism: Boolean,
    // Rough share of retail price that is producer margin; a price drop past
    // this wipes out the margin rather than just squeezing it, so breeding
    // stock gets culled instead of held. Illustrative, not audited.
    val breakevenDrop: Double,
)

// Prices are 2024-2025 U.S. retail averages (USDA ERS Meat Price Spreads,
// USDA egg and dairy market reports); bison and lamb are typical specialty
// retail prices, since USDA does not track them at the same granularity.
private val PROFILES = listOf(
    ElasticityProfile("Chickens", 4.20, "per lb", 0.4, 0.5, false, 0.25),
    ElasticityProfile("Fish", 12.00, "per lb", 0.5, 0.3, false, 0.20),
    ElasticityProfile("Shellfish", 11.00, "per lb", 0.6, 0.3, false, 0.20),
    ElasticityProfile("Turkeys", 2.20, "per lb", 0.4, 0.4, false, 0.20),
    ElasticityProfile("Ducks", 5.50, "per lb", 0.4, 0.4, false, 0.20),
    ElasticityProfile("Pigs", 4.00, "per lb", 0.5, 0.3, false, 0.20),
    ElasticityProfile("Cows", 5.50, "per lb", 0.5, 0.15, false, 0.15),
    ElasticityProfile("Sheep & Lambs", 9.50, "per lb", 0.5, 0.2, false, 0.15),
    ElasticityProfile("Egg-Laying Hens", 3.78, "per dozen eggs", 0.2, 0.3, true, 0.20),
    ElasticityProfile("Dairy Cows", 4.00, "per gallon milk", 0.3, 0.15, true, 0.15),
    // Bison ranching is a small, slow-turning industry: committed buyers, a
    // near-year-long breeding lag, and no factory-farm supply cushion. The
    // same demand shock that dents a chicken farmer's margin can crater theirs.
    ElasticityProfile("Buffalo", 9.00, "per lb", 0.3, 0.1, false, 0.20),
)

fun marketEffects(): List<MarketEffect> = PROFILES.map { profile ->
    val abstainers = if (profile.requiresVeganism) {
        VEGANS_WORLDWIDE
    } else {
        VEGANS_WORLDWIDE + VEGETARIANS_WORLDWIDE
    }
    val demandShockFraction = abstainers.toDouble() / WORLD_POPULATION
    // Standard partial-equilibrium comparative statics for a demand-curve
    // shift: %change in price = -shift / (supply elasticity + |demand elasticity|).
    val percentChange = -demandShockFraction / (profile.supplyElasticity + profile.demandElasticity)
    MarketEffect(
        name = profile.name,
        unit = profile.unit,
        currentPrice = profile.price,
        percentPriceChange = percentChange,
        newPrice = profile.price * (1 + percentChange),
        belowBreakeven = -percentChange >= profile.breakevenDrop,
    )
}
