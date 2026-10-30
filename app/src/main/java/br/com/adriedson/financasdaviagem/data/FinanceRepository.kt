package br.com.adriedson.financasdaviagem.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import br.com.adriedson.financasdaviagem.core.BillMath
import br.com.adriedson.financasdaviagem.core.FinanceMath
import br.com.adriedson.financasdaviagem.core.RecyclingMath
import org.json.JSONArray
import org.json.JSONObject

/** Não usa internet; persistência local privada, sem permissões de armazenamento. */
class FinanceRepository(context: Context, databaseName: String = "financas_viagem.db") {
    private val db = LedgerDb(context.applicationContext, databaseName)
    /** Configurações migradas para a mesma base SQLite dos lançamentos.
     * Uma restauração usa uma única transação para registros e preferências.
     */
    fun settings(): Preferences = db.readableDatabase.rawQuery(
        """SELECT initial_cents,reserve_cents,daily_budget_cents,theme_mode,cans_per_kg,
            cans_price_cents_kg,trip_goal_cents,planned_trip_days,budget_alert_percent
            FROM app_settings WHERE id=1""", null
    ).use { c ->
        check(c.moveToFirst()) { "Configurações não encontradas" }
        Preferences(c.getLong(0), c.getLong(1), c.getLong(2), c.getString(3), c.getInt(4),
            c.getLong(5), c.getLong(6), c.getInt(7), c.getInt(8))
    }

    fun saveSettings(s: Preferences) {
        require(s.initialCents >= 0 && s.reserveCents >= 0 && s.dailyBudgetCents >= 0)
        require(s.themeMode in listOf("system", "dark", "light"))
        require(s.cansPerKg in 1..1000 && s.cansPriceCentsKg in 1..100_000_000)
        require(s.dailyBudgetCents <= 999_999_999_999L && s.tripGoalCents in 0..999_999_999_999L)
        require(s.plannedTripDays in 0..3650 && s.budgetAlertPercent in 50..100)
        val values = ContentValues().apply {
            put("initial_cents", s.initialCents); put("reserve_cents", s.reserveCents)
            put("daily_budget_cents", s.dailyBudgetCents); put("theme_mode", s.themeMode)
            put("cans_per_kg", s.cansPerKg); put("cans_price_cents_kg", s.cansPriceCentsKg)
            put("trip_goal_cents", s.tripGoalCents); put("planned_trip_days", s.plannedTripDays)
            put("budget_alert_percent", s.budgetAlertPercent)
        }
        check(db.writableDatabase.update("app_settings", values, "id=1", null) == 1)
    }

    data class HistoryPage(val entries: List<Entry>, val hasMore: Boolean)

    /** Paginação estável por (created_at, id) em ordem decrescente.
     * Evita ler todo o histórico para aplicar filtros de data/tipo.
     */
    fun historyPage(kind: String?, fromMillis: Long, beforeTime: Long?, beforeId: Long?, limit: Int = 60): HistoryPage {
        require(kind == null || kind in listOf("expense", "income"))
        require(limit in 1..200 && fromMillis >= 0)
        require((beforeTime == null) == (beforeId == null))
        val where = mutableListOf("created_at>=?")
        val args = mutableListOf(fromMillis.toString())
        if (kind != null) { where += "kind=?"; args += kind }
        if (beforeTime != null && beforeId != null) {
            where += "(created_at<? OR (created_at=? AND id<?))"
            args += beforeTime.toString(); args += beforeTime.toString(); args += beforeId.toString()
        }
        args += (limit + 1).toString()
        val result = mutableListOf<Entry>()
        db.readableDatabase.rawQuery("""SELECT id,kind,category,note,city,country,currency,original_minor,
            rate_brl,brl_cents,created_at,item_kind,quantity,unit_minor,account_id
            FROM entries WHERE ${where.joinToString(" AND ")}
            ORDER BY created_at DESC,id DESC LIMIT ?""", args.toTypedArray()).use { c ->
            while (c.moveToNext()) result.add(Entry(c.getLong(0), c.getString(1), c.getString(2),
                c.getString(3), c.getString(4), c.getString(5), c.getString(6), c.getLong(7),
                c.getString(8), c.getLong(9), c.getLong(10),
                if (c.isNull(11)) null else c.getString(11),
                if (c.isNull(12)) null else c.getInt(12),
                if (c.isNull(13)) null else c.getLong(13), c.getLong(14)))
        }
        return HistoryPage(result.take(limit), result.size > limit)
    }

