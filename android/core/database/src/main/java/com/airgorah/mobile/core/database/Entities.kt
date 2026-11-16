package com.airgorah.mobile.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "access_points")
data class AccessPointEntity(
    @PrimaryKey val bssid: String,
    val essid: String,
    val channel: Int,
    val frequencyMhz: Int,
    val band: String,
    val rssiDbm: Int,
    val securityProtocol: String,
    val capabilitiesRaw: String,
    val vendor: String,
    val isHidden: Boolean,
    val hasHandshake: Boolean,
    val hasPmkid: Boolean,
    val firstSeenEpochMs: Long,
    val lastSeenEpochMs: Long,
    val provenance: String,
    val classification: String
)

@Entity(tableName = "wireless_clients")
data class WirelessClientEntity(
    @PrimaryKey val mac: String,
    val associatedBssid: String?,
    val vendor: String,
    val packetCount: Long,
    val rssiDbm: Int?,
    val probedSsids: String,
    val firstSeenEpochMs: Long,
    val lastSeenEpochMs: Long,
    val provenance: String,
    val connectionState: String
)

@Entity(tableName = "wireless_adapters")
data class WirelessAdapterEntity(
    @PrimaryKey val adapterId: String,
    val name: String,
    val category: String,
    val vendorId: Int?,
    val productId: Int?,
    val driverInfo: String,
    val supports24Ghz: Boolean,
    val supports5Ghz: Boolean,
    val supports6Ghz: Boolean,
    val monitorModeStatus: String,
    val rawCaptureStatus: String,
    val isConnected: Boolean,
    val lastSeenEpochMs: Long
)

@Entity(tableName = "capture_sessions")
data class CaptureSessionEntity(
    @PrimaryKey val sessionId: String,
    val adapterId: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long?,
    val channelPlan: String,
    val status: String,
    val packetCount: Long,
    val storageBytes: Long,
    val notes: String
)

@Entity(tableName = "capture_artifacts")
data class CaptureArtifactEntity(
    @PrimaryKey val artifactId: String,
    val fileName: String,
    val uriString: String,
    val fileSizeBytes: Long,
    val linkType: Long,
    val linkTypeName: String,
    val packetCount: Long,
    val decodedFrames: Long,
    val bssidCount: Int,
    val clientCount: Int,
    val handshakeCount: Int,
    val pmkidCount: Int,
    val deauthFrameCount: Long,
    val validationStatus: String,
    val statusDetail: String,
    val analysisJson: String,
    val importedAtEpochMs: Long
)

@Entity(tableName = "security_findings")
data class SecurityFindingEntity(
    @PrimaryKey val findingId: String,
    val category: String,
    val severity: String,
    val title: String,
    val description: String,
    val recommendation: String,
    val targetBssid: String?,
    val provenance: String,
    val createdAtEpochMs: Long
)

@Entity(tableName = "audit_sessions")
data class AuditSessionEntity(
    @PrimaryKey val auditId: String,
    val auditType: String,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long?,
    val summary: String,
    val findingsCount: Int
)

@Entity(tableName = "lab_scenarios")
data class LabScenarioEntity(
    @PrimaryKey val scenarioId: String,
    val name: String,
    val topologyJson: String,
    val pmfEnabled: Boolean,
    val description: String,
    val createdAtEpochMs: Long
)

@Entity(tableName = "lab_events")
data class LabEventEntity(
    @PrimaryKey val eventId: String,
    val scenarioId: String,
    val timestampEpochMs: Long,
    val eventType: String,
    val sourceMac: String,
    val targetMac: String,
    val reasonCode: Int,
    val severity: String,
    val isSimulated: Boolean,
    val detectionNote: String
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val reportId: String,
    val reportType: String,
    val format: String,
    val title: String,
    val fileUri: String,
    val createdAtEpochMs: Long,
    val itemCount: Int
)

@Entity(tableName = "application_settings")
data class ApplicationSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val displayHiddenAps: Boolean = true,
    val autoEnrichOui: Boolean = true,
    val default24Ghz: Boolean = true,
    val default5Ghz: Boolean = true,
    val channelFilter: String = "",
    val pmfSimulationDefault: Boolean = false,
    val lastUpdatedEpochMs: Long = System.currentTimeMillis()
)
