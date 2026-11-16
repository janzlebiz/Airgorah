package com.airgorah.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airgorah.mobile.core.common.CapabilityStatus
import com.airgorah.mobile.core.common.OperationState
import com.airgorah.mobile.ui.theme.CyberAmber
import com.airgorah.mobile.ui.theme.CyberCyan
import com.airgorah.mobile.ui.theme.CyberEmerald
import com.airgorah.mobile.ui.theme.CyberPurple
import com.airgorah.mobile.ui.theme.CyberRed
import com.airgorah.mobile.ui.theme.CyberSurface
import com.airgorah.mobile.ui.theme.CyberSurfaceVariant
import com.airgorah.mobile.ui.theme.CyberTextMuted

@Composable
fun StatusBadge(label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

fun operationStateColor(state: OperationState): Color = when (state) {
    OperationState.READY -> CyberCyan
    OperationState.RUNNING -> CyberAmber
    OperationState.COMPLETED -> CyberEmerald
    OperationState.UNSUPPORTED -> CyberRed
    OperationState.PERMISSION_REQUIRED -> CyberAmber
    OperationState.HARDWARE_UNAVAILABLE -> CyberRed
    OperationState.SIMULATED -> CyberPurple
}

fun operationStateLabel(state: OperationState): String = when (state) {
    OperationState.READY -> "Ready"
    OperationState.RUNNING -> "Running"
    OperationState.COMPLETED -> "Completed"
    OperationState.UNSUPPORTED -> "Unsupported"
    OperationState.PERMISSION_REQUIRED -> "Permission required"
    OperationState.HARDWARE_UNAVAILABLE -> "Hardware unavailable"
    OperationState.SIMULATED -> "Simulated"
}

fun capabilityColor(status: CapabilityStatus): Color = when (status) {
    CapabilityStatus.SUPPORTED -> CyberEmerald
    CapabilityStatus.CONDITIONAL -> CyberAmber
    CapabilityStatus.UNSUPPORTED -> CyberRed
    CapabilityStatus.SIMULATED -> CyberPurple
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    accent: Color = CyberCyan,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = CyberTextMuted)
            Text(value, style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = CyberTextMuted)
        }
    }
}

@Composable
fun SectionHeader(title: String, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        trailing?.invoke()
    }
}

@Composable
fun EmptyStateCard(title: String, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodySmall, color = CyberTextMuted)
        }
    }
}
