package com.hereliesaz.meatpiety

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EconomicsTest {
    @Test
    fun zeroDaysSparesNothing() {
        assertTrue(personalLayer(0, Diet.VEGAN).impacts.all { it.grossAnimals == 0.0 && it.netAnimals == 0.0 })
    }

    @Test
    fun oneYearGrossMatchesPerCapitaAnimals() {
        personalLayer(365, Diet.VEGAN).impacts.forEach {
            assertEquals(it.species.usPerCapita / it.species.unitsPerAnimal, it.grossAnimals, 1e-9)
        }
    }

    @Test
    fun netIsGrossTimesSupplyShareAfterTheLag() {
        // For a marginal buyer, the long-run cut is gross · es / (es + ed), and it
        // only reaches slaughter once the supply lag has passed; the rest is rebound.
        val days = 3650
        personalLayer(days, Diet.VEGAN).impacts.forEach {
            val s = it.species
            val es = s.supplyElasticity * s.farmedShare
            val share = es / (es + s.demandElasticity)
            val lag = s.biology?.supplyLagDays ?: 90
            val expected = it.grossAnimals / days * share * (days - lag)
            assertEquals(expected, it.netAnimals + (it.cullWave ?: 0.0), expected * 1e-3)
            assertTrue(it.reboundAnimals > 0)
            // Meat animals: births fall before slaughter does. (Egg and milk producers cut output by culling, before births.)
            if (s.biology?.outputLagDays == null) assertTrue(it.neverBred >= it.netAnimals, s.name)
        }
    }

    @Test
    fun nothingIsSparedBeforeProducersCanRespond() {
        val chicken = personalLayer(30, Diet.VEGAN).impacts.first { it.species.name == "Chickens" }
        // Before the lag, the only change in slaughter is the early cull of breeders.
        assertEquals(-chicken.cullWave!!, chicken.netAnimals, 1e-12)
        assertEquals(chicken.grossAnimals, chicken.bredAnyway, 1e-9)
        assertTrue(chicken.shortRunPriceChange < chicken.priceChange)
    }

    @Test
    fun vegetariansExcludeEggAndDairy() {
        val names = personalLayer(365, Diet.VEGETARIAN).impacts.map { it.species.name }
        SPECIES.filter { it.requiresVeganism }.forEach { assertFalse(it.name in names) }
    }

    @Test
    fun worldShockLowersPricesAndRetiresBreeders() {
        worldLayer(365).impacts.forEach {
            assertTrue(it.priceChange < 0, it.species.name)
            assertTrue(it.retailSalesLostUsd > 0, it.species.name)
            assertTrue(it.counterfactualUsdPrice > it.usdPrice, it.species.name)
            it.breedersNotKept?.let { b -> assertTrue(b > 0) }
        }
    }

    @Test
    fun largerShockMovesPriceMore() {
        val you = personalLayer(365, Diet.VEGAN).impacts.first()
        val world = worldLayer(365).impacts.first { it.species == you.species }
        assertTrue(world.priceChange < you.priceChange)
    }

    @Test
    fun livePricesOverrideDefaults() {
        val chicken = personalLayer(365, Diet.VEGAN, mapOf("Chickens" to 9.0)).impacts.first { it.species.name == "Chickens" }
        assertEquals(9.0, chicken.usdPrice)
    }
}

class InfluenceTest {
    @Test
    fun noAccountsMeansNoInfluence() {
        assertNull(influenceFunnel(emptyList()))
        assertNull(influenceFunnel(listOf(LinkedAccount(Platform.TIKTOK, "x", FollowerLookup.Failed("blocked")))))
    }

    @Test
    fun overlappingAudiencesAreDiscounted() {
        val a = LinkedAccount(Platform.BLUESKY, "a", FollowerLookup.Found(1_000))
        val b = LinkedAccount(Platform.MASTODON, "b", FollowerLookup.Found(1_000))
        val funnel = influenceFunnel(listOf(a, b))!!
        assertEquals(150.0 + 150.0 * OVERLAP_DISCOUNT, funnel.reached, 1e-9)
        assertEquals(funnel.reached * (1 - kotlin.math.exp(-PERSUASION_RATE)) * RETENTION_RATE, funnel.stayed, 1e-9)
    }
}

class MilestoneTest {
    @Test
    fun aYearCrossesAnimalAndOutlivedMilestones() {
        val notices = milestones(365, personalLayer(365, Diet.VEGAN), null, worldLayer(365), Money.USD).map { it.id }
        assertTrue("animal:Chickens:10.0" in notices)
        assertTrue("outlived:Chickens" in notices)
        assertFalse("contagious" in notices)
    }

    @Test
    fun moneyFormatsInLocalCurrency() {
        assertEquals("€9.20", Money("EUR", 0.92).format(10.0))
        assertEquals("¥1,500", Money("JPY", 150.0).format(10.0))
        assertEquals("100.00 SEK", Money("SEK", 10.0).format(10.0))
    }
}

class WholeAnimalTest {
    @Test
    fun fractionsReadAsProgressToTheNextWholeAnimal() {
        assertEquals("2 · the next in 20 days", wholeAnimals(2.5, 100))
        assertEquals("25% of the first · the next in 300 days", wholeAnimals(0.25, 100))
        assertEquals("none yet", wholeAnimals(0.0, 100))
    }

