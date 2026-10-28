package br.com.adriedson.financasdaviagem.core

import br.com.adriedson.financasdaviagem.data.Entry
import br.com.adriedson.financasdaviagem.data.WalletAccount
import br.com.adriedson.financasdaviagem.data.WalletTransfer

/** Calcula saldos reais por conta sem duplicar receitas nas transferências. */
object WalletMath {
    fun balances(accounts: List<WalletAccount>, entries: List<Entry>, transfers: List<WalletTransfer>): Map<Long, Long> {
        val amounts = accounts.associate { it.id to it.openingCents }.toMutableMap()
        entries.forEach { e ->
            val current = requireNotNull(amounts[e.accountId]) { "Conta não encontrada" }
            amounts[e.accountId] = if (e.kind == "income") Math.addExact(current, e.brlCents)
                else Math.subtractExact(current, e.brlCents)
        }
        transfers.forEach { t ->
            require(t.fromId != t.toId && t.cents > 0)
            amounts[t.fromId] = Math.subtractExact(requireNotNull(amounts[t.fromId]), t.cents)
            amounts[t.toId] = Math.addExact(requireNotNull(amounts[t.toId]), t.cents)
        }
        return amounts
    }
    fun usable(accounts: List<WalletAccount>, balances: Map<Long, Long>): Long =
        accounts.filterNot { it.isReserve }.fold(0L) { sum, a -> Math.addExact(sum, requireNotNull(balances[a.id])) }
    fun reserve(accounts: List<WalletAccount>, balances: Map<Long, Long>): Long =
        accounts.filter { it.isReserve }.fold(0L) { sum, a -> Math.addExact(sum, requireNotNull(balances[a.id])) }
}
