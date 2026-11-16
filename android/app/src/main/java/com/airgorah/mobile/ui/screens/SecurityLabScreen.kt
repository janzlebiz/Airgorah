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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.airgorah.mobile.core.database.CaptureArtifactEntity
import com.airgorah.mobile.core.database.LabEventEntity
import com.airgorah.mobile.core.rust.PasswordAuditDto
import com.airgorah.mobile.core.rust.PcapAnalysisDto
import com.airgorah.mobile.ui.theme.CyberAmber
import com.airgorah.mobile.ui.theme.CyberCyan
import com.airgorah.mobile.ui.theme.CyberEmerald
import com.airgorah.mobile.ui.theme.CyberPurple
import com.airgorah.mobile.ui.theme.CyberRed
import com.airgorah.mobile.ui.theme.CyberSurface
import com.airgorah.mobile.ui.theme.CyberTextMuted

@Composable
fun SecurityLabScreen(
    artifacts: List<CaptureArtifactEntity>,
    parseArtifact: (CaptureArtifactEntity) -> PcapAnalysisDto?,
    labEvents: List<LabEventEntity>,
    passwordAudit: PasswordAuditDto?,
    onRunDeauthSim: (Boolean) -> Unit,
    onClearLabEvents: () -> Unit,
    onEvaluatePassword: (CharArray) -> Unit,
    onClearPasswordAudit: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Deauth Lab", "Handshake", "PMKID", "Password Audit")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, maxLines = 1) }
                )
            }
        }

        when (selectedTab) {
            0 -> DeauthLabTab(labEvents, onRunDeauthSim, onClearLabEvents)
            1 -> HandshakeTab(artifacts, parseArtifact)
            2 -> PmkidTab(artifacts, parseArtifact)
            3 -> PasswordAuditTab(passwordAudit, onEvaluatePassword, onClearPasswordAudit)
        }
    }
}

@Composable
private fun DeauthLabTab(
    events: List<LabEventEntity>,
    onRunDeauthSim: (Boolean) -> Unit,
    onClearLabEvents: () -> Unit
) {
    var pmfEnabled by remember { mutableStateOf(true) }

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
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Deauthentication Detection Lab", style = MaterialTheme.typography.titleMedium)
                        StatusBadge("SIMULATED", CyberPurple)
                    }
                    Text(
                        "Defensive educational environment. Simulates 802.11 Reason 7 deauthentication bursts and WIDS detection without transmitting any RF frames.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextMuted
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("802.11w PMF (Protected Mgmt Frames)", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = pmfEnabled, onCheckedChange = { pmfEnabled = it })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onRunDeauthSim(pmfEnabled) }) {
                            Text("Run Lab Simulation")
                        }
                        OutlinedButton(onClick = onClearLabEvents) {
                            Text("Clear Timeline")
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Defensive Reference", style = MaterialTheme.typography.titleMedium, color = CyberCyan)
                    Text("• Deauth frames (Subtype 0xC) are unencrypted in legacy 802.11, allowing MAC spoofing.", style = MaterialTheme.typography.bodySmall)
                    Text("• 802.11w (PMF) cryptographically authenticates management frames and uses SA Query to reject spoofed disconnects.", style = MaterialTheme.typography.bodySmall)
                    Text("• Administrators should monitor WIDS logs for sudden Reason 6/7 spikes and client reconnect storms.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item { SectionHeader("Simulated Event Timeline (${events.size})") }
        items(events, key = { it.eventId }) { ev ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(ev.eventType, style = MaterialTheme.typography.titleMedium)
                        StatusBadge("SIMULATED", CyberPurple)
                    }
                    Text("${ev.sourceMac} -> ${ev.targetMac} (Reason ${ev.reasonCode})", style = MaterialTheme.typography.labelMedium, color = CyberCyan)
                    Text(ev.detectionNote, style = MaterialTheme.typography.bodySmall, color = CyberTextMuted)
                }
            }
        }
    }
}

