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
import com.airgorah.mobile.core.common.HardwareCategory
import com.airgorah.mobile.core.hardware.HardwareCapabilityMatrix
import com.airgorah.mobile.ui.theme.CyberCyan
import com.airgorah.mobile.ui.theme.CyberSurface
import com.airgorah.mobile.ui.theme.CyberTextMuted

@Composable
fun HardwareScreen(
    adapters: List<HardwareCapabilityMatrix>,
    lastUsbEvent: String?,
    onRefresh: () -> Unit,
    onRequestUsbPermission: (Int, Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader(
                title = "Hardware Abstraction & Diagnostics",
                trailing = { OutlinedButton(onClick = onRefresh) { Text("Refresh") } }
            )
            if (!lastUsbEvent.isNullOrBlank()) {
                Text("Last USB Event: $lastUsbEvent", style = MaterialTheme.typography.labelMedium, color = CyberCyan)
            }
        }

        items(adapters, key = { it.adapterId }) { dev ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(dev.displayName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        StatusBadge(dev.category.name, CyberCyan)
                    }
                    Text(
                        "Managed Scan: ${dev.canScanManaged} | Monitor: ${dev.monitorModeStatus} | Raw Capture: ${dev.rawCaptureStatus} | Injection: ${dev.packetInjectionStatus}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        "Bands: 2.4GHz=${dev.supports24Ghz}, 5GHz=${dev.supports5Ghz}, 6GHz=${dev.supports6Ghz} | USB Host=${dev.hasUsbHostFeature}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextMuted
                    )
                    if (dev.usbClassSummary != null) {
                        Text(dev.usbClassSummary ?: "", style = MaterialTheme.typography.labelMedium, color = CyberTextMuted)
                    }
                    Text(dev.diagnosticMessage, style = MaterialTheme.typography.bodySmall, color = CyberTextMuted)

                    if (dev.category == HardwareCategory.EXTERNAL_USB && !dev.usbPermissionGranted && dev.vendorId != null && dev.productId != null) {
                        Button(onClick = { onRequestUsbPermission(dev.vendorId!!, dev.productId!!) }) {
                            Text("Request USB Permission")
                        }
                    }
                }
            }
        }
    }
}
