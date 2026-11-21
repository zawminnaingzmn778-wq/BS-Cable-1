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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.domain.model.AcPhaseConfiguration
import com.example.domain.model.CurrentSystemType
import com.example.domain.model.DcConfiguration
import com.example.domain.model.ElectricalSystemInput
import com.example.domain.model.LoadCharacteristic
import com.example.domain.model.LoadInputMode
import com.example.ui.components.EngineeringNumberField
import com.example.ui.components.EngineeringSectionCard
import com.example.ui.components.MonoMetricTile
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Page2ElectricalSystemScreen(
    system: ElectricalSystemInput,
    calculatedIbA: Double?,
    validationErrors: List<String>,
    onUpdate: ((ElectricalSystemInput) -> ElectricalSystemInput) -> Unit
) {
    var voltageText by remember(system.systemType, system.acPhaseConfig) {
        mutableStateOf(formatClean(system.nominalVoltageV))
    }
    var freqText by remember(system.systemType) {
        mutableStateOf(formatClean(system.designFrequencyHz))
    }
    var directIbText by remember(system.loadInputMode) {
        mutableStateOf(formatClean(system.directDesignCurrentA))
    }
    var kwText by remember(system.loadInputMode) {
        mutableStateOf(formatClean(system.activePowerKw))
    }
    var kvaText by remember(system.loadInputMode) {
        mutableStateOf(formatClean(system.apparentPowerKva))
    }
    var pfText by remember { mutableStateOf(formatClean(system.powerFactor)) }
    var effText by remember { mutableStateOf(formatClean(system.efficiencyPct)) }
    var opFactorText by remember { mutableStateOf(formatClean(system.continuousOperatingFactor)) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Page 2 · Electrical System & Design Current", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Configure AC or DC supply parameters. Irrelevant AC parameters are automatically omitted for DC circuits.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Live Design Current Preview
        item {
            MonoMetricTile(
                label = "Calculated Design Current (Ib)",
                value = calculatedIbA?.let { String.format(Locale.US, "%.2f", it) } ?: "Invalid",
                unit = "A",
                highlightColor = if (calculatedIbA != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            EngineeringSectionCard(
                title = "Supply Type & Configuration",
                subtitle = "Select LV AC or LV DC system architecture"
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    CurrentSystemType.entries.forEachIndexed { idx, type ->
                        SegmentedButton(
                            selected = system.systemType == type,
                            onClick = {
                                val nextVoltage = when {
                                    type == CurrentSystemType.DC -> 600.0
                                    system.acPhaseConfig == AcPhaseConfiguration.SINGLE_PHASE_2W -> 230.0
                                    else -> 400.0
                                }
                                val nextMode = if (type == CurrentSystemType.DC && system.loadInputMode == LoadInputMode.APPARENT_POWER_KVA) {
                                    LoadInputMode.ACTIVE_POWER_KW
                                } else {
                                    system.loadInputMode
                                }
                                voltageText = formatClean(nextVoltage)
                                onUpdate {
                                    it.copy(
                                        systemType = type,
                                        nominalVoltageV = nextVoltage,
                                        loadInputMode = nextMode
                                    )
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(idx, CurrentSystemType.entries.size)
                        ) {
                            Text(type.label)
                        }
                    }
                }

                if (system.systemType == CurrentSystemType.AC) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        AcPhaseConfiguration.entries.forEachIndexed { idx, cfg ->
                            SegmentedButton(
                                selected = system.acPhaseConfig == cfg,
                                onClick = {
                                    val defaultV = if (cfg == AcPhaseConfiguration.SINGLE_PHASE_2W) 230.0 else 400.0
                                    voltageText = formatClean(defaultV)
                                    onUpdate { it.copy(acPhaseConfig = cfg, nominalVoltageV = defaultV) }
                                },
                                shape = SegmentedButtonDefaults.itemShape(idx, AcPhaseConfiguration.entries.size)
                            ) {
                                Text(cfg.shortLabel)
                            }
                        }
                    }
                } else {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        DcConfiguration.entries.forEachIndexed { idx, cfg ->
                            SegmentedButton(
                                selected = system.dcConfig == cfg,
                                onClick = { onUpdate { it.copy(dcConfig = cfg) } },
                                shape = SegmentedButtonDefaults.itemShape(idx, DcConfiguration.entries.size)
                            ) {
                                Text(cfg.shortLabel)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EngineeringNumberField(
                        value = voltageText,
                        onValueChange = { txt ->
                            voltageText = txt
                            txt.toDoubleOrNull()?.let { v -> onUpdate { it.copy(nominalVoltageV = v) } }
                        },
                        label = if (system.systemType == CurrentSystemType.AC && system.acPhaseConfig == AcPhaseConfiguration.THREE_PHASE_3W_OR_4W) {
                            "Nominal Line Voltage (VL)"
                        } else {
                            "Nominal Voltage (Vn)"
                        },
                        unit = "V",
                        modifier = Modifier.weight(1f)
                    )

                    if (system.systemType == CurrentSystemType.AC) {
                        EngineeringNumberField(
                            value = freqText,
                            onValueChange = { txt ->
                                freqText = txt
                                txt.toDoubleOrNull()?.let { f -> onUpdate { it.copy(designFrequencyHz = f) } }
                            },
                            label = "Frequency",
                            unit = "Hz",
                            modifier = Modifier.weight(0.6f)
                        )
                    }
                }
            }
        }

        item {
            EngineeringSectionCard(
                title = "Load Input & Design Current Calculation",
                subtitle = "Enter Ib directly or calculate from connected load"
            ) {
                val modes = if (system.systemType == CurrentSystemType.DC) {
                    listOf(LoadInputMode.DIRECT_CURRENT_IB, LoadInputMode.ACTIVE_POWER_KW)
                } else {
                    LoadInputMode.entries
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    modes.forEach { mode ->
                        FilterChip(
                            selected = system.loadInputMode == mode,
                            onClick = { onUpdate { it.copy(loadInputMode = mode) } },
                            label = { Text(mode.label) }
                        )
                    }
                }

                when (system.loadInputMode) {
                    LoadInputMode.DIRECT_CURRENT_IB -> {
                        EngineeringNumberField(
                            value = directIbText,
                            onValueChange = { txt ->
                                directIbText = txt
                                txt.toDoubleOrNull()?.let { a -> onUpdate { it.copy(directDesignCurrentA = a) } }
                            },
                            label = "Design Current (Ib)",
                            unit = "A"
                        )
                    }
                    LoadInputMode.ACTIVE_POWER_KW -> {
                        EngineeringNumberField(
                            value = kwText,
                            onValueChange = { txt ->
                                kwText = txt
                                txt.toDoubleOrNull()?.let { p -> onUpdate { it.copy(activePowerKw = p) } }
                            },
                            label = "Active Power (P)",
                            unit = "kW"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (system.systemType == CurrentSystemType.AC) {
                                EngineeringNumberField(
                                    value = pfText,
                                    onValueChange = { txt ->
                                        pfText = txt
                                        txt.toDoubleOrNull()?.let { pf -> onUpdate { it.copy(powerFactor = pf) } }
                                    },
                                    label = "Power Factor (cos φ)",
                                    supportingText = "0.01 to 1.00",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            EngineeringNumberField(
                                value = effText,
                                onValueChange = { txt ->
                                    effText = txt
                                    txt.toDoubleOrNull()?.let { e -> onUpdate { it.copy(efficiencyPct = e) } }
                                },
                                label = "Efficiency (η)",
                                unit = "%",
                                supportingText = "100% if non-motor",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    LoadInputMode.APPARENT_POWER_KVA -> {
                        EngineeringNumberField(
                            value = kvaText,
                            onValueChange = { txt ->
                                kvaText = txt
                                txt.toDoubleOrNull()?.let { s -> onUpdate { it.copy(apparentPowerKva = s) } }
                            },
                            label = "Apparent Power (S)",
                            unit = "kVA"
                        )
                        EngineeringNumberField(
                            value = pfText,
                            onValueChange = { txt ->
                                pfText = txt
                                txt.toDoubleOrNull()?.let { pf -> onUpdate { it.copy(powerFactor = pf) } }
                            },
                            label = "Load Power Factor (for Voltage Drop)",
                            supportingText = "Used in R·cosφ + X·sinφ voltage drop formula"
                        )
                    }
                }

                EngineeringNumberField(
                    value = opFactorText,
                    onValueChange = { txt ->
                        opFactorText = txt
                        txt.toDoubleOrNull()?.let { k -> onUpdate { it.copy(continuousOperatingFactor = k) } }
                    },
                    label = "Continuous Operating / Diversity Factor",
                    supportingText = "1.00 standard; e.g. 1.25 for continuous PV/EV duty if required by design spec"
                )
            }
        }

        item {
            EngineeringSectionCard(
                title = "Load Characteristics",
                subtitle = "Documents circuit duty and voltage-drop reference context"
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LoadCharacteristic.entries.forEach { ch ->
                        FilterChip(
                            selected = system.loadCharacteristic == ch,
                            onClick = { onUpdate { it.copy(loadCharacteristic = ch) } },
                            label = { Text(ch.label, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }
        }

        if (validationErrors.isNotEmpty()) {
            item {
                EngineeringSectionCard(title = "Input Validation") {
                    validationErrors.forEach { err ->
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

private fun formatClean(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}
