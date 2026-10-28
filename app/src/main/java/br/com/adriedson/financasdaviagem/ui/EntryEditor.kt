package br.com.adriedson.financasdaviagem.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import br.com.adriedson.financasdaviagem.data.WalletAccount
import br.com.adriedson.financasdaviagem.data.Entry
import br.com.adriedson.financasdaviagem.data.expenseCategories
import br.com.adriedson.financasdaviagem.data.incomeCategories

@Composable
private fun ChoiceMenu(label: String, selection: String, choices: List<String>, onSelected: (String) -> Unit,
                       modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                Text(selection, modifier = Modifier.weight(1f), maxLines = 1)
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                choices.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = {
                        expanded = false
                        onSelected(option)
                    })
                }
            }
        }
    }
}

@Composable
fun EntryEditor(
    existing: Entry?, initialKind: String, accounts: List<WalletAccount>,
    initialCategory: String? = null,
    onDismiss: () -> Unit, onSave: (Entry) -> Unit, onDelete: ((Entry) -> Unit)
) {
    var accountId by remember(existing?.id, accounts) { mutableLongStateOf(existing?.accountId ?: accounts.firstOrNull { !it.isReserve }?.id ?: 1L) }
    var kind by remember(existing?.id) { mutableStateOf(existing?.kind ?: initialKind) }
    var category by remember(existing?.id, initialKind, initialCategory) {
        mutableStateOf(existing?.category ?: initialCategory ?:
            if (initialKind == "income") incomeCategories.first() else expenseCategories.first())
    }
    var amount by remember(existing?.id) {
        mutableStateOf(existing?.let { FinanceMath.decimalInput(it.originalMinor, FinanceMath.digits(it.currency)) } ?: "")
    }
    var currency by remember(existing?.id) { mutableStateOf(existing?.currency ?: "BRL") }
    var rate by remember(existing?.id) { mutableStateOf(existing?.rateBrl?.replace('.', ',') ?: "") }
    var note by remember(existing?.id) { mutableStateOf(existing?.note ?: "") }
    var city by remember(existing?.id) { mutableStateOf(existing?.city ?: "") }
    var country by remember(existing?.id) { mutableStateOf(existing?.country ?: "") }
    var useQuantity by remember(existing?.id) { mutableStateOf(existing?.itemKind != null) }
    var itemKind by remember(existing?.id) { mutableStateOf(existing?.itemKind ?: "cans") }
    var quantity by remember(existing?.id) { mutableStateOf(existing?.quantity?.toString() ?: "1") }
    var unitPrice by remember(existing?.id) {
        mutableStateOf(existing?.unitMinor?.let { FinanceMath.decimalInput(it, FinanceMath.digits(existing.currency)) } ?: "")
    }
    var showLocation by remember(existing?.id) { mutableStateOf(!existing?.city.isNullOrBlank() || !existing?.country.isNullOrBlank()) }
    var error by remember(existing?.id) { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    val categories = if (kind == "expense") expenseCategories else incomeCategories
    val digits = FinanceMath.digits(currency)
    val parsedUnit = FinanceMath.parseAmount(unitPrice, digits)
    val parsedQuantity = quantity.toIntOrNull()?.takeIf { it in 1..1_000_000 }
    val amountMinor = if (useQuantity) {
        if (parsedUnit != null && parsedQuantity != null) FinanceMath.quantityTotal(parsedUnit, parsedQuantity) else null
    } else FinanceMath.parseAmount(amount, digits)
    val converted = amountMinor?.takeIf { it > 0 }?.let {
        FinanceMath.convertToBrl(it, currency, if (currency == "BRL") "1" else rate)
    }

    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.95f).imePadding()
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ReceiptLong, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(if (existing == null) "Novo lançamento" else "Editar lançamento",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "Fechar") }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        listOf("expense" to "Despesa", "income" to "Receita").forEach { (value, title) ->
                            val selected = kind == value
                            if (selected) Button(onClick = {}, modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(13.dp)) { Text(title) }
                            else OutlinedButton(onClick = {
                                kind = value
                                category = if (value == "expense") expenseCategories.first() else incomeCategories.first()
                                if (useQuantity && itemKind == "cans") {
                                    category = if (value == "income") "Venda de latinhas" else "Bebidas"
                                }
                                error = ""
                            }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp)) { Text(title) }
                        }
                    }

                    WalletAccountPicker("Conta utilizada", accounts, accountId) { accountId = it }

                    Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(if (useQuantity) "Total calculado" else "Valor do lançamento",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (useQuantity) {
                                Text(amountMinor?.let { FinanceMath.format(it, currency) } ?: "Preencha os itens",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold)
                            } else {
                                MoneyField(value = amount, onValueChange = { amount = it; error = "" },
                                    label = "Valor ($currency)", currency = currency, decimals = digits,
                                    modifier = Modifier.fillMaxWidth())
                            }
                            if (currency != "BRL" && converted != null) {
                                Text("Equivalente: ${FinanceMath.format(converted)}",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ChoiceMenu("Moeda", currency, FinanceMath.supportedCurrencies, { chosen ->
                            if (chosen != currency) {
                                currency = chosen
                                amount = ""
                                unitPrice = ""
                                if (chosen == "BRL") rate = ""
                                error = ""
                            }
                        }, Modifier.weight(1f))
                        ChoiceMenu("Categoria", category, categories, { category = it }, Modifier.weight(1.65f))
                    }
                    if (currency != "BRL") {
                        OutlinedTextField(rate, { if (it.length <= 22) rate = it },
                            label = { Text("Cotação: R$ por 1 $currency") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(13.dp),
                            supportingText = { Text("Informe a cotação real usada; sem atualização automática.") })
                    }


                    Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Inventory2, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Registrar quantidade", fontWeight = FontWeight.SemiBold)
                                    Text("Latinhas ou outros itens (opcional)",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(checked = useQuantity, onCheckedChange = { enabled ->
                                    useQuantity = enabled
                                    error = ""
                                    if (enabled && itemKind == "cans") {
                                        category = if (kind == "income") "Venda de latinhas" else "Bebidas"
                                    } else if (!enabled && amount.isBlank() && amountMinor != null) {
                                        amount = FinanceMath.decimalInput(amountMinor, digits)
                                    }
                                })
                            }
                            if (useQuantity) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(selected = itemKind == "cans", onClick = {
                                        itemKind = "cans"
                                        category = if (kind == "income") "Venda de latinhas" else "Bebidas"
                                    }, label = { Text("Latinhas") })
                                    FilterChip(selected = itemKind == "units", onClick = { itemKind = "units" },
                                        label = { Text("Outros itens") })
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    MoneyField(value = unitPrice, onValueChange = { unitPrice = it; error = "" },
                                        label = "Preço/unid.", currency = currency, decimals = digits,
                                        modifier = Modifier.weight(1.15f))
                                    OutlinedTextField(quantity, {
                                        if (it.length <= 7 && it.all(Char::isDigit)) { quantity = it; error = "" }
                                    }, label = { Text("Quantidade") },
                                        modifier = Modifier.weight(1f), singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(13.dp))
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text("Ajustar: ", style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    OutlinedIconButton(onClick = {
                                        quantity = ((parsedQuantity ?: 1) - 1).coerceAtLeast(1).toString()
                                    }, enabled = (parsedQuantity ?: 1) > 1) {
                                        Icon(Icons.Outlined.Remove, contentDescription = "Diminuir quantidade")
                                    }
                                    Text(" ${parsedQuantity ?: 0} ", style = MaterialTheme.typography.titleMedium)
                                    OutlinedIconButton(onClick = {
                                        quantity = ((parsedQuantity ?: 0) + 1).coerceAtMost(1_000_000).toString()
                                    }, enabled = (parsedQuantity ?: 0) < 1_000_000) {
                                        Icon(Icons.Outlined.Add, contentDescription = "Aumentar quantidade")
                                    }
                                }
                                Text("O total é calculado automaticamente. Não é necessário preencher o valor duas vezes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    OutlinedTextField(note, { if (it.length <= 300) note = it },
                        label = { Text("Descrição (opcional)") },
                        placeholder = { Text("Ex.: venda de latinhas no centro") },
                        modifier = Modifier.fillMaxWidth(), maxLines = 2,
                        shape = RoundedCornerShape(13.dp))
                    TextButton(onClick = { showLocation = !showLocation },
                        contentPadding = PaddingValues(horizontal = 0.dp)) {
                        Icon(if (showLocation) Icons.Outlined.ExpandLess else Icons.Outlined.AddLocationAlt,
                            contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (showLocation) "Ocultar localização" else "Adicionar cidade e país")
                    }
                    if (showLocation) {
                        OutlinedTextField(city, { if (it.length <= 100) city = it },
                            label = { Text("Cidade (opcional)") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            shape = RoundedCornerShape(13.dp))
                        OutlinedTextField(country, { if (it.length <= 100) country = it },
                            label = { Text("País (opcional)") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            shape = RoundedCornerShape(13.dp))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (existing != null) {
                            IconButton(onClick = { confirmDelete = true }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Excluir lançamento",
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        TextButton(onClick = onDismiss) { Text("Cancelar") }
                        Button(onClick = {
                            when {
                                useQuantity && (parsedUnit == null || parsedUnit <= 0 || parsedQuantity == null) ->
                                    error = "Informe quantidade e preço unitário válidos."
                                converted == null || amountMinor == null || amountMinor <= 0 ->
                                    error = "Informe um valor positivo e uma cotação válida."
                                else -> onSave(Entry(
                                    id = existing?.id ?: 0L, kind = kind, category = category,
                                    note = note.trim(), city = city.trim(), country = country.trim(),
                                    currency = currency, originalMinor = amountMinor,
                                    rateBrl = if (currency == "BRL") "1" else rate.trim().replace(',', '.'),
                                    brlCents = converted,
                                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                                    itemKind = if (useQuantity) itemKind else null,
                                    quantity = if (useQuantity) parsedQuantity else null,
                                    unitMinor = if (useQuantity) parsedUnit else null,
                                    accountId = accountId
                                ))
                            }
                        }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp)) {
                            Icon(Icons.Outlined.Check, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Salvar lançamento", fontWeight = FontWeight.SemiBold)
                        }
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
