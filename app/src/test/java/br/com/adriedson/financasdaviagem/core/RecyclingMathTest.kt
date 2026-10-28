package br.com.adriedson.financasdaviagem.core

import org.junit.Assert.*
import org.junit.Test

class RecyclingMathTest {
    @Test fun defaultStartingParameters() {
        assertEquals(70, RecyclingMath.DEFAULT_CANS_PER_KG)
        assertEquals(600L, RecyclingMath.DEFAULT_CANS_PRICE_CENTS_KG)
    }

    @Test fun seventyCansAndOneKgMatch() {
        assertEquals(1000L, RecyclingMath.estimatedGrams(70L, 70))
        assertEquals(600L, RecyclingMath.totalCents(1000L, 600L))
        assertEquals(2000L, RecyclingMath.estimatedGrams(140L, 70))
        assertEquals(1200L, RecyclingMath.totalCents(2000L, 600L))
    }

    @Test fun weightIsEstimateNotUniversalFact() {
        assertEquals(14L, RecyclingMath.estimatedGrams(1L, 70))
        assertEquals(500L, RecyclingMath.estimatedGrams(35L, 70))
        assertEquals(0L, RecyclingMath.estimatedGrams(0L, 70))
    }

    @Test fun exactMoneyRoundingInCents() {
        assertEquals(300L, RecyclingMath.totalCents(500L, 600L))
        assertEquals(8L, RecyclingMath.totalCents(14L, 600L))
        assertEquals(1L, RecyclingMath.totalCents(1L, 600L))
        assertEquals(875L, RecyclingMath.totalCents(1250L, 700L))
    }

    @Test fun copperMassMustBeEnteredNotGuessed() {
        assertEquals(375L, RecyclingMath.parseKg("0,375"))
        assertEquals(1250L, RecyclingMath.parseKg("1,250"))
        assertEquals("1,25", RecyclingMath.kilograms(1250L))
        assertNull(RecyclingMath.parseKg("-1"))
        assertNull(RecyclingMath.parseKg("1.250,5"))
    }

    @Test fun rejectInvalidOrExcessiveAmounts() {
        assertNull(RecyclingMath.totalCents(0, 600L))
        assertNull(RecyclingMath.totalCents(1000L, 0L))
        assertNull(RecyclingMath.totalCents(1_000_000_001L, 600L))
        assertNull(RecyclingMath.estimatedGrams(70L, 0))
        assertNull(RecyclingMath.estimatedGrams(-1L, 70))
    }

    @Test fun onlyTransportableMaterialsSupported() {
        assertTrue(RecyclingMath.validMaterial("cans"))
        assertTrue(RecyclingMath.validMaterial("copper"))
        assertFalse(RecyclingMath.validMaterial("pet"))
        assertFalse(RecyclingMath.validMaterial("paper"))
    }
}
