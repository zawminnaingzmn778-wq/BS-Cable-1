package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.CableConstruction
import com.example.domain.model.ReferenceMethodCode
import com.example.domain.model.ReferenceMethodSpec
import com.example.domain.model.RouteSegment
import com.example.domain.standards.ReferenceMethodCatalog
import com.example.ui.components.ReferenceMethodDiagram
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun Page5ReferenceMethodScreen(
    segments: List<RouteSegment>,
    activeSegmentIndex: Int,
    cableConstruction: CableConstruction,
    onSelectSegment: (Int) -> Unit,
    onSelectMethodForSegment: (Int, ReferenceMethodCode) -> Unit,
    standaloneGuideMode: Boolean = false
) {
    var inspectedMethod by remember { mutableStateOf<ReferenceMethodSpec?>(null) }
    val safeIndex = activeSegmentIndex.coerceIn(0, (segments.size - 1).coerceAtLeast(0))
    val currentSegment = segments.getOrNull(safeIndex)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (standaloneGuideMode) "BS 7671 Reference Methods (A1–G)" else "Page 5 · BS Reference Installation Method",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = if (standaloneGuideMode) {
                        "Inspect cross-section diagrams, applicable cable constructions, and limitations."
                    } else {
                        "Select the installation method for each route segment. Compatibility is checked against ${cableConstruction.label}."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!standaloneGuideMode && segments.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        segments.forEachIndexed { idx, seg ->
                            FilterChip(
                                selected = idx == safeIndex,
                                onClick = { onSelectSegment(idx) },
                                label = { Text("#${idx + 1}: ${seg.label} (${seg.methodCode.code})") }
                            )
                        }
                    }
                }
            }
        }

        items(ReferenceMethodCatalog.allMethods, key = { it.code.name }) { spec ->
            val isCompatible = ReferenceMethodCatalog.isCompatible(spec.code, cableConstruction)
            val isSelected = !standaloneGuideMode && currentSegment?.methodCode == spec.code

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("method_card_${spec.code.code}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        ReferenceMethodDiagram(
                            methodCode = spec.code,
                            modifier = Modifier.size(92.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Method ${spec.code.code}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                if (!standaloneGuideMode) {
                                    if (isCompatible) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = "Compatible",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Filled.Warning,
                                            contentDescription = "Check cable construction",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = spec.code.shortTitle,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = spec.shortSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Applicable: " + spec.applicableConstructions.joinToString(", ") { it.shortLabel },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Limitations: ${spec.limitations.firstOrNull() ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { inspectedMethod = spec },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.ZoomIn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Larger Diagram")
                        }
                        if (!standaloneGuideMode && currentSegment != null) {
                            Button(
                                onClick = { onSelectMethodForSegment(safeIndex, spec.code) },
                                enabled = !isSelected,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isSelected) "Selected" else "Assign ${spec.code.code}")
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    inspectedMethod?.let { spec ->
        AlertDialog(
            onDismissRequest = { inspectedMethod = null },
            title = { Text(spec.title, style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReferenceMethodDiagram(
                        methodCode = spec.code,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                    )
                    Text(spec.detailedDescription, style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "Conductor Arrangements:\n• " + spec.conductorArrangements.joinToString("\n• "),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Limitations:\n• " + spec.limitations.joinToString("\n• "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Ref: ${spec.standardReference}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                if (!standaloneGuideMode && currentSegment != null) {
                    Button(onClick = {
                        onSelectMethodForSegment(safeIndex, spec.code)
                        inspectedMethod = null
                    }) {
                        Text("Select Method ${spec.code.code}")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { inspectedMethod = null }) {
                    Text("Close")
                }
            }
        )
    }
}
