package br.com.adriedson.financasdaviagem.data

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Independent temporary databases; never deletes the user's real finance database. */
@RunWith(AndroidJUnit4::class)
class SettingsMigrationAndRestoreTest {
    @Test fun legacyV5UpgradesSettingsWithoutDeletingLegacyData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "alpha11-migration-test.db"
        context.deleteDatabase(name)
        try {
            val file = context.getDatabasePath(name)
            file.parentFile?.mkdirs()
            SQLiteDatabase.openOrCreateDatabase(file, null).use { database ->
                database.execSQL("CREATE TABLE accounts(id INTEGER PRIMARY KEY, name TEXT NOT NULL)")
                database.execSQL("INSERT INTO accounts VALUES(4, 'Conta antiga')")
                database.version = 5
            }
            LedgerDb(context, name).writableDatabase.use { database ->
                assertEquals(6, database.version)
                database.rawQuery("SELECT name FROM accounts WHERE id=4", null).use { cursor ->
                    assertTrue(cursor.moveToFirst()); assertEquals("Conta antiga", cursor.getString(0))
                }
                database.rawQuery("SELECT COUNT(*) FROM app_settings WHERE id=1", null).use { cursor ->
                    assertTrue(cursor.moveToFirst()); assertEquals(1, cursor.getInt(0))
                }
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun restoreSettingsAndEntriesCommitOrRollbackTogether() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "alpha11-restore-test.db"
        context.deleteDatabase(name)
        try {
            val repo = FinanceRepository(context, name)
            val accounts = repo.accounts()
            val e = Entry(id=17, kind="income", category="Trabalho", note="Primeiro teste",
                originalMinor=1280, brlCents=1280, accountId=accounts.first().id)
            val pref = repo.settings().copy(dailyBudgetCents=3500, themeMode="dark")
            repo.restore(FinanceRepository.Backup(pref, listOf(e), emptyList(), accounts, emptyList()))
            assertEquals(3500, repo.settings().dailyBudgetCents)
            assertEquals(listOf(e), repo.all())
            // Trigger fails only when preferences are updated, AFTER previous rows were changed.
            val db = SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null,
                SQLiteDatabase.OPEN_READWRITE)
            db.use {
                it.execSQL("""CREATE TRIGGER refuse_setting_change BEFORE UPDATE ON app_settings
                    BEGIN SELECT RAISE(ABORT, 'simulate interrupted restore'); END""")
            }
            val other = e.copy(id=31, note="Substituição não confirmada")
            val changed = pref.copy(dailyBudgetCents=5000, themeMode="light")
            try {
                repo.restore(FinanceRepository.Backup(changed, listOf(other), emptyList(), accounts, emptyList()))
                fail("Restore must roll back when settings cannot be written")
            } catch (_: Exception) { /* Expected SQL rollback */ }
            assertEquals(3500, repo.settings().dailyBudgetCents)
            assertEquals("dark", repo.settings().themeMode)
            assertEquals(listOf(e), repo.all())
        } finally { context.deleteDatabase(name) }
    }
}
