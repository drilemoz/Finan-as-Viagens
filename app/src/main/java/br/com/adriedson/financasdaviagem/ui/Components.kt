package br.com.adriedson.financasdaviagem.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground)
}

@Composable
fun ValueCard(
    title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier,
    subtitle: String? = null, highlight: Boolean = false, compact: Boolean = false
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(if (compact) 19.dp else 23.dp)) {
        Column(Modifier.padding(if (compact) 13.dp else 17.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(if (compact) 18.dp else 19.dp))
                Text(title, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            Text(value, fontSize = if (compact) {
                if (value.length > 10) 18.sp else 22.sp
            } else if (value.length > 13) 20.sp else 25.sp,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EmptyState(title: String, description: String, icon: ImageVector, modifier: Modifier = Modifier,
    compact: Boolean = false) {
    Column(modifier.fillMaxWidth().padding(vertical = if (compact) 10.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 9.dp)) {
        Icon(icon, null, modifier = Modifier.size(if (compact) 32.dp else 40.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun CategoryBar(title: String, cents: Long, total: Long, color: Color) {
    val progress = if (total > 0) (cents.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f) else 0f
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
            Text(br.com.adriedson.financasdaviagem.core.FinanceMath.format(cents),
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(7.dp),
            color = color, trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}
