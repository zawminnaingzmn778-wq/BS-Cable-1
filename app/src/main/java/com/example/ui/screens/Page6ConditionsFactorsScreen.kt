package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.DataSourceType
import com.example.domain.model.ReferenceMethodCode
import com.example.domain.model.RouteSegment
import com.example.domain.model.SegmentEvaluationResult
import com.example.ui.components.EngineeringNumberField
import com.example.ui.components.EngineeringSectionCard
import com.example.ui.components.VerificationPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.util.Locale

@Composable
fun Page6ConditionsFactorsScreen(
    segments: List<RouteSegment>,
    segmentResults: List<SegmentEvaluationResult>,
    activeSegmentIndex: Int,
    onSelectSegment: (Int) -> Unit,
    onUpdateSegment: (Int, (RouteSegment) -> RouteSegment) -> Unit
) {
    val safeIndex = activeSegmentIndex.coerceIn(0, (segments.size - 1).coerceAtLeast(0))
    val segment = segments.getOrNull(safeIndex) ?: return
    val segEval = segmentResults.firstOrNull { it.segmentId == segment.id }
    val cond = segment.conditions
    val isUnderground = segment.methodCode == ReferenceMethodCode.D1 || segment.methodCode == ReferenceMethodCode.D2

    var airTempText by remember(segment.id) { mutableStateOf(cond.ambientAirTempC.toString()) }
    var groundTempText by remember(segment.id) { mutableStateOf(cond.groundTempC.toString()) }
    var groupCountText by remember(segment.id) { mutableStateOf(cond.circuitsGroupedCount.toString()) }
    var thermalLenText by remember(segment.id) { mutableStateOf(cond.thermalInsulationLengthMm.toString()) }
    var depthText by remember(segment.id) { mutableStateOf(cond.buriedDepthM.toString()) }
    var soilResText by remember(segment.id) { mutableStateOf(cond.soilThermalResistivityKmPerW.toString()) }

    var caOverrideText by remember(segment.id) { mutableStateOf(cond.manualFactors.caValue.toString()) }
    var cgOverrideText by remember(segment.id) { mutableStateOf(cond.manualFactors.cgValue.toString()) }
    var ciOverrideText by remember(segment.id) { mutableStateOf(cond.manualFactors.ciValue.toString()) }
    var cdOverrideText by remember(segment.id) { mutableStateOf(cond.manualFactors.cdValue.toString()) }
    var csOverrideText by remember(segment.id) { mutableStateOf(cond.manualFactors.csValue.toString()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Page 6 · Installation Conditions & Factors", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Set independent environmental conditions for each route segment. Only relevant factors for Method ${segment.methodCode.code} are applied.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    segments.forEachIndexed { idx, s ->
                        FilterChip(
                            selected = idx == safeIndex,
                            onClick = { onSelectSegment(idx) },
                            label = { Text("#${idx + 1}: ${s.label} (${s.methodCode.code})") }
                        )
                    }
                }
            }
        }

        item {
            EngineeringSectionCard(
                title = "Environmental & Grouping Conditions (${segment.label})",
                subtitle = "Method ${segment.methodCode.code} · ${if (isUnderground) "Underground Thermal Environment" else "Air / Building Envelope"}"
            ) {
                if (!isUnderground) {
                    EngineeringNumberField(
                        value = airTempText,
                        onValueChange = { txt ->
                            airTempText = txt
                            txt.toDoubleOrNull()?.let { t ->
                                onUpdateSegment(safeIndex) { it.copy(conditions = it.conditions.copy(ambientAirTempC = t)) }
                            }
                        },
                        label = "Ambient Air Temperature",
                        unit = "°C",
                        supportingText = "Reference baseline is 30°C"
                    )
                } else {
                    EngineeringNumberField(
                        value = groundTempText,
                        onValueChange = { txt ->
                            groundTempText = txt
                            txt.toDoubleOrNull()?.let { t ->
                                onUpdateSegment(safeIndex) { it.copy(conditions = it.conditions.copy(groundTempC = t)) }
                            }
                        },
                        label = "Ground Ambient Temperature",
                        unit = "°C",
                        supportingText = "Reference baseline is 20°C"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EngineeringNumberField(
                            value = depthText,
                            onValueChange = { txt ->
                                depthText = txt
                                txt.toDoubleOrNull()?.let { d ->
                                    onUpdateSegment(safeIndex) { it.copy(conditions = it.conditions.copy(buriedDepthM = d)) }
                                }
                            },
                            label = "Burial Depth",
                            unit = "m",
                            modifier = Modifier.weight(1f)
                        )
                        EngineeringNumberField(
                            value = soilResText,
                            onValueChange = { txt ->
                                soilResText = txt
                                txt.toDoubleOrNull()?.let { s ->
                                    onUpdateSegment(safeIndex) { it.copy(conditions = it.conditions.copy(soilThermalResistivityKmPerW = s)) }
                                }
                            },
                            label = "Soil Resistivity",
                            unit = "K·m/W",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EngineeringNumberField(
                        value = groupCountText,
                        onValueChange = { txt ->
                            groupCountText = txt
                            txt.toIntOrNull()?.let { g ->
                                onUpdateSegment(safeIndex) { it.copy(conditions = it.conditions.copy(circuitsGroupedCount = g.coerceAtLeast(1))) }
                            }
                        },
                        label = "Grouped Circuits",
                        isDecimal = false,
                        supportingText = "1 = isolated",
                        modifier = Modifier.weight(1f)
                    )
                    if (!isUnderground) {
                        EngineeringNumberField(
                            value = thermalLenText,
                            onValueChange = { txt ->
                                thermalLenText = txt
                                txt.toDoubleOrNull()?.let { len ->
                                    onUpdateSegment(safeIndex) { it.copy(conditions = it.conditions.copy(thermalInsulationLengthMm = len.coerceAtLeast(0.0))) }
                                }
                            },
                            label = "Thermal Insulation",
                            unit = "mm",
                            supportingText = "0 if none",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Factor Breakdown & Authorized Override Card
        item {
            EngineeringSectionCard(
                title = "Applied Correction Factors & Provenance",
                subtitle = "Combined C_total = ${segEval?.let { String.format(Locale.US, "%.3f", it.totalCorrectionFactor) } ?: "1.000"}"
            ) {
                segEval?.appliedFactors?.forEach { factor ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${factor.symbol} · ${factor.name}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = String.format(Locale.US, "%.3f", factor.value),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            VerificationPill(
                                status = factor.verificationStatus,
                                source = factor.dataSource
                            )
                            Text(
                                text = "Ref: ${factor.tableReference}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = factor.calculationNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enter Verified BS 7671 Factor Overrides", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Key in exact factors from your authorized BS 7671 Appendix 4 tables",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = cond.useManualFactorOverrides,
                        onCheckedChange = { checked ->
                            onUpdateSegment(safeIndex) {
                                it.copy(
                                    conditions = it.conditions.copy(
                                        useManualFactorOverrides = checked,
                                        manualFactors = it.conditions.manualFactors.copy(
                                            caSource = if (checked) DataSourceType.AUTHORIZED_BS7671 else DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT,
                                            cgSource = if (checked) DataSourceType.AUTHORIZED_BS7671 else DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT,
                                            ciSource = if (checked) DataSourceType.AUTHORIZED_BS7671 else DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT,
                                            cdSource = if (checked) DataSourceType.AUTHORIZED_BS7671 else DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT,
                                            csSource = if (checked) DataSourceType.AUTHORIZED_BS7671 else DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT
                                        )
                                    )
                                )
                            }
                        }
                    )
                }

                if (cond.useManualFactorOverrides) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EngineeringNumberField(
                            value = caOverrideText,
                            onValueChange = { txt ->
                                caOverrideText = txt
                                txt.toDoubleOrNull()?.let { v ->
                                    onUpdateSegment(safeIndex) {
                                        it.copy(conditions = it.conditions.copy(manualFactors = it.conditions.manualFactors.copy(caValue = v, caSource = DataSourceType.AUTHORIZED_BS7671)))
                                    }
                                }
                            },
                            label = "Ca (Temp)",
                            modifier = Modifier.weight(1f)
                        )
                        EngineeringNumberField(
                            value = cgOverrideText,
                            onValueChange = { txt ->
                                cgOverrideText = txt
                                txt.toDoubleOrNull()?.let { v ->
                                    onUpdateSegment(safeIndex) {
                                        it.copy(conditions = it.conditions.copy(manualFactors = it.conditions.manualFactors.copy(cgValue = v, cgSource = DataSourceType.AUTHORIZED_BS7671)))
                                    }
                                }
                            },
                            label = "Cg (Grouping)",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EngineeringNumberField(
                            value = ciOverrideText,
                            onValueChange = { txt ->
                                ciOverrideText = txt
                                txt.toDoubleOrNull()?.let { v ->
                                    onUpdateSegment(safeIndex) {
                                        it.copy(conditions = it.conditions.copy(manualFactors = it.conditions.manualFactors.copy(ciValue = v, ciSource = DataSourceType.AUTHORIZED_BS7671)))
                                    }
                                }
                            },
                            label = "Ci (Thermal Ins)",
                            modifier = Modifier.weight(1f)
                        )
                        if (isUnderground) {
                            EngineeringNumberField(
                                value = cdOverrideText,
                                onValueChange = { txt ->
                                    cdOverrideText = txt
                                    txt.toDoubleOrNull()?.let { v ->
                                        onUpdateSegment(safeIndex) {
                                            it.copy(conditions = it.conditions.copy(manualFactors = it.conditions.manualFactors.copy(cdValue = v, cdSource = DataSourceType.AUTHORIZED_BS7671)))
                                        }
                                    }
                                },
                                label = "Cd (Depth)",
                                modifier = Modifier.weight(1f)
                            )
                            EngineeringNumberField(
                                value = csOverrideText,
                                onValueChange = { txt ->
                                    csOverrideText = txt
                                    txt.toDoubleOrNull()?.let { v ->
                                        onUpdateSegment(safeIndex) {
                                            it.copy(conditions = it.conditions.copy(manualFactors = it.conditions.manualFactors.copy(csValue = v, csSource = DataSourceType.AUTHORIZED_BS7671)))
                                        }
                                    }
                                },
                                label = "Cs (Soil)",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
