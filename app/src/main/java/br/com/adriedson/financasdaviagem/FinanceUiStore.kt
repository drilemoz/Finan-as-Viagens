package br.com.adriedson.financasdaviagem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import br.com.adriedson.financasdaviagem.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Read-model for each screen. Keep heavy SQLite queries off the Compose/UI thread.
 * Tickets are per area so a late full refresh cannot erase a newer targeted refresh.
 * Call reload on the main dispatcher, where Compose state is updated.
 */
internal enum class FinanceArea { ENTRIES, SETTINGS, RECYCLING, ACCOUNTS, TRANSFERS, BILLS }

internal class FinanceUiStore(private val repo: FinanceRepository) {
    var entries by mutableStateOf<List<Entry>>(emptyList()); private set
    var settings by mutableStateOf(repo.settings())
    var recycling by mutableStateOf<List<RecyclingEvent>>(emptyList()); private set
    var accounts by mutableStateOf<List<WalletAccount>>(emptyList()); private set
    var transfers by mutableStateOf<List<WalletTransfer>>(emptyList()); private set
    var bills by mutableStateOf<List<Bill>>(emptyList()); private set
    private val generations = LongArray(FinanceArea.entries.size)

    /** Invalidates any earlier settings read before publishing freshly committed preferences. */
    fun applySettings(next: Preferences) {
        ++generations[FinanceArea.SETTINGS.ordinal]
        settings = next
    }

    suspend fun reload(vararg areas: FinanceArea) {
        val requested = if (areas.isEmpty()) FinanceArea.entries.toSet() else areas.toSet()
        val issued = requested.associateWith { area -> ++generations[area.ordinal] }
        val loaded = withContext(Dispatchers.IO) {
            FinanceReadBatch(
                entries = if (FinanceArea.ENTRIES in requested) repo.all() else null,
                settings = if (FinanceArea.SETTINGS in requested) repo.settings() else null,
                recycling = if (FinanceArea.RECYCLING in requested) repo.recyclingEvents() else null,
                accounts = if (FinanceArea.ACCOUNTS in requested) repo.accounts() else null,
                transfers = if (FinanceArea.TRANSFERS in requested) repo.transfers() else null,
                bills = if (FinanceArea.BILLS in requested) repo.bills() else null
            )
        }
        fun current(area: FinanceArea) = issued[area] == generations[area.ordinal]
        if (current(FinanceArea.ENTRIES)) loaded.entries?.let { entries = it }
        if (current(FinanceArea.SETTINGS)) loaded.settings?.let { settings = it }
        if (current(FinanceArea.RECYCLING)) loaded.recycling?.let { recycling = it }
        if (current(FinanceArea.ACCOUNTS)) loaded.accounts?.let { accounts = it }
        if (current(FinanceArea.TRANSFERS)) loaded.transfers?.let { transfers = it }
        if (current(FinanceArea.BILLS)) loaded.bills?.let { bills = it }
    }
}

/** Nullable fields mean "not requested", never "empty the data". */
private data class FinanceReadBatch(
    val entries: List<Entry>? = null,
    val settings: Preferences? = null,
    val recycling: List<RecyclingEvent>? = null,
    val accounts: List<WalletAccount>? = null,
    val transfers: List<WalletTransfer>? = null,
    val bills: List<Bill>? = null
)
