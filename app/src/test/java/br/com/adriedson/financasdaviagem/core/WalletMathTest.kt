package br.com.adriedson.financasdaviagem.core

import br.com.adriedson.financasdaviagem.data.Entry
import br.com.adriedson.financasdaviagem.data.WalletAccount
import br.com.adriedson.financasdaviagem.data.WalletTransfer
import org.junit.Assert.assertEquals
import org.junit.Test

class WalletMathTest {
    private val accounts = listOf(
        WalletAccount(1, "Dinheiro físico", 300_000),
        WalletAccount(2, "Reserva", 20_000, true),
        WalletAccount(3, "Banco / Pix", 0)
    )
    private val entries = listOf(
        Entry(kind = "income", brlCents = 7_500, originalMinor = 7_500, accountId = 1),
        Entry(kind = "expense", brlCents = 10_000, originalMinor = 10_000, accountId = 1)
    )

    @Test fun aberturaLegadaPreservaSaldoEReserva() {
        val balances = WalletMath.balances(accounts, entries, emptyList())
        assertEquals(297_500L, WalletMath.usable(accounts, balances))
        assertEquals(20_000L, WalletMath.reserve(accounts, balances))
    }

    @Test fun transferenciaEntreCarteirasNaoCriaRenda() {
        val transfer = WalletTransfer(fromId = 1, toId = 3, cents = 10_000)
        val balances = WalletMath.balances(accounts, entries, listOf(transfer))
        assertEquals(287_500L, balances.getValue(1))
        assertEquals(10_000L, balances.getValue(3))
        assertEquals(297_500L, WalletMath.usable(accounts, balances))
    }

    @Test fun transferenciaParaReservaDiminuiValorUtilizavel() {
        val transfer = WalletTransfer(fromId = 1, toId = 2, cents = 5_000)
        val balances = WalletMath.balances(accounts, entries, listOf(transfer))
        assertEquals(292_500L, WalletMath.usable(accounts, balances))
        assertEquals(25_000L, WalletMath.reserve(accounts, balances))
    }

    @Test fun recebimentoDeReciclagemEntraNaContaDeDestino() {
        val receipt = Entry(kind = "income", category = "Trabalho", accountId = 3,
            originalMinor = 1_200, brlCents = 1_200)
        val balances = WalletMath.balances(accounts, entries + receipt, emptyList())
        assertEquals(1_200L, balances.getValue(3))
        assertEquals(298_700L, WalletMath.usable(accounts, balances))
    }
}
