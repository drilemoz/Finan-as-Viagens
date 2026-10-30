package br.com.adriedson.financasdaviagem.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Valores são sempre inteiros em unidades menores: centavos BRL, e 1 PYG. */
object FinanceMath {
    val localeBr = Locale.forLanguageTag("pt-BR")
    val supportedCurrencies = listOf("BRL", "ARS", "PYG", "UYU", "BOB", "CLP", "PEN")
    // Cada thread mantém seus próprios formatadores: reutilização barata sem compartilhar NumberFormat mutável.
    private val currencyFormatters = ThreadLocal.withInitial { mutableMapOf<String, NumberFormat>() }
    fun digits(currency: String) = if (currency == "PYG" || currency == "CLP") 0 else 2

    fun parseAmount(input: String, decimals: Int = 2): Long? {
        val sanitized = input.trim().replace(',', '.')
        if (decimals !in 0..3) return null
        val pattern = if (decimals == 0) Regex("^\\d{1,12}$")
            else Regex("^\\d{1,12}(?:\\.\\d{1,$decimals})?$")
        if (!pattern.matches(sanitized)) return null
        return try {
            val units = BigDecimal(sanitized).movePointRight(decimals).longValueExact()
            units.takeIf { it >= 0 && it <= 999_999_999_999L }
        } catch (_: Exception) { null }
    }

    /** Taxa é R$ por uma unidade da moeda estrangeira, digitada manualmente. */
    fun convertToBrl(originalMinor: Long, currency: String, rate: String): Long? {
        if (currency == "BRL") return originalMinor
        if (currency !in supportedCurrencies || originalMinor <= 0) return null
        return try {
            if (!Regex("^\\d{1,8}(?:[.,]\\d{1,10})?$").matches(rate.trim())) return null
            val fx = BigDecimal(rate.trim().replace(',', '.'))
            if (fx <= BigDecimal.ZERO) return null
            val result = BigDecimal.valueOf(originalMinor)
                .movePointLeft(digits(currency))
                .multiply(fx)
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP).longValueExact()
            result.takeIf { it > 0 && it <= 999_999_999_999L }
        } catch (_: Exception) { null }
    }

    fun format(minor: Long, currency: String = "BRL"): String {
        val formatter = currencyFormatters.get().getOrPut(currency) {
            NumberFormat.getCurrencyInstance(localeBr).apply {
                this.currency = Currency.getInstance(currency)
                minimumFractionDigits = digits(currency)
                maximumFractionDigits = digits(currency)
            }
        }
        return formatter.format(BigDecimal.valueOf(minor).movePointLeft(digits(currency)))
    }

    fun decimalInput(minor: Long, decimals: Int = 2): String =
        BigDecimal.valueOf(minor).movePointLeft(decimals).setScale(decimals).toPlainString().replace('.', ',')

    fun autonomyDays(usableCents: Long, dailyBudgetCents: Long): Long? =
        if (usableCents <= 0 || dailyBudgetCents <= 0) null else usableCents / dailyBudgetCents

    /** Total exato por itens: 1..1.000.000 unidades, sem overflow nem ponto flutuante. */
    fun quantityTotal(unitMinor: Long, quantity: Int): Long? {
        if (unitMinor <= 0 || quantity !in 1..1_000_000) return null
        return try {
            Math.multiplyExact(unitMinor, quantity.toLong()).takeIf { it in 1..999_999_999_999L }
        } catch (_: ArithmeticException) { null }
    }

    fun safeAdd(a: Long, b: Long): Long = Math.addExact(a, b)
}
