package com.hereliesaz.meatpiety

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnimalLedgerTest {
    @Test
    fun zeroDaysSparesNothing() {
        assertTrue(animalsSpared(0, Diet.VEGAN).all { it.count == 0.0 })
    }

    @Test
    fun oneYearEqualsAnnualRate() {
        val spared = animalsSpared(365, Diet.VEGAN).associateBy { it.name }
        RATES.forEach { rate ->
            val expected = if (rate.existsBecauseOfDemand) -rate.perYearOnStandardDiet else rate.perYearOnStandardDiet
            assertEquals(expected, spared.getValue(rate.name).count, 1e-9)
        }
    }

    @Test
    fun vegetariansExcludeEggAndDairyLines() {
        val names = animalsSpared(365, Diet.VEGETARIAN).map { it.name }
        RATES.filter { it.requiresVeganism }.forEach { assertFalse(it.name in names) }
        assertEquals(RATES.count { !it.requiresVeganism }, names.size)
    }

    @Test
    fun buffaloRunsBackward() {
        val buffalo = animalsSpared(365, Diet.VEGAN).single { it.existsBecauseOfDemand }
        assertTrue(buffalo.count < 0)
    }

    @Test
    fun sphereScalesByFriendsPlusSelf() {
        val personal = animalsSpared(365, Diet.VEGAN)
        val sphere = sphereOfInfluence(personal, 9)
        personal.zip(sphere).forEach { (p, s) -> assertEquals(p.count * 10, s.count, 1e-9) }
    }

    @Test
    fun worldTallyCombinesVegansAndVegetarians() {
        val tally = worldTally(365)
        RATES.forEach { rate ->
            val people = if (rate.requiresVeganism) VEGANS_WORLDWIDE else VEGANS_WORLDWIDE + VEGETARIANS_WORLDWIDE
            val sign = if (rate.existsBecauseOfDemand) -1 else 1
            val expected = sign * rate.perYearOnStandardDiet * people
            assertEquals(expected, tally.animals.single { it.name == rate.name }.count, kotlin.math.abs(expected) * 1e-9)
        }
    }
}
