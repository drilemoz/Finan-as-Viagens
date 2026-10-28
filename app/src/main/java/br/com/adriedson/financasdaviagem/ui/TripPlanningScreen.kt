package br.com.adriedson.financasdaviagem.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.adriedson.financasdaviagem.core.DailyBudgetAlert
import br.com.adriedson.financasdaviagem.core.FinanceMath
import br.com.adriedson.financasdaviagem.core.TripPlanningMath
import br.com.adriedson.financasdaviagem.core.WalletMath
import br.com.adriedson.financasdaviagem.data.Entry
import br.com.adriedson.financasdaviagem.data.Preferences
import br.com.adriedson.financasdaviagem.data.WalletAccount
import br.com.adriedson.financasdaviagem.data.WalletTransfer
import java.time.LocalDate
import java.time.ZoneId

/** Ajustes persistidos em Preferences. Sem simular entradas de dinheiro ou cotações futuras. */
@Composable
fun TripPlanningScreen(
    settings: Preferences, entries: List<Entry>, accounts: List<WalletAccount>, transfers: List<WalletTransfer>,
    onSave: (Preferences) -> Unit, onBack: () -> Unit
) {
    var goalInput by remember(settings.tripGoalCents) { mutableStateOf(FinanceMath.decimalInput(settings.tripGoalCents)) }
    var budgetInput by remember(settings.dailyBudgetCents) { mutableStateOf(FinanceMath.decimalInput(settings.dailyBudgetCents)) }
    var daysInput by remember(settings.plannedTripDays) { mutableStateOf(settings.plannedTripDays.toString()) }
    var alertPercent by remember(settings.budgetAlertPercent) { mutableIntStateOf(settings.budgetAlertPercent) }
    var validationError by remember { mutableStateOf("") }

    val balances = WalletMath.balances(accounts, entries, transfers)
    val usable = WalletMath.usable(accounts, balances)
    val reserve = WalletMath.reserve(accounts, balances)
    val today = LocalDate.now()
    val start = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val now = System.currentTimeMillis()
    val todaySpent = entries.asSequence()
        .filter { it.kind == "expense" && it.createdAt >= start && it.createdAt <= now }
        .fold(0L) { sum, e -> Math.addExact(sum, e.brlCents) }
    val plan = TripPlanningMath.calculate(usable, settings.dailyBudgetCents,
        settings.tripGoalCents, settings.plannedTripDays, settings.budgetAlertPercent, todaySpent)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Voltar ao painel") }
                Spacer(Modifier.width(4.dp))
                Text("Planejamento da viagem", fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Dinheiro para a viagem", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(FinanceMath.format(usable), style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (usable < 0L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    Text("Reserva de emergência separada: ${FinanceMath.format(reserve)}",
                        color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    Text("Inclui receitas já recebidas, como trabalho e doações. Não inclui recicláveis sem venda.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Text("Sua meta", style = MaterialTheme.typography.titleMedium) }
        item {
            MoneyField(goalInput, { goalInput = it }, "Meta de dinheiro para a viagem",
                modifier = Modifier.fillMaxWidth())
        }
        item {
            MoneyField(budgetInput, { budgetInput = it }, "Orçamento por dia",
                modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = daysInput,
                onValueChange = { v -> if (v.length <= 4 && v.all(Char::isDigit)) daysInput = v },
                label = { Text("Quantidade de dias planejados") },
                supportingText = { Text("Use 0 para deixar o prazo indefinido.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(12.dp))
        }
        item { Text("Avisar ao atingir parte do orçamento do dia", style = MaterialTheme.typography.titleSmall) }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(50, 70, 80, 90, 100).forEach { percent ->
                    FilterChip(selected = alertPercent == percent, onClick = { alertPercent = percent },
                        label = { Text("$percent%") })
                }
            }
        }
        if (validationError.isNotBlank()) {
            item { Text(validationError, color = MaterialTheme.colorScheme.error) }
        }
        item {
            Button(onClick = {
                val goal = FinanceMath.parseAmount(goalInput)
                val budget = FinanceMath.parseAmount(budgetInput)
                val days = daysInput.toIntOrNull()
                if (goal == null || budget == null || days == null || days !in 0..3650) {
                    validationError = "Confira os valores e informe entre 0 e 3650 dias."
                } else {
                    validationError = ""
                    onSave(settings.copy(tripGoalCents = goal, dailyBudgetCents = budget,
                        plannedTripDays = days, budgetAlertPercent = alertPercent))
                }
            }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(15.dp)) {
                Icon(Icons.Outlined.Save, null); Spacer(Modifier.width(8.dp)); Text("Salvar planejamento")
            }
        }
        item { HorizontalDivider() }
        item { Text("Previsão a partir dos dados salvos", style = MaterialTheme.typography.titleMedium) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ValueCard("Autonomia pelo orçamento", plan.autonomyDays?.let { "$it dias" } ?: "Indisponível",
                    Icons.Outlined.HourglassBottom, Modifier.weight(1f))
                ValueCard("Custo dos dias planejados", plan.plannedCostCents?.let(FinanceMath::format) ?: "Não definido",
                    Icons.Outlined.CalendarMonth, Modifier.weight(1f))
            }
        }
        if (plan.goalCents > 0) {
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Meta de dinheiro disponível", style = MaterialTheme.typography.titleSmall)
                        Text("${FinanceMath.format(usable.coerceAtLeast(0))} de ${FinanceMath.format(plan.goalCents)}")
                        LinearProgressIndicator(progress = { (plan.progressPercent ?: 0) / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp))
                        Text(if (plan.goalRemainingCents == 0L) "Meta atingida" else
                            "Faltam ${FinanceMath.format(plan.goalRemainingCents ?: 0L)} para a meta",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (plan.plannedCostCents != null) {
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Cobertura do roteiro", fontWeight = FontWeight.SemiBold)
                        Text(if (plan.tripShortfallCents == 0L) "O saldo atual cobre o custo planejado." else
                            "Faltam ${FinanceMath.format(plan.tripShortfallCents ?: 0L)} para cobrir os dias planejados.")
                        Text("É uma projeção pelo orçamento diário, sem supor futuras receitas, variações de preços ou câmbio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(16.dp),
                color = if (plan.alert == DailyBudgetAlert.NONE) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.errorContainer) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Orçamento de hoje", fontWeight = FontWeight.SemiBold)
                    Text("${FinanceMath.format(todaySpent)} de ${FinanceMath.format(plan.dailyBudgetCents)}")
                    Text(when (plan.alert) {
                        DailyBudgetAlert.NONE -> if (plan.dailyBudgetCents == 0L)
                            "Defina um orçamento maior que zero para receber alertas." else "Dentro do limite configurado."
                        DailyBudgetAlert.APPROACHING -> if (todaySpent == plan.dailyBudgetCents)
                            "Você atingiu o orçamento diário." else
                            "Atenção: você já gastou pelo menos ${settings.budgetAlertPercent}% do orçamento de hoje."
                        DailyBudgetAlert.EXCEEDED -> "Orçamento diário ultrapassado."
                    }, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Text("Os alertas aparecem no aplicativo, sem notificações de sistema. A reserva não entra no cálculo da autonomia e as contas a pagar só afetam o saldo depois de pagas.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        item { Spacer(Modifier.height(18.dp)) }
    }
}
