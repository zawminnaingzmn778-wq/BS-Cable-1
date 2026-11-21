package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.domain.model.CableArrangement
import com.example.domain.model.CableConstruction
import com.example.domain.model.CableSpecInput
import com.example.domain.model.ConductorMaterial
import com.example.domain.model.CurrentSystemType
import com.example.domain.model.InsulationType
import com.example.domain.model.SizingMode
import com.example.domain.standards.GenericStandardsDataset
import com.example.ui.components.EngineeringNumberField
import com.example.ui.components.EngineeringSectionCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Page3CableCharacteristicsScreen(
    cable: CableSpecInput,
    systemType: CurrentSystemType,
    onUpdate: ((CableSpecInput) -> CableSpecInput) -> Unit
) {
    var parallelText by remember { mutableStateOf(cable.parallelRuns.toString()) }
    var coreText by remember(cable.cableConstruction) { mutableStateOf(cable.coreCount.toString()) }
    var loadedText by remember(cable.cableConstruction) { mutableStateOf(cable.loadedConductors.toString()) }
    var opTempText by remember(cable.insulationType) { mutableStateOf(cable.maxOperatingTempC.toInt().toString()) }
    var customRText by remember { mutableStateOf(cable.customResistanceOhmPerKm?.toString() ?: "") }
    var customXText by remember { mutableStateOf(cable.customReactanceOhmPerKm?.toString() ?: "") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Page 3 · Cable Characteristics", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Define conductor material, insulation rating, cable construction, parallel runs, and sizing mode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Mode A vs Mode B
        item {
            EngineeringSectionCard(
                title = "Operating Mode",
                subtitle = "Automatic selects the smallest compliant cable; Manual verifies a specific CSA"
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SizingMode.entries.forEachIndexed { idx, mode ->
                        SegmentedButton(
                            selected = cable.sizingMode == mode,
                            onClick = { onUpdate { it.copy(sizingMode = mode) } },
                            shape = SegmentedButtonDefaults.itemShape(idx, SizingMode.entries.size)
                        ) {
                            Text(mode.label)
                        }
                    }
                }

                if (cable.sizingMode == SizingMode.MANUAL) {
                    Text(
                        text = "Select Manual Cable Cross-Sectional Area (mm²):",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GenericStandardsDataset.standardCsaSizesMm2.forEach { size ->
                            FilterChip(
                                selected = kotlin.math.abs(cable.manualCableSizeMm2 - size) < 0.01,
                                onClick = { onUpdate { it.copy(manualCableSizeMm2 = size) } },
                                label = {
                                    Text(
                                        if (size % 1.0 == 0.0) "${size.toInt()} mm²" else "$size mm²"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            EngineeringSectionCard(
                title = "Conductor & Insulation",
                subtitle = "Select conductor metal and thermal rating"
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ConductorMaterial.entries.forEachIndexed { idx, mat ->
                        SegmentedButton(
                            selected = cable.conductorMaterial == mat,
                            onClick = { onUpdate { it.copy(conductorMaterial = mat) } },
                            shape = SegmentedButtonDefaults.itemShape(idx, ConductorMaterial.entries.size)
                        ) {
                            Text("${mat.label} (${mat.symbol})")
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    InsulationType.entries.forEach { ins ->
                        FilterChip(
                            selected = cable.insulationType == ins,
                            onClick = {
                                opTempText = ins.defaultMaxTempC.toInt().toString()
                                onUpdate {
                                    it.copy(
                                        insulationType = ins,
                                        maxOperatingTempC = ins.defaultMaxTempC
                                    )
                                }
                            },
                            label = { Text(ins.label) }
                        )
                    }
                }
            }
        }

        item {
            EngineeringSectionCard(
                title = "Cable Construction & Core Arrangement",
                subtitle = "Drives reference installation method compatibility on Page 5"
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CableConstruction.entries.forEach { cons ->
                        FilterChip(
                            selected = cable.cableConstruction == cons,
                            onClick = {
                                val defaultCores = when (cons) {
                                    CableConstruction.SINGLE_CORE_NON_ARMOURED,
                                    CableConstruction.SINGLE_CORE_ARMOURED_AWA,
                                    CableConstruction.SOLAR_DC_DOUBLE_INSULATED -> 1
                                    CableConstruction.MULTICORE_NON_ARMOURED -> 3
                                    CableConstruction.MULTICORE_ARMOURED_SWA -> 4
                                }
                                coreText = defaultCores.toString()
                                onUpdate { it.copy(cableConstruction = cons, coreCount = defaultCores) }
                            },
                            label = { Text(cons.label, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EngineeringNumberField(
                        value = coreText,
                        onValueChange = { txt ->
                            coreText = txt
                            txt.toIntOrNull()?.let { c -> onUpdate { it.copy(coreCount = c.coerceIn(1, 5)) } }
                        },
                        label = "Cores / Cable",
                        isDecimal = false,
                        modifier = Modifier.weight(1f)
                    )
                    EngineeringNumberField(
                        value = loadedText,
                        onValueChange = { txt ->
                            loadedText = txt
                            txt.toIntOrNull()?.let { l -> onUpdate { it.copy(loadedConductors = l.coerceIn(1, 4)) } }
                        },
                        label = "Loaded Conductors",
                        isDecimal = false,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EngineeringNumberField(
                        value = parallelText,
                        onValueChange = { txt ->
                            parallelText = txt
                            txt.toIntOrNull()?.let { p -> onUpdate { it.copy(parallelRuns = p.coerceAtLeast(1)) } }
                        },
                        label = "Parallel Runs / Phase",
                        isDecimal = false,
                        modifier = Modifier.weight(1f)
                    )
                    EngineeringNumberField(
                        value = opTempText,
                        onValueChange = { txt ->
                            opTempText = txt
                            txt.toDoubleOrNull()?.let { t -> onUpdate { it.copy(maxOperatingTempC = t) } }
                        },
                        label = "Max Conductor Temp",
                        unit = "°C",
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Cable Formation / Arrangement", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CableArrangement.entries.forEach { arr ->
                        FilterChip(
                            selected = cable.arrangement == arr,
                            onClick = { onUpdate { it.copy(arrangement = arr) } },
                            label = { Text(arr.label, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }
        }

        item {
            EngineeringSectionCard(
                title = "Cable-Specific Impedance Override (Optional)",
                subtitle = "Use manufacturer datasheet R (20°C) and X (50 Hz) when available"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Override database R & X values", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = cable.useCustomImpedance,
                        onCheckedChange = { checked -> onUpdate { it.copy(useCustomImpedance = checked) } }
                    )
                }

                if (cable.useCustomImpedance) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EngineeringNumberField(
                            value = customRText,
                            onValueChange = { txt ->
                                customRText = txt
                                onUpdate { it.copy(customResistanceOhmPerKm = txt.toDoubleOrNull()) }
                            },
                            label = "R at 20°C",
                            unit = "Ω/km",
                            modifier = Modifier.weight(1f)
                        )
                        if (systemType == CurrentSystemType.AC) {
                            EngineeringNumberField(
                                value = customXText,
                                onValueChange = { txt ->
                                    customXText = txt
                                    onUpdate { it.copy(customReactanceOhmPerKm = txt.toDoubleOrNull()) }
                                },
                                label = "Reactance X",
                                unit = "Ω/km",
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