    @Test
    fun fewerInBarnsWaitsForGestationAndCapsAtSlaughterAge() {
        val cow = { d: Int -> personalLayer(d, Diet.VEGAN).impacts.first { it.species.name == "Cows" } }
        // Nothing is missing from barns before the first birth that doesn't happen.
        assertEquals(0.0, cow(200).fewerInBarns)
        val a = cow(300).fewerInBarns!!
        val b = cow(3650).fewerInBarns!!
        // 300 days is 17 days past gestation (283); ten years caps at slaughter age (540).
        assertEquals(a / 17 * 540, b, b * 1e-9)
    }

    @Test
    fun earlyCullsAreExtraSlaughterThatUnwinds() {
        val pig = { d: Int -> personalLayer(d, Diet.VEGAN).impacts.first { it.species.name == "Pigs" } }
        assertTrue(pig(30).netAnimals < 0)
        assertEquals(0.0, pig(3650).cullWave!!)
    }

    @Test
    fun dairyAndEggsCountLifetimesAndBrothers() {
        val layer = personalLayer(3650, Diet.VEGAN)
        val dairy = layer.impacts.first { it.species.name == "Dairy Cows" }
        // ~0.027 cow-years a person-year ÷ 2.9 milking years per cow.
        assertEquals(76.0 / (2790.0 / 0.35), dairy.grossAnimals / 10, 1e-9)
        assertEquals(dairy.neverBred * 0.45 / 0.35, dairy.malesNeverBorn!!, 1e-9)
        val hens = layer.impacts.first { it.species.name == "Egg-Laying Hens" }
        assertEquals(hens.neverBred, hens.malesNeverBorn!!, 1e-9)
        assertTrue(layer.dairyBeefOffset > 0)
    }

    @Test
    fun wildCatchDoesNotRespond() {
        val fish = personalLayer(3650, Diet.VEGAN).impacts.first { it.species.name == "Fish" }
        val share = 0.3 * 0.51 / (0.3 * 0.51 + 0.5)
        assertEquals(fish.grossUnits * share * (3650 - 90) / 3650, fish.netUnits, fish.netUnits * 1e-3)
        assertTrue(fish.forageFishNotCaught > 0)
    }

    @Test
    fun worldUsesWorldYields() {
        val us = personalLayer(365, Diet.VEGAN).impacts.first { it.species.name == "Dairy Cows" }
        val world = influenceLayer(1.0, 730, Diet.VEGAN).impacts.first { it.species.name == "Dairy Cows" }
        assertEquals(2790.0 / 655.0, world.grossAnimals / world.grossUnits / (us.grossAnimals / us.grossUnits), 1e-9)
    }

    @Test
    fun rangeBracketsTheCentralEstimate() {
        val (low, high) = personalRange(3650, Diet.VEGAN)
        val mid = personalLayer(3650, Diet.VEGAN).netSpared
        assertTrue(low < mid && mid < high)
    }
}

class ProjectionTest {
    @Test
    fun projectionsGrowWithTime() {
        val p = projections(365, Diet.VEGAN)
        assertTrue(p.zipWithNext().all { (a, b) -> b.personal.netSpared > a.personal.netSpared && b.abstainers > a.abstainers })
        assertEquals(personalLayer(365 * 2, Diet.VEGAN).netSpared, p.first().personal.netSpared, 1e-9)
    }
}

class FootprintTest {
    @Test
    fun environmentFollowsNetProduction() {
        val cow = personalLayer(3650, Diet.VEGAN).impacts.first { it.species.name == "Cows" }
        assertEquals(cow.netUnits * 0.4536 * (0.8 * 99.48 + 0.2 * 33.30), cow.environment.co2eKg, 1e-6)
        assertEquals(0.0, personalLayer(30, Diet.VEGAN).grossEnvironment.co2eKg)
        // Early on, only the replacement food has a footprint.
        assertTrue(personalLayer(30, Diet.VEGAN).environment.co2eKg < 0)
    }

    @Test
    fun startDateLabels() {
        assertEquals("1 Jan 1970", StartDate.label(0))
        assertEquals("11 Oct 2026", StartDate.label(20737))
    }
}

class LineageTest {
    @Test
    fun unbornReplacementsAndTheirOffspring() {
        val pig = personalLayer(3650, Diet.VEGAN).impacts.first { it.species.name == "Pigs" }
        val sinceBirths = 3650 - 114
        assertEquals(pig.breedersNotKept!! * 0.45 * sinceBirths / 365.0, pig.replacementsNeverBred!!, 1e-9)
        assertTrue(pig.offspringOfUnbornBreeders!! > 0 && pig.offspringOfUnbornBreeders!! <= pig.neverBred)
        assertEquals(pig.neverBred + pig.replacementsNeverBred!!, pig.totalNeverBorn, 1e-9)
        // Too early for any replacement to have bred.
        assertEquals(0.0, personalLayer(200, Diet.VEGAN).impacts.first { it.species.name == "Pigs" }.offspringOfUnbornBreeders)
    }
}

class ScheduleTest {
    @Test
    fun upcomingMilestonesLandOnTheirDay() {
        val days = 40
        val snapshot = LedgerSnapshot(days, Diet.VEGAN, null, Money.USD)
        val now = milestones(days, personalLayer(days, Diet.VEGAN), null, worldLayer(days), Money.USD)
        val upcoming = upcomingNotices(snapshot, now)
        // Broilers are slaughtered at 47 days: outlived seven days from now.
        val outlived = upcoming.single { it.second.id == "outlived:Chickens" }
        assertEquals(todayEpochDay() + 7, outlived.first)
        assertTrue(upcoming.none { (_, n) -> now.any { it.id == n.id } })
    }
}
