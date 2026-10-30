package br.com.adriedson.financasdaviagem.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class LedgerDb(private val appContext: Context, name: String = "financas_viagem.db") : SQLiteOpenHelper(appContext, name, null, 6) {
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
            unit_minor INTEGER,
            account_id INTEGER NOT NULL DEFAULT 1
        )""")
        db.execSQL("CREATE INDEX idx_entries_time ON entries(created_at DESC)")
        createRecycling(db)
        createWalletTables(db)
        createBills(db)
        createSettings(db)
    }
    /** Migra configurações antigas somente uma vez, mantendo saldos existentes. */
    private fun createSettings(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE app_settings (
            id INTEGER PRIMARY KEY CHECK(id=1),
            initial_cents INTEGER NOT NULL, reserve_cents INTEGER NOT NULL,
            daily_budget_cents INTEGER NOT NULL, theme_mode TEXT NOT NULL,
            cans_per_kg INTEGER NOT NULL, cans_price_cents_kg INTEGER NOT NULL,
            trip_goal_cents INTEGER NOT NULL, planned_trip_days INTEGER NOT NULL,
            budget_alert_percent INTEGER NOT NULL
        )""")
        val prefs = appContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val theme = prefs.getString("theme", "system")?.takeIf { it in listOf("system", "dark", "light") } ?: "system"
        db.execSQL("""INSERT INTO app_settings(id,initial_cents,reserve_cents,daily_budget_cents,
            theme_mode,cans_per_kg,cans_price_cents_kg,trip_goal_cents,planned_trip_days,budget_alert_percent)
            VALUES(1,?,?,?,?,?,?,?,?,?)""", arrayOf(
            prefs.getLong("initial", 0L).coerceAtLeast(0), prefs.getLong("reserve", 0L).coerceAtLeast(0),
            prefs.getLong("budget", 4000L).coerceIn(0, 999_999_999_999L), theme,
            prefs.getInt("cansPerKg", 70).takeIf { it in 1..1000 } ?: 70,
            prefs.getLong("cansPriceKg", 600L).takeIf { it in 1..100_000_000L } ?: 600,
            prefs.getLong("tripGoal", 0L).coerceIn(0, 999_999_999_999L),
            prefs.getInt("tripDays", 0).coerceIn(0, 3650),
            prefs.getInt("budgetAlert", 80).coerceIn(50, 100)
        ))
    }
    private fun createBills(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE bills (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            category TEXT NOT NULL,
            amount_cents INTEGER NOT NULL CHECK(amount_cents > 0),
            due_date TEXT NOT NULL,
            note TEXT NOT NULL DEFAULT '',
            paid_entry_id INTEGER UNIQUE,
            created_at INTEGER NOT NULL
        )""")
        db.execSQL("CREATE INDEX idx_bills_due ON bills(due_date ASC, id DESC)")
    }
    private fun createRecycling(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE recycling_events (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            material TEXT NOT NULL CHECK(material IN ('cans','copper')),
            action TEXT NOT NULL CHECK(action IN ('collect','sale','adjust')),
            units INTEGER NOT NULL CHECK(units != 0),
            measured_grams INTEGER,
            estimated_weight INTEGER NOT NULL DEFAULT 0,
            price_cents_kg INTEGER,
            received_cents INTEGER,
            finance_entry_id INTEGER UNIQUE,
            note TEXT NOT NULL DEFAULT '',
            city TEXT NOT NULL DEFAULT '',
            country TEXT NOT NULL DEFAULT '',
            created_at INTEGER NOT NULL
        )""")
        db.execSQL("CREATE INDEX idx_recycling_time ON recycling_events(created_at DESC, id DESC)")
    }
    private fun createWalletTables(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE accounts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL UNIQUE,
            opening_cents INTEGER NOT NULL,
            is_reserve INTEGER NOT NULL CHECK(is_reserve IN (0,1))
        )""")
        db.execSQL("""CREATE TABLE wallet_transfers (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            from_id INTEGER NOT NULL,
            to_id INTEGER NOT NULL,
            cents INTEGER NOT NULL CHECK(cents > 0),
            note TEXT NOT NULL DEFAULT '',
            created_at INTEGER NOT NULL,
            CHECK(from_id != to_id),
            FOREIGN KEY(from_id) REFERENCES accounts(id),
            FOREIGN KEY(to_id) REFERENCES accounts(id)
        )""")
        db.execSQL("CREATE INDEX idx_wallet_transfers_time ON wallet_transfers(created_at DESC)")
        val prefs = appContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val initial = prefs.getLong("initial", 0L)
        val reserve = prefs.getLong("reserve", 0L)
        val main = Math.subtractExact(initial, reserve)
        db.execSQL("INSERT INTO accounts(id,name,opening_cents,is_reserve) VALUES(1,'Dinheiro físico',?,0)", arrayOf(main))
        db.execSQL("INSERT INTO accounts(id,name,opening_cents,is_reserve) VALUES(2,'Reserva de emergência',?,1)", arrayOf(reserve))
        db.execSQL("INSERT INTO accounts(id,name,opening_cents,is_reserve) VALUES(3,'Banco / Pix',0,0)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE entries ADD COLUMN item_kind TEXT")
            db.execSQL("ALTER TABLE entries ADD COLUMN quantity INTEGER")
            db.execSQL("ALTER TABLE entries ADD COLUMN unit_minor INTEGER")
        }
        if (oldVersion < 3) createRecycling(db)
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE entries ADD COLUMN account_id INTEGER NOT NULL DEFAULT 1")
            createWalletTables(db)
        }
        if (oldVersion < 5) createBills(db)
        if (oldVersion < 6) createSettings(db)
        if (newVersion != 6 || oldVersion !in 1..5) error("Migração SQLite obrigatória: versão $oldVersion -> $newVersion")
    }
}

