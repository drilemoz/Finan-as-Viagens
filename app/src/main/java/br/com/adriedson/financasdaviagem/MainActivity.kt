package br.com.adriedson.financasdaviagem

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import br.com.adriedson.financasdaviagem.data.Entry
import br.com.adriedson.financasdaviagem.data.FinanceRepository
import br.com.adriedson.financasdaviagem.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FinanceApp(this) }
    }
}

@Composable
private fun FinanceApp(activity: ComponentActivity) {
    val context = LocalContext.current
    val repo = remember { FinanceRepository(context) }
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(repo.settings()) }
    var entries by remember { mutableStateOf(emptyList<Entry>()) }
    var screen by remember { mutableStateOf("home") }
    var editing by remember { mutableStateOf<Entry?>(null) }
    var addKind by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<FinanceRepository.Backup?>(null) }
    val notify: (String) -> Unit = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }

    fun refresh() {
        scope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) { repo.all() to repo.settings() }
                entries = loaded.first
                settings = loaded.second
            } catch (t: Exception) { notify("Não foi possível ler os dados: ${t.message}") }
        }
    }

    fun saveSettings(next: br.com.adriedson.financasdaviagem.data.Preferences) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) { repo.saveSettings(next) }
                settings = next
                notify("Configurações salvas")
            } catch (t: Exception) { notify("Erro ao salvar: ${t.message}") }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val text = repo.createBackup()
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(text) }
                        ?: throw IOException("Não foi possível abrir o arquivo de destino")
                }
                notify("Backup JSON exportado")
            } catch (t: Exception) { notify("Falha no backup: ${t.message}") }
        }
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                        it.write("\uFEFF")
                        it.write(repo.exportCsv())
                    } ?: throw IOException("Não foi possível escrever o arquivo CSV")
                }
                notify("CSV exportado")
            } catch (t: Exception) { notify("Falha no CSV: ${t.message}") }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                pendingImport = withContext(Dispatchers.IO) {
                    val raw = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                        // Limite de leitura para impedir backups enormes em aparelhos com pouca RAM.
                        val chunk = CharArray(4096)
                        val output = StringBuilder()
                        while (true) {
                            val read = reader.read(chunk)
                            if (read == -1) break
                            if (output.length + read > 4_000_000) throw IOException("Arquivo excede 4 MB")
                            output.append(chunk, 0, read)
                        }
                        output.toString()
                    } ?: throw IOException("Arquivo inacessível")
                    repo.parseBackup(raw)
                }
            } catch (t: Exception) { notify("Backup inválido: ${t.message}") }
        }
    }

    FinancasTheme(settings.themeMode) {
        val scheme = MaterialTheme.colorScheme
        val isLight = scheme.background.red > 0.5f
        SideEffect {
            @Suppress("DEPRECATION")
            activity.window.statusBarColor = scheme.background.toArgb()
            @Suppress("DEPRECATION")
            activity.window.navigationBarColor = scheme.background.toArgb()
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightStatusBars = isLight
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightNavigationBars = isLight
        }
        BackHandler(enabled = screen != "home" && addKind == null && editing == null) {
            screen = if (screen == "about") "settings" else "home"
        }
        Scaffold(containerColor = scheme.background,
            bottomBar = {
                if (screen != "about") {
                    NavigationBar(containerColor = scheme.background) {
                        listOf(Triple("home", "Painel", Icons.Outlined.Home),
                            Triple("history", "Histórico", Icons.Outlined.ReceiptLong),
                            Triple("settings", "Configurações", Icons.Outlined.Settings)).forEach { (target, label, icon) ->
                            NavigationBarItem(selected = screen == target, onClick = { screen = target },
                                icon = { Icon(icon, contentDescription = label) }, label = { Text(label) })
                        }
                    }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (screen) {
                    "home" -> DashboardScreen(entries, settings,
                        onAddExpense = { addKind = "expense" },
                        onAddIncome = { addKind = "income" },
                        onHistory = { screen = "history" },
                        onSettings = { screen = "settings" })
                    "history" -> HistoryScreen(entries, onEdit = { editing = it })
                    "settings" -> SettingsScreen(settings, ::saveSettings,
                        onBackup = {
                            val date = SimpleDateFormat("yyyyMMdd-HHmm", Locale.ROOT).format(Date())
                            backupLauncher.launch("financas-da-viagem-backup-$date.json")
                        },
                        onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                        onCsv = { csvLauncher.launch("financas-da-viagem-lancamentos.csv") },
                        onAbout = { screen = "about" })
                    "about" -> AboutScreen(onBack = { screen = "settings" })
                }
            }
        }
        if (addKind != null || editing != null) {
            EntryEditor(existing = editing, initialKind = addKind ?: "expense",
                onDismiss = { editing = null; addKind = null },
                onSave = { entry ->
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { repo.save(entry) }
                            editing = null; addKind = null; refresh()
                            notify("Lançamento salvo")
                        } catch (t: Exception) { notify("Erro ao salvar: ${t.message}") }
                    }
                },
                onDelete = { entry ->
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { repo.delete(entry.id) }
                            editing = null; addKind = null; refresh()
                            notify("Lançamento excluído")
                        } catch (t: Exception) { notify("Erro ao excluir: ${t.message}") }
                    }
                })
        }
        if (pendingImport != null) {
            val backup = pendingImport!!
            AlertDialog(onDismissRequest = { pendingImport = null },
                title = { Text("Restaurar backup?") },
                text = { Text("Os ${backup.entries.size} lançamentos do arquivo irão SUBSTITUIR todos os lançamentos e configurações atuais. Exporte um backup antes se quiser preservar os dados atuais.") },
                confirmButton = {
                    TextButton(onClick = {
                        pendingImport = null
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { repo.restore(backup) }
                                refresh(); notify("Backup restaurado")
                            } catch (t: Exception) { notify("Falha ao restaurar: ${t.message}") }
                        }
                    }) { Text("Substituir dados") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingImport = null }) { Text("Cancelar") }
                })
        }
    }
}
