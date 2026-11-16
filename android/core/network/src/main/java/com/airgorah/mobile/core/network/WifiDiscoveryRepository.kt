package com.airgorah.mobile.core.network

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import com.airgorah.mobile.core.common.DataProvenance
import com.airgorah.mobile.core.common.FindingSeverity
import com.airgorah.mobile.core.common.OperationState
import com.airgorah.mobile.core.common.frequencyToBand
import com.airgorah.mobile.core.common.frequencyToChannel
import com.airgorah.mobile.core.database.AccessPointDao
import com.airgorah.mobile.core.database.AccessPointEntity
import com.airgorah.mobile.core.database.SecurityFindingDao
import com.airgorah.mobile.core.database.SecurityFindingEntity
import com.airgorah.mobile.core.permissions.PermissionCoordinator
import com.airgorah.mobile.core.rust.NativeBridge
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiDiscoveryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accessPointDao: AccessPointDao,
    private val findingDao: SecurityFindingDao,
    private val permissionCoordinator: PermissionCoordinator
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _scanState = MutableStateFlow(OperationState.READY)
    val scanState: StateFlow<OperationState> = _scanState.asStateFlow()

    private val _statusMessage = MutableStateFlow("Ready to scan nearby Wi-Fi networks")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private var receiverRegistered = false

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                val updated = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                ingestLatestScanResults(updated)
            }
        }
    }

    fun registerScanReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(scanReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(scanReceiver, filter)
        }
        receiverRegistered = true
    }

    fun unregisterScanReceiver() {
        if (!receiverRegistered) return
        runCatching { context.unregisterReceiver(scanReceiver) }
        receiverRegistered = false
    }

    @SuppressLint("MissingPermission")
    fun triggerScan() {
        val perms = permissionCoordinator.refresh()
        if (!perms.canPerformWifiScan) {
            _scanState.value = OperationState.PERMISSION_REQUIRED
            _statusMessage.value = perms.rationaleMessage
            return
        }

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManager == null) {
            _scanState.value = OperationState.HARDWARE_UNAVAILABLE
            _statusMessage.value = "WifiManager service unavailable on this device."
            return
        }
        if (!wifiManager.isWifiEnabled) {
            _scanState.value = OperationState.HARDWARE_UNAVAILABLE
            _statusMessage.value = "Wi-Fi is disabled. Enable Wi-Fi to scan."
            return
        }

        registerScanReceiver()
        _scanState.value = OperationState.RUNNING
        _statusMessage.value = "Scanning nearby wireless networks..."

        @Suppress("DEPRECATION")
        val started = wifiManager.startScan()
        if (!started) {
            _statusMessage.value = "Scan throttled by Android OS; showing cached system scan results."
            ingestLatestScanResults(updated = false)
        }
    }

    @SuppressLint("MissingPermission")
    fun ingestLatestScanResults(updated: Boolean) {
        val perms = permissionCoordinator.refresh()
        if (!perms.canPerformWifiScan) {
            _scanState.value = OperationState.PERMISSION_REQUIRED
            return
        }
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return
        val results = runCatching { wifiManager.scanResults }.getOrDefault(emptyList())
        val now = System.currentTimeMillis()

        scope.launch {
            val bssids = results.mapNotNull { it.BSSID?.uppercase() }.distinct()
            val vendorMap = NativeBridge.lookupVendorsBatch(bssids).getOrDefault(emptyMap())

            val entities = results.mapNotNull { sr ->
                val bssid = sr.BSSID?.uppercase() ?: return@mapNotNull null
                val ssid = extractSsid(sr)
                val isHidden = ssid.isBlank()
                val displayEssid = if (isHidden) "[Hidden]" else ssid
                val freq = sr.frequency
                val channel = frequencyToChannel(freq) ?: 0
                val band = frequencyToBand(freq)
                val protocol = classifyProtocol(sr.capabilities ?: "")
                val existing = accessPointDao.getByBssid(bssid)

                AccessPointEntity(
                    bssid = bssid,
                    essid = displayEssid,
                    channel = channel,
                    frequencyMhz = freq,
                    band = band,
                    rssiDbm = sr.level,
                    securityProtocol = protocol,
                    capabilitiesRaw = sr.capabilities ?: "",
                    vendor = vendorMap[bssid] ?: existing?.vendor ?: "Unknown",
                    isHidden = isHidden,
                    hasHandshake = existing?.hasHandshake ?: false,
                    hasPmkid = existing?.hasPmkid ?: false,
                    firstSeenEpochMs = existing?.firstSeenEpochMs ?: now,
                    lastSeenEpochMs = now,
                    provenance = DataProvenance.OBSERVED.name,
                    classification = classifyRisk(protocol)
                )
            }

            if (entities.isNotEmpty()) {
                accessPointDao.upsertAll(entities)
                val findings = entities.mapNotNull { ap ->
                    when (ap.securityProtocol) {
                        "OPEN" -> SecurityFindingEntity(
                            findingId = "open-${ap.bssid}",
                            category = "Network Security",
                            severity = FindingSeverity.HIGH.name,
                            title = "Unencrypted Open Network (${ap.essid})",
                            description = "Access point ${ap.bssid} broadcasts without link-layer encryption.",
                            recommendation = "Enable WPA2-AES or WPA3-SAE encryption.",
                            targetBssid = ap.bssid,
                            provenance = DataProvenance.OBSERVED.name,
                            createdAtEpochMs = now
                        )
                        "WEP" -> SecurityFindingEntity(
                            findingId = "wep-${ap.bssid}",
                            category = "Network Security",
                            severity = FindingSeverity.CRITICAL.name,
                            title = "Deprecated WEP Encryption (${ap.essid})",
                            description = "Access point ${ap.bssid} uses broken WEP cryptography.",
                            recommendation = "Migrate immediately to WPA3-SAE or WPA2-PSK (AES/CCMP).",
                            targetBssid = ap.bssid,
                            provenance = DataProvenance.OBSERVED.name,
                            createdAtEpochMs = now
                        )
                        else -> null
                    }
                }
                if (findings.isNotEmpty()) {
                    findingDao.upsertAll(findings)
                }
            }

            _scanState.value = OperationState.COMPLETED
            _statusMessage.value = if (updated) {
                "Scan completed: ${entities.size} access points observed."
            } else {
                "Loaded ${entities.size} cached access points from Android Wi-Fi subsystem."
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun extractSsid(sr: ScanResult): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            sr.wifiSsid?.toString()?.removeSurrounding("\"") ?: ""
        } else {
            sr.SSID ?: ""
        }
    }

    companion object {
        fun classifyProtocol(caps: String): String {
            val upper = caps.uppercase()
            return when {
                "SAE" in upper || "WPA3" in upper -> "WPA3"
                "WPA2" in upper || "RSN" in upper -> "WPA2"
                "WPA" in upper -> "WPA"
                "WEP" in upper -> "WEP"
                else -> "OPEN"
            }
        }

        fun classifyRisk(protocol: String): String = when (protocol) {
            "WPA3" -> "Strong (WPA3)"
            "WPA2" -> "Standard (WPA2)"
            "WPA" -> "Legacy (WPA)"
            "WEP" -> "Insecure (WEP)"
            "OPEN" -> "Unencrypted"
            else -> "Unknown"
        }
    }
}
