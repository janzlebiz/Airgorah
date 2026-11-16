package com.airgorah.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airgorah.mobile.core.database.ApplicationSettingsEntity
import com.airgorah.mobile.ui.theme.CyberCyan
import com.airgorah.mobile.ui.theme.CyberRed
import com.airgorah.mobile.ui.theme.CyberSurface
import com.airgorah.mobile.ui.theme.CyberTextMuted

@Composable
fun SettingsScreen(
    settings: ApplicationSettingsEntity,
    channelFilterValid: Boolean,
    nativeDiagnostic: String,
    onUpdateSettings: (ApplicationSettingsEntity) -> Unit,
    onClearAllData: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Discovery & Analysis Preferences", style = MaterialTheme.typography.titleMedium)

                    SettingSwitchRow(
                        label = "Display Hidden Access Points",
                        checked = settings.displayHiddenAps,
                        onCheckedChange = { onUpdateSettings(settings.copy(displayHiddenAps = it)) }
                    )
                    SettingSwitchRow(
                        label = "Auto-Enrich OUI Vendor Names (Rust)",
                        checked = settings.autoEnrichOui,
                        onCheckedChange = { onUpdateSettings(settings.copy(autoEnrichOui = it)) }
                    )
                    SettingSwitchRow(
                        label = "Default 2.4 GHz Band",
                        checked = settings.default24Ghz,
                        onCheckedChange = { onUpdateSettings(settings.copy(default24Ghz = it)) }
                    )
                    SettingSwitchRow(
                        label = "Default 5 GHz Band",
                        checked = settings.default5Ghz,
                        onCheckedChange = { onUpdateSettings(settings.copy(default5Ghz = it)) }
                    )

                    OutlinedTextField(
                        value = settings.channelFilter,
                        onValueChange = { onUpdateSettings(settings.copy(channelFilter = it)) },
                        label = { Text("Channel Filter (e.g. 1,6,11 or 36,149)") },
                        isError = !channelFilterValid,
                        supportingText = {
                            if (!channelFilterValid) {
                                Text("Invalid channel filter for selected bands (validated by Rust core)")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Native Core & Attribution", style = MaterialTheme.typography.titleMedium)
                    Text(nativeDiagnostic, style = MaterialTheme.typography.labelMedium, color = CyberCyan)
                    Text(
                        "Airgorah Mobile — Offline-first native Android Wi-Fi security auditing application based on martin-olivier/airgorah (MIT License, Copyright (c) 2026 Martin Olivier).",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextMuted
                    )
                }
            }
        }

        item {
            Button(
                onClick = onClearAllData,
                colors = ButtonDefaults.buttonColors(containerColor = CyberRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear All Local Scan, Client & Capture Data")
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
