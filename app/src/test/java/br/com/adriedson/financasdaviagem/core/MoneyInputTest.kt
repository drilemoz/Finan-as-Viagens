package br.com.adriedson.financasdaviagem.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyInputTest {
    @Test fun automaticCents() {
        assertEquals("0,01", MoneyInput.fromTyped("1"))
        assertEquals("0,12", MoneyInput.fromTyped("12"))
        assertEquals("1,23", MoneyInput.fromTyped("123"))
        assertEquals("1.234,56", MoneyInput.fromTyped("123456"))
        assertEquals("1234,56", MoneyInput.canonical("1.234,56"))
        assertEquals(123456L, FinanceMath.parseAmount(MoneyInput.canonical("1.234,56")))
    }
    @Test fun currenciesWithoutFraction() {
        assertEquals("123.456", MoneyInput.fromTyped("123456", 0))
        assertEquals("123456", MoneyInput.canonical("123.456", 0))
        assertEquals(123456L, FinanceMath.parseAmount(MoneyInput.canonical("123.456", 0), 0))
    }
    @Test fun pasteAndOverflow() {
        assertEquals("12,34", MoneyInput.fromTyped("R$ 12,34"))
        assertEquals("999.999.999,99", MoneyInput.fromTyped("99999999999"))
        assertEquals("", MoneyInput.fromTyped(""))
        assertNull(FinanceMath.parseAmount("1.234,56")) // raw parser remains strict
    }
}
