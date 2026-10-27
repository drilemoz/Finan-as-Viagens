package br.com.adriedson.financasdaviagem.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import br.com.adriedson.financasdaviagem.core.FinanceMath
import br.com.adriedson.financasdaviagem.data.Entry
import br.com.adriedson.financasdaviagem.data.expenseCategories
import br.com.adriedson.financasdaviagem.data.incomeCategories

@Composable
private fun ChoiceMenu(label: String, selection: String, choices: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(onClick = { expanded = true }) { Text(selection) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                choices.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = {
                        expanded = false; onSelected(option)
                    })
                }
            }
        }
    }
}

@Composable
fun EntryEditor(
    existing: Entry?, initialKind: String,
    onDismiss: () -> Unit, onSave: (Entry) -> Unit, onDelete: ((Entry) -> Unit)
) {
    var kind by remember(existing?.id) { mutableStateOf(existing?.kind ?: initialKind) }
    var category by remember(existing?.id) {
        mutableStateOf(existing?.category ?: if (initialKind == "income") incomeCategories.first() else expenseCategories.first())
    }
    var amount by remember(existing?.id) {
        mutableStateOf(existing?.let { FinanceMath.decimalInput(it.originalMinor, FinanceMath.digits(it.currency)) } ?: "")
    }
    var currency by remember(existing?.id) { mutableStateOf(existing?.currency ?: "BRL") }
    var rate by remember(existing?.id) { mutableStateOf(existing?.rateBrl?.replace('.', ',') ?: "") }
    var note by remember(existing?.id) { mutableStateOf(existing?.note ?: "") }
    var city by remember(existing?.id) { mutableStateOf(existing?.city ?: "") }
    var country by remember(existing?.id) { mutableStateOf(existing?.country ?: "") }
    var error by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    val categories = if (kind == "expense") expenseCategories else incomeCategories
    val amountMinor = FinanceMath.parseAmount(amount, FinanceMath.digits(currency))
    val converted = amountMinor?.takeIf { it > 0 }?.let {
        FinanceMath.convertToBrl(it, currency, if (currency == "BRL") "1" else rate)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().heightIn(max = 660.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (existing == null) "Novo lançamento" else "Editar lançamento",
                    style = MaterialTheme.typography.titleLarge)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = kind == "expense", onClick = {
                            if (kind != "expense") { kind = "expense"; category = expenseCategories.first() }
                        }, label = { Text("Despesa") })
                        FilterChip(selected = kind == "income", onClick = {
                            if (kind != "income") { kind = "income"; category = incomeCategories.first() }
                        }, label = { Text("Receita") })
                    }
                    OutlinedTextField(amount, { amount = it }, label = { Text("Valor") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    Text("Não use ponto de milhar. Exemplo: 18,50.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ChoiceMenu("Moeda", currency, FinanceMath.supportedCurrencies) { selected ->
                        if (currency != selected) {
                            currency = selected
                            amount = ""
                            if (selected == "BRL") rate = ""
                        }
                    }
                    if (currency != "BRL") {
                        OutlinedTextField(rate, { rate = it }, modifier = Modifier.fillMaxWidth(),
                            label = { Text("Taxa: R$ por 1 $currency") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        Text("Cotação manual. Ex.: 0,005 significa R$ 0,005 por unidade.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (converted != null) Text("Convertido: ${FinanceMath.format(converted)}",
                            color = MaterialTheme.colorScheme.primary)
                    }
                    ChoiceMenu("Categoria", category, categories) { category = it }
                    OutlinedTextField(note, { if (it.length <= 300) note = it },
                        label = { Text("Descrição (opcional)") }, modifier = Modifier.fillMaxWidth(),
                        maxLines = 2)
                    OutlinedTextField(city, { if (it.length <= 100) city = it },
                        label = { Text("Cidade (opcional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(country, { if (it.length <= 100) country = it },
                        label = { Text("País (opcional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (existing != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Excluir lançamento",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    } else Spacer(Modifier.width(38.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = onDismiss) { Text("Cancelar") }
                        Button(onClick = {
                            if (converted == null || amountMinor == null || amountMinor <= 0) {
                                error = "Informe um valor positivo e uma cotação válida."
                            } else {
                                onSave(Entry(
                                    id = existing?.id ?: 0L, kind = kind, category = category,
                                    note = note.trim(), city = city.trim(), country = country.trim(),
                                    currency = currency, originalMinor = amountMinor,
                                    rateBrl = if (currency == "BRL") "1" else rate.trim().replace(',', '.'),
                                    brlCents = converted,
                                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                                ))
                            }
                        }) { Text("Salvar") }
                    }
                }
            }
        }
    }
    if (confirmDelete && existing != null) {
        AlertDialog(onDismissRequest = { confirmDelete = false },
            title = { Text("Excluir lançamento?") },
            text = { Text("Esta movimentação será removida do histórico e dos totais.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(existing) }) { Text("Excluir") }
            }, dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") }
            })
    }
}
