package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.domain.model.CircuitDesignProject
import com.example.domain.model.EngineeringCalculationResult
import com.example.pdf.PdfReportGenerator
import com.example.ui.components.EngineeringSectionCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.VerificationPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.util.Locale

@Composable
fun Page9ReportScreen(
    project: CircuitDesignProject,
    result: EngineeringCalculationResult,
    onSaveSnapshot: () -> Unit
) {
    val context = LocalContext.current
    var pdfFeedback by remember { mutableStateOf<String?>(null) }

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    PdfReportGenerator.writePdf(project, result, out)
                }
            }.onSuccess {
                pdfFeedback = "PDF engineering report exported successfully."
            }.onFailure { err ->
                pdfFeedback = "PDF export failed: ${err.message}"
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Page 9 · Engineering Report", style = MaterialTheme.typography.headlineSmall)
                    StatusBadge(status = result.overallStatus)
                }
                Text(
                    text = "Comprehensive traceable calculation report ready for PDF export or archiving.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // PDF & Snapshot Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val circuit = project.metadata.circuitId.ifBlank { "C1" }
                            createPdfLauncher.launch("BS7671_Cable_Report_$circuit.pdf")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_pdf_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save PDF")
                    }
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                val file = PdfReportGenerator.generateToFile(context, project, result)
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, "BS 7671 Cable Report - ${project.metadata.circuitId}")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Engineering PDF"))
                            }.onFailure { err ->
                                pdfFeedback = "Share failed: ${err.message}"
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_pdf_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share PDF")
                    }
                }
                OutlinedButton(
                    onClick = onSaveSnapshot,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Archive Report Snapshot to Saved Reports")
                }

                pdfFeedback?.let { msg ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // Report Preview Document
        item {
            EngineeringSectionCard(
                title = "1. Project & Electrical Design Summary",
                subtitle = "${project.metadata.projectName.ifBlank { "Untitled" }} · Circuit ${project.metadata.circuitId}"
            ) {
                VerificationPill(status = result.dataVerificationLevel)
                Text("Standard: ${project.standardEdition}", style = MaterialTheme.typography.bodySmall)
                Text("Client / Site: ${project.metadata.client.ifBlank { "-" }} / ${project.metadata.siteLocation.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall)
                Text("Engineer / Date: ${project.metadata.engineerName.ifBlank { "-" }} / ${project.metadata.designDate.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Text(
                    text = "System: ${project.system.systemType.label}, ${fmt(project.system.nominalVoltageV)} V · Ib = ${result.designCurrentIbA?.let { fmt(it) } ?: "?"} A",
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "Cable: ${project.cable.conductorMaterial.label}, ${project.cable.insulationType.label}, ${project.cable.cableConstruction.label} (${project.cable.parallelRuns}× runs)",
                    style = MaterialTheme.typography.bodySmall
                )
                val active = result.recommendedCandidate ?: result.evaluatedManualCandidate
                Text(
                    text = "Evaluated / Recommended Size: ${active?.let { "${it.sizeMm2} mm² (${it.decision.label})" } ?: "None compliant"}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        item {
            EngineeringSectionCard(
                title = "2. Route Segments & Correction Factors",
                subtitle = "Total length ${fmt(result.totalRouteLengthM)} m"
            ) {
                result.segmentResults.forEach { seg ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "${seg.orderIndex + 1}. ${seg.label} (${fmt(seg.lengthMeters)} m · Method ${seg.methodCode.code})${if (seg.isGoverningAmpacity) " [GOVERNING]" else ""}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        val fList = seg.appliedFactors.joinToString(", ") { "${it.symbol}=${String.format(Locale.US, "%.3f", it.value)}" }
                        Text(
                            text = "It=${seg.tabulatedItA?.let { fmt(it) } ?: "?"} A | $fList | C_tot=${String.format(Locale.US, "%.3f", seg.totalCorrectionFactor)} | Iz=${seg.effectiveTotalIzA?.let { fmt(it) } ?: "?"} A | ΔV=${seg.segmentVoltageDropV?.let { fmt(it) } ?: "?"} V",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider()
                }
            }
        }

        item {
            EngineeringSectionCard(
                title = "3. Traceable Formulas & Substitutions",
                subtitle = "${result.calculationTraces.size} engineering verification steps"
            ) {
                result.calculationTraces.forEach { tr ->
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("${tr.stepNumber}. ${tr.calculationName} [${tr.outcome.label}]", style = MaterialTheme.typography.titleSmall)
                        Text("Formula: ${tr.formula}", fontFamily = JetBrainsMonoFontFamily, style = MaterialTheme.typography.labelSmall)
                        Text("Substitution: ${tr.substitution} = ${tr.finalResult} ${tr.unit}", fontFamily = JetBrainsMonoFontFamily, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text("Ref: ${tr.standardReference}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                }
            }
        }

        item {
            EngineeringSectionCard(
                title = "4. Engineering Assumptions & Warnings",
                subtitle = "Review prior to final sign-off"
            ) {
                result.warnings.forEach { w ->
                    Text("⚠ $w", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                result.engineeringAssumptions.forEach { a ->
                    Text("• $a", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

private fun fmt(v: Double): String = String.format(Locale.US, "%.2f", v)
