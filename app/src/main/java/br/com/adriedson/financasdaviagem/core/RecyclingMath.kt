package br.com.adriedson.financasdaviagem.core

import java.math.BigDecimal
import java.math.RoundingMode

/** Cents and grams, never floating-point money or a presumed measured weight. */
object RecyclingMath {
    const val DEFAULT_CANS_PER_KG = 70
    const val DEFAULT_CANS_PRICE_CENTS_KG = 600L

    /** Estimate only. The real mass of a batch must be measured for an exact kg sale. */
    fun estimatedGrams(cans: Long, cansPerKg: Int): Long? {
        if (cans < 0 || cansPerKg !in 1..1000) return null
        return try { BigDecimal.valueOf(cans).multiply(BigDecimal.valueOf(1000))
            .divide(BigDecimal.valueOf(cansPerKg.toLong()), 0, RoundingMode.HALF_UP).longValueExact() }
        catch (_: Exception) { null }
    }

    /** Exact amount in cents rounded once, from grams and cents/kg. */
    fun totalCents(grams: Long, priceCentsPerKg: Long): Long? {
        if (grams <= 0 || grams > 1_000_000_000L || priceCentsPerKg <= 0 || priceCentsPerKg > 100_000_000L) return null
        return try {
            BigDecimal.valueOf(grams).multiply(BigDecimal.valueOf(priceCentsPerKg))
                .divide(BigDecimal.valueOf(1000), 0, RoundingMode.HALF_UP).longValueExact()
                .takeIf { it in 1..999_999_999_999L }
        } catch (_: Exception) { null }
    }

    /** Grams can be entered with up to three decimal kg digits. */
    fun parseKg(text: String): Long? = FinanceMath.parseAmount(text, 3)

    fun kilograms(grams: Long): String = BigDecimal.valueOf(grams).movePointLeft(3)
        .stripTrailingZeros().toPlainString().replace('.', ',')

    fun validMaterial(s: String) = s == "cans" || s == "copper"
    fun validAction(s: String) = s == "collect" || s == "sale" || s == "adjust"
}
