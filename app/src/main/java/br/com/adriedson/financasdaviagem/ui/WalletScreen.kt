package br.com.adriedson.financasdaviagem.ui

import androidx.compose.foundation.layout.*
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
import br.com.adriedson.financasdaviagem.core.FinanceMath
import br.com.adriedson.financasdaviagem.core.WalletMath
import br.com.adriedson.financasdaviagem.data.Entry
import br.com.adriedson.financasdaviagem.data.WalletAccount
import br.com.adriedson.financasdaviagem.data.WalletTransfer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WalletScreen(
    accounts: List<WalletAccount>, transfers: List<WalletTransfer>, entries: List<Entry>,
    onSaveAccount: (WalletAccount) -> Unit,
    onTransfer: (WalletTransfer) -> Unit, onDeleteTransfer: (Long) -> Unit,
    onExport: () -> Unit
) {
    var editing by remember { mutableStateOf<WalletAccount?>(null) }
    var creating by remember { mutableStateOf(false) }
    var addingTransfer by remember { mutableStateOf(false) }
    var deleteTransfer by remember { mutableStateOf<WalletTransfer?>(null) }
    val balances = remember(accounts, transfers, entries) {
        runCatching { WalletMath.balances(accounts, entries, transfers) }.getOrDefault(emptyMap())
    }
    val usable = WalletMath.usable(accounts, balances)
    val reserve = WalletMath.reserve(accounts, balances)
    val names = accounts.associate { it.id to it.name }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Carteiras e contas", fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                Text("Dinheiro físico, bancos e reserva. Transferir não é ganhar ou gastar.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Surface(shape = RoundedCornerShape(19.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Disponível para viajar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(FinanceMath.format(usable), fontSize = 30.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Text("Reserva separada: ${FinanceMath.format(reserve)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { addingTransfer = true }, Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Outlined.SwapHoriz, null); Spacer(Modifier.width(5.dp)); Text("Transferir")
                }
                OutlinedButton(onClick = { creating = true }, Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(4.dp)); Text("Nova conta")
                }
            }
        }
        item { Text("Saldos por conta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        items(accounts, key = { "account-${it.id}" }) { a ->
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (a.isReserve) Icons.Outlined.Savings else Icons.Outlined.AccountBalanceWallet,
                        null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(a.name, fontWeight = FontWeight.SemiBold)
                        Text(if (a.isReserve) "Fora da autonomia da viagem" else "Disponível",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(FinanceMath.format(balances[a.id] ?: 0L), style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(onClick = { editing = a }) { Icon(Icons.Outlined.Edit, "Editar conta") }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Transferências", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onExport) { Icon(Icons.Outlined.FileDownload, null); Text("CSV") }
            }
        }
        if (transfers.isEmpty()) item {
            Text("Nenhuma transferência. Movimentações entre contas não alteram o patrimônio total.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(transfers, key = { "transfer-${it.id}" }) { t ->
            Surface(shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 3.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.SwapHoriz, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${names[t.fromId] ?: "?"} → ${names[t.toId] ?: "?"}", fontWeight = FontWeight.Medium)
                        Text("${FinanceMath.format(t.cents)} · " + SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
                            .format(Date(t.createdAt)), style = MaterialTheme.typography.bodySmall)
                        if (t.note.isNotBlank()) Text(t.note, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { deleteTransfer = t }) { Icon(Icons.Outlined.DeleteOutline, "Excluir transferência") }
                }
            }
        }
    }
    if (creating || editing != null) {
        val account = editing
        var name by remember(creating, account?.id) { mutableStateOf(account?.name ?: "") }
        var opening by remember(creating, account?.id) {
            mutableStateOf(account?.openingCents?.let { FinanceMath.decimalInput(it) } ?: "0,00")
        }
        var reserveFlag by remember(creating, account?.id) { mutableStateOf(account?.isReserve ?: false) }
        var error by remember(creating, account?.id) { mutableStateOf("") }
        AlertDialog(onDismissRequest = { creating = false; editing = null },
            title = { Text(if (account == null) "Nova conta" else "Editar conta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(name, { if (it.length <= 50) name = it }, label = { Text("Nome") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(opening, { if (it.length <= 22) opening = it },
                        label = { Text("Saldo inicial em R$ (não é receita)") },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = reserveFlag, onCheckedChange = { reserveFlag = it })
                        Spacer(Modifier.width(9.dp)); Text("Reserva de emergência")
                    }
                    Text("Alterar o saldo inicial recalcula o saldo atual. Use transferências para mover dinheiro entre contas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = FinanceMath.parseAmount(opening)
                    if (name.trim().length !in 2..50 || amount == null) error = "Informe nome e saldo inicial válidos."
                    else {
                        onSaveAccount(WalletAccount(account?.id ?: 0L, name.trim(), amount, reserveFlag))
                        creating = false; editing = null
                    }
                }) { Text("Salvar") }
            }, dismissButton = { TextButton(onClick = { creating = false; editing = null }) { Text("Cancelar") } })
    }
    if (addingTransfer) {
        var from by remember { mutableStateOf(accounts.firstOrNull { !it.isReserve }?.id ?: accounts.firstOrNull()?.id ?: 1L) }
        var to by remember { mutableStateOf(accounts.firstOrNull { it.id != from }?.id ?: from) }
        var amount by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        var error by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { addingTransfer = false }, title = { Text("Transferir dinheiro") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    WalletAccountPicker("De", accounts, from) { from = it; if (from == to) to = accounts.firstOrNull { a -> a.id != from }?.id ?: from }
                    WalletAccountPicker("Para", accounts, to) { to = it }
                    OutlinedTextField(amount, { if (it.length <= 22) amount = it },
                        label = { Text("Valor em R$") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(note, { if (it.length <= 200) note = it },
                        label = { Text("Observação (opcional)") }, maxLines = 2)
                    Text("Disponível na origem: ${FinanceMath.format(balances[from] ?: 0L)}",
                        style = MaterialTheme.typography.bodySmall)
                    if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
                }
            }, confirmButton = {
                TextButton(onClick = {
                    val cents = FinanceMath.parseAmount(amount)
                    if (cents == null || cents <= 0 || from == to || cents > (balances[from] ?: 0L)) {
                        error = "Escolha contas distintas e um valor dentro do saldo disponível."
                    } else { onTransfer(WalletTransfer(fromId = from, toId = to, cents = cents, note = note.trim())); addingTransfer = false }
                }) { Text("Transferir") }
            }, dismissButton = { TextButton(onClick = { addingTransfer = false }) { Text("Cancelar") } })
    }
    deleteTransfer?.let { t ->
        AlertDialog(onDismissRequest = { deleteTransfer = null }, title = { Text("Excluir transferência?") },
            text = { Text("O dinheiro voltará para a conta de origem. Não será criada receita nem despesa.") },
            confirmButton = { TextButton(onClick = { onDeleteTransfer(t.id); deleteTransfer = null }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { deleteTransfer = null }) { Text("Cancelar") } })
    }
}

@Composable
fun WalletAccountPicker(label: String, accounts: List<WalletAccount>, selectedId: Long, onSelected: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(accounts.firstOrNull { it.id == selectedId }?.name ?: "Selecionar", Modifier.weight(1f))
                Icon(Icons.Outlined.ArrowDropDown, null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                accounts.forEach { a -> DropdownMenuItem(text = { Text(a.name) }, onClick = { onSelected(a.id); expanded = false }) }
            }
        }
    }
}
