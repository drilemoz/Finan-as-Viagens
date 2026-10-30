package br.com.adriedson.financasdaviagem.core

import br.com.adriedson.financasdaviagem.data.Entry
import br.com.adriedson.financasdaviagem.data.RecyclingEvent

/** Uma única passagem pelo histórico, reaproveitada enquanto os dados não mudarem. */
data class DashboardTotals(
    val incomeCents: Long,
    val expenseCents: Long,
    val donationCents: Long,
    val todayExpenseCents: Long,
    val categories: List<Pair<String, Long>>
)

data class RecyclingTotals(
    val cansInStock: Long,
    val collectedCans: Long,
    val soldCans: Long,
    val copperGramsInStock: Long,
    val paidCents: Long
)

object ScreenSummaries {
    fun dashboard(entries: List<Entry>, dayStartMillis: Long, nowMillis: Long): DashboardTotals {
        var income = 0L
        var expense = 0L
        var donation = 0L
        var todayExpense = 0L
        val categories = HashMap<String, Long>()
        for (entry in entries) {
            if (entry.kind == "income") {
                income = Math.addExact(income, entry.brlCents)
                if (entry.category == "Doação") donation = Math.addExact(donation, entry.brlCents)
            } else if (entry.kind == "expense") {
                expense = Math.addExact(expense, entry.brlCents)
                categories[entry.category] = Math.addExact(categories[entry.category] ?: 0L, entry.brlCents)
                if (entry.createdAt in dayStartMillis..nowMillis)
                    todayExpense = Math.addExact(todayExpense, entry.brlCents)
            }
        }
        return DashboardTotals(income, expense, donation, todayExpense,
            categories.toList().sortedByDescending { it.second })
    }

    /** Coletas, vendas e ajustes são movimentos de estoque; valor só inclui vendas confirmadas. */
    fun recycling(events: List<RecyclingEvent>): RecyclingTotals {
        var cans = 0L
        var collected = 0L
        var sold = 0L
        var copper = 0L
        var paid = 0L
        for (event in events) {
            when (event.material) {
                "cans" -> {
                    cans = Math.addExact(cans, event.units)
                    if (event.action == "collect") collected = Math.addExact(collected, event.units)
                    if (event.action == "sale") sold = Math.subtractExact(sold, event.units)
                }
                "copper" -> copper = Math.addExact(copper, event.units)
            }
            if (event.action == "sale") paid = Math.addExact(paid, event.receivedCents ?: 0L)
        }
        return RecyclingTotals(cans, collected, sold, copper, paid)
    }
}
