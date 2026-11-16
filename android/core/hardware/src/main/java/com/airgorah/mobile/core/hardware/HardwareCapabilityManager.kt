package com.airgorah.mobile.core.hardware

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.wifi.WifiManager
import android.os.Build
import com.airgorah.mobile.core.common.CapabilityStatus
import com.airgorah.mobile.core.common.HardwareCategory
import com.airgorah.mobile.core.database.WirelessAdapterDao
import com.airgorah.mobile.core.database.WirelessAdapterEntity
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

data class HardwareCapabilityMatrix(
    val adapterId: String,
    val displayName: String,
    val category: HardwareCategory,
    val isConnected: Boolean,
    val isWifiEnabled: Boolean,
    val hasUsbHostFeature: Boolean,
    val usbPermissionGranted: Boolean,
    val vendorId: Int? = null,
    val productId: Int? = null,
    val usbClassSummary: String? = null,
    val supports24Ghz: Boolean,
    val supports5Ghz: Boolean,
    val supports6Ghz: Boolean,
    val canScanManaged: Boolean,
    val monitorModeStatus: CapabilityStatus,
    val rawCaptureStatus: CapabilityStatus,
    val packetInjectionStatus: CapabilityStatus = CapabilityStatus.UNSUPPORTED,
    val diagnosticMessage: String
)

