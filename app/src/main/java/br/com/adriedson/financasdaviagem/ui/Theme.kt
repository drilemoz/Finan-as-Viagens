package br.com.adriedson.financasdaviagem.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Mint = Color(0xFF62D4B7)
val Amber = Color(0xFFE7B66B)
val Blue = Color(0xFF83B4F4)

private val DarkScheme = darkColorScheme(
    primary = Mint,
    onPrimary = Color(0xFF10252B),
    background = Color(0xFF101D23),
    onBackground = Color(0xFFF4FAFA),
    surface = Color(0xFF1D3037),
    onSurface = Color(0xFFF4FAFA),
    surfaceVariant = Color(0xFF293E45),
    onSurfaceVariant = Color(0xFFB5C6C9),
    outline = Color(0xFF557077),
    secondary = Amber,
    error = Color(0xFFFF918E)
)
private val LightScheme = lightColorScheme(
    primary = Color(0xFF087D69),
    onPrimary = Color.White,
    background = Color(0xFFF2F7F7),
    onBackground = Color(0xFF14242A),
    surface = Color.White,
    onSurface = Color(0xFF14242A),
    surfaceVariant = Color(0xFFE4EEEE),
    onSurfaceVariant = Color(0xFF51666C),
    outline = Color(0xFFA3B7B9),
    secondary = Color(0xFF956017),
    error = Color(0xFFB3261E)
)

@Composable
fun FinancasTheme(mode: String, content: @Composable () -> Unit) {
    val dark = when (mode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, content = content)
}
