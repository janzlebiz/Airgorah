package com.airgorah.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airgorah.mobile.core.database.CaptureArtifactEntity
import com.airgorah.mobile.ui.theme.CyberAmber
import com.airgorah.mobile.ui.theme.CyberCyan
import com.airgorah.mobile.ui.theme.CyberEmerald
import com.airgorah.mobile.ui.theme.CyberRed
import com.airgorah.mobile.ui.theme.CyberSurface
import com.airgorah.mobile.ui.theme.CyberTextMuted

@Composable
fun CapturesScreen(
    artifacts: List<CaptureArtifactEntity>,
    onImportPcap: () -> Unit,
    onDeleteArtifact: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader(
                title = "PCAP Captures (${artifacts.size})",
                trailing = {
                    Button(onClick = onImportPcap) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Text(" Import PCAP", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            )
            Text(
                "Imports classic libpcap (.cap/.pcap) files via Android Storage Access Framework and validates IEEE 802.11 / Radiotap frames in Rust.",
                style = MaterialTheme.typography.bodySmall,
                color = CyberTextMuted
            )
        }

        if (artifacts.isEmpty()) {
            item {
                EmptyStateCard(
                    "No Capture Artifacts Imported",
                    "Tap Import PCAP to analyze an 802.11 capture file for beacons, clients, 4-way handshakes, and PMKIDs."
                )
            }
        } else {
            items(artifacts, key = { it.artifactId }) { art ->
                val statusColor = when (art.validationStatus.uppercase()) {
                    "VALID" -> CyberEmerald
                    "INCOMPLETE" -> CyberAmber
                    else -> CyberRed
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(art.fileName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            StatusBadge(art.validationStatus, statusColor)
                            IconButton(onClick = { onDeleteArtifact(art.artifactId) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete capture")
                            }
                        }
                        Text("${art.linkTypeName} • ${art.fileSizeBytes} bytes", style = MaterialTheme.typography.labelMedium, color = CyberCyan)
                        Text(
                            "Packets: ${art.packetCount} (Decoded: ${art.decodedFrames}) | APs: ${art.bssidCount} | Clients: ${art.clientCount}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Handshakes: ${art.handshakeCount} | PMKIDs: ${art.pmkidCount} | Deauth frames: ${art.deauthFrameCount}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(art.statusDetail, style = MaterialTheme.typography.bodySmall, color = CyberTextMuted)
                    }
                }
            }
        }
    }
}
