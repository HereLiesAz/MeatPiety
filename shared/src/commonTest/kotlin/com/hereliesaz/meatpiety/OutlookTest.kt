package com.hereliesaz.meatpiety

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OutlookTest {
    @Test
    fun fasterGrowthEndsCommercialBreedingSooner() {
        val slow = extinctionOutlook(0.02).associateBy { it.species.name }
        val fast = extinctionOutlook(0.05).associateBy { it.species.name }
        fast.forEach { (name, f) -> assertTrue(f.yearsToCommercialEnd!! < slow.getValue(name).yearsToCommercialEnd!!, name) }
    }

    @Test
    fun noGrowthMeansNoEnd() {
        assertTrue(extinctionOutlook(0.0).all { it.yearsToCommercialEnd == null })
        assertNull(extinctionOutlook(0.0).first().herdAtEnd)
    }

    @Test
    fun soyFollowsNetProduction() {
        assertEquals(0.0, personalLayer(30, Diet.VEGAN).soy.soyFreedKg)
        val s = personalLayer(3650, Diet.VEGAN).soy
        assertEquals(s.soyFreedKg * TOFU_KG_PER_SOY_KG, s.tofuKg, 1e-9)
        assertTrue(s.soyFreedKg > 0)
    }
}
