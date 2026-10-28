package br.com.adriedson.financasdaviagem.core

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

/** Dates are calendar dates, not timezone-dependent timestamps. */
object BillMath {
    private val brazilian = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.forLanguageTag("pt-BR"))
        .withResolverStyle(ResolverStyle.STRICT)

    fun parseDate(input: String): String? = try {
        LocalDate.parse(input.trim(), brazilian).takeIf { it.year in 2000..2100 }?.toString()
    } catch (_: Exception) { null }

    fun displayDate(iso: String): String = LocalDate.parse(iso).format(brazilian)

    fun isValidIso(iso: String): Boolean = try {
        val d = LocalDate.parse(iso)
        d.year in 2000..2100 && d.toString() == iso
    } catch (_: Exception) { false }

    fun isOverdue(iso: String, today: String = LocalDate.now().toString()): Boolean =
        iso < today
}
