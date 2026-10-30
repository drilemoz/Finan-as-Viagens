package br.com.adriedson.financasdaviagem.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.adriedson.financasdaviagem.core.FinanceMath
import br.com.adriedson.financasdaviagem.core.RecyclingMath
import br.com.adriedson.financasdaviagem.data.Preferences
import br.com.adriedson.financasdaviagem.data.WalletAccount
import br.com.adriedson.financasdaviagem.data.RecyclingEvent

@Composable
internal fun RecyclingEditor(action: String, config: Preferences, stockCans: Long, stockCopperGrams: Long,
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
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = unitsInput, onValueChange = { if (it.length < 20) { unitsInput = it; error = "" } },
                            label = { Text(if (material == "cans") "Quantidade de latinhas" else "Peso de cobre (kg)") },
                            modifier = Modifier.weight(1f), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = if (material == "cans") KeyboardType.Number else KeyboardType.Decimal))
                        if (action == "sale") {
                            val available = if (material == "cans") stockCans else stockCopperGrams
                            OutlinedButton(onClick = {
                                unitsInput = if (material == "cans") stockCans.toString()
                                    else RecyclingMath.kilograms(stockCopperGrams)
                                // Measured kilograms from a scale take precedence. Leave this blank for
                                // an estimated cans mass unless the user entered an actual weighing.
                                error = ""
                            }, enabled = available > 0, contentPadding = PaddingValues(horizontal = 9.dp)) {
                                Text("Usar todas", maxLines = 2)
                            }
                        }
                    }
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
                        MoneyField(value = unitPrice, onValueChange = { unitPrice = it },
                            label = "Preço por kg", modifier = Modifier.fillMaxWidth())
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
internal fun RecyclingConfigEditor(config: Preferences, onDismiss: () -> Unit, onSave: (Preferences) -> Unit) {
    var count by remember { mutableStateOf(config.cansPerKg.toString()) }
    var price by remember { mutableStateOf(FinanceMath.decimalInput(config.cansPriceCentsKg)) }
    var error by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Parâmetros das latinhas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Estimativa inicial: 70 latinhas por kg e R$ 6,00/kg. Você pode mudar esses valores; não substituem uma pesagem real.")
                OutlinedTextField(count, { count = it }, label = { Text("Latinhas estimadas por kg") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                MoneyField(value = price, onValueChange = { price = it },
                    label = "Preço estimado por kg", modifier = Modifier.fillMaxWidth())
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
