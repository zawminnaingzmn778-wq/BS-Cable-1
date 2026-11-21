package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.CheckStatus
import com.example.domain.model.DataSourceType
import com.example.domain.model.VerificationStatus
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusColors

@Composable
fun StatusBadge(
    status: CheckStatus,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val (bg, fg, icon) = when (status) {
        CheckStatus.PASS -> Triple(
            if (isDark) StatusColors.PassBgDark else StatusColors.PassBgLight,
            if (isDark) StatusColors.PassTextDark else StatusColors.PassTextLight,
            Icons.Filled.CheckCircle
        )
        CheckStatus.FAIL -> Triple(
            if (isDark) StatusColors.FailBgDark else StatusColors.FailBgLight,
            if (isDark) StatusColors.FailTextDark else StatusColors.FailTextLight,
            Icons.Filled.Error
        )
        CheckStatus.NOT_VERIFIED -> Triple(
            if (isDark) StatusColors.WarnBgDark else StatusColors.WarnBgLight,
            if (isDark) StatusColors.WarnTextDark else StatusColors.WarnTextLight,
            Icons.Filled.Warning
        )
    }

    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = status.label, tint = fg, modifier = Modifier.size(15.dp))
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelMedium,
                color = fg,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun VerificationPill(
    status: VerificationStatus,
    source: DataSourceType? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val (bg, fg) = when (status) {
        VerificationStatus.VERIFIED -> (if (isDark) StatusColors.PassBgDark else StatusColors.PassBgLight) to
            (if (isDark) StatusColors.PassTextDark else StatusColors.PassTextLight)
        VerificationStatus.PARTIALLY_VERIFIED -> (if (isDark) StatusColors.WarnBgDark else StatusColors.WarnBgLight) to
            (if (isDark) StatusColors.WarnTextDark else StatusColors.WarnTextLight)
        VerificationStatus.NOT_VERIFIED -> (if (isDark) StatusColors.WarnBgDark else StatusColors.WarnBgLight) to
            (if (isDark) StatusColors.WarnTextDark else StatusColors.WarnTextLight)
        VerificationStatus.UNSUPPORTED -> (if (isDark) StatusColors.FailBgDark else StatusColors.FailBgLight) to
            (if (isDark) StatusColors.FailTextDark else StatusColors.FailTextLight)
    }

    val text = if (source != null) "${status.label} · ${source.label}" else status.label
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EngineeringSectionCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            content()
        }
    }
}

@Composable
fun EngineeringNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    unit: String = "",
    supportingText: String? = null,
    modifier: Modifier = Modifier,
    isDecimal: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val filtered = if (isDecimal) {
                raw.filter { it.isDigit() || it == '.' || it == '-' }
            } else {
                raw.filter { it.isDigit() }
            }
            onValueChange(filtered)
        },
        label = { Text(label) },
        suffix = if (unit.isNotBlank()) {
            {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else null,
        supportingText = if (!supportingText.isNullOrBlank()) {
            { Text(supportingText, style = MaterialTheme.typography.bodySmall) }
        } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isDecimal) KeyboardType.Decimal else KeyboardType.Number
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun EngineeringTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotBlank()) { { Text(placeholder) } } else null,
        singleLine = singleLine,
        minLines = minLines,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun MonoMetricTile(
    label: String,
    value: String,
    unit: String,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = highlightColor
                )
                if (unit.isNotBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}
