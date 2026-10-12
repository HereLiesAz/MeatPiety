package com.hereliesaz.meatpiety

import android.content.Context

/**
 * Everything the home-screen widgets show, computed from the saved start date
 * and diet with the same model as the app (built-in prices; widgets don't hit
 * the network). Null until the user has picked a start date.
 */
data class WidgetSnapshot(
    val days: Int,
    val vegan: Boolean,
    /** Fewer animals slaughtered so far, net of culls and the dairy–beef offset. */
    val fewerSlaughtered: Double,
    /** Every animal never born, including replacement breeders and males. */
    val neverBorn: Double,
    val litters: Double,
    val malesNeverBorn: Double,
    /** Species ranked by animals never bred, top first: name to count. */
    val bySpecies: List<Pair<String, Double>>,
    /** The species whose next whole animal is soonest, with progress 0–1 and days to go. */
    val nextAnimal: NextAnimal?,
    val co2eKg: Double,
    val landM2: Double,
    val waterL: Double,
    /** How many full farm lifetimes (slaughter ages) your span covers, per species. */
    val lifetimesOutlived: List<Pair<String, Int>>,
)

data class NextAnimal(val species: String, val singular: String, val count: Long, val progress: Double, val daysToNext: Long)

fun widgetSnapshot(context: Context): WidgetSnapshot? {
    AppContext.init(context)
    val days = StartDate.days() ?: return null
    val diet = Storage.get("diet")?.let { d -> Diet.entries.firstOrNull { it.name == d } } ?: Diet.VEGAN
    val layer = personalLayer(days, diet)
    val alive = layer.impacts.filterNot { it.species.existsBecauseOfDemand }
    val env = layer.environment
    val next = alive.filter { it.neverBred > 0 && days > 0 }.map { i ->
        val perDay = i.neverBred / days
        val whole = kotlin.math.floor(i.neverBred)
        NextAnimal(
            species = i.species.name,
            singular = i.species.singular(),
            count = whole.toLong(),
            progress = i.neverBred - whole,
            daysToNext = kotlin.math.ceil((whole + 1 - i.neverBred) / perDay).toLong(),
        )
    }.minByOrNull { it.daysToNext }
    return WidgetSnapshot(
        days = days,
        vegan = diet == Diet.VEGAN,
        fewerSlaughtered = layer.netSpared,
        neverBorn = layer.neverBorn,
        litters = alive.sumOf { it.birthingsNotNeeded ?: 0.0 },
        malesNeverBorn = alive.sumOf { it.malesNeverBorn ?: 0.0 },
        bySpecies = alive.map { it.species.name to it.totalNeverBorn }.sortedByDescending { it.second },
        nextAnimal = next,
        co2eKg = env.co2eKg,
        landM2 = env.landM2,
        waterL = env.waterL,
        lifetimesOutlived = SPECIES.mapNotNull { s -> s.biology?.let { s.singular() to days / it.slaughterAgeDays } }
            .filter { it.second > 0 }.sortedByDescending { it.second },
    )
}

/** Short number for widgets: 1.2k, 3.4M. */
fun compact(n: Double): String = when {
    n >= 1e6 -> "${formatOneDecimal(n / 1e6)}M"
    n >= 1e4 -> "${kotlin.math.round(n / 1e3).toLong()}k"
    n >= 1e3 -> "${formatOneDecimal(n / 1e3)}k"
    n >= 10 -> kotlin.math.round(n).toLong().toString()
    else -> formatOneDecimal(n)
}
