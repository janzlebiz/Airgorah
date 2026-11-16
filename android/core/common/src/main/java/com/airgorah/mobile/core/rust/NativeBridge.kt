package com.airgorah.mobile.core.rust

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class JniEnvelope<T>(
    val ok: Boolean,
    val data: T? = null,
    val error: String? = null
)

@Serializable
data class CoreVersionInfo(
    val version: String,
    val arch: String,
    val os: String
)

@Serializable
data class HandshakeArtifactDto(
    val bssid: String,
    val essid: String,
    @SerialName("station_mac") val stationMac: String,
    @SerialName("has_m1") val hasM1: Boolean,
    @SerialName("has_m2") val hasM2: Boolean,
    @SerialName("has_m3") val hasM3: Boolean,
    @SerialName("has_m4") val hasM4: Boolean,
    @SerialName("is_complete_handshake") val isCompleteHandshake: Boolean,
    val status: String
)

@Serializable
data class PmkidArtifactDto(
    val bssid: String,
    val essid: String,
    @SerialName("station_mac") val stationMac: String,
    @SerialName("key_descriptor_version") val keyDescriptorVersion: Int,
    @SerialName("is_wpa2_psk_pmkid") val isWpa2PskPmkid: Boolean,
    @SerialName("is_wpa3_sae_pmkid") val isWpa3SaePmkid: Boolean,
    val status: String
)

@Serializable
data class ExtractedApDto(
    val bssid: String,
    val essid: String,
    val hidden: Boolean,
    val channel: Int? = null,
    val band: String,
    val privacy: String,
    @SerialName("signal_dbm") val signalDbm: Int? = null,
    val vendor: String,
    @SerialName("beacon_count") val beaconCount: Long
)

@Serializable
data class ExtractedClientDto(
    val mac: String,
    @SerialName("associated_bssid") val associatedBssid: String? = null,
    val vendor: String,
    @SerialName("packet_count") val packetCount: Long,
    @SerialName("signal_dbm") val signalDbm: Int? = null,
    @SerialName("probed_ssids") val probedSsids: List<String> = emptyList()
)

@Serializable
data class PcapAnalysisDto(
    @SerialName("validation_status") val validationStatus: String,
    @SerialName("status_detail") val statusDetail: String,
    @SerialName("link_type") val linkType: Long,
    @SerialName("link_type_name") val linkTypeName: String,
    @SerialName("total_packets") val totalPackets: Long,
    @SerialName("decoded_80211_frames") val decoded80211Frames: Long,
    @SerialName("undecodable_frames") val undecodableFrames: Long,
    @SerialName("truncated_trailing_record") val truncatedTrailingRecord: Boolean,
    @SerialName("beacon_frames") val beaconFrames: Long,
    @SerialName("probe_request_frames") val probeRequestFrames: Long,
    @SerialName("probe_response_frames") val probeResponseFrames: Long,
    @SerialName("data_frames") val dataFrames: Long,
    @SerialName("eapol_frames") val eapolFrames: Long,
    @SerialName("deauth_frames") val deauthFrames: Long,
    @SerialName("disassoc_frames") val disassocFrames: Long,
    @SerialName("access_points") val accessPoints: List<ExtractedApDto> = emptyList(),
    val clients: List<ExtractedClientDto> = emptyList(),
    val handshakes: List<HandshakeArtifactDto> = emptyList(),
    val pmkids: List<PmkidArtifactDto> = emptyList()
)

@Serializable
data class PasswordAuditDto(
    val length: Int,
    @SerialName("wpa_psk_length_valid") val wpaPskLengthValid: Boolean,
    @SerialName("has_lowercase") val hasLowercase: Boolean,
    @SerialName("has_uppercase") val hasUppercase: Boolean,
    @SerialName("has_digits") val hasDigits: Boolean,
    @SerialName("has_symbols") val hasSymbols: Boolean,
    @SerialName("estimated_entropy_bits") val estimatedEntropyBits: Double,
    @SerialName("entropy_methodology_note") val entropyMethodologyNote: String,
    val rating: String,
    val warnings: List<String> = emptyList(),
    val recommendations: List<String> = emptyList()
)

object NativeBridge {
    private val json = Json { ignoreUnknownKeys = true }

    var isLoaded: Boolean = false
        private set
    var loadError: String? = null
        private set

    init {
        try {
            System.loadLibrary("airgorah_core")
            isLoaded = true
        } catch (t: Throwable) {
            isLoaded = false
            loadError = t.message ?: t::class.java.simpleName
        }
    }

    external fun nativeGetVersion(): String
    external fun nativeLookupVendor(mac: String): String
    external fun nativeLookupVendorsBatch(macsJson: String): String
    external fun nativeValidateChannelFilter(filter: String, ghz24: Boolean, ghz5: Boolean): String
    external fun nativeAnalyzePcapBytes(data: ByteArray): String
    external fun nativeAuditPasswordStrength(passwordBytes: ByteArray): String

    fun getVersion(): Result<CoreVersionInfo> = callSafe {
        val raw = nativeGetVersion()
        val env = json.decodeFromString<JniEnvelope<CoreVersionInfo>>(raw)
        if (env.ok && env.data != null) env.data else error(env.error ?: "Unknown native error")
    }

    fun lookupVendor(mac: String): Result<String> = callSafe {
        val raw = nativeLookupVendor(mac)
        val env = json.decodeFromString<JniEnvelope<String>>(raw)
        if (env.ok && env.data != null) env.data else error(env.error ?: "Unknown native error")
    }

    fun lookupVendorsBatch(macs: List<String>): Result<Map<String, String>> = callSafe {
        val raw = nativeLookupVendorsBatch(json.encodeToString(macs))
        val env = json.decodeFromString<JniEnvelope<Map<String, String>>>(raw)
        if (env.ok && env.data != null) env.data else error(env.error ?: "Unknown native error")
    }

    fun validateChannelFilter(filter: String, ghz24: Boolean, ghz5: Boolean): Result<Boolean> = callSafe {
        val raw = nativeValidateChannelFilter(filter, ghz24, ghz5)
        val env = json.decodeFromString<JniEnvelope<Boolean>>(raw)
        if (env.ok && env.data != null) env.data else error(env.error ?: "Unknown native error")
    }

    fun analyzePcapBytes(data: ByteArray): Result<PcapAnalysisDto> = callSafe {
        val raw = nativeAnalyzePcapBytes(data)
        val env = json.decodeFromString<JniEnvelope<PcapAnalysisDto>>(raw)
        if (env.ok && env.data != null) env.data else error(env.error ?: "Unknown native error")
    }

    fun auditPasswordStrength(passwordChars: CharArray): Result<PasswordAuditDto> = callSafe {
        val bytes = String(passwordChars).toByteArray(Charsets.UTF_8)
        try {
            val raw = nativeAuditPasswordStrength(bytes)
            val env = json.decodeFromString<JniEnvelope<PasswordAuditDto>>(raw)
            if (env.ok && env.data != null) env.data else error(env.error ?: "Unknown native error")
        } finally {
            bytes.fill(0)
        }
    }

    private inline fun <T> callSafe(block: () -> T): Result<T> {
        if (!isLoaded) {
            return Result.failure(IllegalStateException("Native library not loaded: ${loadError ?: "unknown"}"))
        }
        return runCatching(block)
    }
}
