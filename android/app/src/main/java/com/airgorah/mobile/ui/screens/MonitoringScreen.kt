package com.airgorah.mobile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airgorah.mobile.core.database.CaptureArtifactEntity
import com.airgorah.mobile.core.database.CaptureSessionEntity
import com.airgorah.mobile.core.hardware.HardwareCapabilityMatrix
import com.airgorah.mobile.ui.theme.CyberCyan
import com.airgorah.mobile.ui.theme.CyberSurface
import com.airgorah.mobile.ui.theme.CyberTextMuted

@Composable
fun MonitoringScreen(
    adapters: List<HardwareCapabilityMatrix>,
    selectedAdapterId: String,
    sessions: List<CaptureSessionEntity>,
    artifacts: List<CaptureArtifactEntity>,
    onSelectAdapter: (String) -> Unit
) {
    val selected = adapters.firstOrNull { it.adapterId == selectedAdapterId } ?: adapters.firstOrNull()
    val totalStorageBytes = artifacts.sumOf { it.fileSizeBytes }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader("Adapter Selection & Monitoring Capabilities")
            Text(
                "Monitor-mode availability is determined strictly by Android platform/driver exposure — never inferred from chipset names or USB VID/PID.",
                style = MaterialTheme.typography.bodySmall,
                color = CyberTextMuted
            )
        }

        items(adapters, key = { it.adapterId }) { adapter ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelectAdapter(adapter.adapterId) },
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = adapter.adapterId == selectedAdapterId,
                        onClick = { onSelectAdapter(adapter.adapterId) }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(adapter.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(adapter.diagnosticMessage, style = MaterialTheme.typography.bodySmall, color = CyberTextMuted)
                    }
                    StatusBadge(adapter.monitorModeStatus.name, capabilityColor(adapter.monitorModeStatus))
                }
            }
        }

        if (selected != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Selected Adapter Profile", style = MaterialTheme.typography.titleMedium, color = CyberCyan)
                        val bands = buildList {
                            if (selected.supports24Ghz) add("2.4 GHz (Ch 1–13)")
                            if (selected.supports5Ghz) add("5 GHz (UNII 1–3)")
                            if (selected.supports6Ghz) add("6 GHz")
                        }.ifEmpty { listOf("None") }
                        Text("Supported Frequencies: ${bands.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                        Text("Monitor Mode: ${selected.monitorModeStatus}", style = MaterialTheme.typography.bodySmall)
                        Text("Raw Capture: ${selected.rawCaptureStatus}", style = MaterialTheme.typography.bodySmall)
                        Text("Imported Capture Storage: ${totalStorageBytes / 1024} KB (${artifacts.size} artifacts)", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            SectionHeader("Capture Session History (${sessions.size})")
        }
        if (sessions.isEmpty()) {
            item {
                EmptyStateCard(
                    "No Live Monitor Sessions",
                    "Standard non-rooted Android does not expose raw monitor sockets. Use Captures to import and inspect PCAP files."
                )
            }
        } else {
            items(sessions, key = { it.sessionId }) { session ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Adapter: ${session.adapterId} • ${session.status}", style = MaterialTheme.typography.titleMedium)
                        Text("Channels: ${session.channelPlan} • Packets: ${session.packetCount}", style = MaterialTheme.typography.bodySmall, color = CyberTextMuted)
                    }
                }
            }
        }
    }
}
