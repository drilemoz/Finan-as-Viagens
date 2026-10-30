package br.com.adriedson.financasdaviagem.ui

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
import br.com.adriedson.financasdaviagem.data.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow
import br.com.adriedson.financasdaviagem.data.Preferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun historyTodayStart(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

@Composable
fun HistoryScreen(
    refreshToken: List<Entry>,
    loadPage: suspend (String?, Long, Long?, Long?, Int) -> FinanceRepository.HistoryPage,
    onEdit: (Entry) -> Unit
) {
    val scope = rememberCoroutineScope()
    var type by remember { mutableStateOf("Todos") }
    var period by remember { mutableStateOf("Tudo") }
    val state = rememberLazyListState()
    var rows by remember { mutableStateOf<List<Entry>>(emptyList()) }
    var hasMore by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // The start is recalculated when filters or source data change.
    val start = remember(period, refreshToken) {
        when (period) {
            "Hoje" -> historyTodayStart()
            "7 dias" -> System.currentTimeMillis() - 7L * 86_400_000L
            "30 dias" -> System.currentTimeMillis() - 30L * 86_400_000L
            else -> 0L
        }
    }
    val kind = when (type) { "Despesas" -> "expense"; "Receitas" -> "income"; else -> null }
    // UI state is owned by this screen; SQL work runs via the injected IO loader.
    suspend fun appendPage() {
        if (loading) return
        loading = true
        error = null
        try {
            val last = rows.lastOrNull()
            val page = loadPage(kind, start, last?.createdAt, last?.id, 60)
            rows = rows + page.entries
            hasMore = page.hasMore
        } catch (e: Exception) {
            error = e.message ?: "Erro ao consultar histórico"
        } finally { loading = false }
    }
    // Invalidate the paging cursor after a new entry, deletion, or filter change.
    LaunchedEffect(type, period, refreshToken) {
        rows = emptyList()
        hasMore = true
        loading = false
        error = null
        state.scrollToItem(0)
        appendPage()
    }
    // Fetch another page only when the last visible item approaches the end.
    LaunchedEffect(type, period, refreshToken) {
        snapshotFlow { (state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) to rows.size }
            .distinctUntilChanged().collect { (lastVisible, count) ->
                if (count > 0 && hasMore && !loading && lastVisible >= count - 3) appendPage()
            }
    }
    LazyColumn(Modifier.fillMaxSize(), state = state,
        contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(key="title", contentType="header") {
            Text("Histórico", fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
        }
        item(key="explain", contentType="header") {
            Text("Toque em um lançamento para editar ou excluir.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item(key="type", contentType="filters") {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("Todos", "Despesas", "Receitas").forEach {
                    FilterChip(selected = type == it, onClick = { type = it }, label = { Text(it) })
                }
            }
        }
        item(key="period", contentType="filters") {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("Tudo", "Hoje", "7 dias", "30 dias").forEach {
                    FilterChip(selected = period == it, onClick = { period = it }, label = { Text(it) })
                }
            }
        }
        if (rows.isEmpty() && !loading && error == null) item(key="empty", contentType="message") {
            EmptyState("Nenhum lançamento encontrado", "Experimente outro filtro ou adicione uma movimentação.",
                Icons.Outlined.ReceiptLong)
        }
        items(rows, key = { "entry-${it.id}" }, contentType = { "entry" }) { e -> EntryRow(e, onClick = { onEdit(e) }) }
        if (loading) item(key="loading", contentType="message") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator(Modifier.size(28.dp)) }
        }
        if (error != null) item(key="error", contentType="message") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Falha ao carregar: $error", color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { scope.launch { appendPage() } }) { Text("Tentar novamente") }
            }
        }
        if (hasMore && !loading) item(key="more", contentType="message") {
            // The scroll trigger normally loads pages automatically; a retry button
            // remains for accessibility and devices that don't emit layout updates.
            Text("Role para carregar mais lançamentos", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
