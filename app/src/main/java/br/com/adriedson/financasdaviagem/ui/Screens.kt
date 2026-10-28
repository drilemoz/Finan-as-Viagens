package br.com.adriedson.financasdaviagem.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.adriedson.financasdaviagem.core.FinanceMath
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

private fun expenseTotals(entries: List<Entry>) = entries.filter { it.kind == "expense" }
    .groupBy { it.category }.mapValues { (_, list) -> list.sumOf { it.brlCents } }
    .toList().sortedByDescending { it.second }

@Composable
fun DashboardScreen(
    entries: List<Entry>, settings: Preferences,
    onAddExpense: () -> Unit, onAddIncome: () -> Unit,
    onHistory: () -> Unit, onSettings: () -> Unit
) {
    val income = entries.filter { it.kind == "income" }.sumOf { it.brlCents }
    val expense = entries.filter { it.kind == "expense" }.sumOf { it.brlCents }
    val balance = settings.initialCents + income - expense - settings.reserveCents
    val today = entries.filter { it.kind == "expense" && it.createdAt >= todayStart() }.sumOf { it.brlCents }
    val autonomy = FinanceMath.autonomyDays(balance, settings.dailyBudgetCents)
    val categories = expenseTotals(entries)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 25.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Finanças da viagem", fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
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
            Surface(shape = RoundedCornerShape(23.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                    Text("Saldo disponível", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(FinanceMath.format(balance), fontSize = 34.sp, fontWeight = FontWeight.Bold,
                        color = if (balance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    Text("Reserva de emergência separada: ${FinanceMath.format(settings.reserveCents)}",
                        color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ValueCard("Orçamento diário", FinanceMath.format(settings.dailyBudgetCents),
                    Icons.Outlined.CalendarMonth, Modifier.weight(1f))
                ValueCard("Gasto hoje", FinanceMath.format(today), Icons.Outlined.AccountBalanceWallet, Modifier.weight(1f),
                    highlight = settings.dailyBudgetCents > 0 && today > settings.dailyBudgetCents)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ValueCard("Dias de autonomia", autonomy?.let { "$it dias" } ?: "Indisponível",
                    Icons.Outlined.HourglassBottom, Modifier.weight(1f))
                ValueCard("Gastos da viagem", FinanceMath.format(expense),
                    Icons.Outlined.TrendingDown, Modifier.weight(1f))
            }
        }
        if (settings.dailyBudgetCents > 0 && today > settings.dailyBudgetCents) {
            item {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(16.dp)) {
                    Text("Seu gasto de hoje ultrapassou o orçamento diário.",
                        Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
        item { SectionTitle("Despesas por categoria") }
        if (categories.isEmpty()) {
            item { EmptyState("Nenhuma despesa registrada", "Seus gastos aparecerão aqui.", Icons.Outlined.ReceiptLong) }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    categories.take(6).forEachIndexed { i, (category, value) ->
                        CategoryBar(category, value, expense, colors[i % colors.size])
                    }
                }
            }
        }
        item {
            Button(onClick = onAddExpense, modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(10.dp))
                Text("Adicionar despesa", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }
        item {
            OutlinedButton(onClick = onAddIncome, modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Adicionar receita")
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
            item { Text("Cadastre seu saldo inicial nas Configurações e depois registre as movimentações.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(entries.take(4), key = { it.id }) { entry -> EntryRow(entry, null) }
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
                val itemSummary = entry.quantity?.let { qty ->
                    "$qty ${if (entry.itemKind == "cans") "latinhas" else "itens"}" +
                        (entry.unitMinor?.let { " × ${FinanceMath.format(it, entry.currency)}" } ?: "")
                }.orEmpty()
                Text(listOf(itemSummary, entry.note, entry.city, entry.country)
                    .filter { it.isNotBlank() }.joinToString(" · ").ifBlank { time }, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (originalDiffers) Text(FinanceMath.format(entry.originalMinor, entry.currency),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text((if (expense) "−" else "+") + FinanceMath.format(entry.brlCents),
                color = if (expense) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun HistoryScreen(entries: List<Entry>, onEdit: (Entry) -> Unit) {
    var type by remember { mutableStateOf("Todos") }
    var period by remember { mutableStateOf("Tudo") }
    val start = remember(period) {
        when (period) {
            "Hoje" -> todayStart()
            "7 dias" -> System.currentTimeMillis() - 7L * 86_400_000L
            "30 dias" -> System.currentTimeMillis() - 30L * 86_400_000L
            else -> 0L
        }
    }
    val rows = remember(entries, type, start) {
        entries.filter { it.createdAt >= start && (type == "Todos" ||
            (type == "Despesas" && it.kind == "expense") || (type == "Receitas" && it.kind == "income")) }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Histórico", fontSize = 25.sp, fontWeight = FontWeight.SemiBold) }
        item { Text("Toque em um lançamento para editar ou excluir.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("Todos", "Despesas", "Receitas").forEach {
                    FilterChip(selected = type == it, onClick = { type = it }, label = { Text(it) })
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("Tudo", "Hoje", "7 dias", "30 dias").forEach {
                    FilterChip(selected = period == it, onClick = { period = it }, label = { Text(it) })
                }
            }
        }
        if (rows.isEmpty()) item {
            EmptyState("Nenhum lançamento encontrado", "Experimente outro filtro ou adicione uma movimentação.",
                Icons.Outlined.ReceiptLong)
        }
        items(rows, key = { it.id }) { e -> EntryRow(e, onClick = { onEdit(e) }) }
    }
}

@Composable
private fun SettingsTile(icon: ImageVector, label: String, description: String, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.Medium)
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SettingsScreen(
    settings: Preferences, onSave: (Preferences) -> Unit,
    onBackup: () -> Unit, onImport: () -> Unit, onCsv: () -> Unit, onAbout: () -> Unit
) {
    var initial by remember(settings.initialCents) { mutableStateOf(FinanceMath.decimalInput(settings.initialCents)) }
    var reserve by remember(settings.reserveCents) { mutableStateOf(FinanceMath.decimalInput(settings.reserveCents)) }
    var budget by remember(settings.dailyBudgetCents) { mutableStateOf(FinanceMath.decimalInput(settings.dailyBudgetCents)) }
    var error by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item { Text("Configurações", fontSize = 25.sp, fontWeight = FontWeight.SemiBold) }
        item { SectionTitle("Aparência") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("system" to "Sistema", "dark" to "Escuro", "light" to "Claro").forEach { (mode, title) ->
                    FilterChip(selected = settings.themeMode == mode,
                        onClick = { onSave(settings.copy(themeMode = mode)) }, label = { Text(title) })
                }
            }
        }
        item { SectionTitle("Planejamento financeiro") }
        item { Text("Valores em reais. Saldo disponível = saldo inicial + receitas − despesas − reserva.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
        item { OutlinedTextField(value = initial, onValueChange = { initial = it },
            label = { Text("Saldo inicial (R$)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)) }
        item { OutlinedTextField(value = reserve, onValueChange = { reserve = it },
            label = { Text("Reserva de emergência (R$)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)) }
        item { OutlinedTextField(value = budget, onValueChange = { budget = it },
            label = { Text("Orçamento diário (R$)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)) }
        if (error.isNotEmpty()) item { Text(error, color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall) }
        item {
            Button(onClick = {
                val initialCents = FinanceMath.parseAmount(initial)
                val reserveCents = FinanceMath.parseAmount(reserve)
                val budgetCents = FinanceMath.parseAmount(budget)
                if (initialCents == null || reserveCents == null || budgetCents == null) {
                    error = "Digite números válidos, sem separador de milhar (ex.: 3200,00)."
                } else {
                    error = ""
                    onSave(settings.copy(initialCents = initialCents, reserveCents = reserveCents,
                        dailyBudgetCents = budgetCents))
                }
            }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) {
                Icon(Icons.Outlined.Save, null); Spacer(Modifier.width(7.dp)); Text("Salvar valores")
            }
        }
        item { HorizontalDivider() }
        item { SectionTitle("Dados e backup") }
        item { SettingsTile(Icons.Outlined.FileDownload, "Exportar backup JSON", "Salvar lançamentos e configurações no dispositivo", onBackup) }
        item { SettingsTile(Icons.Outlined.FileUpload, "Restaurar backup JSON", "Substituir dados após confirmação", onImport) }
        item { SettingsTile(Icons.Outlined.TableView, "Exportar CSV", "Abrir os lançamentos em planilhas", onCsv) }
        item { Text("Os arquivos são escolhidos por você no seletor do Android. Não há sincronização automática.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
        item { HorizontalDivider() }
        item { SettingsTile(Icons.Outlined.Info, "Sobre", "Versão, privacidade e funcionamento", onAbout) }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Voltar") } }
        item { Icon(Icons.Outlined.DirectionsBike, null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(54.dp)) }
        item { Text("Finanças da Viagem", fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        item { Text("Versão 0.1.0-alpha.3 · versionCode 3", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { SectionTitle("Sobre o aplicativo") }
        item { Text("Controle financeiro offline para a cicloviagem: receitas, despesas, categorias, saldo disponível, orçamento diário e autonomia estimada.") }
        item { SectionTitle("Privacidade e dados") }
        item { Text("Os lançamentos são gravados no armazenamento privado do Android. O app não solicita acesso à internet, localização ou contas bancárias. O backup só é criado ou restaurado quando você escolhe um arquivo.") }
        item { SectionTitle("Moedas e taxas") }
        item { Text("Os valores exibidos no painel são em reais. Lançamentos em outras moedas usam a taxa de câmbio informada manualmente na data do registro; o aplicativo não consulta cotações automaticamente.") }
        item { SectionTitle("Autonomia") }
        item { Text("A estimativa é saldo utilizável ÷ orçamento diário. Não é uma previsão de gastos futuros ou garantia de duração do dinheiro.") }
        item { Text("Projeto inicial · Android nativo · Kotlin / Jetpack Compose", color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall) }
    }
}
