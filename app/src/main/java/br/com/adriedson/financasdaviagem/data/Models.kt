package br.com.adriedson.financasdaviagem.data

import br.com.adriedson.financasdaviagem.core.RecyclingMath

data class Entry(
    val id: Long = 0,
    val kind: String = "expense",
    val category: String = "Alimentação",
    val note: String = "",
    val city: String = "",
    val country: String = "",
    val currency: String = "BRL",
    val originalMinor: Long = 0,
    val rateBrl: String = "1",
    val brlCents: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    /** null = valor direto; cans/units = venda ou compra por unidade. */
    val itemKind: String? = null,
    val quantity: Int? = null,
    val unitMinor: Long? = null,
    val accountId: Long = 1L
)

/** units = can count for cans, grams for copper. Negative for sale/withdrawal. */
data class RecyclingEvent(
    val id: Long = 0,
    val material: String,
    val action: String,
    val units: Long,
    val measuredGrams: Long? = null,
    val estimatedWeight: Boolean = false,
    val priceCentsKg: Long? = null,
    val receivedCents: Long? = null,
    val financeEntryId: Long? = null,
    val note: String = "",
    val city: String = "",
    val country: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class WalletAccount(
    val id: Long = 0,
    val name: String,
    val openingCents: Long = 0,
    val isReserve: Boolean = false
)

data class WalletTransfer(
    val id: Long = 0,
    val fromId: Long,
    val toId: Long,
    val cents: Long,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/** Pending bill does not change the financial balance. Paying it creates one linked expense. */
data class Bill(
    val id: Long = 0,
    val title: String,
    val category: String = "Outros",
    val amountCents: Long,
    val dueDate: String,
    val note: String = "",
    val paidEntryId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class Preferences(
    val initialCents: Long = 0,
    val reserveCents: Long = 0,
    val dailyBudgetCents: Long = 4000,
    val themeMode: String = "system",
    val cansPerKg: Int = RecyclingMath.DEFAULT_CANS_PER_KG,
    val cansPriceCentsKg: Long = RecyclingMath.DEFAULT_CANS_PRICE_CENTS_KG,
    /** Zero means not configured. Goal applies only to non-reserve accounts. */
    val tripGoalCents: Long = 0,
    val plannedTripDays: Int = 0,
    val budgetAlertPercent: Int = 80
)

val expenseCategories = listOf("Alimentação", "Bebidas", "Água", "Hospedagem", "Manutenção", "Saúde", "Transporte", "Equipamentos", "Imprevistos", "Outros")
val incomeCategories = listOf("Economias", "Venda de latinhas", "Trabalho", "Doação", "Reembolso", "Outras receitas")

