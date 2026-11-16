package com.airgorah.mobile.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airgorah.mobile.core.database.AccessPointEntity
import com.airgorah.mobile.ui.NetworkSortField
import com.airgorah.mobile.ui.theme.CyberAmber
import com.airgorah.mobile.ui.theme.CyberCyan
import com.airgorah.mobile.ui.theme.CyberEmerald
import com.airgorah.mobile.ui.theme.CyberRed
import com.airgorah.mobile.ui.theme.CyberSurface
import com.airgorah.mobile.ui.theme.CyberTextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NetworksScreen(
    accessPoints: List<AccessPointEntity>,
    searchQuery: String,
    protocolFilter: String,
    sortField: NetworkSortField,
    selectedAp: AccessPointEntity?,
    onSearchChange: (String) -> Unit,
    onProtocolFilterChange: (String) -> Unit,
    onSortChange: (NetworkSortField) -> Unit,
    onRefresh: () -> Unit,
    onSelectAp: (AccessPointEntity?) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search SSID, BSSID, vendor") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh scan")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL", "WPA3", "WPA2", "WPA", "WEP", "OPEN").forEach { proto ->
                FilterChip(
                    selected = protocolFilter == proto,
                    onClick = { onProtocolFilterChange(proto) },
                    label = { Text(proto) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NetworkSortField.entries.forEach { field ->
                FilterChip(
                    selected = sortField == field,
                    onClick = { onSortChange(field) },
                    label = { Text("Sort: ${field.name}") }
                )
            }
        }

        if (accessPoints.isEmpty()) {
            EmptyStateCard(
                "No Access Points",
                "Tap Refresh to scan nearby Wi-Fi networks or import a PCAP capture."
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(accessPoints, key = { it.bssid }) { ap ->
                    val protoColor = when (ap.securityProtocol) {
                        "WPA3" -> CyberEmerald
                        "WPA2" -> CyberCyan
                        "WPA" -> CyberAmber
                        else -> CyberRed
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onSelectAp(ap) },
                        colors = CardDefaults.cardColors(containerColor = CyberSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(ap.essid, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                StatusBadge(ap.securityProtocol, protoColor)
                            }
                            Text("${ap.bssid} • ${ap.vendor}", style = MaterialTheme.typography.labelMedium, color = CyberTextMuted)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Ch ${ap.channel} (${ap.band}) • ${ap.rssiDbm} dBm", style = MaterialTheme.typography.bodySmall)
                                StatusBadge(ap.provenance, CyberCyan)
                            }
                            Text(
                                "Classification: ${ap.classification} • Last: ${formatEpoch(ap.lastSeenEpochMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberTextMuted
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedAp != null) {
        AlertDialog(
            onDismissRequest = { onSelectAp(null) },
            title = { Text(selectedAp.essid) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("BSSID: ${selectedAp.bssid}")
                    Text("Vendor: ${selectedAp.vendor}")
                    Text("Protocol: ${selectedAp.securityProtocol} (${selectedAp.classification})")
                    Text("Channel / Freq: Ch ${selectedAp.channel} / ${selectedAp.frequencyMhz} MHz (${selectedAp.band})")
                    Text("Signal: ${selectedAp.rssiDbm} dBm")
                    Text("Capabilities: ${selectedAp.capabilitiesRaw}")
                    Text("Provenance: ${selectedAp.provenance}")
                    Text("Handshake: ${if (selectedAp.hasHandshake) "Detected" else "None"} | PMKID: ${if (selectedAp.hasPmkid) "Detected" else "None"}")
                }
            },
            confirmButton = {
                TextButton(onClick = { onSelectAp(null) }) { Text("Close") }
            }
        )
    }
}

private fun formatEpoch(ms: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(ms))
