package br.com.adriedson.financasdaviagem.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.adriedson.financasdaviagem.core.FinanceMath
import br.com.adriedson.financasdaviagem.core.ReportMath
import br.com.adriedson.financasdaviagem.core.ReportRecord
import br.com.adriedson.financasdaviagem.data.Entry
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ReportsScreen(entries: List<Entry>, onBack: () -> Unit) {
    var period by remember { mutableStateOf("30 dias") }
    val days = when (period) { "7 dias" -> 7; "30 dias" -> 30; else -> null }
    val summary = remember(entries, days) {
        ReportMath.calculate(entries.map { ReportRecord(it.kind, it.category, it.brlCents, it.createdAt) }, days)
    }
    val income = summary.incomeCents
    val expense = summary.expenseCents
    val maxCategory = summary.categories.maxOfOrNull { it.cents } ?: 0L
    val maxDaily = summary.lastSevenDays.maxOfOrNull { it.expenseCents } ?: 0L
    val daysFormat = remember { DateTimeFormatter.ofPattern("dd/MM", Locale.forLanguageTag("pt-BR")) }

    LazyColumn(Modifier.fillMaxSize(), state = rememberLazyListState(), contentPadding = PaddingValues(17.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Voltar ao painel") }
                Spacer(Modifier.width(6.dp))
                Column {
                    Text("Relatórios", fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                    Text("Movimentações realmente recebidas ou pagas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("7 dias", "30 dias", "Tudo").forEach { p ->
                    FilterChip(selected = period == p, onClick = { period = p }, label = { Text(p) })
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Resultado financeiro", style = MaterialTheme.typography.labelLarge)
                    Text(FinanceMath.format(summary.netCents), fontSize = 29.sp, fontWeight = FontWeight.Bold,
                        color = if (summary.netCents < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    HorizontalDivider()
                    ReportAmountRow("Receitas", income, MaterialTheme.colorScheme.primary)
                    ReportAmountRow("Despesas", expense, MaterialTheme.colorScheme.onSurface)
                    Text("${summary.count} lançamentos no período. Transferências e coletas ainda não vendidas não entram no cálculo.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Surface(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Trabalho / reciclagem", style = MaterialTheme.typography.labelMedium)
                        Text(FinanceMath.format(summary.workCents), fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleLarge)
                    }
                }
                Surface(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Doações recebidas", style = MaterialTheme.typography.labelMedium)
                        Text(FinanceMath.format(summary.donationCents), fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
        item { SectionTitle("Despesas por categoria") }
        if (summary.categories.isEmpty()) {
            item { EmptyState("Sem despesas no período", "Os gráficos surgem após registrar despesas.", Icons.Outlined.BarChart) }
        } else {
            item {
                Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                        summary.categories.forEach { category ->
                            CategoryBar(category.name, category.cents, maxCategory, MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        item { SectionTitle("Gastos nos últimos 7 dias") }
        item {
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    summary.lastSevenDays.forEach { day ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            Text(day.date.format(daysFormat), modifier = Modifier.width(49.dp),
                                style = MaterialTheme.typography.bodySmall)
                            val p = if (maxDaily > 0) (day.expenseCents.toDouble() / maxDaily.toDouble())
                                .toFloat().coerceIn(0f, 1f) else 0f
                            LinearProgressIndicator(progress = { p }, modifier = Modifier.weight(1f).height(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant)
                            Text(FinanceMath.format(day.expenseCents),
                                modifier = Modifier.widthIn(min = 70.dp), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Text("O gráfico reflete apenas pagamentos registrados. Não é previsão de gastos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ReportAmountRow(label: String, cents: Long, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(FinanceMath.format(cents), fontWeight = FontWeight.SemiBold, color = color)
    }
}
