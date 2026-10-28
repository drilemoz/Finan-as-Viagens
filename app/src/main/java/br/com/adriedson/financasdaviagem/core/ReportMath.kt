package br.com.adriedson.financasdaviagem.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Calcula relatórios em centavos sobre lançamentos reais; transferências não entram aqui. */
data class ReportRecord(val kind: String, val category: String, val cents: Long, val createdAt: Long)
data class ReportCategory(val name: String, val cents: Long)
data class ReportDay(val date: LocalDate, val incomeCents: Long, val expenseCents: Long)
data class ReportSummary(
    val incomeCents: Long, val expenseCents: Long,
    val donationCents: Long, val workCents: Long,
    val categories: List<ReportCategory>, val lastSevenDays: List<ReportDay>, val count: Int
) {
    val netCents: Long get() = Math.subtractExact(incomeCents, expenseCents)
}

object ReportMath {
    fun calculate(records: List<ReportRecord>, periodDays: Int?, nowMillis: Long = System.currentTimeMillis(),
                  zone: ZoneId = ZoneId.systemDefault()): ReportSummary {
        require(periodDays == null || periodDays in 1..3650)
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val start = periodDays?.let { today.minusDays((it - 1).toLong()) }
        val selected = records.filter {
            val day = Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
            it.createdAt <= nowMillis && (start == null || !day.isBefore(start)) && !day.isAfter(today)
        }
        fun sum(rows: List<ReportRecord>) = rows.fold(0L) { acc, row -> Math.addExact(acc, row.cents) }
        val income = selected.filter { it.kind == "income" }
        val expense = selected.filter { it.kind == "expense" }
        val categories = expense.groupBy { it.category }
            .map { (name, rows) -> ReportCategory(name, sum(rows)) }
            .sortedByDescending { it.cents }
        val byDay = selected.groupBy { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() }
        val lastSeven = (6 downTo 0).map { days ->
            val date = today.minusDays(days.toLong())
            val items = byDay[date].orEmpty()
            ReportDay(date, sum(items.filter { it.kind == "income" }), sum(items.filter { it.kind == "expense" }))
        }
        return ReportSummary(sum(income), sum(expense), sum(income.filter { it.category == "Doação" }),
            sum(income.filter { it.category == "Trabalho" || it.category == "Venda de latinhas" }),
            categories, lastSeven, selected.size)
    }
}
