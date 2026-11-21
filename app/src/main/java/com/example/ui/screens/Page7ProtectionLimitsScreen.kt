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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.domain.model.ConductorMaterial
import com.example.domain.model.CurrentSystemType
import com.example.domain.model.DataSourceType
import com.example.domain.model.ProtectionAndLimitsInput
import com.example.domain.model.ProtectiveDeviceType
import com.example.ui.components.EngineeringNumberField
import com.example.ui.components.EngineeringSectionCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Page7ProtectionLimitsScreen(
    systemType: CurrentSystemType,
    protection: ProtectionAndLimitsInput,
    onUpdate: ((ProtectionAndLimitsInput) -> ProtectionAndLimitsInput) -> Unit
) {
    var inText by remember { mutableStateOf(protection.ratedCurrentInA.toString()) }
    var irText by remember { mutableStateOf(protection.deviceSettingIrA?.toString() ?: "") }
    var icuText by remember { mutableStateOf(protection.breakingCapacityKa.toString()) }
    var i2RatioText by remember(protection.deviceType) { mutableStateOf(protection.overloadFactorI2Ratio.toString()) }
    var ikText by remember { mutableStateOf(protection.prospectiveFaultCurrentIkKa.toString()) }
    var faultTimeText by remember { mutableStateOf(protection.faultDurationS.toString()) }
    var discTimeText by remember { mutableStateOf(protection.disconnectionTimeS.toString()) }
    var vdLimitText by remember { mutableStateOf(protection.voltageDropLimitPct.toString()) }
    var maxZsText by remember { mutableStateOf(protection.maxEarthFaultLoopImpedanceZsOhm?.toString() ?: "") }
    var actualZsText by remember { mutableStateOf(protection.actualZsOhm?.toString() ?: "") }
    var cpcSizeText by remember { mutableStateOf(protection.cpcSizeMm2?.toString() ?: "") }
    var kOverrideText by remember { mutableStateOf(protection.kValueOverride?.toString() ?: "") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Page 7 · Protection & Design Limits", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Configure protective device coordination (Ib ≤ In ≤ Iz, I2 ≤ 1.45×Iz), voltage drop limit, and adiabatic short-circuit criteria.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            EngineeringSectionCard(
                title = "Protective Device Selection",
                subtitle = "Device options filtered for ${systemType.label}"
            ) {
                val devices = ProtectiveDeviceType.entries.filter {
                    if (systemType == CurrentSystemType.AC) it.supportsAc else it.supportsDc
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    devices.forEach { dev ->
                        FilterChip(
                            selected = protection.deviceType == dev,
                            onClick = {
                                i2RatioText = dev.defaultOverloadRatioI2ToIn.toString()
                                onUpdate {
                                    it.copy(
                                        deviceType = dev,
                                        overloadFactorI2Ratio = dev.defaultOverloadRatioI2ToIn
                                    )
                                }
                            },
                            label = { Text(dev.label, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EngineeringNumberField(
                        value = inText,
                        onValueChange = { txt ->
                            inText = txt
                            txt.toDoubleOrNull()?.let { v -> onUpdate { it.copy(ratedCurrentInA = v) } }
                        },
                        label = "Rated Current (In)",
                        unit = "A",
                        modifier = Modifier.weight(1f)
                    )
                    EngineeringNumberField(
                        value = irText,
                        onValueChange = { txt ->
                            irText = txt
                            onUpdate { it.copy(deviceSettingIrA = txt.toDoubleOrNull()) }
                        },
                        label = "Setting Ir (Optional)",
                        unit = "A",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EngineeringNumberField(
                        value = icuText,
                        onValueChange = { txt ->
                            icuText = txt
                            txt.toDoubleOrNull()?.let { v -> onUpdate { it.copy(breakingCapacityKa = v) } }
                        },
                        label = "Breaking Capacity (Icu)",
                        unit = "kA",
                        modifier = Modifier.weight(1f)
                    )
                    EngineeringNumberField(
                        value = i2RatioText,
                        onValueChange = { txt ->
                            i2RatioText = txt
                            txt.toDoubleOrNull()?.let { v -> onUpdate { it.copy(overloadFactorI2Ratio = v) } }
                        },
                        label = "I2 / In Ratio",
                        supportingText = "e.g. 1.45 MCB",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            EngineeringSectionCard(
                title = "Fault Current, Adiabatic & Voltage Drop Limits",
                subtitle = "Short-circuit withstand S = √(I²t) / k and cumulative ΔV%"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EngineeringNumberField(
                        value = vdLimitText,
                        onValueChange = { txt ->
                            vdLimitText = txt
                            txt.toDoubleOrNull()?.let { v -> onUpdate { it.copy(voltageDropLimitPct = v) } }
                        },
                        label = "Max Voltage Drop",
                        unit = "%",
                        supportingText = "e.g. 3% light, 5% power",
                        modifier = Modifier.weight(1f)
                    )
                    EngineeringNumberField(
                        value = ikText,
                        onValueChange = { txt ->
                            ikText = txt
                            txt.toDoubleOrNull()?.let { v -> onUpdate { it.copy(prospectiveFaultCurrentIkKa = v) } }
                        },
                        label = "Prospective Fault (Ik)",
                        unit = "kA",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EngineeringNumberField(
                        value = faultTimeText,
                        onValueChange = { txt ->
                            faultTimeText = txt
                            txt.toDoubleOrNull()?.let { v -> onUpdate { it.copy(faultDurationS = v) } }
                        },
                        label = "Fault Duration (t)",
                        unit = "s",
                        modifier = Modifier.weight(1f)
                    )
                    EngineeringNumberField(
                        value = discTimeText,
                        onValueChange = { txt ->
                            discTimeText = txt
                            txt.toDoubleOrNull()?.let { v -> onUpdate { it.copy(disconnectionTimeS = v) } }
                        },
                        label = "Disconnection Time",
                        unit = "s",
                        modifier = Modifier.weight(1f)
                    )
                }

                EngineeringNumberField(
                    value = kOverrideText,
                    onValueChange = { txt ->
                        kOverrideText = txt
                        onUpdate {
                            it.copy(
                                kValueOverride = txt.toDoubleOrNull(),
                                kValueSource = if (txt.toDoubleOrNull() != null) DataSourceType.AUTHORIZED_BS7671 else DataSourceType.GENERIC_MANUFACTURER_INDEPENDENT
                            )
                        }
                    },
                    label = "Verified k-Factor Override (Optional)",
                    supportingText = "Leave blank to use cable catalog k-value slot (verify against BS 7671 Table 43.1)"
                )
            }
        }

        item {
            EngineeringSectionCard(
                title = "Earth Fault Loop Impedance & CPC",
                subtitle = "Verify ADS disconnection and protective conductor sizing"
            ) {
                if (systemType == CurrentSystemType.AC) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EngineeringNumberField(
                            value = maxZsText,
                            onValueChange = { txt ->
                                maxZsText = txt
                                onUpdate { it.copy(maxEarthFaultLoopImpedanceZsOhm = txt.toDoubleOrNull()) }
                            },
                            label = "Max Permitted Zs",
                            unit = "Ω",
                            modifier = Modifier.weight(1f)
                        )
                        EngineeringNumberField(
                            value = actualZsText,
                            onValueChange = { txt ->
                                actualZsText = txt
                                onUpdate { it.copy(actualZsOhm = txt.toDoubleOrNull()) }
                            },
                            label = "Actual / Design Zs",
                            unit = "Ω",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EngineeringNumberField(
                        value = cpcSizeText,
                        onValueChange = { txt ->
                            cpcSizeText = txt
                            onUpdate { it.copy(cpcSizeMm2 = txt.toDoubleOrNull()) }
                        },
                        label = "CPC Size (Optional)",
                        unit = "mm²",
                        modifier = Modifier.weight(1f)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("CPC Material", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ConductorMaterial.entries.forEach { mat ->
                                FilterChip(
                                    selected = protection.cpcMaterial == mat,
                                    onClick = { onUpdate { it.copy(cpcMaterial = mat) } },
                                    label = { Text(mat.symbol) }
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
