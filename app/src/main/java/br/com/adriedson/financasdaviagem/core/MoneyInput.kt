package br.com.adriedson.financasdaviagem.core

/** Máscara de digitação monetária. Cada dígito novo desloca os centavos para a esquerda.
 * A representação usada no banco continua em Long; não há conversões para Double.
 */
object MoneyInput {
    private const val MAX_MINOR_DIGITS = 12

    fun fromTyped(input: String, decimals: Int = 2): String {
        require(decimals in 0..3)
        val digitString = input.filter { it in '0'..'9' }.take(MAX_MINOR_DIGITS)
        if (digitString.isEmpty()) return ""
        return fromMinor(digitString.toLong(), decimals)
    }

    /** Exibe milhares com ponto e fração com vírgula, inclusive em moedas sem centavos. */
    fun fromMinor(minor: Long, decimals: Int = 2): String {
        require(decimals in 0..3 && minor in 0..999_999_999_999L)
        val raw = minor.toString().padStart(decimals + 1, '0')
        val integerPart = if (decimals == 0) raw else raw.dropLast(decimals)
        val grouped = integerPart.reversed().chunked(3).joinToString(".").reversed()
        return if (decimals == 0) grouped else "$grouped,${raw.takeLast(decimals)}"
    }

    /** Retorna texto sem separador de milhar para o parser monetário legado. */
    fun canonical(display: String, decimals: Int = 2): String {
        if (display.isBlank()) return ""
        return fromTyped(display, decimals).replace(".", "")
    }

    /** Usado apenas para posicionar o cursor após reformatar uma edição. */
    fun cursorWithDigitsToRight(display: String, digitsToRight: Int): Int {
        if (digitsToRight <= 0) return display.length
        var found = 0
        for (i in display.indices.reversed()) {
            if (display[i] in '0'..'9') found++
            if (found >= digitsToRight) return i
        }
        return 0
    }
}
