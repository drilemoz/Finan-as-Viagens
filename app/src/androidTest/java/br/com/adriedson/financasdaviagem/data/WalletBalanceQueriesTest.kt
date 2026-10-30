package br.com.adriedson.financasdaviagem.data

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.adriedson.financasdaviagem.core.WalletMath
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Executes real Android SQLite rather than stubbed SQL and validates integer balances. */
@RunWith(AndroidJUnit4::class)
class WalletBalanceQueriesTest {
    @Test fun sqlBalancesMatchExactWalletMathForThousandOperations() {
        val db = SQLiteDatabase.create(null)
        try {
            db.execSQL("CREATE TABLE accounts (id INTEGER PRIMARY KEY, opening_cents INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE entries (account_id INTEGER NOT NULL, kind TEXT NOT NULL, brl_cents INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE wallet_transfers (from_id INTEGER NOT NULL, to_id INTEGER NOT NULL, cents INTEGER NOT NULL)")
            val accounts = listOf(
                WalletAccount(1, "Dinheiro", 100_000),
                WalletAccount(2, "Reserva", 75_000, true),
                WalletAccount(3, "Pix", 40_000)
            )
            accounts.forEach { db.execSQL("INSERT INTO accounts(id,opening_cents) VALUES(?,?)", arrayOf(it.id, it.openingCents)) }
            val entries = mutableListOf<Entry>()
            val transfers = mutableListOf<WalletTransfer>()
            repeat(1000) { index ->
                val id = (index % 3 + 1).toLong()
                val cents = (index % 19 + 1).toLong() * 101
                if (index % 5 == 0) {
                    val next = id % 3 + 1
                    transfers.add(WalletTransfer(fromId = id, toId = next, cents = cents))
                    db.execSQL("INSERT INTO wallet_transfers(from_id,to_id,cents) VALUES(?,?,?)", arrayOf(id, next, cents))
                } else {
                    val kind = if (index % 2 == 0) "income" else "expense"
                    entries.add(Entry(accountId = id, kind = kind, brlCents = cents))
                    db.execSQL("INSERT INTO entries(account_id,kind,brl_cents) VALUES(?,?,?)", arrayOf(id, kind, cents))
                }
            }
            val expected = WalletMath.balances(accounts, entries, transfers)
            accounts.forEach { assertEquals(expected.getValue(it.id), WalletBalanceQueries.balance(db, it.id)) }
        } finally { db.close() }
    }
}
