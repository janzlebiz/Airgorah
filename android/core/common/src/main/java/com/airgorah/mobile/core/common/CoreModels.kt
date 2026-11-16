package com.airgorah.mobile.core.common

import kotlinx.serialization.Serializable

@Serializable
enum class DataProvenance {
    OBSERVED,
    IMPORTED,
    SIMULATED,
    INFERRED
}

@Serializable
enum class OperationState {
    READY,
    RUNNING,
    COMPLETED,
    UNSUPPORTED,
    PERMISSION_REQUIRED,
    HARDWARE_UNAVAILABLE,
    SIMULATED
}

@Serializable
enum class CapabilityStatus {
    SUPPORTED,
    UNSUPPORTED,
    CONDITIONAL,
    SIMULATED
}

@Serializable
enum class HardwareCategory {
    BUILT_IN_WIFI,
    EXTERNAL_USB,
    UNSUPPORTED,
    SIMULATED
}

@Serializable
enum class ArtifactValidationStatus {
    VALID,
    INVALID,
    INCOMPLETE,
    UNSUPPORTED_FORMAT,
    ANALYSIS_FAILED
}

@Serializable
enum class FindingSeverity {
    INFO,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

@Serializable
enum class SecurityProtocol {
    OPEN,
    WEP,
    WPA,
    WPA2,
    WPA3,
    UNKNOWN
}

fun frequencyToChannel(freqMhz: Int): Int? = when {
    freqMhz == 2484 -> 14
    freqMhz in 2412..2472 -> (freqMhz - 2407) / 5
    freqMhz in 5180..5885 -> (freqMhz - 5000) / 5
    freqMhz in 5955..7115 -> (freqMhz - 5950) / 5
    else -> null
}

fun frequencyToBand(freqMhz: Int): String = when {
    freqMhz in 2400..2500 -> "2.4 GHz"
    freqMhz in 5000..5900 -> "5 GHz"
    freqMhz in 5925..7125 -> "6 GHz"
    else -> "Unknown"
}