    fun all(): List<Entry> {
        val output = mutableListOf<Entry>()
        db.readableDatabase.rawQuery("SELECT id,kind,category,note,city,country,currency,original_minor,rate_brl,brl_cents,created_at,item_kind,quantity,unit_minor,account_id FROM entries ORDER BY created_at DESC,id DESC", null).use { c ->
            while (c.moveToNext()) output.add(Entry(
                c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5),
                c.getString(6), c.getLong(7), c.getString(8), c.getLong(9), c.getLong(10),
                if (c.isNull(11)) null else c.getString(11),
                if (c.isNull(12)) null else c.getInt(12),
                if (c.isNull(13)) null else c.getLong(13), c.getLong(14)
            ))
        }
        return output
    }

    private fun verify(e: Entry) {
        require(e.kind in listOf("expense", "income"))
        require(e.accountId > 0)
        require(e.category.length in 1..50 && e.note.length <= 300 && e.city.length <= 100 && e.country.length <= 100)
        require(e.currency in FinanceMath.supportedCurrencies)
        require(e.originalMinor > 0 && e.brlCents > 0 && e.originalMinor <= 999_999_999_999L && e.brlCents <= 999_999_999_999L)
        require(e.createdAt > 0 && e.createdAt <= System.currentTimeMillis() + 86_400_000L)
        require(FinanceMath.convertToBrl(e.originalMinor, e.currency, e.rateBrl) == e.brlCents)
        if (e.itemKind == null) {
            require(e.quantity == null && e.unitMinor == null)
        } else {
            require(e.itemKind in listOf("cans", "units"))
            require(e.quantity != null && e.unitMinor != null)
            require(FinanceMath.quantityTotal(e.unitMinor, e.quantity) == e.originalMinor)
        }
    }

    private fun values(e: Entry) = ContentValues().apply {
        put("kind", e.kind); put("category", e.category); put("note", e.note)
        put("city", e.city); put("country", e.country); put("currency", e.currency)
        put("original_minor", e.originalMinor); put("rate_brl", e.rateBrl)
        put("brl_cents", e.brlCents); put("created_at", e.createdAt)
        put("item_kind", e.itemKind); put("quantity", e.quantity); put("unit_minor", e.unitMinor); put("account_id", e.accountId)
    }

    private fun isLinkedEntry(writable: SQLiteDatabase, id: Long): Boolean =
        writable.rawQuery("SELECT 1 FROM recycling_events WHERE finance_entry_id=? LIMIT 1", arrayOf(id.toString()))
            .use { it.moveToFirst() }

    private fun isBillEntry(writable: SQLiteDatabase, id: Long): Boolean =
        writable.rawQuery("SELECT 1 FROM bills WHERE paid_entry_id=? LIMIT 1", arrayOf(id.toString()))
            .use { it.moveToFirst() }

    fun isManagedEntry(id: Long): Boolean {
        val read = db.readableDatabase
        return isLinkedEntry(read, id) || isBillEntry(read, id)
    }

    fun save(e: Entry) {
        verify(e)
        require(accounts().any { it.id == e.accountId }) { "Conta inexistente" }
        if (e.id > 0) require(!isManagedEntry(e.id)) {
            "Lançamento vinculado: gerencie-o na aba Recicláveis ou em Contas a pagar"
        }
        if (e.id <= 0) {
            check(db.writableDatabase.insertOrThrow("entries", null, values(e)) > 0)
        } else {
            check(db.writableDatabase.update("entries", values(e), "id=?", arrayOf(e.id.toString())) == 1)
        }
    }

    fun delete(id: Long): Int {
        val writable = db.writableDatabase
        require(!isManagedEntry(id)) { "Lançamento vinculado: gerencie pela origem" }
        return writable.delete("entries", "id=?", arrayOf(id.toString()))
    }

    fun isRecyclingEntry(id: Long): Boolean = isLinkedEntry(db.readableDatabase, id)

    fun accounts(): List<WalletAccount> {
        val list = mutableListOf<WalletAccount>()
        db.readableDatabase.rawQuery("SELECT id,name,opening_cents,is_reserve FROM accounts ORDER BY id", null).use { c ->
            while (c.moveToNext()) list.add(WalletAccount(c.getLong(0), c.getString(1), c.getLong(2), c.getInt(3) == 1))
        }
        return list
    }

    fun transfers(): List<WalletTransfer> {
        val list = mutableListOf<WalletTransfer>()
        db.readableDatabase.rawQuery("SELECT id,from_id,to_id,cents,note,created_at FROM wallet_transfers ORDER BY created_at DESC,id DESC", null).use { c ->
            while (c.moveToNext()) list.add(WalletTransfer(c.getLong(0), c.getLong(1), c.getLong(2), c.getLong(3), c.getString(4), c.getLong(5)))
        }
        return list
    }

    private fun validateAccount(a: WalletAccount) {
        require(a.name.trim() == a.name && a.name.length in 2..50 && !a.name.any { it.code < 32 || it.code == 127 })
        require(a.openingCents in -999_999_999_999L..999_999_999_999L)
    }

    fun saveAccount(account: WalletAccount) {
        validateAccount(account)
        require(accounts().none { it.id != account.id && it.name.equals(account.name, ignoreCase = true) }) {
            "Já existe uma conta com esse nome"
        }
        val w = db.writableDatabase
        val v = ContentValues().apply {
            put("name", account.name); put("opening_cents", account.openingCents)
            put("is_reserve", (if (account.isReserve) 1 else 0).toInt())
        }
        if (account.id == 0L) check(w.insertOrThrow("accounts", null, v) > 0)
        else check(w.update("accounts", v, "id=?", arrayOf(account.id.toString())) == 1)
    }

    fun addTransfer(t: WalletTransfer) {
        require(t.id == 0L && t.cents in 1..999_999_999_999L && t.fromId != t.toId)
        require(t.note.length <= 200 && t.createdAt > 0 && t.createdAt <= System.currentTimeMillis() + 86_400_000L)
        val w = db.writableDatabase
        w.beginTransaction()
        try {
            val ac = accounts()
            require(ac.any { it.id == t.fromId } && ac.any { it.id == t.toId }) { "Conta inexistente" }
            require(WalletBalanceQueries.balance(w, t.fromId) >= t.cents) { "Saldo insuficiente na origem" }
            val v = ContentValues().apply {
                put("from_id", t.fromId); put("to_id", t.toId); put("cents", t.cents)
                put("note", t.note); put("created_at", t.createdAt)
            }
            check(w.insertOrThrow("wallet_transfers", null, v) > 0)
            w.setTransactionSuccessful()
        } finally { w.endTransaction() }
    }

    fun deleteTransfer(id: Long) {
        val w = db.writableDatabase
        w.beginTransaction()
        try {
            val existing = w.rawQuery("SELECT to_id,cents FROM wallet_transfers WHERE id=?", arrayOf(id.toString()))
                .use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) to cursor.getLong(1) else error("Transferência inexistente") }
            require(WalletBalanceQueries.balance(w, existing.first) >= existing.second) {
                "Saldo insuficiente no destino para desfazer a transferência"
            }
            check(w.delete("wallet_transfers", "id=?", arrayOf(id.toString())) == 1)
            w.setTransactionSuccessful()
        } finally { w.endTransaction() }
    }

    private fun validateBill(b: Bill) {
        require(b.title.trim() == b.title && b.title.length in 2..100 &&
            b.title.none { it.code < 32 || it.code == 127 })
        require(b.category in expenseCategories && b.note.length <= 180)
        require(b.amountCents in 1..999_999_999_999L)
        require(BillMath.isValidIso(b.dueDate)) { "Vencimento inválido" }
        require(b.createdAt > 0 && b.createdAt <= System.currentTimeMillis() + 86_400_000L)
    }

    private fun billValues(b: Bill) = ContentValues().apply {
        put("title", b.title); put("category", b.category); put("amount_cents", b.amountCents)
        put("due_date", b.dueDate); put("note", b.note)
        put("paid_entry_id", b.paidEntryId); put("created_at", b.createdAt)
    }

    fun bills(): List<Bill> {
        val list = mutableListOf<Bill>()
        db.readableDatabase.rawQuery("""SELECT id,title,category,amount_cents,due_date,note,paid_entry_id,created_at
            FROM bills ORDER BY (paid_entry_id IS NOT NULL),due_date ASC,id DESC""", null).use { c ->
            while (c.moveToNext()) list.add(Bill(c.getLong(0), c.getString(1), c.getString(2),
                c.getLong(3), c.getString(4), c.getString(5),
                if (c.isNull(6)) null else c.getLong(6), c.getLong(7)))
        }
        return list
    }

    fun saveBill(b: Bill) {
        validateBill(b)
        require(b.paidEntryId == null) { "Contas pagas não podem ser editadas" }
        val w = db.writableDatabase
        w.beginTransaction()
        try {
            if (b.id == 0L) check(w.insertOrThrow("bills", null, billValues(b)) > 0)
            else {
                val existing = bills().singleOrNull { it.id == b.id } ?: error("Conta inexistente")
                require(existing.paidEntryId == null) { "Conta já paga" }
                check(w.update("bills", billValues(b.copy(createdAt = existing.createdAt)), "id=?", arrayOf(b.id.toString())) == 1)
            }
            w.setTransactionSuccessful()
        } finally { w.endTransaction() }
    }

    fun deleteBill(id: Long) {
        val w = db.writableDatabase
        w.beginTransaction()
        try {
            val b = bills().singleOrNull { it.id == id } ?: error("Conta inexistente")
            require(b.paidEntryId == null) { "Contas pagas não podem ser excluídas: a despesa já foi registrada" }
            check(w.delete("bills", "id=?", arrayOf(id.toString())) == 1)
            w.setTransactionSuccessful()
        } finally { w.endTransaction() }
    }

    /** Atomic payment. No entry is produced while the bill is only pending. */
    fun payBill(id: Long, accountId: Long) {
        val w = db.writableDatabase
        w.beginTransaction()
        try {
            val bill = bills().singleOrNull { it.id == id } ?: error("Conta inexistente")
            require(bill.paidEntryId == null) { "Esta conta já foi paga" }
            require(accounts().any { it.id == accountId }) { "Carteira não encontrada" }
            val now = System.currentTimeMillis()
            val e = Entry(kind = "expense", category = bill.category,
                note = "Conta: ${bill.title}" + if (bill.note.isBlank()) "" else " · ${bill.note}",
                originalMinor = bill.amountCents, brlCents = bill.amountCents,
                createdAt = now, accountId = accountId)
            verify(e)
            val entryId = w.insertOrThrow("entries", null, values(e))
            check(entryId > 0L)
            val update = ContentValues().apply { put("paid_entry_id", entryId) }
            check(w.update("bills", update, "id=? AND paid_entry_id IS NULL", arrayOf(id.toString())) == 1)
            w.setTransactionSuccessful()
        } finally { w.endTransaction() }
    }

    fun recyclingEvents(): List<RecyclingEvent> = readRecycling(db.readableDatabase)

    private fun readRecycling(database: SQLiteDatabase): List<RecyclingEvent> {
        val result = mutableListOf<RecyclingEvent>()
        database.rawQuery("""SELECT id,material,action,units,measured_grams,estimated_weight,price_cents_kg,
            received_cents,finance_entry_id,note,city,country,created_at
            FROM recycling_events ORDER BY created_at ASC,id ASC""", null).use { c ->
            while (c.moveToNext()) result.add(RecyclingEvent(
                id = c.getLong(0), material = c.getString(1), action = c.getString(2), units = c.getLong(3),
                measuredGrams = if (c.isNull(4)) null else c.getLong(4),
                estimatedWeight = c.getInt(5) == 1,
                priceCentsKg = if (c.isNull(6)) null else c.getLong(6),
                receivedCents = if (c.isNull(7)) null else c.getLong(7),
                financeEntryId = if (c.isNull(8)) null else c.getLong(8),
                note = c.getString(9), city = c.getString(10), country = c.getString(11), createdAt = c.getLong(12)
            ))
        }
        return result
    }

    /** Returns live inventory. Strict chronological replay prevents rewriting a sale before its stock existed. */
    private fun stock(events: List<RecyclingEvent>): Map<String, Long> {
        val totals = mutableMapOf("cans" to 0L, "copper" to 0L)
        events.sortedWith(compareBy<RecyclingEvent> { it.createdAt }.thenBy { it.id }).forEach { event ->
            val balance = Math.addExact(totals.getValue(event.material), event.units)
            require(balance >= 0) { "Estoque insuficiente em ${event.material}" }
            totals[event.material] = balance
        }
        return totals
    }

    private fun validateRecycle(event: RecyclingEvent, allowLinked: Boolean = false) {
        require(RecyclingMath.validAction(event.action) && RecyclingMath.validMaterial(event.material))
        require(event.units != 0L && event.units in -1_000_000_000L..1_000_000_000L)
        require(event.createdAt > 0 && event.createdAt <= System.currentTimeMillis() + 86_400_000L)
        require(event.note.length <= 300 && event.city.length <= 100 && event.country.length <= 100)
        when (event.action) {
            "collect" -> require(event.units > 0 && !event.estimatedWeight && event.measuredGrams == null && event.priceCentsKg == null && event.receivedCents == null && event.financeEntryId == null)
            "adjust" -> require(event.note.isNotBlank() && !event.estimatedWeight && event.measuredGrams == null && event.priceCentsKg == null && event.receivedCents == null && event.financeEntryId == null)
            "sale" -> {
                require(event.units < 0 && event.priceCentsKg != null && event.receivedCents != null)
                if (allowLinked) require(event.financeEntryId != null && event.financeEntryId > 0)
                else require(event.financeEntryId == null)
                val weight = if (event.material == "copper") -event.units else event.measuredGrams
                require(weight != null && weight > 0)
                require(event.material == "cans" || event.measuredGrams == null)
                require(event.material != "cans" || event.measuredGrams != null)
                require(!event.estimatedWeight || event.material == "cans")
                require(RecyclingMath.totalCents(weight, event.priceCentsKg) == event.receivedCents)
            }
        }
    }

    private fun eventValues(e: RecyclingEvent) = ContentValues().apply {
        put("material", e.material); put("action", e.action); put("units", e.units)
        put("measured_grams", e.measuredGrams); put("estimated_weight", (if (e.estimatedWeight) 1 else 0).toInt()); put("price_cents_kg", e.priceCentsKg)
        put("received_cents", e.receivedCents); put("finance_entry_id", e.financeEntryId)
        put("note", e.note); put("city", e.city); put("country", e.country); put("created_at", e.createdAt)
    }

    /** Collection/adjustment affects stock; a completed sale affects stock and income atomically. */
    fun addRecyclingEvent(event: RecyclingEvent, accountId: Long = 1L) {
        require(event.id == 0L)
        validateRecycle(event)
        require(accounts().any { it.id == accountId }) { "Conta de recebimento inexistente" }
        val writable = db.writableDatabase
        writable.beginTransaction()
        try {
            val events = readRecycling(writable)
            stock(events + event.copy(id = (events.maxOfOrNull { it.id } ?: 0) + 1))
            val linked = if (event.action == "sale") {
                val note = "Reciclagem · ${if (event.material == "cans") "Latinhas" else "Cobre"}" +
                    if (event.note.isBlank()) "" else " · ${event.note}"
                val amount = requireNotNull(event.receivedCents)
                val entry = Entry(kind = "income", category = "Trabalho", note = note,
                    city = event.city, country = event.country, originalMinor = amount,
                    brlCents = amount, createdAt = event.createdAt, accountId = accountId)
                writable.insertOrThrow("entries", null, values(entry))
            } else null
            check(writable.insertOrThrow("recycling_events", null,
                eventValues(event.copy(financeEntryId = linked))) > 0)
            writable.setTransactionSuccessful()
        } finally { writable.endTransaction() }
    }

    /** Removing a collection is rejected when it would make a later sale impossible. */
    fun deleteRecyclingEvent(id: Long) {
        val writable = db.writableDatabase
        writable.beginTransaction()
        try {
            val events = readRecycling(writable)
            val selected = events.singleOrNull { it.id == id } ?: error("Registro inexistente")
            stock(events.filterNot { it.id == id })
            check(writable.delete("recycling_events", "id=?", arrayOf(id.toString())) == 1)
            selected.financeEntryId?.let { check(writable.delete("entries", "id=?", arrayOf(it.toString())) == 1) }
            writable.setTransactionSuccessful()
        } finally { writable.endTransaction() }
    }

    fun createBackup(): String {
        val root = JSONObject().put("schemaVersion", 6).put("createdAt", System.currentTimeMillis())
        val s = settings()
        root.put("settings", JSONObject().put("initialCents", s.initialCents).put("reserveCents", s.reserveCents)
            .put("dailyBudgetCents", s.dailyBudgetCents).put("themeMode", s.themeMode)
            .put("cansPerKg", s.cansPerKg).put("cansPriceCentsKg", s.cansPriceCentsKg)
            .put("tripGoalCents", s.tripGoalCents).put("plannedTripDays", s.plannedTripDays)
            .put("budgetAlertPercent", s.budgetAlertPercent))
        val data = JSONArray()
        all().forEach { e ->
            data.put(JSONObject().put("id", e.id).put("kind", e.kind).put("category", e.category)
                .put("note", e.note).put("city", e.city).put("country", e.country)
                .put("currency", e.currency).put("originalMinor", e.originalMinor)
                .put("rateBrl", e.rateBrl).put("brlCents", e.brlCents).put("createdAt", e.createdAt)
                .put("itemKind", e.itemKind).put("quantity", e.quantity).put("unitMinor", e.unitMinor)
                .put("accountId", e.accountId))
        }
        val recycling = JSONArray()
        recyclingEvents().forEach { e ->
            recycling.put(JSONObject().put("id", e.id).put("material", e.material).put("action", e.action)
                .put("units", e.units).put("measuredGrams", e.measuredGrams)
                .put("estimatedWeight", e.estimatedWeight)
                .put("priceCentsKg", e.priceCentsKg).put("receivedCents", e.receivedCents)
                .put("financeEntryId", e.financeEntryId).put("note", e.note).put("city", e.city)
                .put("country", e.country).put("createdAt", e.createdAt))
        }
        val accountsData = JSONArray()
        accounts().forEach { a -> accountsData.put(JSONObject().put("id", a.id).put("name", a.name)
            .put("openingCents", a.openingCents).put("isReserve", a.isReserve)) }
        val transfersData = JSONArray()
        transfers().forEach { t -> transfersData.put(JSONObject().put("id", t.id).put("fromId", t.fromId)
            .put("toId", t.toId).put("cents", t.cents).put("note", t.note).put("createdAt", t.createdAt)) }
        val billsData = JSONArray()
        bills().forEach { b -> billsData.put(JSONObject().put("id", b.id).put("title", b.title)
            .put("category", b.category).put("amountCents", b.amountCents).put("dueDate", b.dueDate)
            .put("note", b.note).put("paidEntryId", b.paidEntryId).put("createdAt", b.createdAt)) }
        return root.put("entries", data).put("recyclingEvents", recycling)
            .put("accounts", accountsData).put("transfers", transfersData)
            .put("bills", billsData).toString(2)
    }

    data class Backup(
        val settings: Preferences, val entries: List<Entry>, val recyclingEvents: List<RecyclingEvent>,
        val accounts: List<WalletAccount>, val transfers: List<WalletTransfer>,
        val bills: List<Bill> = emptyList()
    )

    fun parseBackup(content: String): Backup {
        require(content.length <= 4_000_000) { "Arquivo muito grande" }
        val root = JSONObject(content)
        val schema = root.getInt("schemaVersion")
        require(schema in 1..6) { "Versão de backup incompatível" }
        val pref = root.getJSONObject("settings")
        val settings = Preferences(pref.getLong("initialCents"), pref.getLong("reserveCents"),
            pref.getLong("dailyBudgetCents"), pref.getString("themeMode"),
            if (schema >= 3) pref.getInt("cansPerKg") else 70,
            if (schema >= 3) pref.getLong("cansPriceCentsKg") else 600,
            if (schema >= 6) pref.getLong("tripGoalCents") else 0,
            if (schema >= 6) pref.getInt("plannedTripDays") else 0,
            if (schema >= 6) pref.getInt("budgetAlertPercent") else 80)
        require(settings.initialCents >= 0 && settings.reserveCents >= 0 && settings.dailyBudgetCents >= 0)
        require(settings.themeMode in listOf("system", "light", "dark"))
        require(settings.cansPerKg in 1..1000 && settings.cansPriceCentsKg in 1..100_000_000)
        require(settings.tripGoalCents in 0..999_999_999_999L && settings.plannedTripDays in 0..3650)
        require(settings.dailyBudgetCents <= 999_999_999_999L && settings.budgetAlertPercent in 50..100)
        val arr = root.getJSONArray("entries")
        require(arr.length() <= 10000) { "Backup contém lançamentos demais" }
        val items = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Entry(o.getLong("id"), o.getString("kind"), o.getString("category"),
                o.getString("note"), o.getString("city"), o.getString("country"),
                o.getString("currency"), o.getLong("originalMinor"), o.getString("rateBrl"),
                o.getLong("brlCents"), o.getLong("createdAt"),
                if (schema >= 2 && !o.isNull("itemKind")) o.getString("itemKind") else null,
                if (schema >= 2 && !o.isNull("quantity")) o.getInt("quantity") else null,
                if (schema >= 2 && !o.isNull("unitMinor")) o.getLong("unitMinor") else null,
                if (schema >= 4) o.getLong("accountId") else 1L)
        }
        items.forEach { verify(it); require(it.id > 0) }
        require(items.map { it.id }.distinct().size == items.size) { "IDs duplicados" }
        val recycling = if (schema < 3) emptyList() else {
            val data = root.getJSONArray("recyclingEvents")
            require(data.length() <= 10000) { "Eventos de reciclagem demais" }
            (0 until data.length()).map { i ->
                val o = data.getJSONObject(i)
                fun nullableLong(key: String): Long? = if (o.isNull(key)) null else o.getLong(key)
                RecyclingEvent(o.getLong("id"), o.getString("material"), o.getString("action"),
                    o.getLong("units"), nullableLong("measuredGrams"), o.getBoolean("estimatedWeight"), nullableLong("priceCentsKg"),
                    nullableLong("receivedCents"), nullableLong("financeEntryId"), o.getString("note"),
                    o.getString("city"), o.getString("country"), o.getLong("createdAt"))
            }
        }
        recycling.forEach { validateRecycle(it, allowLinked = trueIfSale(it)) ; require(it.id > 0) }
        require(recycling.map { it.id }.distinct().size == recycling.size)
        require(recycling.mapNotNull { it.financeEntryId }.distinct().size == recycling.count { it.financeEntryId != null })
        val mapped = items.associateBy { it.id }
        recycling.filter { it.action == "sale" }.forEach { sale ->
            val entry = mapped[sale.financeEntryId] ?: error("Receita de venda ausente no backup")
            require(entry.kind == "income" && entry.category == "Trabalho" && entry.currency == "BRL"
                && entry.brlCents == sale.receivedCents && entry.originalMinor == sale.receivedCents
                && entry.createdAt == sale.createdAt)
        }
        stock(recycling)
        val ac = if (schema >= 4) {
            val array = root.getJSONArray("accounts")
            require(array.length() in 1..100)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                WalletAccount(o.getLong("id"), o.getString("name"), o.getLong("openingCents"), o.getBoolean("isReserve"))
            }
        } else listOf(
            WalletAccount(1, "Dinheiro físico", Math.subtractExact(settings.initialCents, settings.reserveCents)),
            WalletAccount(2, "Reserva de emergência", settings.reserveCents, true),
            WalletAccount(3, "Banco / Pix")
        )
        val tx = if (schema >= 4) {
            val array = root.getJSONArray("transfers")
            require(array.length() <= 10000)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                WalletTransfer(o.getLong("id"), o.getLong("fromId"), o.getLong("toId"),
                    o.getLong("cents"), o.getString("note"), o.getLong("createdAt"))
            }
        } else emptyList()
        val bills = if (schema >= 5) {
            val array = root.getJSONArray("bills")
            require(array.length() <= 10000) { "Contas demais" }
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Bill(o.getLong("id"), o.getString("title"), o.getString("category"),
                    o.getLong("amountCents"), o.getString("dueDate"), o.getString("note"),
                    if (o.isNull("paidEntryId")) null else o.getLong("paidEntryId"), o.getLong("createdAt"))
            }
        } else emptyList()
        validateWalletData(ac, items, tx)
        validateBills(bills, items, recycling)
        return Backup(settings, items, recycling, ac, tx, bills)
    }

    private fun trueIfSale(event: RecyclingEvent): Boolean = event.action == "sale"

    private fun validateWalletData(accounts: List<WalletAccount>, entries: List<Entry>, transfers: List<WalletTransfer>) {
        accounts.forEach { validateAccount(it); require(it.id > 0) }
        require(accounts.map { it.id }.distinct().size == accounts.size)
        require(accounts.map { it.name.lowercase(java.util.Locale.ROOT) }.distinct().size == accounts.size)
        val ids = accounts.map { it.id }.toSet()
        entries.forEach { require(it.accountId in ids) { "Lançamento referencia conta inexistente" } }
        transfers.forEach { t ->
            require(t.id > 0 && t.fromId != t.toId && t.fromId in ids && t.toId in ids)
            require(t.cents in 1..999_999_999_999L && t.note.length <= 200)
            require(t.createdAt > 0 && t.createdAt <= System.currentTimeMillis() + 86_400_000L)
        }
        require(transfers.map { it.id }.distinct().size == transfers.size)
        br.com.adriedson.financasdaviagem.core.WalletMath.balances(accounts, entries, transfers)
    }

    private fun validateBills(bills: List<Bill>, entries: List<Entry>, recycling: List<RecyclingEvent>) {
        bills.forEach { validateBill(it); require(it.id > 0) }
        require(bills.map { it.id }.distinct().size == bills.size) { "Contas duplicadas" }
        val linked = bills.mapNotNull { it.paidEntryId }
        require(linked.distinct().size == linked.size) { "Despesa vinculada mais de uma vez" }
        require(linked.none { id -> recycling.any { it.financeEntryId == id } })
        val byId = entries.associateBy { it.id }
        bills.filter { it.paidEntryId != null }.forEach { b ->
            val e = byId[b.paidEntryId] ?: error("Despesa da conta paga ausente no backup")
            require(e.kind == "expense" && e.category == b.category && e.currency == "BRL" &&
                e.brlCents == b.amountCents && e.originalMinor == b.amountCents)
        }
    }

    fun restore(backup: Backup) {
        backup.entries.forEach { verify(it) }
        backup.recyclingEvents.forEach { validateRecycle(it, allowLinked = trueIfSale(it)) }
        stock(backup.recyclingEvents)
        validateWalletData(backup.accounts, backup.entries, backup.transfers)
        validateBills(backup.bills, backup.entries, backup.recyclingEvents)
        val writable = db.writableDatabase
        writable.beginTransaction()
        try {
            writable.delete("bills", null, null)
            writable.delete("recycling_events", null, null)
            writable.delete("entries", null, null)
            writable.delete("wallet_transfers", null, null)
            writable.delete("accounts", null, null)
            backup.accounts.forEach { a ->
                val v = ContentValues().apply {
                    put("id", a.id); put("name", a.name); put("opening_cents", a.openingCents)
                    put("is_reserve", (if (a.isReserve) 1 else 0).toInt())
                }
                writable.insertOrThrow("accounts", null, v)
            }
            backup.entries.forEach { e ->
                val v = values(e).apply { put("id", e.id) }
                writable.insertOrThrow("entries", null, v)
            }
            backup.transfers.forEach { t ->
                val v = ContentValues().apply {
                    put("id", t.id); put("from_id", t.fromId); put("to_id", t.toId)
                    put("cents", t.cents); put("note", t.note); put("created_at", t.createdAt)
                }
                writable.insertOrThrow("wallet_transfers", null, v)
            }
            backup.recyclingEvents.forEach { e ->
                writable.insertOrThrow("recycling_events", null, eventValues(e).apply { put("id", e.id) })
            }
            backup.bills.forEach { b ->
                writable.insertOrThrow("bills", null, billValues(b).apply { put("id", b.id) })
            }
            // Configurações e dados agora fazem parte da MESMA transação SQLite.
            // Em falha antes do commit, ambos voltam ao estado anterior.
            saveSettings(backup.settings)
            writable.setTransactionSuccessful()
        } finally { writable.endTransaction() }
    }

    fun exportCsv(): String = CsvExports(this).exportCsv()
    fun exportTransfersCsv(): String = CsvExports(this).exportTransfersCsv()
    fun exportRecyclingCsv(): String = CsvExports(this).exportRecyclingCsv()
    fun exportBillsCsv(): String = CsvExports(this).exportBillsCsv()

}

