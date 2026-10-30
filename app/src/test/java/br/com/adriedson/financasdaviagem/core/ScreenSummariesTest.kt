package br.com.adriedson.financasdaviagem.core

import br.com.adriedson.financasdaviagem.data.Entry
import br.com.adriedson.financasdaviagem.data.RecyclingEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenSummariesTest {
    @Test fun dashboardCountsPaymentsOnlyAndSortsCategories() {
        val entries = listOf(
            Entry(kind = "income", category = "Doação", brlCents = 900, createdAt = 1_001),
            Entry(kind = "income", category = "Trabalho", brlCents = 600, createdAt = 1_002),
            Entry(kind = "expense", category = "Alimentação", brlCents = 420, createdAt = 1_030),
            Entry(kind = "expense", category = "Manutenção", brlCents = 550, createdAt = 999),
            Entry(kind = "expense", category = "Alimentação", brlCents = 180, createdAt = 1_090)
        )
        val totals = ScreenSummaries.dashboard(entries, 1_000L, 1_100L)
        assertEquals(1500L, totals.incomeCents)
        assertEquals(1150L, totals.expenseCents)
        assertEquals(900L, totals.donationCents)
        assertEquals(600L, totals.todayExpenseCents)
        assertEquals(listOf("Alimentação" to 600L, "Manutenção" to 550L), totals.categories)
    }

    @Test fun recyclingKeepsEstimatesOutOfReceivedMoney() {
        val events = listOf(
            RecyclingEvent(material = "cans", action = "collect", units = 140),
            RecyclingEvent(material = "cans", action = "sale", units = -70, receivedCents = 600),
            RecyclingEvent(material = "cans", action = "adjust", units = 10),
            RecyclingEvent(material = "copper", action = "collect", units = 2_300),
            RecyclingEvent(material = "copper", action = "sale", units = -300, receivedCents = 150)
        )
        val totals = ScreenSummaries.recycling(events)
        assertEquals(80L, totals.cansInStock)
        assertEquals(140L, totals.collectedCans)
        assertEquals(70L, totals.soldCans)
        assertEquals(2000L, totals.copperGramsInStock)
        assertEquals(750L, totals.paidCents)
    }

    @Test fun oldAndNewSummariesMatchOnAllValidKinds() {
        val entries = (1..800).map { index ->
            Entry(kind = if (index % 3 == 0) "income" else "expense",
                category = if (index % 5 == 0) "Doação" else "Alimentação",
                brlCents = index.toLong(), createdAt = index.toLong())
        }
        val totals = ScreenSummaries.dashboard(entries, 200L, 700L)
        assertEquals(entries.filter { it.kind == "income" }.sumOf { it.brlCents }, totals.incomeCents)
        assertEquals(entries.filter { it.kind == "expense" }.sumOf { it.brlCents }, totals.expenseCents)
        assertEquals(entries.filter { it.kind == "expense" && it.createdAt in 200L..700L }.sumOf { it.brlCents }, totals.todayExpenseCents)
    }
}
