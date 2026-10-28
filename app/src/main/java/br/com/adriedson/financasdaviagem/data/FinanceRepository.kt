package br.com.adriedson.financasdaviagem.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import br.com.adriedson.financasdaviagem.core.FinanceMath
import org.json.JSONArray
import org.json.JSONObject

data class Entry(
    val id: Long = 0,
    val kind: String = "expense",
    val category: String = "Alimentação",
    val note: String = "",
    val city: String = "",
    val country: String = "",
    val currency: String = "BRL",
    val originalMinor: Long = 0,
    val rateBrl: String = "1",
    val brlCents: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    /** null = valor direto; cans/units = venda ou compra por unidade. */
    val itemKind: String? = null,
    val quantity: Int? = null,
    val unitMinor: Long? = null
)

data class Preferences(
    val initialCents: Long = 0,
    val reserveCents: Long = 0,
    val dailyBudgetCents: Long = 4000,
    val themeMode: String = "system"
)

val expenseCategories = listOf("Alimentação", "Bebidas", "Água", "Hospedagem", "Manutenção", "Saúde", "Transporte", "Equipamentos", "Imprevistos", "Outros")
val incomeCategories = listOf("Economias", "Venda de latinhas", "Trabalho", "Doação", "Reembolso", "Outras receitas")

