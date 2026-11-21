package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.domain.model.CandidateDecision
import com.example.domain.model.CandidateEvaluation
import com.example.domain.model.CheckStatus
import com.example.domain.model.CircuitDesignProject
import com.example.domain.model.EngineeringCalculationResult
import com.example.domain.model.SizingMode
import com.example.ui.components.EngineeringSectionCard
import com.example.ui.components.MonoMetricTile
import com.example.ui.components.StatusBadge
import com.example.ui.components.VerificationPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.util.Locale

@Composable
fun Page8ResultsScreen(
    project: CircuitDesignProject,
    result: EngineeringCalculationResult,
    onOpenReport: () -> Unit
) {
    val activeCandidate = if (project.cable.sizingMode == SizingMode.AUTOMATIC) {
        result.recommendedCandidate ?: result.evaluatedManualCandidate
    } else {
        result.evaluatedManualCandidate
    }

    var expandedTraceIds by remember { mutableStateOf(setOf(1, 2)) }

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
                    Text("Page 8 · Engineering Results", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "${project.cable.sizingMode.label} · ${project.standardEdition}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(status = result.overallStatus, modifier = Modifier.testTag("overall_status_badge"))
            }
        }

        // Standards Provenance Notice
        item {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VerificationPill(status = result.dataVerificationLevel)
                    Text(
                        text = result.standardsVerificationNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Validation Errors if any
        if (result.validationErrors.isNotEmpty()) {
            item {
                EngineeringSectionCard(title = "Validation Errors") {
                    result.validationErrors.forEach { err ->
                        Text("• $err", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Key Result Summary Tiles
        item {
            EngineeringSectionCard(
                title = if (project.cable.sizingMode == SizingMode.AUTOMATIC) {
                    "Recommended Cable Summary"
                } else {
                    "Manual Cable Verification Summary"
                },
                subtitle = "${project.cable.conductorMaterial.label} · ${project.cable.insulationType.label} · ${project.cable.cableConstruction.shortLabel}"
            ) {
                val cableHeadline = when {
                    project.cable.sizingMode == SizingMode.AUTOMATIC && result.recommendedCandidate != null -> {
                        val c = result.recommendedCandidate
                        "${formatSize(c.sizeMm2)} mm² (${c.parallelRuns}×)"
                    }
                    project.cable.sizingMode == SizingMode.MANUAL -> {
                        "${formatSize(project.cable.manualCableSizeMm2)} mm² (${project.cable.parallelRuns}×)"
                    }
                    else -> "No Compliant Size"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MonoMetricTile(
                        label = if (project.cable.sizingMode == SizingMode.AUTOMATIC) "Recommended Size" else "Evaluated Size",
                        value = cableHeadline,
                        unit = "",
                        highlightColor = if (result.overallStatus == CheckStatus.FAIL) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("recommended_cable_tile")
                    )
                    MonoMetricTile(
                        label = "Design Current (Ib)",
                        value = result.designCurrentIbA?.let { fmt(it) } ?: "-",
                        unit = "A",
                        modifier = Modifier.weight(0.8f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MonoMetricTile(
                        label = "Corrected Iz",
                        value = activeCandidate?.correctedIzA?.let { fmt(it) } ?: "-",
                        unit = "A",
                        modifier = Modifier.weight(1f)
                    )
                    MonoMetricTile(
                        label = "Voltage Drop",
                        value = activeCandidate?.voltageDropV?.let { fmt(it) } ?: "-",
                        unit = activeCandidate?.voltageDropPct?.let { "V (${fmt(it)}%)" } ?: "V",
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Route: ${fmt(result.totalRouteLengthM)} m across ${result.segmentResults.size} segment(s) · Governing: ${result.governingSegment?.let { "${it.label} (Method ${it.methodCode.code})" } ?: "-"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (activeCandidate != null) {
                    Text(
                        text = activeCandidate.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = onOpenReport,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_full_report_button")
                ) {
                    Icon(Icons.Filled.Assessment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Detailed Engineering Report & PDF")
                }
            }
        }

        // Individual Verification Checks
        item {
            Text("Individual Verification Checks", style = MaterialTheme.typography.titleMedium)
        }

        items(result.individualChecks, key = { it.id }) { chk ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(chk.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusBadge(status = chk.status)
                    }
                    Text(
                        text = "Req: ${chk.requirementText}  |  Actual: ${chk.calculatedText}",
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = chk.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Ref: ${chk.standardRef}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Selected & Rejected Cable Comparison
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Selected & Rejected Cable Comparison", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Every candidate size evaluated across all route segments and protection criteria.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(result.candidateComparisons, key = { it.cableEntry.cableId }) { cand ->
            CandidateComparisonCard(cand = cand)
        }

        // Step-by-Step Calculation Traceability
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Calculation Traceability (Step-by-Step)", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Tap any calculation step to inspect formula, variable definitions, and numerical substitution.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(result.calculationTraces, key = { it.stepNumber }) { trace ->
            val expanded = expandedTraceIds.contains(trace.stepNumber)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        expandedTraceIds = if (expanded) {
                            expandedTraceIds - trace.stepNumber
                        } else {
                            expandedTraceIds + trace.stepNumber
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Step ${trace.stepNumber} · ${trace.calculationName}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "${trace.finalResult} ${trace.unit}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusBadge(status = trace.outcome)
                            Icon(
                                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = "Toggle trace details"
                            )
                        }
                    }

                    AnimatedVisibility(visible = expanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            HorizontalDivider()
                            Text("Formula: ${trace.formula}", fontFamily = JetBrainsMonoFontFamily, style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "Variables:\n" + trace.variableDefinitions.joinToString("\n") { "  • $it" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("Inputs: ${trace.inputValues}", style = MaterialTheme.typography.bodySmall)
                            Text("Substitution: ${trace.substitution}", fontFamily = JetBrainsMonoFontFamily, style = MaterialTheme.typography.labelSmall)
                            Text("Intermediate: ${trace.intermediateResult}", style = MaterialTheme.typography.bodySmall)
                            Text("Result: ${trace.finalResult} ${trace.unit}", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            VerificationPill(status = trace.verificationStatus, source = trace.dataSource)
                            Text("Standard Ref: ${trace.standardReference}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun CandidateComparisonCard(cand: CandidateEvaluation) {
    val borderColor = when (cand.decision) {
        CandidateDecision.SELECTED -> MaterialTheme.colorScheme.primary
        CandidateDecision.ACCEPTED_ALTERNATIVE -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
        CandidateDecision.REJECTED -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }

    val decisionStatus = when (cand.decision) {
        CandidateDecision.SELECTED, CandidateDecision.ACCEPTED_ALTERNATIVE -> CheckStatus.PASS
        CandidateDecision.REJECTED -> if (cand.rejectionReasons.any { it.name == "MISSING_VERIFICATION_DATA" } && cand.rejectionReasons.size == 1) {
            CheckStatus.NOT_VERIFIED
        } else {
            CheckStatus.FAIL
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("candidate_card_${formatSize(cand.sizeMm2)}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (cand.decision == CandidateDecision.SELECTED) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(if (cand.decision == CandidateDecision.SELECTED) 1.8.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatSize(cand.sizeMm2)} mm² (${cand.parallelRuns}×) — ${cand.decision.label}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(status = decisionStatus)
            }
            Text(
                text = "It=${cand.tabulatedItA?.let { fmt(it) } ?: "?"} A · Cmin=${String.format(Locale.US, "%.3f", cand.minTotalCorrectionFactor)} · Iz=${cand.correctedIzA?.let { fmt(it) } ?: "?"} A · ΔV=${cand.voltageDropPct?.let { fmt(it) } ?: "?"}% · Smin=${cand.adiabaticMinCsaMm2?.let { fmt(it) } ?: "?"} mm²",
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = cand.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = if (cand.decision == CandidateDecision.REJECTED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatSize(s: Double): String = if (s % 1.0 == 0.0) s.toInt().toString() else s.toString()
private fun fmt(v: Double): String = String.format(Locale.US, "%.2f", v)