@Singleton
class HardwareCapabilityManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adapterDao: WirelessAdapterDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _adapters = MutableStateFlow<List<HardwareCapabilityMatrix>>(emptyList())
    val adapters: StateFlow<List<HardwareCapabilityMatrix>> = _adapters.asStateFlow()

    private val _selectedAdapterId = MutableStateFlow("builtin-wlan0")
    val selectedAdapterId: StateFlow<String> = _selectedAdapterId.asStateFlow()

    private val _lastUsbEvent = MutableStateFlow<String?>(null)
    val lastUsbEvent: StateFlow<String?> = _lastUsbEvent.asStateFlow()

    private var receiverRegistered = false

    companion object {
        const val ACTION_USB_PERMISSION = "com.airgorah.mobile.USB_PERMISSION"
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }
            when (action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    _lastUsbEvent.value = "USB device attached: ${device?.productName ?: device?.deviceName ?: "Unknown"}"
                    refreshHardware()
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    _lastUsbEvent.value = "USB device disconnected: ${device?.productName ?: device?.deviceName ?: "Unknown"}"
                    if (device != null && _selectedAdapterId.value == usbAdapterId(device)) {
                        _selectedAdapterId.value = "builtin-wlan0"
                    }
                    refreshHardware()
                }
                ACTION_USB_PERMISSION -> {
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    _lastUsbEvent.value = if (granted) {
                        "USB permission granted for ${device?.productName ?: "adapter"}"
                    } else {
                        "USB permission denied by user"
                    }
                    refreshHardware()
                }
            }
        }
    }

    fun registerUsbReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(ACTION_USB_PERMISSION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(usbReceiver, filter)
        }
        receiverRegistered = true
        refreshHardware()
    }

    fun unregisterUsbReceiver() {
        if (!receiverRegistered) return
        runCatching { context.unregisterReceiver(usbReceiver) }
        receiverRegistered = false
    }

    fun selectAdapter(adapterId: String) {
        _selectedAdapterId.value = adapterId
    }

    fun requestUsbPermission(vendorId: Int, productId: Int): Boolean {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return false
        val device = usbManager.deviceList.values.firstOrNull {
            it.vendorId == vendorId && it.productId == productId
        } ?: return false

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        val pi = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), flags)
        usbManager.requestPermission(device, pi)
        return true
    }

    fun refreshHardware(): List<HardwareCapabilityMatrix> {
        val pm = context.packageManager
        val hasWifiFeature = pm.hasSystemFeature(PackageManager.FEATURE_WIFI)
        val hasUsbHost = pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager

        val list = mutableListOf<HardwareCapabilityMatrix>()

        // A. Built-in Android Wi-Fi
        if (hasWifiFeature && wifiManager != null) {
            val is5Ghz = wifiManager.is5GHzBandSupported
            val is6Ghz = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                wifiManager.is6GHzBandSupported
            } else {
                false
            }
            val wifiEnabled = wifiManager.isWifiEnabled
            list += HardwareCapabilityMatrix(
                adapterId = "builtin-wlan0",
                displayName = "Built-in Android Wi-Fi (${Build.MANUFACTURER} ${Build.MODEL})",
                category = HardwareCategory.BUILT_IN_WIFI,
                isConnected = wifiEnabled,
                isWifiEnabled = wifiEnabled,
                hasUsbHostFeature = hasUsbHost,
                usbPermissionGranted = false,
                supports24Ghz = true,
                supports5Ghz = is5Ghz,
                supports6Ghz = is6Ghz,
                canScanManaged = wifiEnabled,
                monitorModeStatus = CapabilityStatus.UNSUPPORTED,
                rawCaptureStatus = CapabilityStatus.UNSUPPORTED,
                packetInjectionStatus = CapabilityStatus.UNSUPPORTED,
                diagnosticMessage = if (wifiEnabled) {
                    "Managed-mode Wi-Fi scanning supported via WifiManager. Raw 802.11 monitor mode and frame injection are not exposed to unprivileged Android apps."
                } else {
                    "Built-in Wi-Fi radio is disabled. Enable Wi-Fi to perform managed network discovery."
                }
            )
        } else {
            // C. Unsupported Hardware fallback
            list += HardwareCapabilityMatrix(
                adapterId = "builtin-unsupported",
                displayName = "Built-in Wi-Fi Unavailable",
                category = HardwareCategory.UNSUPPORTED,
                isConnected = false,
                isWifiEnabled = false,
                hasUsbHostFeature = hasUsbHost,
                usbPermissionGranted = false,
                supports24Ghz = false,
                supports5Ghz = false,
                supports6Ghz = false,
                canScanManaged = false,
                monitorModeStatus = CapabilityStatus.UNSUPPORTED,
                rawCaptureStatus = CapabilityStatus.UNSUPPORTED,
                packetInjectionStatus = CapabilityStatus.UNSUPPORTED,
                diagnosticMessage = "Device does not report android.hardware.wifi capability."
            )
        }

        // B. External USB-OTG Devices (never assume monitor mode from VID/PID alone)
        if (hasUsbHost && usbManager != null) {
            usbManager.deviceList.values.forEach { dev ->
                val hasPerm = usbManager.hasPermission(dev)
                val isWirelessCandidate = isPotentialWirelessUsbDevice(dev)
                val category = if (isWirelessCandidate) HardwareCategory.EXTERNAL_USB else HardwareCategory.UNSUPPORTED
                list += HardwareCapabilityMatrix(
                    adapterId = usbAdapterId(dev),
                    displayName = dev.productName ?: "USB Device (${formatVidPid(dev.vendorId, dev.productId)})",
                    category = category,
                    isConnected = true,
                    isWifiEnabled = false,
                    hasUsbHostFeature = true,
                    usbPermissionGranted = hasPerm,
                    vendorId = dev.vendorId,
                    productId = dev.productId,
                    usbClassSummary = "Class ${dev.deviceClass}/${dev.deviceSubclass} (${dev.interfaceCount} interfaces)",
                    supports24Ghz = isWirelessCandidate,
                    supports5Ghz = false,
                    supports6Ghz = false,
                    canScanManaged = false,
                    monitorModeStatus = if (isWirelessCandidate) CapabilityStatus.CONDITIONAL else CapabilityStatus.UNSUPPORTED,
                    rawCaptureStatus = if (isWirelessCandidate) CapabilityStatus.CONDITIONAL else CapabilityStatus.UNSUPPORTED,
                    packetInjectionStatus = CapabilityStatus.UNSUPPORTED,
                    diagnosticMessage = when {
                        !isWirelessCandidate -> "Connected USB device does not advertise a wireless/vendor-specific network class."
                        !hasPerm -> "External USB adapter detected (${formatVidPid(dev.vendorId, dev.productId)}). Grant USB permission to inspect descriptors. Monitor mode is not assumed from VID/PID."
                        else -> "USB permission granted (${formatVidPid(dev.vendorId, dev.productId)}). Unprivileged Android does not expose kernel nl80211/mac80211 monitor mode; use PCAP import for packet analysis."
                    }
                )
            }
        }

        // D. Simulated Test Hardware
        list += HardwareCapabilityMatrix(
            adapterId = "sim-wlan0",
            displayName = "Simulated Lab Adapter (SIM-WLAN-01)",
            category = HardwareCategory.SIMULATED,
            isConnected = true,
            isWifiEnabled = true,
            hasUsbHostFeature = hasUsbHost,
            usbPermissionGranted = true,
            supports24Ghz = true,
            supports5Ghz = true,
            supports6Ghz = false,
            canScanManaged = false,
            monitorModeStatus = CapabilityStatus.SIMULATED,
            rawCaptureStatus = CapabilityStatus.SIMULATED,
            packetInjectionStatus = CapabilityStatus.UNSUPPORTED,
            diagnosticMessage = "Simulated adapter for Deauthentication Lab and UI state verification. Never produces real radio traffic."
        )

        _adapters.value = list
        persistAdapters(list)
        return list
    }

    private fun persistAdapters(list: List<HardwareCapabilityMatrix>) {
        val now = System.currentTimeMillis()
        scope.launch {
            adapterDao.upsertAll(
                list.map {
                    WirelessAdapterEntity(
                        adapterId = it.adapterId,
                        name = it.displayName,
                        category = it.category.name,
                        vendorId = it.vendorId,
                        productId = it.productId,
                        driverInfo = it.usbClassSummary ?: "Android Platform",
                        supports24Ghz = it.supports24Ghz,
                        supports5Ghz = it.supports5Ghz,
                        supports6Ghz = it.supports6Ghz,
                        monitorModeStatus = it.monitorModeStatus.name,
                        rawCaptureStatus = it.rawCaptureStatus.name,
                        isConnected = it.isConnected,
                        lastSeenEpochMs = now
                    )
                }
            )
        }
    }

    private fun isPotentialWirelessUsbDevice(dev: UsbDevice): Boolean {
        if (dev.deviceClass == UsbConstants.USB_CLASS_WIRELESS_CONTROLLER ||
            dev.deviceClass == UsbConstants.USB_CLASS_VENDOR_SPEC ||
            dev.deviceClass == UsbConstants.USB_CLASS_PER_INTERFACE
        ) {
            return true
        }
        for (i in 0 until dev.interfaceCount) {
            val intf = dev.getInterface(i)
            if (intf.interfaceClass == UsbConstants.USB_CLASS_WIRELESS_CONTROLLER ||
                intf.interfaceClass == UsbConstants.USB_CLASS_VENDOR_SPEC
            ) {
                return true
            }
        }
        return false
    }

    private fun usbAdapterId(dev: UsbDevice): String =
        "usb-${formatVidPid(dev.vendorId, dev.productId)}-${dev.deviceId}"

    private fun formatVidPid(vid: Int, pid: Int): String =
        "%04X:%04X".format(vid, pid)
}
