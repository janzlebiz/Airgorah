package com.airgorah.mobile.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class PermissionStatusSnapshot(
    val hasLocationPermission: Boolean = false,
    val hasNearbyWifiPermission: Boolean = false,
    val isLocationServiceEnabled: Boolean = false,
    val requiredWifiScanPermissions: List<String> = emptyList(),
    val missingPermissions: List<String> = emptyList(),
    val rationaleMessage: String = ""
) {
    val canPerformWifiScan: Boolean
        get() = missingPermissions.isEmpty() && isLocationServiceEnabled
}

@Singleton
class PermissionCoordinator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _status = MutableStateFlow(evaluate())
    val status: StateFlow<PermissionStatusSnapshot> = _status.asStateFlow()

    fun refresh(): PermissionStatusSnapshot {
        val next = evaluate()
        _status.value = next
        return next
    }

    private fun evaluate(): PermissionStatusSnapshot {
        val required = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }
        val missing = required.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        val hasLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasNearby = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val locationEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm?.isLocationEnabled == true
        } else {
            true
        }

        val rationale = when {
            missing.isNotEmpty() ->
                "Android requires Location and Nearby Wi-Fi permissions to read Wi-Fi scan results (SSID, BSSID, channel, signal). Airgorah Mobile never transmits location data."
            !locationEnabled ->
                "Device Location Services must be enabled on Android/Samsung devices for WifiManager to return nearby access points."
            else ->
                "All required Wi-Fi discovery permissions are granted."
        }

        return PermissionStatusSnapshot(
            hasLocationPermission = hasLocation,
            hasNearbyWifiPermission = hasNearby,
            isLocationServiceEnabled = locationEnabled,
            requiredWifiScanPermissions = required,
            missingPermissions = missing,
            rationaleMessage = rationale
        )
    }
}
