package com.airgorah.mobile.ui.screens

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airgorah.mobile.core.database.WirelessClientEntity
import com.airgorah.mobile.ui.theme.CyberCyan
import com.airgorah.mobile.ui.theme.CyberEmerald
import com.airgorah.mobile.ui.theme.CyberSurface
import com.airgorah.mobile.ui.theme.CyberTextMuted

@Composable
fun ClientsScreen(clients: List<WirelessClientEntity>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader("Authorized Client Inventory (${clients.size})")
            Text(
                "Client MACs and probe requests are populated from imported PCAP captures or authorized local observations. Observed vs Inferred provenance is explicitly labeled.",
                style = MaterialTheme.typography.bodySmall,
                color = CyberTextMuted
            )
        }
        if (clients.isEmpty()) {
            item {
                EmptyStateCard(
                    "No Clients Observed",
                    "Standard Android WifiManager does not expose other stations' MAC addresses. Import a PCAP capture in Captures to inspect client frames."
                )
            }
        } else {
            items(clients, key = { it.mac }) { cl ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(cl.mac, style = MaterialTheme.typography.titleMedium)
                            StatusBadge(cl.provenance, CyberCyan)
                        }
                        Text("Vendor: ${cl.vendor}", style = MaterialTheme.typography.bodySmall)
                        Text("Associated AP: ${cl.associatedBssid ?: "Unlinked"}", style = MaterialTheme.typography.bodySmall, color = CyberTextMuted)
                        Text("State: ${cl.connectionState} • Packets: ${cl.packetCount} • Signal: ${cl.rssiDbm ?: "--"} dBm", style = MaterialTheme.typography.bodySmall)
                        if (cl.probedSsids.isNotBlank()) {
                            Text("Probes: ${cl.probedSsids}", style = MaterialTheme.typography.labelMedium, color = CyberEmerald)
                        }
                    }
                }
            }
        }
    }
}
