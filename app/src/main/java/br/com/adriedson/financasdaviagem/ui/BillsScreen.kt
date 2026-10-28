package br.com.adriedson.financasdaviagem.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.adriedson.financasdaviagem.core.BillMath
import br.com.adriedson.financasdaviagem.core.FinanceMath
import br.com.adriedson.financasdaviagem.data.Bill
import br.com.adriedson.financasdaviagem.data.WalletAccount
import br.com.adriedson.financasdaviagem.data.expenseCategories
import java.time.LocalDate

@Composable
fun BillsScreen(
    bills: List<Bill>, accounts: List<WalletAccount>,
    onBack: () -> Unit,
    onSave: (Bill) -> Unit,
    onPay: (Long, Long, (Boolean) -> Unit) -> Unit,
    onDelete: (Long) -> Unit,
    onExport: () -> Unit
) {
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Bill?>(null) }
    var paying by remember { mutableStateOf<Bill?>(null) }
    var deleting by remember { mutableStateOf<Bill?>(null) }
    val pending = bills.filter { it.paidEntryId == null }
    val overdue = pending.count { BillMath.isOverdue(it.dueDate) }
    val pendingTotal = pending.sumOf { it.amountCents }
    val paidTotal = bills.filter { it.paidEntryId != null }.sumOf { it.amountCents }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 17.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Voltar para Carteiras") }
                Column(Modifier.weight(1f)) {
                    Text("Contas e despesas", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    Text("Organize os pagamentos da viagem", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onExport) { Icon(Icons.Outlined.FileDownload, "Exportar contas CSV") }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("A pagar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(FinanceMath.format(pendingTotal), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("${pending.size} pendentes · $overdue vencidas · ${FinanceMath.format(paidTotal)} já pago",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overdue > 0) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Contas previstas não reduzem o saldo. O valor sai da carteira ao confirmar o pagamento.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Outlined.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Adicionar conta a pagar")
            }
        }
        if (bills.isEmpty()) {
            item {
                Text("Nenhuma conta cadastrada. Adicione uma despesa prevista, com valor e vencimento.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(bills, key = { it.id }) { bill ->
            val isPaid = bill.paidEntryId != null
            val late = !isPaid && BillMath.isOverdue(bill.dueDate)
            Surface(shape = RoundedCornerShape(17.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(bill.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                            Text("${bill.category} · Vence ${BillMath.displayDate(bill.dueDate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(FinanceMath.format(bill.amountCents), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                    }
                    if (bill.note.isNotBlank()) Text(bill.note, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(when { isPaid -> "Paga"; late -> "Vencida"; else -> "Pendente" },
                            color = when { isPaid -> MaterialTheme.colorScheme.primary
                                late -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant },
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f))
                        if (!isPaid) {
                            TextButton(onClick = { editing = bill }) { Text("Editar") }
                            TextButton(onClick = { deleting = bill }) { Icon(Icons.Outlined.DeleteOutline, "Excluir") }
                            Button(onClick = { paying = bill }, shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Pagar") }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
    if (creating || editing != null) {
        val old = editing
        BillEditor(existing = old, onDismiss = { creating = false; editing = null }, onSave = { next ->
            onSave(next)
            creating = false
            editing = null
        })
    }
    paying?.let { selected ->
        var account by remember(selected.id) {
            mutableLongStateOf(accounts.firstOrNull { !it.isReserve }?.id ?: accounts.firstOrNull()?.id ?: 0L)
        }
        var saving by remember(selected.id) { mutableStateOf(false) }
        var error by remember(selected.id) { mutableStateOf("") }
        AlertDialog(onDismissRequest = { if (!saving) paying = null },
            title = { Text("Confirmar pagamento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Text("${selected.title} · ${FinanceMath.format(selected.amountCents)}")
                    WalletAccountPicker("Pagar com", accounts, account) { account = it }
                    Text("O valor será registrado como despesa uma única vez, na carteira escolhida.",
                        style = MaterialTheme.typography.bodySmall)
                    if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
                }
            }, confirmButton = {
                TextButton(enabled = !saving && account > 0, onClick = {
                    saving = true
                    onPay(selected.id, account) { ok ->
                        saving = false
                        if (ok) paying = null else error = "Não foi possível confirmar o pagamento."
                    }
                }) { Text(if (saving) "Salvando..." else "Confirmar") }
            }, dismissButton = {
                TextButton(enabled = !saving, onClick = { paying = null }) { Text("Cancelar") }
            })
    }
    deleting?.let { selected ->
        AlertDialog(onDismissRequest = { deleting = null },
            title = { Text("Excluir conta pendente?") },
            text = { Text("${selected.title} será removida. Nenhuma receita ou despesa será criada.") },
            confirmButton = { TextButton(onClick = { onDelete(selected.id); deleting = null }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } })
    }
}

@Composable
private fun BillEditor(existing: Bill?, onDismiss: () -> Unit, onSave: (Bill) -> Unit) {
    var title by remember(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var category by remember(existing?.id) { mutableStateOf(existing?.category ?: "Outros") }
    var amount by remember(existing?.id) {
        mutableStateOf(existing?.amountCents?.let { FinanceMath.decimalInput(it) } ?: "")
    }
    var date by remember(existing?.id) {
        mutableStateOf(existing?.dueDate?.let { BillMath.displayDate(it) } ?:
            LocalDate.now().plusDays(7).let { BillMath.displayDate(it.toString()) })
    }
    var note by remember(existing?.id) { mutableStateOf(existing?.note ?: "") }
    var categoriesOpen by remember(existing?.id) { mutableStateOf(false) }
    var error by remember(existing?.id) { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.94f).fillMaxHeight(.86f).imePadding(),
            shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (existing == null) "Nova conta a pagar" else "Editar conta",
                        modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Fechar") }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(title, { if (it.length <= 100) title = it },
                        label = { Text("Conta ou despesa") }, placeholder = { Text("Ex.: Internet, hospedagem") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(amount, { if (it.length <= 22) amount = it },
                        label = { Text("Valor (R$)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(date, { if (it.length <= 10) date = it },
                        label = { Text("Vencimento (DD/MM/AAAA)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Column {
                        Text("Categoria", style = MaterialTheme.typography.labelMedium)
                        Box {
                            OutlinedButton(onClick = { categoriesOpen = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(category, Modifier.weight(1f))
                                Icon(Icons.Outlined.ArrowDropDown, null)
                            }
                            DropdownMenu(expanded = categoriesOpen, onDismissRequest = { categoriesOpen = false }) {
                                expenseCategories.forEach { candidate ->
                                    DropdownMenuItem(text = { Text(candidate) }, onClick = {
                                        category = candidate
                                        categoriesOpen = false
                                    })
                                }
                            }
                        }
                    }
                    OutlinedTextField(note, { if (it.length <= 180) note = it },
                        label = { Text("Observação (opcional)") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                    Text("Esta conta não reduz o dinheiro disponível até que você marque como paga.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
                Button(onClick = {
                    val cents = FinanceMath.parseAmount(amount)
                    val due = BillMath.parseDate(date)
                    if (title.trim().length !in 2..100 || cents == null || cents <= 0L || due == null) {
                        error = "Informe nome, valor positivo e vencimento válido (DD/MM/AAAA)."
                    } else onSave(Bill(id = existing?.id ?: 0L, title = title.trim(),
                        category = category, amountCents = cents, dueDate = due, note = note.trim(),
                        createdAt = existing?.createdAt ?: System.currentTimeMillis()))
                }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                    Text("Salvar conta")
                }
            }
        }
    }
}
