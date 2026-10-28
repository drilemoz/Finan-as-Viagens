package br.com.adriedson.financasdaviagem.core

import java.math.BigInteger

/** Planejamento offline sobre saldos efetivos, sem supor receitas futuras. */
data class TripPlanSummary(
    val usableCents: Long,
    val goalCents: Long,
    val dailyBudgetCents: Long,
    val plannedDays: Int,
    val plannedCostCents: Long?,
    val goalRemainingCents: Long?,
    val tripShortfallCents: Long?,
    val autonomyDays: Long?,
    val progressPercent: Int?,
    val todaySpentCents: Long,
    val todayPercent: Int?,
    val alert: DailyBudgetAlert
)

enum class DailyBudgetAlert { NONE, APPROACHING, EXCEEDED }

object TripPlanningMath {
    private fun ratioPercent(numerator: Long, denominator: Long): Int? {
        if (denominator <= 0) return null
        if (numerator <= 0) return 0
        val result = BigInteger.valueOf(numerator).multiply(BigInteger.valueOf(100))
            .divide(BigInteger.valueOf(denominator))
        return result.min(BigInteger.valueOf(100)).toInt()
    }

    fun calculate(
        usableCents: Long, dailyBudgetCents: Long, goalCents: Long,
        plannedDays: Int, alertAtPercent: Int, todaySpentCents: Long
    ): TripPlanSummary {
        require(dailyBudgetCents >= 0 && goalCents >= 0 && plannedDays in 0..3650)
        require(alertAtPercent in 50..100 && todaySpentCents >= 0)
        val planned = if (plannedDays > 0 && dailyBudgetCents > 0)
            Math.multiplyExact(dailyBudgetCents, plannedDays.toLong()) else null
        val remainingGoal = if (goalCents > 0) Math.subtractExact(goalCents, usableCents)
            .coerceAtLeast(0) else null
        val deficit = planned?.let { Math.subtractExact(it, usableCents).coerceAtLeast(0) }
        val budgetAlert = when {
            dailyBudgetCents == 0L -> DailyBudgetAlert.NONE
            todaySpentCents > dailyBudgetCents -> DailyBudgetAlert.EXCEEDED
            BigInteger.valueOf(todaySpentCents).multiply(BigInteger.valueOf(100)) >=
                BigInteger.valueOf(dailyBudgetCents).multiply(BigInteger.valueOf(alertAtPercent.toLong())) ->
                DailyBudgetAlert.APPROACHING
            else -> DailyBudgetAlert.NONE
        }
        return TripPlanSummary(
            usableCents, goalCents, dailyBudgetCents, plannedDays, planned,
            remainingGoal, deficit, FinanceMath.autonomyDays(usableCents, dailyBudgetCents),
            ratioPercent(usableCents, goalCents), todaySpentCents,
            ratioPercent(todaySpentCents, dailyBudgetCents), budgetAlert
        )
    }
}
