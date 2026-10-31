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
import br.com.adriedson.financasdaviagem.data.Preferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
private fun SettingsTile(icon: ImageVector, label: String, description: String, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.Medium)
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SettingsScreen(
    settings: Preferences, onSave: (Preferences) -> Unit,
    onBackup: () -> Unit, onImport: () -> Unit, onCsv: () -> Unit, onAbout: () -> Unit,
    onAccounts: () -> Unit, onPlanning: () -> Unit, onPrivacy: () -> Unit
) {
    var budget by remember(settings.dailyBudgetCents) { mutableStateOf(FinanceMath.decimalInput(settings.dailyBudgetCents)) }
    var error by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item { Text("Configurações", fontSize = 25.sp, fontWeight = FontWeight.SemiBold) }
        item { SectionTitle("Aparência") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("system" to "Sistema", "dark" to "Escuro", "light" to "Claro").forEach { (mode, title) ->
                    FilterChip(selected = settings.themeMode == mode,
                        onClick = { onSave(settings.copy(themeMode = mode)) }, label = { Text(title) })
                }
            }
        }
        item { SectionTitle("Planejamento financeiro") }
        item { Text("O saldo inicial e a reserva são gerenciados por conta na aba Carteiras.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
        item { SettingsTile(Icons.Outlined.AccountBalanceWallet, "Carteiras e reserva", "Definir saldos iniciais e transferir entre contas", onAccounts) }
        item { SettingsTile(Icons.Outlined.Flag, "Planejamento da viagem", "Meta, dias planejados e alertas do orçamento", onPlanning) }
        item { MoneyField(value = budget, onValueChange = { budget = it },
            label = "Orçamento diário", modifier = Modifier.fillMaxWidth()) }
        if (error.isNotEmpty()) item { Text(error, color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall) }
        item {
            Button(onClick = {
                val budgetCents = FinanceMath.parseAmount(budget)
                if (budgetCents == null) {
                    error = "Informe um orçamento válido em reais."
                } else {
                    error = ""
                    onSave(settings.copy(dailyBudgetCents = budgetCents))
                }
            }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) {
                Icon(Icons.Outlined.Save, null); Spacer(Modifier.width(7.dp)); Text("Salvar valores")
            }
        }
        item { HorizontalDivider() }
        item { SectionTitle("Dados e backup") }
        item { SettingsTile(Icons.Outlined.FileDownload, "Exportar backup JSON", "Salvar lançamentos e configurações no dispositivo", onBackup) }
        item { SettingsTile(Icons.Outlined.FileUpload, "Restaurar backup JSON", "Substituir dados após confirmação", onImport) }
        item { SettingsTile(Icons.Outlined.TableView, "Exportar CSV", "Abrir os lançamentos em planilhas", onCsv) }
        item { Text("Os arquivos são escolhidos por você no seletor do Android. Não há sincronização automática.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
        item { HorizontalDivider() }
        item { SettingsTile(Icons.Outlined.PrivacyTip, "Política de privacidade", "Leia como os dados financeiros são tratados", onPrivacy) }
        item { SettingsTile(Icons.Outlined.Info, "Sobre", "Versão, privacidade e funcionamento", onAbout) }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit, onPrivacy: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Voltar") } }
        item { Icon(Icons.Outlined.DirectionsBike, null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(54.dp)) }
        item { Text("Finanças da Viagem", fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        item { Text("Versão 1.0.0 · versionCode 13", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { SectionTitle("Sobre o aplicativo") }
        item { Text("Controle financeiro e de reciclagem offline para a cicloviagem: carteiras, contas a pagar, transferências, doações, receitas, despesas, latinhas, cobre, metas e orçamento diário.") }
        item { SectionTitle("Novidades da versão 1.0.0") }
        item { Text("• Compatibilidade com Android 16 (API 36).\n• Interface adaptada às barras do sistema.\n• Política de privacidade acessível no aplicativo.\n• Preparação para distribuição por Google Play, sem alterar cálculos ou dados financeiros.") }
        item { TextButton(onClick = onPrivacy) { Icon(Icons.Outlined.PrivacyTip, null); Spacer(Modifier.width(8.dp)); Text("Ler política de privacidade") } }
        item { SectionTitle("Privacidade e dados") }
        item { Text("Os lançamentos são gravados no armazenamento privado do Android. O app não solicita acesso à internet, localização ou contas bancárias. O backup só é criado ou restaurado quando você escolhe um arquivo.") }
        item { SectionTitle("Moedas e taxas") }
        item { Text("Os valores exibidos no painel são em reais. Lançamentos em outras moedas usam a taxa de câmbio informada manualmente na data do registro; o aplicativo não consulta cotações automaticamente.") }
        item { SectionTitle("Autonomia") }
        item { Text("A estimativa é saldo utilizável ÷ orçamento diário. O custo planejado é dias × orçamento, sem considerar receitas futuras. Nenhum valor de recicláveis ainda não vendidos é tratado como dinheiro disponível.") }
        item { Text("Projeto inicial · Android nativo · Kotlin / Jetpack Compose", color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall) }
    }
}
