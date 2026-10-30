package br.com.adriedson.financasdaviagem.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.adriedson.financasdaviagem.core.FinanceMath
import br.com.adriedson.financasdaviagem.core.RecyclingMath
import br.com.adriedson.financasdaviagem.core.ScreenSummaries
import br.com.adriedson.financasdaviagem.data.Preferences
import br.com.adriedson.financasdaviagem.data.WalletAccount
import br.com.adriedson.financasdaviagem.data.RecyclingEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun labelMaterial(material: String) = if (material == "cans") "Latinhas de alumínio" else "Cobre"
private fun qtyLabel(material: String, units: Long): String =
    if (material == "cans") "${kotlin.math.abs(units)} latinhas" else "${RecyclingMath.kilograms(kotlin.math.abs(units))} kg"

@Composable
fun RecyclingScreen(
    events: List<RecyclingEvent>, config: Preferences, accounts: List<WalletAccount>,
    onAdd: (RecyclingEvent, Long, (Boolean) -> Unit) -> Unit,
    onDelete: (Long) -> Unit,
    onSaveConfig: (Preferences) -> Unit, onExport: () -> Unit
) {
    var openAction by remember { mutableStateOf<String?>(null) }
    var remove by remember { mutableStateOf<RecyclingEvent?>(null) }
    var showConfig by remember { mutableStateOf(false) }
    val totals = remember(events) { ScreenSummaries.recycling(events) }
    val cans = totals.cansInStock
    val collectedCans = totals.collectedCans
    val soldCans = totals.soldCans
    val copper = totals.copperGramsInStock
    val paid = totals.paidCents
    val estimatedCansG = remember(cans, config.cansPerKg) {
        RecyclingMath.estimatedGrams(cans, config.cansPerKg) ?: 0L
    }
    val estimatedCanValue = remember(estimatedCansG, config.cansPriceCentsKg) {
        RecyclingMath.totalCents(estimatedCansG, config.cansPriceCentsKg) ?: 0L
    }
    val recentEvents = remember(events) { events.asReversed() }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = rememberLazyListState(),
        contentPadding = PaddingValues(horizontal = 17.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Recicláveis", fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                    Text("Coleta e renda da cicloviagem", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { showConfig = true }) {
                    Icon(Icons.Outlined.Tune, contentDescription = "Parâmetros de latinhas")
                }
            }
        }
        item {
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Dinheiro recebido com reciclagem", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(FinanceMath.format(paid), fontSize = 32.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Text("Já incluído como receita de Trabalho no painel financeiro. Não somar outra vez.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(Modifier.weight(1f), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Latinhas no estoque", style = MaterialTheme.typography.bodySmall)
                        Text("$cans", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text("≈ ${RecyclingMath.kilograms(estimatedCansG)} kg estimados",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Surface(Modifier.weight(1f), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Cobre no estoque", style = MaterialTheme.typography.bodySmall)
                        Text("${RecyclingMath.kilograms(copper)} kg", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        Text("Peso informado por você", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Text("Total coletado: $collectedCans latinhas  •  Total vendido: $soldCans latinhas",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        item {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(17.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Valor potencial das latinhas (não recebido)", style = MaterialTheme.typography.labelLarge)
                    Text(FinanceMath.format(estimatedCanValue), style = MaterialTheme.typography.titleLarge)
                    Text("Estimativa: ${config.cansPerKg} latinhas ≈ 1 kg • ${FinanceMath.format(config.cansPriceCentsKg)}/kg. " +
                        "Não integra o saldo até registrar uma venda.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Button(onClick = { openAction = "collect" }, Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 5.dp)) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(19.dp)); Spacer(Modifier.width(3.dp)); Text("Coletar")
                }
                Button(onClick = { openAction = "sale" }, Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 5.dp)) {
                    Icon(Icons.Outlined.Payments, null, Modifier.size(19.dp)); Spacer(Modifier.width(3.dp)); Text("Vender")
                }
                OutlinedButton(onClick = { openAction = "adjust" }, Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text("Ajustar")
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Histórico da reciclagem", modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onExport) { Icon(Icons.Outlined.FileDownload, null); Text(" CSV") }
            }
            Text("Toque na lixeira de um registro para excluir. Vendas atualizam também a receita de Trabalho.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        if (events.isEmpty()) item {
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(15.dp)) {
                Text("Nenhuma coleta ou venda registrada. Comece registrando o material coletado.",
                    Modifier.fillMaxWidth().padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(recentEvents, key = { it.id }, contentType = { "recycling-event" }) { event ->
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(15.dp)) {
                Row(Modifier.fillMaxWidth().padding(start = 13.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(when (event.action) {
                        "sale" -> Icons.Outlined.Payments
                        "adjust" -> Icons.Outlined.Build
                        else -> Icons.Outlined.Recycling
                    }, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("${when(event.action) { "sale" -> "Venda"; "adjust" -> "Ajuste"; else -> "Coleta" }} · ${labelMaterial(event.material)}",
                            fontWeight = FontWeight.SemiBold)
                        Text("${if (event.units < 0) "−" else "+"}${qtyLabel(event.material, event.units)}" +
                            (if (event.action == "sale") " • +${FinanceMath.format(event.receivedCents ?: 0)}" else ""),
                            color = if (event.action == "sale") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium)
                        if (event.estimatedWeight) Text("Peso estimado pela relação latinhas/kg", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val labelTime = remember(event.createdAt, event.note) {
                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR")).format(Date(event.createdAt)) +
                                if (event.note.isBlank()) "" else " · ${event.note}"
                        }
                        Text(labelTime,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { remove = event }) { Icon(Icons.Outlined.DeleteOutline, "Excluir registro") }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }

    openAction?.let { action ->
        RecyclingEditor(action, config, cans, copper, accounts, onDismiss = { openAction = null }, onSave = { event, accountId, complete ->
            onAdd(event, accountId, complete)
        })
    }
    remove?.let { selected ->
        AlertDialog(onDismissRequest = { remove = null },
            title = { Text("Excluir registro?") },
            text = { Text(if (selected.action == "sale")
                "A venda e sua receita de Trabalho serão excluídas juntas."
                else "O estoque será recalculado. A exclusão será recusada se deixar vendas sem estoque.") },
            confirmButton = { TextButton(onClick = { onDelete(selected.id); remove = null }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { remove = null }) { Text("Cancelar") } })
    }
    if (showConfig) RecyclingConfigEditor(config,
        onDismiss = { showConfig = false },
        onSave = { onSaveConfig(it); showConfig = false })
}

