package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.CableCatalogEntry
import com.example.domain.model.ConductorMaterial
import com.example.domain.model.DataSourceType
import com.example.domain.model.ReferenceMethodCode
import com.example.domain.model.VerificationStatus
import com.example.ui.components.EngineeringNumberField
import com.example.ui.components.EngineeringTextField
import com.example.ui.components.VerificationPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CableDatabaseScreen(
    entries: List<CableCatalogEntry>,
    statusMessage: String?,
    onClearMessage: () -> Unit,
    onUpsertEntry: (CableCatalogEntry) -> Unit,
    onImportJsonPack: (String) -> Unit,
    onExportJsonPack: () -> String,
    onResetBaseline: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var filterMaterial by remember { mutableStateOf<ConductorMaterial?>(null) }
    var editingEntry by remember { mutableStateOf<CableCatalogEntry?>(null) }
    var showPasteJsonDialog by remember { mutableStateOf(false) }

    LaunchedEffect(statusMessage) {
        if (!statusMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(statusMessage)
            onClearMessage()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.onSuccess { text ->
                if (!text.isNullOrBlank()) {
                    onImportJsonPack(text)
                }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val json = onExportJsonPack()
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) }
            }
        }
    }

    val filtered = entries.filter { filterMaterial == null || it.material == filterMaterial }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text("Generic Cable Database", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${entries.size} records · Standards-data provenance layer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onResetBaseline) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = "Reset to Generic Baseline")
                    }
                }
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Standards Data Import & Verification",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Copyrighted BS 7671 tables are not reproduced verbatim. Export the JSON template, populate lawfully obtained standard or manufacturer ratings, and import back with VERIFIED status.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import JSON")
                                }
                                OutlinedButton(
                                    onClick = { exportLauncher.launch("bs7671_cable_pack_template.json") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export JSON")
                                }
                            }
                            TextButton(onClick = { showPasteJsonDialog = true }) {
                                Text("Or paste JSON Standards Pack directly")
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filterMaterial == null,
                            onClick = { filterMaterial = null },
                            label = { Text("All (${entries.size})") }
                        )
                        ConductorMaterial.entries.forEach { mat ->
                            val count = entries.count { it.material == mat }
                            FilterChip(
                                selected = filterMaterial == mat,
                                onClick = { filterMaterial = mat },
                                label = { Text("${mat.label} ($count)") }
                            )
                        }
                    }
                }

                items(filtered, key = { it.cableId }) { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingEntry = entry },
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
                                Text(
                                    text = "${entry.sizeMm2} mm² ${entry.material.symbol} · ${entry.insulation.name} · ${entry.construction.shortLabel} (${entry.coreCount}C)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Edit & Verify Record",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            VerificationPill(status = entry.verificationStatus, source = entry.dataSource)

                            val ampSummary = entry.ampacityByMethod.entries
                                .sortedBy { it.key.ordinal }
                                .joinToString(" · ") { "${it.key.code}:${it.value.toInt()}A" }
                            Text(
                                text = ampSummary,
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "R20=${entry.resistanceOhmPerKm20C?.let { String.format(Locale.US, "%.3f", it) } ?: "-"} Ω/km · X=${entry.reactanceOhmPerKm50Hz?.let { String.format(Locale.US, "%.3f", it) } ?: "-"} Ω/km · k=${entry.kFactorAdiabatic ?: "-"}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Ref: ${entry.referenceIdentifier} (${entry.referenceEdition})",
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

    editingEntry?.let { entry ->
        EditCableEntryDialog(
            entry = entry,
            onDismiss = { editingEntry = null },
            onSave = { updated ->
                onUpsertEntry(updated)
                editingEntry = null
            }
        )
    }

    if (showPasteJsonDialog) {
        var jsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasteJsonDialog = false },
            title = { Text("Paste Standards Pack JSON") },
            text = {
                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    label = { Text("JSON Payload") },
                    minLines = 6,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onImportJsonPack(jsonInput)
                    showPasteJsonDialog = false
                }) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteJsonDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditCableEntryDialog(
    entry: CableCatalogEntry,
    onDismiss: () -> Unit,
    onSave: (CableCatalogEntry) -> Unit
) {
    var refIdText by remember { mutableStateOf(entry.referenceIdentifier) }
    var kText by remember { mutableStateOf(entry.kFactorAdiabatic?.toString() ?: "") }
    var rText by remember { mutableStateOf(entry.resistanceOhmPerKm20C?.toString() ?: "") }
    var xText by remember { mutableStateOf(entry.reactanceOhmPerKm50Hz?.toString() ?: "") }
    var selectedSource by remember { mutableStateOf(entry.dataSource) }
    var selectedVerification by remember { mutableStateOf(entry.verificationStatus) }
    var ampMap by remember { mutableStateOf(entry.ampacityByMethod) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit & Verify ${entry.sizeMm2} mm² ${entry.material.symbol}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EngineeringTextField(
                    value = refIdText,
                    onValueChange = { refIdText = it },
                    label = "Reference Table / Datasheet ID"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EngineeringNumberField(
                        value = rText,
                        onValueChange = { rText = it },
                        label = "R20 (Ω/km)",
                        modifier = Modifier.weight(1f)
                    )
                    EngineeringNumberField(
                        value = xText,
                        onValueChange = { xText = it },
                        label = "X (Ω/km)",
                        modifier = Modifier.weight(1f)
                    )
                    EngineeringNumberField(
                        value = kText,
                        onValueChange = { kText = it },
                        label = "k factor",
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Tabulated Ampacity by Reference Method (A)", style = MaterialTheme.typography.labelLarge)
                ampMap.keys.sortedBy { it.ordinal }.forEach { method ->
                    EngineeringNumberField(
                        value = ampMap[method]?.toString() ?: "",
                        onValueChange = { txt ->
                            val v = txt.toDoubleOrNull()
                            if (v != null) {
                                ampMap = ampMap.toMutableMap().apply { put(method, v) }
                            }
                        },
                        label = "Method ${method.code}",
                        unit = "A"
                    )
                }

                Text("Data Source Provenance", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DataSourceType.entries.forEach { src ->
                        FilterChip(
                            selected = selectedSource == src,
                            onClick = { selectedSource = src },
                            label = { Text(src.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text("Verification Status", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VerificationStatus.entries.forEach { st ->
                        FilterChip(
                            selected = selectedVerification == st,
                            onClick = { selectedVerification = st },
                            label = { Text(st.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    entry.copy(
                        referenceIdentifier = refIdText,
                        resistanceOhmPerKm20C = rText.toDoubleOrNull(),
                        reactanceOhmPerKm50Hz = xText.toDoubleOrNull(),
                        kFactorAdiabatic = kText.toDoubleOrNull(),
                        ampacityByMethod = ampMap,
                        dataSource = selectedSource,
                        verificationStatus = selectedVerification
                    )
                )
            }) {
                Text("Save Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
