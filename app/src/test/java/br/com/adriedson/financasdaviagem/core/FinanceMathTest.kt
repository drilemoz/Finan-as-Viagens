package br.com.adriedson.financasdaviagem.core

import org.junit.Assert.*
import org.junit.Test

class FinanceMathTest {
    @Test fun parseBrazilianDecimal() {
        assertEquals(1850L, FinanceMath.parseAmount("18,50"))
        assertEquals(320000L, FinanceMath.parseAmount("3200,00"))
        assertNull(FinanceMath.parseAmount("3.200,00"))
        assertNull(FinanceMath.parseAmount("-1"))
        assertNull(FinanceMath.parseAmount("1,234"))
    }

    @Test fun zeroDecimalCurrencies() {
        assertEquals(1500L, FinanceMath.parseAmount("1500", 0))
        assertNull(FinanceMath.parseAmount("1,50", 0))
        assertEquals(150L, FinanceMath.convertToBrl(1500L, "PYG", "0,001"))
    }

    @Test fun exchangeAndRounding() {
        assertEquals(750L, FinanceMath.convertToBrl(150000L, "ARS", "0,005"))
        assertEquals(1L, FinanceMath.convertToBrl(1L, "ARS", "0,5"))
        assertEquals(1234L, FinanceMath.convertToBrl(1234L, "BRL", "1"))
        assertNull(FinanceMath.convertToBrl(100, "ARS", "0"))
        assertNull(FinanceMath.convertToBrl(100, "ARS", "abc"))
    }

    @Test fun autonomy() {
        assertEquals(80L, FinanceMath.autonomyDays(320000L, 4000L))
        assertNull(FinanceMath.autonomyDays(0, 4000))
        assertNull(FinanceMath.autonomyDays(1000, 0))
    }
}
