package com.airgorah.mobile.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airgorah.mobile.core.database.ReportEntity
import com.airgorah.mobile.core.reporting.ReportFormat
import com.airgorah.mobile.core.reporting.ReportType
import com.airgorah.mobile.ui.theme.CyberCyan
import com.airgorah.mobile.ui.theme.CyberSurface
import com.airgorah.mobile.ui.theme.CyberTextMuted

@Composable
fun ReportsScreen(
    reports: List<ReportEntity>,
    onGenerateReport: (ReportType, ReportFormat) -> Unit,
    onDeleteReport: (String) -> Unit
) {
    var selectedType by remember { mutableStateOf(ReportType.NETWORK_INVENTORY) }
    var selectedFormat by remember { mutableStateOf(ReportFormat.PDF) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Generate Offline Security Report", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Reports explicitly distinguish OBSERVED, IMPORTED, SIMULATED, and INFERRED data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextMuted
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReportType.entries.forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type.title) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReportFormat.entries.forEach { fmt ->
                            FilterChip(
                                selected = selectedFormat == fmt,
                                onClick = { selectedFormat = fmt },
                                label = { Text(fmt.name) }
                            )
                        }
                    }
                    Button(onClick = { onGenerateReport(selectedType, selectedFormat) }) {
                        Text("Export ${selectedFormat.name} Report")
                    }
                }
            }
        }

        item { SectionHeader("Generated Reports (${reports.size})") }
        if (reports.isEmpty()) {
            item {
                EmptyStateCard(
                    "No Reports Generated",
                    "Select a report type and export format above to create an offline PDF, JSON, or CSV report."
                )
            }
        } else {
            items(reports, key = { it.reportId }) { rep ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rep.title, style = MaterialTheme.typography.titleMedium)
                            Text("Items: ${rep.itemCount}", style = MaterialTheme.typography.bodySmall, color = CyberCyan)
                            Text(rep.fileUri, style = MaterialTheme.typography.labelMedium, color = CyberTextMuted)
                        }
                        IconButton(onClick = { onDeleteReport(rep.reportId) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete report")
                        }
                    }
                }
            }
        }
    }
}
