package br.com.adriedson.financasdaviagem.data

import br.com.adriedson.financasdaviagem.core.BillMath
import br.com.adriedson.financasdaviagem.core.FinanceMath

/** CSV ownership is separate from SQLite writes; exports use repository public reads. */
internal class CsvExports(private val repo: FinanceRepository) {
    fun exportCsv(): String {
        fun escaped(x: String): String {
            val safe = if (Regex("^[=+@-]").containsMatchIn(x.trimStart())) "'" + x else x
            return "\"" + safe.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\""
        }
        return buildString {
            append("data_iso;tipo;categoria;valor_original;moeda;taxa_brl;valor_brl;descricao;cidade;pais;tipo_item;quantidade;preco_unitario;conta_id\r\n")
            repo.all().forEach { e ->
                val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.ROOT).format(java.util.Date(e.createdAt))
                val cols = listOf(date, e.kind, e.category,
                    FinanceMath.decimalInput(e.originalMinor, FinanceMath.digits(e.currency)), e.currency,
                    e.rateBrl, FinanceMath.decimalInput(e.brlCents), e.note, e.city, e.country,
                    e.itemKind ?: "", e.quantity?.toString() ?: "",
                    e.unitMinor?.let { FinanceMath.decimalInput(it, FinanceMath.digits(e.currency)) } ?: "", e.accountId.toString())
                append(cols.joinToString(";") { escaped(it) }); append("\r\n")
            }
        }
    }
    fun exportTransfersCsv(): String = buildString {
        append("data_iso;origem;destino;valor_brl;descricao\r\n")
        val names = repo.accounts().associate { it.id to it.name }
        repo.transfers().forEach { t ->
            fun cell(x: String): String = "\"" +
                (if (Regex("^[=+@-]").containsMatchIn(x.trimStart())) "'" + x else x)
                    .replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\""
            val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.ROOT)
                .format(java.util.Date(t.createdAt))
            append(listOf(date, names[t.fromId] ?: "?", names[t.toId] ?: "?",
                FinanceMath.decimalInput(t.cents), t.note).joinToString(";") { cell(it) })
            append("\r\n")
        }
    }

    /** Operational log CSV separate from financial CSV: collections never masquerade as income. */
    fun exportRecyclingCsv(): String {
        fun esc(value: String): String {
            val x = if (Regex("^[=+@-]").containsMatchIn(value.trimStart())) "'" + value else value
            return "\"" + x.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\""
        }
        return buildString {
            append("data_iso;acao;material;variacao_estoque;unidade;peso_venda_g;peso_estimado;preco_centavos_kg;recebido_centavos;id_receita;descricao;cidade;pais\r\n")
            repo.recyclingEvents().forEach { e ->
                val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.ROOT)
                    .format(java.util.Date(e.createdAt))
                val cols = listOf(date, e.action, e.material, e.units.toString(),
                    if (e.material == "cans") "latinhas" else "gramas",
                    e.measuredGrams?.toString() ?: "", e.estimatedWeight.toString(),
                    e.priceCentsKg?.toString() ?: "", e.receivedCents?.toString() ?: "",
                    e.financeEntryId?.toString() ?: "", e.note, e.city, e.country)
                append(cols.joinToString(";") { esc(it) }); append("\r\n")
            }
        }
    }

    fun exportBillsCsv(): String = buildString {
        append("vencimento;descricao;categoria;valor_brl;status;data_pagamento;conta_pagamento;observacao\r\n")
        val entriesById = repo.all().associateBy { it.id }
        val names = repo.accounts().associate { it.id to it.name }
        fun esc(value: String): String = "\"" +
            (if (Regex("^[=+@-]").containsMatchIn(value.trimStart())) "'" + value else value)
                .replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\""
        repo.bills().forEach { bill ->
            val entry = bill.paidEntryId?.let { entriesById[it] }
            append(listOf(BillMath.displayDate(bill.dueDate), bill.title, bill.category,
                FinanceMath.decimalInput(bill.amountCents), if (entry == null) "Pendente" else "Pago",
                entry?.let { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.forLanguageTag("pt-BR"))
                    .format(java.util.Date(it.createdAt)) } ?: "",
                entry?.let { names[it.accountId] } ?: "", bill.note).joinToString(";") { esc(it) })
            append("\r\n")
        }
    }

}
