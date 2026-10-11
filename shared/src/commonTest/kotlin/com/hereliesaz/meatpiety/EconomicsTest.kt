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
    fun netIsGrossTimesSupplyShareForSmallShocks() {
        // For a marginal buyer, net = gross · es / (es + ed); the rest is rebound.
        personalLayer(365, Diet.VEGAN).impacts.forEach {
            val s = it.species
            val share = s.supplyElasticity / (s.supplyElasticity + s.demandElasticity)
            assertEquals(it.grossAnimals * share, it.netAnimals, it.grossAnimals * 1e-4)
            assertTrue(it.reboundAnimals > 0)
        }
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