@Composable
private fun HandshakeTab(
    artifacts: List<CaptureArtifactEntity>,
    parseArtifact: (CaptureArtifactEntity) -> PcapAnalysisDto?
) {
    val handshakes = artifacts.flatMap { art ->
        parseArtifact(art)?.handshakes?.map { art.fileName to it } ?: emptyList()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader("WPA 4-Way Handshake Artifacts (${handshakes.size})")
            Text(
                "Inspects EAPOL-Key M1–M4 completeness (M2 SNonce+MIC paired with M1 or M3 ANonce) for defensive capture verification. Credential recovery is disabled.",
                style = MaterialTheme.typography.bodySmall,
                color = CyberTextMuted
            )
        }
        if (handshakes.isEmpty()) {
            item {
                EmptyStateCard(
                    "No Handshake Artifacts",
                    "Import a PCAP capture containing EAPOL 4-way handshake frames in the Captures tab."
                )
            }
        } else {
            items(handshakes) { (file, hs) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${hs.essid} (${hs.bssid})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            StatusBadge(hs.status, if (hs.isCompleteHandshake) CyberEmerald else CyberAmber)
                        }
                        Text("Station: ${hs.stationMac} • Source: $file", style = MaterialTheme.typography.bodySmall, color = CyberTextMuted)
                        Text(
                            "Messages: M1=${hs.hasM1} M2=${hs.hasM2} M3=${hs.hasM3} M4=${hs.hasM4}",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberCyan
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PmkidTab(
    artifacts: List<CaptureArtifactEntity>,
    parseArtifact: (CaptureArtifactEntity) -> PcapAnalysisDto?
) {
    val pmkids = artifacts.flatMap { art ->
        parseArtifact(art)?.pmkids?.map { art.fileName to it } ?: emptyList()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader("PMKID Artifacts (${pmkids.size})")
            Text(
                "Identifies RSN PMKID Key Data Elements in EAPOL M1 frames and distinguishes WPA2-PSK (PBKDF2) from WPA3-SAE (non-PSK).",
                style = MaterialTheme.typography.bodySmall,
                color = CyberTextMuted
            )
        }
        if (pmkids.isEmpty()) {
            item {
                EmptyStateCard(
                    "No PMKID Artifacts",
                    "Import a PCAP capture containing EAPOL M1 PMKID KDEs in the Captures tab."
                )
            }
        } else {
            items(pmkids) { (file, pm) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${pm.essid} (${pm.bssid})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            StatusBadge(if (pm.isWpa3SaePmkid) "WPA3-SAE" else "WPA2-PSK", if (pm.isWpa3SaePmkid) CyberEmerald else CyberAmber)
                        }
                        Text("Station: ${pm.stationMac} • Key Descriptor v${pm.keyDescriptorVersion} • $file", style = MaterialTheme.typography.bodySmall, color = CyberTextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordAuditTab(
    audit: PasswordAuditDto?,
    onEvaluatePassword: (CharArray) -> Unit,
    onClearAudit: () -> Unit
) {
    var input by remember { mutableStateOf("") }

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
                    Text("Defensive Password Strength Auditor", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Evaluates passphrases offline in Rust memory. Passwords are never stored, logged, or transmitted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextMuted
                    )
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = { Text("Test Wi-Fi Passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val chars = input.toCharArray()
                            onEvaluatePassword(chars)
                        }) {
                            Text("Analyze Strength")
                        }
                        OutlinedButton(onClick = {
                            input = ""
                            onClearAudit()
                        }) {
                            Text("Clear")
                        }
                    }
                }
            }
        }

        if (audit != null) {
            item {
                val ratingColor = when (audit.rating) {
                    "VeryStrong", "Strong" -> CyberEmerald
                    "Moderate" -> CyberCyan
                    "Weak" -> CyberAmber
                    else -> CyberRed
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Assessment Result", style = MaterialTheme.typography.titleMedium)
                            StatusBadge(audit.rating, ratingColor)
                        }
                        Text("Length: ${audit.length} chars (WPA PSK valid: ${audit.wpaPskLengthValid})", style = MaterialTheme.typography.bodySmall)
                        Text("Estimated Entropy: ${audit.estimatedEntropyBits} bits", style = MaterialTheme.typography.bodyMedium, color = CyberCyan)
                        Text(audit.entropyMethodologyNote, style = MaterialTheme.typography.bodySmall, color = CyberTextMuted)
                        audit.warnings.forEach { w ->
                            Text("⚠ $w", style = MaterialTheme.typography.bodySmall, color = CyberAmber)
                        }
                        audit.recommendations.forEach { r ->
                            Text("✓ $r", style = MaterialTheme.typography.bodySmall, color = CyberEmerald)
                        }
                    }
                }
            }
        }
    }
}
