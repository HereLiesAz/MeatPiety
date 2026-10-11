package com.hereliesaz.meatpiety

/**
 * In-app notices that fire as the calculation crosses thresholds. Each [id]
 * names one threshold, so the UI can show only those newly crossed since the
 * last result.
 */
data class Notice(val id: String, val title: String, val body: String)

private val ANIMAL_STEPS = listOf(1.0, 10.0, 100.0, 1_000.0, 10_000.0)
private val POUND_STEPS = listOf(100.0, 1_000.0, 10_000.0)
private val MONEY_STEPS_USD = listOf(100.0, 1_000.0, 10_000.0)

fun milestones(days: Int, personal: Layer, influence: Layer?, world: Layer, money: Money): List<Notice> = buildList {
    // Whole animals you didn't eat, per species.
    personal.impacts.filterNot { it.species.existsBecauseOfDemand }.forEach { i ->
        ANIMAL_STEPS.filter { i.grossAnimals >= it }.maxOrNull()?.let { step ->
            val n = format(step)
            val title = if (step == 1.0) "Your first whole ${i.species.singular()}" else "$n ${i.species.name.lowercase()} off the ledger"
            add(Notice("animal:${i.species.name}:$step", title, "About ${format(step * i.species.unitsPerAnimal)} ${i.species.unit} you didn't buy."))
        }
        i.birthingsNotNeeded?.takeIf { it >= 1 }?.let {
            val b = i.species.biology!!
            add(Notice("birthing:${i.species.name}", "You've cancelled a ${b.birthing}", "One ${i.species.singular()} ${b.birthing} — about ${format(b.perBirthing)} — no longer needed."))
        }
    }

    personal.impacts.sumOf { if (it.species.unit == "lb") it.grossUnits else 0.0 }.let { lb ->
        POUND_STEPS.filter { lb >= it }.maxOrNull()?.let {
            add(Notice("pounds:$it", "${format(it)} lb of meat, unbought", "That's demand the market has to absorb or abandon."))
        }
    }

    personal.impacts.sumOf { it.grossUnits * it.usdPrice }.let { usd ->
        MONEY_STEPS_USD.filter { usd >= it }.maxOrNull()?.let {
            add(Notice("money:$it", "${money.format(it)} kept from the industry", "Retail spending you didn't hand over."))
        }
    }

    personal.impacts.firstOrNull { it.reboundAnimals >= 1 && !it.species.existsBecauseOfDemand }?.let {
        add(Notice(
            "rebound", "Someone else ate your ${it.species.singular()}",
            "Less demand lowers the price, and cheaper meat gets bought. Only part of what you skip goes unraised.",
        ))
    }

    personal.impacts.firstOrNull { it.species.existsBecauseOfDemand && it.grossAnimals >= 0.01 }?.let {
        add(Notice("bison", "Bison got rarer", "Ranchers breed them because people eat them. That line runs backward."))
    }

    SPECIES.mapNotNull { s -> s.biology?.let { s to it } }
        .filter { (s, b) -> s.name in OUTLIVED && days >= b.slaughterAgeDays }
        .forEach { (s, b) ->
            add(Notice("outlived:${s.name}", "You've outlived a ${s.singular()}", "Abstaining longer than a ${s.singular()} is allowed to live: ${b.slaughterAgeDays} days."))
        }

    if (influence != null && influence.netSpared > personal.netSpared) {
        add(Notice("contagious", "Your followers just out-spared you", "The people you've influenced now account for more animals than you do."))
    }

    world.impacts.filterNot { it.species.existsBecauseOfDemand }.forEach { i ->
        val cents = -i.priceChange * i.usdPrice
        if (cents >= 0.01) {
            add(Notice("price:${i.species.name}", "${i.species.name} is ${money.format(cents)} cheaper", "Per ${i.species.unit}, because of people like you."))
        }
        if (i.belowBreakeven) {
            add(Notice("breakeven:${i.species.name}", "A ${i.species.singular()} farmer went under water", "World abstention pushes the price past the producer margin."))
        }
        i.breedersNotKept?.takeIf { it >= 1 }?.let {
            add(Notice("breeders:${i.species.name}", "${format(it)} fewer breeding ${i.species.name.lowercase()}", "Kept to make more ${i.species.name.lowercase()}; no longer needed."))
        }
    }
}

private val OUTLIVED = setOf("Chickens", "Pigs", "Cows")

internal fun Species.singular(): String = when (name) {
    "Chickens" -> "chicken"
    "Turkeys" -> "turkey"
    "Ducks" -> "duck"
    "Pigs" -> "pig"
    "Cows" -> "cow"
    "Sheep & Lambs" -> "lamb"
    "Egg-Laying Hens" -> "hen"
    "Dairy Cows" -> "dairy cow"
    "Buffalo" -> "bison"
    else -> name.lowercase()
}
