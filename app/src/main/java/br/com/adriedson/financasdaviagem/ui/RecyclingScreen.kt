package br.com.adriedson.financasdaviagem.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
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
    val cans = events.filter { it.material == "cans" }.sumOf { it.units }
    val collectedCans = events.filter { it.material == "cans" && it.action == "collect" }.sumOf { it.units }
    val soldCans = -events.filter { it.material == "cans" && it.action == "sale" }.sumOf { it.units }
    val copper = events.filter { it.material == "copper" }.sumOf { it.units }
    val paid = events.filter { it.action == "sale" }.sumOf { it.receivedCents ?: 0L }
    val estimatedCansG = RecyclingMath.estimatedGrams(cans, config.cansPerKg) ?: 0L
    val estimatedCanValue = RecyclingMath.totalCents(estimatedCansG, config.cansPriceCentsKg) ?: 0L
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
        items(events.asReversed(), key = { it.id }) { event ->
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
                        Text(SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR")).format(Date(event.createdAt)) +
                            if (event.note.isBlank()) "" else " · ${event.note}",
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

@Composable
private fun RecyclingEditor(action: String, config: Preferences, stockCans: Long, stockCopperGrams: Long,
                            accounts: List<WalletAccount>, onDismiss: () -> Unit,
                            onSave: (RecyclingEvent, Long, (Boolean) -> Unit) -> Unit) {
    var receivingAccount by remember(action) { mutableLongStateOf(accounts.firstOrNull { !it.isReserve }?.id ?: 1L) }
    var material by remember(action) { mutableStateOf("cans") }
    var unitsInput by remember(action, material) { mutableStateOf("") }
    var measuredKg by remember(action, material) { mutableStateOf("") }
    var unitPrice by remember(action, material) {
        mutableStateOf(if (material == "cans") FinanceMath.decimalInput(config.cansPriceCentsKg) else "")
    }
    var negativeAdjustment by remember(action, material) { mutableStateOf(false) }
    var note by remember(action) { mutableStateOf("") }
    var city by remember(action) { mutableStateOf("") }
    var country by remember(action) { mutableStateOf("") }
    var location by remember(action) { mutableStateOf(false) }
    var error by remember(action) { mutableStateOf("") }
    var saving by remember(action) { mutableStateOf(false) }
    val inputUnits = if (material == "cans") unitsInput.toLongOrNull()?.takeIf { it in 1..1_000_000 }
        else RecyclingMath.parseKg(unitsInput)?.takeIf { it in 1..1_000_000_000L }
    val price = FinanceMath.parseAmount(unitPrice)?.takeIf { it > 0 }
    val weight = if (material == "copper") inputUnits else {
        if (measuredKg.isBlank()) inputUnits?.let { RecyclingMath.estimatedGrams(it, config.cansPerKg) }
        else RecyclingMath.parseKg(measuredKg)?.takeIf { it > 0 }
    }
    val total = if (action == "sale" && price != null && weight != null) RecyclingMath.totalCents(weight, price) else null
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.95f).fillMaxHeight(.88f).imePadding(),
            shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(when (action) { "sale" -> "Registrar venda"; "adjust" -> "Ajustar estoque"; else -> "Registrar coleta" },
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Fechar") }
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("cans" to "Latinhas", "copper" to "Cobre").forEach { (id, label) ->
                            FilterChip(selected = material == id, onClick = { material = id; error = "" }, label = { Text(label) })
                        }
                    }
                    Text(if (material == "cans") "Estoque: $stockCans latinhas" else "Estoque: ${RecyclingMath.kilograms(stockCopperGrams)} kg",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = unitsInput, onValueChange = { if (it.length < 20) { unitsInput = it; error = "" } },
                        label = { Text(if (material == "cans") "Quantidade de latinhas" else "Peso de cobre (kg)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = if (material == "cans") KeyboardType.Number else KeyboardType.Decimal))
                    if (action == "adjust") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = negativeAdjustment, onCheckedChange = { negativeAdjustment = it })
                            Text("Retirar material do estoque")
                        }
                        Text("Use ajustes para corrigir contagens e perdas, com justificativa.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (action == "sale") {
                        WalletAccountPicker("Receber em", accounts, receivingAccount) { receivingAccount = it }
                        if (material == "cans") {
                            OutlinedTextField(measuredKg, { if (it.length < 20) measuredKg = it },
                                label = { Text("Peso medido (kg, opcional)") }, modifier = Modifier.fillMaxWidth(),
                                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                            Text(if (measuredKg.isBlank())
                                "Sem pesagem: estimativa de ${config.cansPerKg} latinhas por kg. Peso calculado: ${weight?.let { RecyclingMath.kilograms(it) } ?: "—"} kg."
                                else "Será usado o peso medido que você informar.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedTextField(unitPrice, { if (it.length < 20) unitPrice = it },
                            label = { Text("Preço por kg (R$)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(15.dp)) {
                            Column(Modifier.fillMaxWidth().padding(13.dp)) {
                                Text("Receita de Trabalho", style = MaterialTheme.typography.labelMedium)
                                Text(total?.let { FinanceMath.format(it) } ?: "Preencha os dados",
                                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                Text("Registrar somente após receber o pagamento. Não duplique uma venda já lançada no Histórico.",
                            style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    OutlinedTextField(note, { if (it.length <= 300) note = it },
                        label = { Text(if (action == "adjust") "Motivo do ajuste" else "Observação (opcional)") },
                        modifier = Modifier.fillMaxWidth(), maxLines = 2)
                    TextButton(onClick = { location = !location }) {
                        Icon(Icons.Outlined.Place, null); Text("  ${if (location) "Ocultar local" else "Informar cidade e país"}")
                    }
                    if (location) {
                        OutlinedTextField(city, { if (it.length <= 100) city = it }, label = { Text("Cidade") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(country, { if (it.length <= 100) country = it }, label = { Text("País") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                }
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Button(onClick = {
                    if (inputUnits == null || (action == "adjust" && note.isBlank()) ||
                        (action == "sale" && (total == null || total == 0L || weight == null ||
                            (if (material == "cans") inputUnits > stockCans else inputUnits > stockCopperGrams)))) {
                        error = "Confira a quantidade, o estoque, o preço e o motivo do ajuste."
                    } else {
                        val signedUnits = if (action == "sale" || (action == "adjust" && negativeAdjustment)) -inputUnits else inputUnits
                        saving = true
                        onSave(RecyclingEvent(material = material, action = action, units = signedUnits,
                            measuredGrams = if (action == "sale" && material == "cans") weight else null,
                            estimatedWeight = action == "sale" && material == "cans" && measuredKg.isBlank(),
                            priceCentsKg = if (action == "sale") price else null,
                            receivedCents = if (action == "sale") total else null,
                            note = note.trim(), city = city.trim(), country = country.trim()), receivingAccount) { ok ->
                            saving = false
                            if (ok) onDismiss() else error = "Falha ao salvar. Confira o estoque e tente novamente."
                        }
                    }
                }, enabled = !saving, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                    Text(if (saving) "Salvando..." else if (action == "sale") "Confirmar pagamento e venda" else "Salvar registro")
                }
            }
        }
    }
}

@Composable
private fun RecyclingConfigEditor(config: Preferences, onDismiss: () -> Unit, onSave: (Preferences) -> Unit) {
    var count by remember { mutableStateOf(config.cansPerKg.toString()) }
    var price by remember { mutableStateOf(FinanceMath.decimalInput(config.cansPriceCentsKg)) }
    var error by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Parâmetros das latinhas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Estimativa inicial: 70 latinhas por kg e R$ 6,00/kg. Você pode mudar esses valores; não substituem uma pesagem real.")
                OutlinedTextField(count, { count = it }, label = { Text("Latinhas estimadas por kg") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(price, { price = it }, label = { Text("Preço estimado por kg (R$)") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        }, confirmButton = {
            TextButton(onClick = {
                val cans = count.toIntOrNull()?.takeIf { it in 1..1000 }
                val cents = FinanceMath.parseAmount(price)?.takeIf { it in 1..100_000_000L }
                if (cans == null || cents == null) error = "Use uma quantidade válida e um preço maior que zero."
                else onSave(config.copy(cansPerKg = cans, cansPriceCentsKg = cents))
            }) { Text("Salvar") }
        }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}
