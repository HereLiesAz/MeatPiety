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
            val share = s.supplyElasticity / (s.supplyElasticity + s.demandElasticity)
            val lag = s.biology?.supplyLagDays ?: 90
            val expected = it.grossAnimals / days * share * (days - lag)
            assertEquals(expected, it.netAnimals, expected * 1e-4)
            assertTrue(it.reboundAnimals > 0)
            assertTrue(it.neverBred >= it.netAnimals)
        }
    }

    @Test
    fun nothingIsSparedBeforeProducersCanRespond() {
        val chicken = personalLayer(30, Diet.VEGAN).impacts.first { it.species.name == "Chickens" }
        assertEquals(0.0, chicken.netAnimals)
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
            assertTrue(it.producerRevenueLostUsd > 0, it.species.name)
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
        assertEquals(funnel.reached * PERSUASION_RATE * RETENTION_RATE, funnel.stayed, 1e-9)
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
    fun stillAliveIsThePipelineCappedAtSlaughterAge() {
        val year = personalLayer(365, Diet.VEGAN).impacts.first { it.species.name == "Chickens" }
        val early = personalLayer(20, Diet.VEGAN).impacts.first { it.species.name == "Chickens" }
        // A day's share per elapsed day, capped at slaughter age (47 days).
        assertEquals(early.stillAlive!! / 20 * 47, year.stillAlive!!, 1e-9)
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
        assertEquals(cow.netUnits * 0.4536 * 99.48, cow.environment.co2eKg, 1e-6)
        assertEquals(0.0, personalLayer(30, Diet.VEGAN).environment.co2eKg)
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
