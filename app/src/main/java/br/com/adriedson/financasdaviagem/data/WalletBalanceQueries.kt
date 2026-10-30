package br.com.adriedson.financasdaviagem.data

import android.database.sqlite.SQLiteDatabase

/** Query a single account without loading every entry and transfer into Kotlin memory.
 * Runs on the same writable SQLite connection while a transfer transaction is open.
 * SQLite integer SUM raises on overflow rather than silently returning a rounded float.
 */
internal object WalletBalanceQueries {
    private const val SQL = """SELECT a.opening_cents +
        COALESCE((SELECT SUM(CASE WHEN e.kind='income' THEN e.brl_cents ELSE -e.brl_cents END)
            FROM entries e WHERE e.account_id=a.id), 0) +
        COALESCE((SELECT SUM(CASE WHEN t.to_id=a.id THEN t.cents ELSE -t.cents END)
            FROM wallet_transfers t WHERE t.from_id=a.id OR t.to_id=a.id), 0)
        FROM accounts a WHERE a.id=?"""

    fun balance(db: SQLiteDatabase, accountId: Long): Long =
        db.rawQuery(SQL, arrayOf(accountId.toString())).use { cursor ->
            require(cursor.moveToFirst()) { "Conta inexistente" }
            require(!cursor.isNull(0)) { "Saldo fora do intervalo inteiro" }
            cursor.getLong(0)
        }
}
