package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.CircuitDesignProject
import com.example.domain.model.EngineeringCalculationResult
import com.example.domain.model.SizingMode
import com.example.ui.components.StatusBadge
import com.example.ui.viewmodel.MainEngineeringViewModel

private val WizardPageTitles = listOf(
    "1. Project",
    "2. System",
    "3. Cable",
    "4. Route",
    "5. Method",
    "6. Factors",
    "7. Protection",
    "8. Results",
    "9. Report"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardShellScreen(
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
    onNavigateHome: () -> Unit,
    viewModel: MainEngineeringViewModel,
    project: CircuitDesignProject,
    calcResult: EngineeringCalculationResult,
    activeSegmentIndex: Int,
    statusMessage: String?
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        if (!statusMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(statusMessage)
            viewModel.clearStatusMessage()
        }
    }

    BackHandler {
        if (currentPage > 0) {
            onPageSelected(currentPage - 1)
        } else {
            onNavigateHome()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateHome,
                            modifier = Modifier.testTag("wizard_home_button")
                        ) {
                            Icon(Icons.Filled.Home, contentDescription = "Back to Dashboard")
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = project.metadata.circuitId.ifBlank { "C1" } + " · " +
                                    project.metadata.projectName.ifBlank { "Untitled Project" },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Step ${currentPage + 1} of 9 · ${project.cable.sizingMode.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        StatusBadge(status = calcResult.overallStatus)
                        IconButton(
                            onClick = { viewModel.saveActiveProject() },
                            modifier = Modifier.testTag("save_project_button")
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = "Save Project")
                        }
                    }
                )
                LinearProgressIndicator(
                    progress = { (currentPage + 1) / 9f },
                    modifier = Modifier.fillMaxWidth()
                )
                // Horizontal quick-step navigation strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WizardPageTitles.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = idx == currentPage,
                            onClick = { onPageSelected(idx) },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (idx == currentPage) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            modifier = Modifier.testTag("wizard_tab_${idx + 1}")
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (currentPage > 0) onPageSelected(currentPage - 1) else onNavigateHome()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("wizard_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (currentPage == 0) "Dashboard" else "Back")
                    }

                    // Quick Mode Chip
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        onClick = {
                            val toggled = if (project.cable.sizingMode == SizingMode.AUTOMATIC) {
                                SizingMode.MANUAL
                            } else {
                                SizingMode.AUTOMATIC
                            }
                            viewModel.setSizingMode(toggled)
                        }
                    ) {
                        Text(
                            text = if (project.cable.sizingMode == SizingMode.AUTOMATIC) "Mode: Auto" else "Mode: Manual",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (currentPage < 8) {
                                onPageSelected(currentPage + 1)
                            } else {
                                viewModel.saveReportSnapshot()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("wizard_next_button")
                    ) {
                        Text(
                            when (currentPage) {
                                6 -> "Calculate"
                                8 -> "Save Snapshot"
                                else -> "Next"
                            }
                        )
                        if (currentPage < 8) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp)
            ) {
                when (currentPage) {
                    0 -> Page1ProjectInfoScreen(
                        metadata = project.metadata,
                        standardEdition = project.standardEdition,
                        onUpdate = { viewModel.updateMetadata(it) }
                    )
                    1 -> Page2ElectricalSystemScreen(
                        system = project.system,
                        calculatedIbA = calcResult.designCurrentIbA,
                        validationErrors = calcResult.validationErrors,
                        onUpdate = { viewModel.updateSystem(it) }
                    )
                    2 -> Page3CableCharacteristicsScreen(
                        cable = project.cable,
                        systemType = project.system.systemType,
                        onUpdate = { viewModel.updateCable(it) }
                    )
                    3 -> Page4RouteSegmentsScreen(
                        segments = project.segments,
                        segmentResults = calcResult.segmentResults,
                        totalLengthM = calcResult.totalRouteLengthM,
                        activeSegmentIndex = activeSegmentIndex,
                        onSelectSegment = { viewModel.selectActiveSegment(it) },
                        onAddSegment = { viewModel.addSegment() },
                        onUpdateSegment = { idx, fn -> viewModel.updateSegment(idx, fn) },
                        onDuplicateSegment = { viewModel.duplicateSegment(it) },
                        onDeleteSegment = { viewModel.deleteSegment(it) },
                        onMoveSegment = { idx, delta -> viewModel.moveSegment(idx, delta) },
                        onPickMethodForSegment = { idx ->
                            viewModel.selectActiveSegment(idx)
                            onPageSelected(4)
                        },
                        onEditConditionsForSegment = { idx ->
                            viewModel.selectActiveSegment(idx)
                            onPageSelected(5)
                        }
                    )
                    4 -> Page5ReferenceMethodScreen(
                        segments = project.segments,
                        activeSegmentIndex = activeSegmentIndex,
                        cableConstruction = project.cable.cableConstruction,
                        onSelectSegment = { viewModel.selectActiveSegment(it) },
                        onSelectMethodForSegment = { idx, code ->
                            viewModel.updateSegment(idx) { it.copy(methodCode = code) }
                        }
                    )
                    5 -> Page6ConditionsFactorsScreen(
                        segments = project.segments,
                        segmentResults = calcResult.segmentResults,
                        activeSegmentIndex = activeSegmentIndex,
                        onSelectSegment = { viewModel.selectActiveSegment(it) },
                        onUpdateSegment = { idx, fn -> viewModel.updateSegment(idx, fn) }
                    )
                    6 -> Page7ProtectionLimitsScreen(
                        systemType = project.system.systemType,
                        protection = project.protection,
                        onUpdate = { viewModel.updateProtection(it) }
                    )
                    7 -> Page8ResultsScreen(
                        project = project,
                        result = calcResult,
                        onOpenReport = { onPageSelected(8) }
                    )
                    8 -> Page9ReportScreen(
                        project = project,
                        result = calcResult,
                        onSaveSnapshot = { viewModel.saveReportSnapshot() }
                    )
                }
            }
        }
    }
}
