package br.com.adriedson.financasdaviagem.core

import org.junit.Assert.*
import org.junit.Test

class BillMathTest {
    @Test fun parsesBrazilianDateStrictly() {
        assertEquals("2026-10-27", BillMath.parseDate("27/10/2026"))
        assertEquals("29/02/2028", BillMath.displayDate("2028-02-29"))
        assertNull(BillMath.parseDate("31/02/2026"))
        assertNull(BillMath.parseDate("29/02/2026"))
        assertNull(BillMath.parseDate("2026-10-27"))
        assertNull(BillMath.parseDate("01/01/2201"))
    }
    @Test fun detectsLateBillsWithoutTimezoneRounding() {
        assertTrue(BillMath.isOverdue("2026-10-20", "2026-10-27"))
        assertFalse(BillMath.isOverdue("2026-10-27", "2026-10-27"))
        assertFalse(BillMath.isOverdue("2026-10-28", "2026-10-27"))
        assertTrue(BillMath.isValidIso("2026-10-27"))
        assertFalse(BillMath.isValidIso("2026-02-30"))
    }
    @Test fun preservesCentsCalculationForInventory() {
        assertEquals(686L, RecyclingMath.totalCents(1143L, 600L))
        assertEquals(1143L, RecyclingMath.estimatedGrams(80L, 70))
    }
}
