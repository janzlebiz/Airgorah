package com.airgorah.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airgorah.mobile.core.common.OperationState
import com.airgorah.mobile.core.database.AccessPointEntity
import com.airgorah.mobile.core.database.CaptureArtifactEntity
import com.airgorah.mobile.core.database.SecurityFindingEntity
import com.airgorah.mobile.core.database.WirelessClientEntity
import com.airgorah.mobile.core.hardware.HardwareCapabilityMatrix
import com.airgorah.mobile.core.permissions.PermissionStatusSnapshot
import com.airgorah.mobile.ui.theme.CyberAmber
import com.airgorah.mobile.ui.theme.CyberCyan
import com.airgorah.mobile.ui.theme.CyberEmerald
import com.airgorah.mobile.ui.theme.CyberRed
import com.airgorah.mobile.ui.theme.CyberSurface
import com.airgorah.mobile.ui.theme.CyberTextMuted

@Composable
fun DashboardScreen(
    scanState: OperationState,
    statusMessage: String,
    permissionStatus: PermissionStatusSnapshot,
    adapters: List<HardwareCapabilityMatrix>,
    accessPoints: List<AccessPointEntity>,
    clients: List<WirelessClientEntity>,
    artifacts: List<CaptureArtifactEntity>,
    findings: List<SecurityFindingEntity>,
    nativeDiagnostic: String,
    onRequestPermissions: () -> Unit,
    onTriggerScan: () -> Unit,
    onOpenSecurityLab: () -> Unit
) {
    val builtIn = adapters.firstOrNull { it.adapterId == "builtin-wlan0" }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Airgorah Mobile", style = MaterialTheme.typography.titleLarge)
                        StatusBadge(operationStateLabel(scanState), operationStateColor(scanState))
                    }
                    Text(statusMessage, style = MaterialTheme.typography.bodySmall, color = CyberTextMuted)
                    Text(nativeDiagnostic, style = MaterialTheme.typography.labelMedium, color = CyberCyan)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            if (permissionStatus.canPerformWifiScan) onTriggerScan() else onRequestPermissions()
                        }) {
                            Text(if (permissionStatus.canPerformWifiScan) "Scan Wi-Fi" else "Grant Permissions")
                        }
                        OutlinedButton(onClick = onOpenSecurityLab) {
                            Text("Security Lab")
                        }
                    }
                }
            }
        }

        if (!permissionStatus.canPerformWifiScan) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberAmber.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Permission / Location Required", style = MaterialTheme.typography.titleMedium, color = CyberAmber)
                        Text(permissionStatus.rationaleMessage, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    title = "Networks",
                    value = accessPoints.size.toString(),
                    subtitle = "${accessPoints.count { it.securityProtocol == "OPEN" || it.securityProtocol == "WEP" }} weak/open",
                    accent = CyberCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Clients",
                    value = clients.size.toString(),
                    subtitle = "Authorized inventory",
                    accent = CyberEmerald,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    title = "PCAP Captures",
                    value = artifacts.size.toString(),
                    subtitle = "${artifacts.sumOf { it.handshakeCount }} HS / ${artifacts.sumOf { it.pmkidCount }} PMKID",
                    accent = CyberAmber,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Wi-Fi Radio",
                    value = if (builtIn?.isWifiEnabled == true) "Active" else "Off",
                    subtitle = if (builtIn?.supports5Ghz == true) "2.4 + 5 GHz" else "2.4 GHz",
                    accent = if (builtIn?.isWifiEnabled == true) CyberEmerald else CyberRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            SectionHeader("Hardware Compatibility")
        }
        items(adapters) { adapter ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(adapter.displayName, style = MaterialTheme.typography.bodyMedium)
                        Text(adapter.category.name, style = MaterialTheme.typography.labelMedium, color = CyberTextMuted)
                    }
                    StatusBadge(adapter.monitorModeStatus.name, capabilityColor(adapter.monitorModeStatus))
                }
            }
        }

        item {
            SectionHeader("Recent Security Findings (${findings.size})")
        }
        if (findings.isEmpty()) {
            item {
                EmptyStateCard(
                    "No findings recorded yet",
                    "Run a Wi-Fi scan, import a PCAP capture, or execute a Security Lab drill."
                )
            }
        } else {
            items(findings.take(5)) { finding ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(finding.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            StatusBadge(finding.provenance, CyberCyan)
                        }
                        Text(finding.description, style = MaterialTheme.typography.bodySmall, color = CyberTextMuted)
                        Text("Recommendation: ${finding.recommendation}", style = MaterialTheme.typography.bodySmall, color = CyberEmerald)
                    }
                }
            }
        }
    }
}