private class LedgerDb(context: Context) : SQLiteOpenHelper(context, "financas_viagem.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE entries (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            kind TEXT NOT NULL CHECK(kind IN ('expense','income')),
            category TEXT NOT NULL,
            note TEXT NOT NULL DEFAULT '',
            city TEXT NOT NULL DEFAULT '',
            country TEXT NOT NULL DEFAULT '',
            currency TEXT NOT NULL,
            original_minor INTEGER NOT NULL CHECK(original_minor > 0),
            rate_brl TEXT NOT NULL,
            brl_cents INTEGER NOT NULL CHECK(brl_cents > 0),
            created_at INTEGER NOT NULL,
            item_kind TEXT,
            quantity INTEGER,
            unit_minor INTEGER
        )""")
        db.execSQL("CREATE INDEX idx_entries_time ON entries(created_at DESC)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 1 && newVersion == 2) {
            db.execSQL("ALTER TABLE entries ADD COLUMN item_kind TEXT")
            db.execSQL("ALTER TABLE entries ADD COLUMN quantity INTEGER")
            db.execSQL("ALTER TABLE entries ADD COLUMN unit_minor INTEGER")
        } else error("Migração SQLite obrigatória: versão $oldVersion -> $newVersion")
    }
}

/** Não usa internet; persistência local privada, sem permissões de armazenamento. */
class FinanceRepository(context: Context) {
    private val db = LedgerDb(context.applicationContext)
    private val sp = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun settings(): Preferences = Preferences(
        initialCents = sp.getLong("initial", 0),
        reserveCents = sp.getLong("reserve", 0),
        dailyBudgetCents = sp.getLong("budget", 4000),
        themeMode = sp.getString("theme", "system")?.takeIf { it in listOf("system", "dark", "light") } ?: "system"
    )

    fun saveSettings(s: Preferences) {
        require(s.initialCents >= 0 && s.reserveCents >= 0 && s.dailyBudgetCents >= 0)
        require(s.themeMode in listOf("system", "dark", "light"))
        check(sp.edit().putLong("initial", s.initialCents).putLong("reserve", s.reserveCents)
            .putLong("budget", s.dailyBudgetCents).putString("theme", s.themeMode).commit())
    }

    fun all(): List<Entry> {
        val output = mutableListOf<Entry>()
        db.readableDatabase.rawQuery("SELECT id,kind,category,note,city,country,currency,original_minor,rate_brl,brl_cents,created_at,item_kind,quantity,unit_minor FROM entries ORDER BY created_at DESC,id DESC", null).use { c ->
            while (c.moveToNext()) output.add(Entry(
                c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5),
                c.getString(6), c.getLong(7), c.getString(8), c.getLong(9), c.getLong(10),
                if (c.isNull(11)) null else c.getString(11),
                if (c.isNull(12)) null else c.getInt(12),
                if (c.isNull(13)) null else c.getLong(13)
            ))
        }
        return output
    }

    private fun verify(e: Entry) {
        require(e.kind in listOf("expense", "income"))
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
        put("item_kind", e.itemKind); put("quantity", e.quantity); put("unit_minor", e.unitMinor)
    }

    fun save(e: Entry) {
        verify(e)
        if (e.id <= 0) {
            check(db.writableDatabase.insertOrThrow("entries", null, values(e)) > 0)
        } else {
            check(db.writableDatabase.update("entries", values(e), "id=?", arrayOf(e.id.toString())) == 1)
        }
    }

    fun delete(id: Long) = db.writableDatabase.delete("entries", "id=?", arrayOf(id.toString()))

    fun createBackup(): String {
        val root = JSONObject().put("schemaVersion", 2).put("createdAt", System.currentTimeMillis())
        val s = settings()
        root.put("settings", JSONObject().put("initialCents", s.initialCents).put("reserveCents", s.reserveCents)
            .put("dailyBudgetCents", s.dailyBudgetCents).put("themeMode", s.themeMode))
        val data = JSONArray()
        all().forEach { e ->
            data.put(JSONObject().put("id", e.id).put("kind", e.kind).put("category", e.category)
                .put("note", e.note).put("city", e.city).put("country", e.country)
                .put("currency", e.currency).put("originalMinor", e.originalMinor)
                .put("rateBrl", e.rateBrl).put("brlCents", e.brlCents).put("createdAt", e.createdAt)
                .put("itemKind", e.itemKind).put("quantity", e.quantity).put("unitMinor", e.unitMinor))
        }
        return root.put("entries", data).toString(2)
    }

    data class Backup(val settings: Preferences, val entries: List<Entry>)

    fun parseBackup(content: String): Backup {
        require(content.length <= 4_000_000) { "Arquivo muito grande" }
        val root = JSONObject(content)
        val schema = root.getInt("schemaVersion")
        require(schema == 1 || schema == 2) { "Versão de backup incompatível" }
        val pref = root.getJSONObject("settings")
        val settings = Preferences(pref.getLong("initialCents"), pref.getLong("reserveCents"),
            pref.getLong("dailyBudgetCents"), pref.getString("themeMode"))
        require(settings.initialCents >= 0 && settings.reserveCents >= 0 && settings.dailyBudgetCents >= 0)
        require(settings.themeMode in listOf("system", "light", "dark"))
        val arr = root.getJSONArray("entries")
        require(arr.length() <= 10000) { "Backup contém lançamentos demais" }
        val items = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Entry(o.getLong("id"), o.getString("kind"), o.getString("category"),
                o.getString("note"), o.getString("city"), o.getString("country"),
                o.getString("currency"), o.getLong("originalMinor"), o.getString("rateBrl"),
                o.getLong("brlCents"), o.getLong("createdAt"),
                if (schema == 2 && !o.isNull("itemKind")) o.getString("itemKind") else null,
                if (schema == 2 && !o.isNull("quantity")) o.getInt("quantity") else null,
                if (schema == 2 && !o.isNull("unitMinor")) o.getLong("unitMinor") else null)
        }
        items.forEach { verify(it); require(it.id > 0) }
        require(items.map { it.id }.distinct().size == items.size) { "IDs duplicados" }
        return Backup(settings, items)
    }

    fun restore(backup: Backup) {
        backup.entries.forEach { verify(it) }
        val writable = db.writableDatabase
        writable.beginTransaction()
        try {
            writable.delete("entries", null, null)
            backup.entries.forEach { e ->
                val v = values(e).apply { put("id", e.id) }
                writable.insertOrThrow("entries", null, v)
            }
            // O banco é transacional. As preferências usam SharedPreferences e
            // não possuem atomicidade conjunta em caso de interrupção do processo.
            saveSettings(backup.settings)
            writable.setTransactionSuccessful()
        } finally { writable.endTransaction() }
    }

    fun exportCsv(): String {
        fun escaped(x: String): String {
            val safe = if (Regex("^[=+@-]").containsMatchIn(x.trimStart())) "'" + x else x
            return "\"" + safe.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\""
        }
        return buildString {
            append("data_iso;tipo;categoria;valor_original;moeda;taxa_brl;valor_brl;descricao;cidade;pais;tipo_item;quantidade;preco_unitario\r\n")
            all().forEach { e ->
                val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.ROOT).format(java.util.Date(e.createdAt))
                val cols = listOf(date, e.kind, e.category,
                    FinanceMath.decimalInput(e.originalMinor, FinanceMath.digits(e.currency)), e.currency,
                    e.rateBrl, FinanceMath.decimalInput(e.brlCents), e.note, e.city, e.country,
                    e.itemKind ?: "", e.quantity?.toString() ?: "",
                    e.unitMinor?.let { FinanceMath.decimalInput(it, FinanceMath.digits(e.currency)) } ?: "")
                append(cols.joinToString(";") { escaped(it) }); append("\r\n")
            }
        }
    }
}
