package br.com.adriedson.financasdaviagem.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.adriedson.financasdaviagem.core.DailyBudgetAlert
import br.com.adriedson.financasdaviagem.core.TripPlanningMath
import br.com.adriedson.financasdaviagem.core.FinanceMath
import br.com.adriedson.financasdaviagem.core.WalletMath
import br.com.adriedson.financasdaviagem.core.ScreenSummaries
import br.com.adriedson.financasdaviagem.data.WalletAccount
import br.com.adriedson.financasdaviagem.data.WalletTransfer
import br.com.adriedson.financasdaviagem.data.Entry
import br.com.adriedson.financasdaviagem.data.Preferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val colors = listOf(Mint, Amber, Blue, Color(0xFFC4A2E8), Color(0xFFFF9991))

private fun todayStart(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

@Composable
fun DashboardScreen(
    entries: List<Entry>, settings: Preferences, accounts: List<WalletAccount>, transfers: List<WalletTransfer>,
    onAddExpense: () -> Unit, onAddIncome: () -> Unit, onAddDonation: () -> Unit,
    onBills: () -> Unit, onHistory: () -> Unit, onReports: () -> Unit,
    onPlanning: () -> Unit, onSettings: () -> Unit
) {
    // Os totais e saldos não mudam quando a lista apenas é rolada ou um diálogo abre.
    val dayStart = todayStart()
    val totals = remember(entries, dayStart) {
        ScreenSummaries.dashboard(entries, dayStart, System.currentTimeMillis())
    }
    val expense = totals.expenseCents
    val donationIncome = totals.donationCents
    val balances = remember(accounts, entries, transfers) {
        WalletMath.balances(accounts, entries, transfers)
    }
    val balance = WalletMath.usable(accounts, balances)
    val reserve = WalletMath.reserve(accounts, balances)
    val today = totals.todayExpenseCents
    val budgetAlert = TripPlanningMath.calculate(balance, settings.dailyBudgetCents, settings.tripGoalCents,
        settings.plannedTripDays, settings.budgetAlertPercent, today).alert
    val todayIncome = totals.todayIncomeCents
    val todayNet = totals.todayNetCents
    val categories = totals.categories
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = rememberLazyListState(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Finanças da viagem", fontSize = 23.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground)
                    Text("Painel financeiro", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Outlined.DirectionsBike, contentDescription = "Configurações",
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 15.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Saldo disponível", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(FinanceMath.format(balance), fontSize = 30.sp, fontWeight = FontWeight.Bold,
                        color = if (balance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    Text("Reserva de emergência separada: ${FinanceMath.format(reserve)}",
                        color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ValueCard("Orçamento diário", FinanceMath.format(settings.dailyBudgetCents),
                    Icons.Outlined.CalendarMonth, Modifier.weight(1f), compact = true)
                ValueCard("Gasto hoje", FinanceMath.format(today), Icons.Outlined.AccountBalanceWallet, Modifier.weight(1f),
                    highlight = settings.dailyBudgetCents > 0 && today > settings.dailyBudgetCents, compact = true)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ValueCard("Ganhos hoje", FinanceMath.format(todayIncome),
                    Icons.Outlined.TrendingUp, Modifier.weight(1f), compact = true)
                ValueCard("Gastos da viagem", FinanceMath.format(expense),
                    Icons.Outlined.TrendingDown, Modifier.weight(1f), compact = true)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Resultado de hoje", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(FinanceMath.format(todayNet),
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    color = if (todayNet < 0L) MaterialTheme.colorScheme.error
                        else if (todayNet > 0L) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface)
            }
        }
        if (budgetAlert != DailyBudgetAlert.NONE) {
            item {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(16.dp)) {
                    Text(if (budgetAlert == DailyBudgetAlert.EXCEEDED)
                        "Seu gasto de hoje ultrapassou o orçamento diário."
                        else if (today == settings.dailyBudgetCents) "Você atingiu o orçamento diário."
                        else "Atenção: você já gastou pelo menos ${settings.budgetAlertPercent}% do orçamento diário.",
                        Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
        item { SectionTitle("Gastos por categoria") }
        if (categories.isEmpty()) {
            item { EmptyState("Nenhuma despesa registrada", "Seus gastos aparecerão aqui.",
                Icons.Outlined.ReceiptLong, compact = true) }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    categories.take(6).forEachIndexed { i, (category, value) ->
                        CategoryBar(category, value, expense, colors[i % colors.size])
                    }
                }
            }
        }
        item {
            Button(onClick = onAddExpense, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(8.dp))
                Text("Adicionar despesa", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
        // Receita e Doação compartilham um contêiner, mas continuam ações distintas.
        item {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onAddIncome, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(21.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Receita", fontWeight = FontWeight.Medium)
                    }
                    VerticalDivider(modifier = Modifier.height(27.dp), thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant)
                    TextButton(onClick = onAddDonation, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Icon(Icons.Outlined.VolunteerActivism, contentDescription = null, modifier = Modifier.size(21.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Doação", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        // Três atalhos independentes em uma única linha, com texto curto para telas estreitas.
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DashboardShortcut("Planejar", Icons.Outlined.Flag, onPlanning, Modifier.weight(1f))
                DashboardShortcut("Relatórios", Icons.Outlined.BarChart, onReports, Modifier.weight(1f))
                DashboardShortcut("Gerenciar", Icons.Outlined.EventNote, onBills, Modifier.weight(1f))
            }
        }
        if (donationIncome > 0L) {
            item {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(15.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text("Doações recebidas", style = MaterialTheme.typography.labelLarge)
                        Text(FinanceMath.format(donationIncome), style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary)
                        Text("Valor já incluído no saldo disponível; não somar novamente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                SectionTitle("Últimos lançamentos")
                TextButton(onClick = onHistory) { Text("Ver todos") }
            }
        }
        if (entries.isEmpty()) {
            item { Text("Defina os saldos iniciais na aba Carteiras e depois registre suas movimentações.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(entries.take(4), key = { "recent-${it.id}" }, contentType = { "entry" }) { entry -> EntryRow(entry, null) }
        }
    }
}

/** Ações rápidas com altura fixa e área de toque adequada para uso na bicicleta. */
@Composable
private fun DashboardShortcut(label: String, icon: ImageVector, onClick: () -> Unit,
    modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = 72.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun EntryRow(entry: Entry, onClick: (() -> Unit)?) {
    val expense = entry.kind == "expense"
    val originalDiffers = entry.currency != "BRL"
    val time = remember(entry.createdAt) {
        SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.forLanguageTag("pt-BR")).format(Date(entry.createdAt))
    }
    val itemSummary = remember(entry) {
        entry.quantity?.let { qty ->
            "$qty ${if (entry.itemKind == "cans") "latinhas" else "itens"}" +
                (entry.unitMinor?.let { " × ${FinanceMath.format(it, entry.currency)}" } ?: "")
        }.orEmpty()
    }
    val detail = remember(entry, itemSummary, time) {
        listOf(itemSummary, entry.note, entry.city, entry.country)
            .filter { it.isNotBlank() }.joinToString(" · ").ifBlank { time }
    }
    val formattedAmount = remember(entry.brlCents, entry.kind) {
        (if (expense) "−" else "+") + FinanceMath.format(entry.brlCents)
    }
    val formattedOriginal = remember(entry.originalMinor, entry.currency) {
        if (originalDiffers) FinanceMath.format(entry.originalMinor, entry.currency) else ""
    }
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Icon(if (expense) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                    contentDescription = if (expense) "Despesa" else "Receita",
                    tint = if (expense) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(11.dp).size(21.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(entry.category, color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (originalDiffers) Text(formattedOriginal,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formattedAmount,
                color = if (expense) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge)
        }
    }
}

