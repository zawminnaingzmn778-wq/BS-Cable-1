package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.domain.model.ProjectMetadata
import com.example.ui.components.EngineeringSectionCard
import com.example.ui.components.EngineeringTextField

@Composable
fun Page1ProjectInfoScreen(
    metadata: ProjectMetadata,
    standardEdition: String,
    onUpdate: ((ProjectMetadata) -> ProjectMetadata) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Page 1 · Project & Circuit Information", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Document project identification and circuit details for the engineering report ($standardEdition).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            EngineeringSectionCard(
                title = "Project Identification",
                subtitle = "Appears in the report header and project index"
            ) {
                EngineeringTextField(
                    value = metadata.projectName,
                    onValueChange = { v -> onUpdate { it.copy(projectName = v) } },
                    label = "Project Name",
                    placeholder = "e.g., Commercial Building Sub-Main Distribution"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EngineeringTextField(
                        value = metadata.projectReference,
                        onValueChange = { v -> onUpdate { it.copy(projectReference = v) } },
                        label = "Project Reference",
                        placeholder = "PRJ-2026-01",
                        modifier = Modifier.weight(1f)
                    )
                    EngineeringTextField(
                        value = metadata.designDate,
                        onValueChange = { v -> onUpdate { it.copy(designDate = v) } },
                        label = "Design Date",
                        placeholder = "YYYY-MM-DD",
                        modifier = Modifier.weight(1f)
                    )
                }
                EngineeringTextField(
                    value = metadata.client,
                    onValueChange = { v -> onUpdate { it.copy(client = v) } },
                    label = "Client",
                    placeholder = "Client or organization name"
                )
                EngineeringTextField(
                    value = metadata.siteLocation,
                    onValueChange = { v -> onUpdate { it.copy(siteLocation = v) } },
                    label = "Site / Location",
                    placeholder = "Facility address or plant area"
                )
                EngineeringTextField(
                    value = metadata.engineerName,
                    onValueChange = { v -> onUpdate { it.copy(engineerName = v) } },
                    label = "Engineer Name",
                    placeholder = "Designing electrical engineer"
                )
            }
        }

        item {
            EngineeringSectionCard(
                title = "Circuit Details & Design Notes",
                subtitle = "Traceable circuit identifier and engineering remarks"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EngineeringTextField(
                        value = metadata.circuitId,
                        onValueChange = { v -> onUpdate { it.copy(circuitId = v) } },
                        label = "Circuit ID",
                        placeholder = "DB01-C1",
                        modifier = Modifier.weight(0.4f)
                    )
                    EngineeringTextField(
                        value = metadata.circuitDescription,
                        onValueChange = { v -> onUpdate { it.copy(circuitDescription = v) } },
                        label = "Circuit Description",
                        placeholder = "e.g., Main Chiller Feeder",
                        modifier = Modifier.weight(0.6f)
                    )
                }
                EngineeringTextField(
                    value = metadata.designNotes,
                    onValueChange = { v -> onUpdate { it.copy(designNotes = v) } },
                    label = "Design Notes & Assumptions",
                    placeholder = "Specify boundary conditions, harmonic assumptions, or client standards",
                    singleLine = false,
                    minLines = 3
                )
            }
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Tip: Use the Save icon in the top bar at any time to persist this circuit locally for offline editing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
