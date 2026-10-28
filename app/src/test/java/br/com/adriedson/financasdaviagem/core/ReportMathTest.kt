package br.com.adriedson.financasdaviagem.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ReportMathTest {
    private val utc = ZoneId.of("UTC")
    private fun time(day: String): Long = LocalDate.parse(day).atTime(12, 0).atZone(utc).toInstant().toEpochMilli()
    @Test fun excludesOldAndDoesNotCountTransfers() {
        val rows = listOf(
            ReportRecord("income", "Trabalho", 1200, time("2026-10-27")),
            ReportRecord("income", "Doação", 500, time("2026-10-26")),
            ReportRecord("expense", "Alimentação", 800, time("2026-10-26")),
            ReportRecord("expense", "Saúde", 200, time("2026-10-01"))
        )
        val report = ReportMath.calculate(rows, 7, time("2026-10-27"), utc)
        assertEquals(1700L, report.incomeCents)
        assertEquals(800L, report.expenseCents)
        assertEquals(900L, report.netCents)
        assertEquals(500L, report.donationCents)
        assertEquals(1200L, report.workCents)
        assertEquals(7, report.lastSevenDays.size)
        assertEquals(1, report.categories.size)
        assertEquals(1000L, ReportMath.calculate(rows, null, time("2026-10-27"), utc).expenseCents)
    }
    @Test fun zeroWithoutData() {
        val report = ReportMath.calculate(emptyList(), 30, time("2026-10-27"), utc)
        assertEquals(0L, report.netCents)
        assertEquals(0, report.count)
        assertEquals(0L, report.lastSevenDays.sumOf { it.expenseCents })
    }
}
