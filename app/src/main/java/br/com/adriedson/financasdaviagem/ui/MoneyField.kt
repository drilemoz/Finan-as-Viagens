package br.com.adriedson.financasdaviagem.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import br.com.adriedson.financasdaviagem.core.MoneyInput

/** Campo monetário controlado, com pontuação automática e cursor preservado na edição. */
@Composable
fun MoneyField(value: String, onValueChange: (String) -> Unit, label: String,
               modifier: Modifier = Modifier, currency: String = "BRL", decimals: Int = 2) {
    fun rendered(text: String): String = MoneyInput.fromTyped(text, decimals)
    var field by remember { mutableStateOf(TextFieldValue(rendered(value), TextRange(rendered(value).length))) }
    SideEffect {
        val expected = rendered(value)
        if (expected != field.text) field = TextFieldValue(expected, TextRange(expected.length))
    }
    OutlinedTextField(value = field, onValueChange = { next ->
        // Permitir apagar o zero com backspace, sem prender o campo em "0,00".
        val zeroBeingDeleted = next.text.length < field.text.length &&
            next.text.filter { it in '0'..'9' }.all { it == '0' } &&
            field.text.filter { it in '0'..'9' }.all { it == '0' }
        val masked = if (zeroBeingDeleted) "" else rendered(next.text)
        val afterCursor = next.text.drop(next.selection.end.coerceIn(0, next.text.length)).count { it in '0'..'9' }
        val cursor = MoneyInput.cursorWithDigitsToRight(masked, afterCursor)
        field = TextFieldValue(masked, selection = TextRange(cursor.coerceIn(0, masked.length)))
        onValueChange(MoneyInput.canonical(masked, decimals))
    }, label = { Text(label) }, prefix = { Text(if (currency == "BRL") "R$" else currency) },
        supportingText = { Text(if (decimals == 0) "Milhares automáticos" else "Centavos e milhares automáticos") },
        modifier = modifier, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp))
}
