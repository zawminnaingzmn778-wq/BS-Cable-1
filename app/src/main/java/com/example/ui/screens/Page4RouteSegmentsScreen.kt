package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Schema
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.RouteSegment
import com.example.domain.model.SegmentEvaluationResult
import com.example.ui.components.EngineeringNumberField
import com.example.ui.components.EngineeringTextField
import com.example.ui.components.MonoMetricTile
import com.example.ui.components.ReferenceMethodDiagram
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.util.Locale

@Composable
fun Page4RouteSegmentsScreen(
    segments: List<RouteSegment>,
    segmentResults: List<SegmentEvaluationResult>,
    totalLengthM: Double,
    activeSegmentIndex: Int,
    onSelectSegment: (Int) -> Unit,
    onAddSegment: () -> Unit,
    onUpdateSegment: (Int, (RouteSegment) -> RouteSegment) -> Unit,
    onDuplicateSegment: (Int) -> Unit,
    onDeleteSegment: (Int) -> Unit,
    onMoveSegment: (Int, Int) -> Unit,
    onPickMethodForSegment: (Int) -> Unit,
    onEditConditionsForSegment: (Int) -> Unit
) {
    val totalDv = segmentResults.sumOf { it.segmentVoltageDropV ?: 0.0 }
    val governing = segmentResults.firstOrNull { it.isGoverningAmpacity }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Page 4 · Cable Route & Segments", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "Evaluate continuous multi-segment routes. Governing segment limits ampacity; voltage drops sum across all segments.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onAddSegment,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_segment_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }

        // Route Summary Tiles
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MonoMetricTile(
                    label = "Total Route Length",
                    value = String.format(Locale.US, "%.1f", totalLengthM),
                    unit = "m (${segments.size} seg)",
                    modifier = Modifier.weight(1f)
                )
                MonoMetricTile(
                    label = "Governing Segment",
                    value = governing?.methodCode?.code ?: "-",
                    unit = governing?.label ?: "None",
                    highlightColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                MonoMetricTile(
                    label = "Route ΔV",
                    value = String.format(Locale.US, "%.2f", totalDv),
                    unit = "V",
                    modifier = Modifier.weight(0.8f)
                )
            }
        }

        itemsIndexed(segments, key = { _, seg -> seg.id }) { index, seg ->
            val segEval = segmentResults.firstOrNull { it.segmentId == seg.id }
            val isSelected = index == activeSegmentIndex
            val isGov = segEval?.isGoverningAmpacity == true

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("route_segment_card_$index"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    width = if (isGov) 1.8.dp else if (isSelected) 1.5.dp else 1.dp,
                    color = when {
                        isGov -> MaterialTheme.colorScheme.secondary
                        isSelected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    }
                ),
                onClick = { onSelectSegment(index) }
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "#${index + 1} · ${seg.methodCode.code}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            if (isGov) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "GOVERNING",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Row {
                            IconButton(
                                onClick = { onMoveSegment(index, -1) },
                                enabled = index > 0,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Filled.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { onMoveSegment(index, 1) },
                                enabled = index < segments.lastIndex,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Filled.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { onDuplicateSegment(index) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate Segment", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { onDeleteSegment(index) },
                                enabled = segments.size > 1,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete Segment", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Diagram + Label/Length fields
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ReferenceMethodDiagram(
                            methodCode = seg.methodCode,
                            modifier = Modifier.size(78.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EngineeringTextField(
                                value = seg.label,
                                onValueChange = { lbl -> onUpdateSegment(index) { it.copy(label = lbl) } },
                                label = "Segment Name"
                            )
                            EngineeringNumberField(
                                value = if (seg.lengthMeters % 1.0 == 0.0) seg.lengthMeters.toInt().toString() else seg.lengthMeters.toString(),
                                onValueChange = { txt ->
                                    txt.toDoubleOrNull()?.let { len ->
                                        onUpdateSegment(index) { it.copy(lengthMeters = len) }
                                    }
                                },
                                label = "Length",
                                unit = "m"
                            )
                        }
                    }

                    // Live Segment Calculation Metrics
                    if (segEval != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "It: ${segEval.tabulatedItA?.let { String.format(Locale.US, "%.1f", it) } ?: "N/A"} A",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = "C: ${String.format(Locale.US, "%.3f", segEval.totalCorrectionFactor)}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = "Iz: ${segEval.effectiveTotalIzA?.let { String.format(Locale.US, "%.1f", it) } ?: "N/A"} A",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "ΔV: ${segEval.segmentVoltageDropV?.let { String.format(Locale.US, "%.2f", it) } ?: "?"} V",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        if (!segEval.isSupportedMethod) {
                            Text(
                                text = segEval.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Action Buttons: Method & Conditions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onPickMethodForSegment(index) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.Schema, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Method ${seg.methodCode.code}")
                        }
                        OutlinedButton(
                            onClick = { onEditConditionsForSegment(index) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Conditions")
                        }
                    }

                    // Optional Segment Transition Override
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transition override (CSA / parallel runs on this segment)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = seg.allowTransitionOverride,
                            onCheckedChange = { checked ->
                                onUpdateSegment(index) { it.copy(allowTransitionOverride = checked) }
                            }
                        )
                    }

                    if (seg.allowTransitionOverride) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            EngineeringNumberField(
                                value = seg.overrideCableSizeMm2?.toString() ?: "",
                                onValueChange = { txt ->
                                    onUpdateSegment(index) { it.copy(overrideCableSizeMm2 = txt.toDoubleOrNull()) }
                                },
                                label = "Segment CSA",
                                unit = "mm²",
                                modifier = Modifier.weight(1f)
                            )
                            EngineeringNumberField(
                                value = seg.overrideParallelRuns?.toString() ?: "",
                                onValueChange = { txt ->
                                    onUpdateSegment(index) { it.copy(overrideParallelRuns = txt.toIntOrNull()) }
                                },
                                label = "Runs",
                                isDecimal = false,
                                modifier = Modifier.weight(0.7f)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
