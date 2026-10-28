package br.com.adriedson.financasdaviagem.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripPlanningMathTest {
    @Test fun goalAndCostUseOnlyAvailableMoney() {
        val p = TripPlanningMath.calculate(240_000, 4000, 300_000, 80, 80, 1800)
        assertEquals(60L, p.autonomyDays)
        assertEquals(320_000L, p.plannedCostCents)
        assertEquals(60_000L, p.goalRemainingCents)
        assertEquals(80_000L, p.tripShortfallCents)
        assertEquals(80, p.progressPercent)
        assertEquals(DailyBudgetAlert.NONE, p.alert)
    }
    @Test fun dailyAlertApproachingAndExceeded() {
        assertEquals(DailyBudgetAlert.APPROACHING,
            TripPlanningMath.calculate(10000, 4000, 0, 0, 80, 3200).alert)
        assertEquals(DailyBudgetAlert.APPROACHING,
            TripPlanningMath.calculate(10000, 4000, 0, 0, 80, 4000).alert)
        assertEquals(DailyBudgetAlert.EXCEEDED,
            TripPlanningMath.calculate(10000, 4000, 0, 0, 80, 4001).alert)
    }
    @Test fun unconfiguredSettingsNeverInventProjection() {
        val p = TripPlanningMath.calculate(0, 0, 0, 0, 80, 0)
        assertNull(p.plannedCostCents)
        assertNull(p.goalRemainingCents)
        assertNull(p.tripShortfallCents)
        assertNull(p.autonomyDays)
        assertNull(p.todayPercent)
        assertEquals(DailyBudgetAlert.NONE, p.alert)
    }
    @Test fun negativeUsableAndVeryLargeLongDoNotOverflow() {
        val p = TripPlanningMath.calculate(-1000, 4000, 10000, 1, 80, 0)
        assertEquals(11000L, p.goalRemainingCents)
        assertEquals(5000L, p.tripShortfallCents)
        val huge = TripPlanningMath.calculate(999_999_999_999L, 999_999_999_999L,
            999_999_999_999L, 3650, 80, 999_999_999_999L)
        assertEquals(3_649_999_999_996_350L, huge.plannedCostCents)
        assertEquals(100, huge.progressPercent)
    }
}
