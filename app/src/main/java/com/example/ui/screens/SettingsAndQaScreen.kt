package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.example.data.preferences.EngineeringAppSettings
import com.example.domain.engine.QaTestCaseResult
import com.example.domain.model.CheckStatus
import com.example.ui.components.EngineeringNumberField
import com.example.ui.components.EngineeringSectionCard
import com.example.ui.components.EngineeringTextField
import com.example.ui.components.StatusBadge
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsAndQaScreen(
    settings: EngineeringAppSettings,
    onUpdateSettings: ((EngineeringAppSettings) -> EngineeringAppSettings) -> Unit,
    onRunQaSuite: () -> List<QaTestCaseResult>,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var editionText by remember(settings.activeStandardEdition) { mutableStateOf(settings.activeStandardEdition) }
    var engineerText by remember(settings.defaultEngineerName) { mutableStateOf(settings.defaultEngineerName) }
    var vdPowerText by remember(settings.defaultVoltageDropPowerPct) { mutableStateOf(settings.defaultVoltageDropPowerPct.toString()) }
    var vdLightText by remember(settings.defaultVoltageDropLightingPct) { mutableStateOf(settings.defaultVoltageDropLightingPct.toString()) }
    var qaResults by remember { mutableStateOf<List<QaTestCaseResult>>(emptyList()) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Standards, Settings & QA Suite", style = MaterialTheme.typography.titleMedium) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    EngineeringSectionCard(
                        title = "BS 7671 Edition & Amendment Configuration",
                        subtitle = "Track active reference standard across reports and projects"
                    ) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("BS 7671:2018+A2:2022", "BS 7671:2018+A3:2024").forEach { preset ->
                                FilterChip(
                                    selected = settings.activeStandardEdition == preset,
                                    onClick = {
                                        editionText = preset
                                        onUpdateSettings { it.copy(activeStandardEdition = preset) }
                                    },
                                    label = { Text(preset) }
                                )
                            }
                        }
                        EngineeringTextField(
                            value = editionText,
                            onValueChange = { txt ->
                                editionText = txt
                                onUpdateSettings { it.copy(activeStandardEdition = txt) }
                            },
                            label = "Active Standard Reference String"
                        )
                        EngineeringTextField(
                            value = engineerText,
                            onValueChange = { txt ->
                                engineerText = txt
                                onUpdateSettings { it.copy(defaultEngineerName = txt) }
                            },
                            label = "Default Engineer Name"
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            EngineeringNumberField(
                                value = vdPowerText,
                                onValueChange = { txt ->
                                    vdPowerText = txt
                                    txt.toDoubleOrNull()?.let { v ->
                                        onUpdateSettings { it.copy(defaultVoltageDropPowerPct = v) }
                                    }
                                },
                                label = "Default Power ΔV",
                                unit = "%",
                                modifier = Modifier.weight(1f)
                            )
                            EngineeringNumberField(
                                value = vdLightText,
                                onValueChange = { txt ->
                                    vdLightText = txt
                                    txt.toDoubleOrNull()?.let { v ->
                                        onUpdateSettings { it.copy(defaultVoltageDropLightingPct = v) }
                                    }
                                },
                                label = "Default Lighting ΔV",
                                unit = "%",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text("Theme Appearance", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("SYSTEM" to "System Default", "LIGHT" to "Light", "DARK" to "Dark").forEach { (key, label) ->
                                FilterChip(
                                    selected = settings.themeMode == key,
                                    onClick = { onUpdateSettings { it.copy(themeMode = key) } },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }

                item {
                    EngineeringSectionCard(
                        title = "Engineering Quality Assurance & Verification Plan",
                        subtitle = "Run deterministic unit & hand-calculation comparison tests against the live engine"
                    ) {
                        Button(
                            onClick = { qaResults = onRunQaSuite() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("run_qa_suite_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Execute Engineering QA Verification Suite")
                        }
                    }
                }

                items(qaResults, key = { it.testId }) { qa ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${qa.testId} · ${qa.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                StatusBadge(status = if (qa.passed) CheckStatus.PASS else CheckStatus.FAIL)
                            }
                            Text("Inputs: ${qa.inputSummary}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "Expected: ${qa.expectedResult} | Actual: ${qa.actualResult} | Diff: ${qa.difference}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Source: ${qa.testSource}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}
