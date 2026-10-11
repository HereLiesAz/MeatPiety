package com.hereliesaz.meatpiety

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OutlookTest {
    @Test
    fun fasterGrowthShrinksHerdsMore() {
        val slow = herdOutlook(0.0).associateBy { it.species.name }
        val fast = herdOutlook(0.03).associateBy { it.species.name }
        fast.forEach { (name, f) -> assertTrue(f.herdIn50 < slow.getValue(name).herdIn50, name) }
    }

    @Test
    fun flatAbstentionLetsHerdsGrowWithTheMarket() {
        herdOutlook(0.0).forEach {
            assertNull(it.yearsToHalf, it.species.name)
            assertTrue(it.herdIn25 > it.herdNow, it.species.name)
        }
    }

    @Test
    fun soyFollowsNetProduction() {
        assertEquals(0.0, personalLayer(30, Diet.VEGAN).soy.soyFreedKg)
        val s = personalLayer(3650, Diet.VEGAN).soy
        assertEquals(s.soyFreedKg * TOFU_KG_PER_SOY_KG, s.tofuKg, 1e-9)
        // A year's share, however long the span.
        assertEquals(s.tofuKg / 10 / GLOBAL_TOFU_KG, s.shareOfWorldTofu, 1e-12)
        assertTrue(s.soyFreedKg > 0)
    }

    @Test
    fun squeezedMarginsMoveAnimalsIntoFactoryFarms() {
        val world = worldLayer(365).impacts.first { it.species.name == "Cows" }
        assertTrue(world.shiftedToFactory > 0)
        assertEquals(0.0, worldLayer(365).impacts.first { it.species.name == "Buffalo" }.shiftedToFactory)
        val o = herdOutlook(0.03).first { it.species.name == "Cows" }
        assertTrue(o.factoryShareIn100!! > o.factoryShareNow!!)
    }
}
