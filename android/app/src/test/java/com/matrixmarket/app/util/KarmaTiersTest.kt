package com.matrixmarket.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Student Score (User Defined Feature 3) - must agree with the API's thresholds.
class KarmaTiersTest {

    @Test
    fun tierNamesMatchApiThresholds() {
        assertEquals("New Trader", KarmaTiers.tierFor(0))
        assertEquals("New Trader", KarmaTiers.tierFor(39))
        assertEquals("Rising Trader", KarmaTiers.tierFor(40))
        assertEquals("Trusted Trader", KarmaTiers.tierFor(100))
        assertEquals("Campus Legend", KarmaTiers.tierFor(250))
    }

    @Test
    fun progressTowardNextTier() {
        assertEquals(0f, KarmaTiers.progress(0), 0.001f)
        assertEquals(0.5f, KarmaTiers.progress(20), 0.001f)   // halfway from 0 to 40
        assertEquals(0.5f, KarmaTiers.progress(70), 0.001f)   // halfway from 40 to 100
        assertEquals(1f, KarmaTiers.progress(500), 0.001f)
    }

    @Test
    fun pointsToNextTier() {
        assertEquals(23 to "Trusted Trader", KarmaTiers.pointsToNext(77))
        assertNull(KarmaTiers.pointsToNext(200))
    }
}
