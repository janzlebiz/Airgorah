package com.airgorah.mobile.core.network

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.airgorah.mobile.core.common.DataProvenance
import com.airgorah.mobile.core.common.FindingSeverity
import com.airgorah.mobile.core.database.AccessPointDao
import com.airgorah.mobile.core.database.AccessPointEntity
import com.airgorah.mobile.core.database.CaptureArtifactDao
import com.airgorah.mobile.core.database.CaptureArtifactEntity
import com.airgorah.mobile.core.database.SecurityFindingDao
import com.airgorah.mobile.core.database.SecurityFindingEntity
import com.airgorah.mobile.core.database.WirelessClientDao
import com.airgorah.mobile.core.database.WirelessClientEntity
import com.airgorah.mobile.core.rust.NativeBridge
import com.airgorah.mobile.core.rust.PcapAnalysisDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaptureImportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val artifactDao: CaptureArtifactDao,
    private val accessPointDao: AccessPointDao,
    private val clientDao: WirelessClientDao,
    private val findingDao: SecurityFindingDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun importPcapUri(uri: Uri): Result<CaptureArtifactEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Unable to open selected file stream.")
            val fileName = resolveFileName(uri) ?: "capture_${System.currentTimeMillis()}.cap"
            analyzeAndStoreBytes(fileName, uri.toString(), bytes)
        }
    }

    suspend fun analyzeAndStoreBytes(
        fileName: String,
        uriString: String,
        bytes: ByteArray
    ): CaptureArtifactEntity = withContext(Dispatchers.IO) {
        val analysis = NativeBridge.analyzePcapBytes(bytes).getOrElse { err ->
            PcapAnalysisDto(
                validationStatus = "ANALYSIS_FAILED",
                statusDetail = err.message ?: "Native analysis failed",
                linkType = 0,
                linkTypeName = "Unknown",
                totalPackets = 0,
                decoded80211Frames = 0,
                undecodableFrames = 0,
                truncatedTrailingRecord = false,
                beaconFrames = 0,
                probeRequestFrames = 0,
                probeResponseFrames = 0,
                dataFrames = 0,
                eapolFrames = 0,
                deauthFrames = 0,
                disassocFrames = 0
            )
        }

        val now = System.currentTimeMillis()
        val artifact = CaptureArtifactEntity(
            artifactId = UUID.randomUUID().toString(),
            fileName = fileName,
            uriString = uriString,
            fileSizeBytes = bytes.size.toLong(),
            linkType = analysis.linkType,
            linkTypeName = analysis.linkTypeName,
            packetCount = analysis.totalPackets,
            decodedFrames = analysis.decoded80211Frames,
            bssidCount = analysis.accessPoints.size,
            clientCount = analysis.clients.size,
            handshakeCount = analysis.handshakes.size,
            pmkidCount = analysis.pmkids.size,
            deauthFrameCount = analysis.deauthFrames,
            validationStatus = analysis.validationStatus,
            statusDetail = analysis.statusDetail,
            analysisJson = json.encodeToString(analysis),
            importedAtEpochMs = now
        )
        artifactDao.upsert(artifact)

        if (analysis.accessPoints.isNotEmpty()) {
            val handshakeBssids = analysis.handshakes.filter { it.isCompleteHandshake }.map { it.bssid }.toSet()
            val pmkidBssids = analysis.pmkids.map { it.bssid }.toSet()
            accessPointDao.upsertAll(
                analysis.accessPoints.map { ap ->
                    AccessPointEntity(
                        bssid = ap.bssid,
                        essid = ap.essid,
                        channel = ap.channel ?: 0,
                        frequencyMhz = 0,
                        band = ap.band,
                        rssiDbm = ap.signalDbm ?: -99,
                        securityProtocol = ap.privacy,
                        capabilitiesRaw = "Imported PCAP",
                        vendor = ap.vendor,
                        isHidden = ap.hidden,
                        hasHandshake = ap.bssid in handshakeBssids,
                        hasPmkid = ap.bssid in pmkidBssids,
                        firstSeenEpochMs = now,
                        lastSeenEpochMs = now,
                        provenance = DataProvenance.IMPORTED.name,
                        classification = WifiDiscoveryRepository.classifyRisk(ap.privacy)
                    )
                }
            )
        }

        if (analysis.clients.isNotEmpty()) {
            clientDao.upsertAll(
                analysis.clients.map { cl ->
                    WirelessClientEntity(
                        mac = cl.mac,
                        associatedBssid = cl.associatedBssid,
                        vendor = cl.vendor,
                        packetCount = cl.packetCount,
                        rssiDbm = cl.signalDbm,
                        probedSsids = cl.probedSsids.joinToString(", "),
                        firstSeenEpochMs = now,
                        lastSeenEpochMs = now,
                        provenance = DataProvenance.IMPORTED.name,
                        connectionState = if (cl.associatedBssid != null) "Associated (Observed)" else "Unlinked / Probing"
                    )
                }
            )
        }

        val newFindings = mutableListOf<SecurityFindingEntity>()
        if (analysis.deauthFrames > 5) {
            newFindings += SecurityFindingEntity(
                findingId = "pcap-deauth-${artifact.artifactId}",
                category = "Deauthentication Detection",
                severity = FindingSeverity.HIGH.name,
                title = "Elevated Deauthentication Frames in ${artifact.fileName}",
                description = "Detected ${analysis.deauthFrames} 802.11 deauthentication frames in imported capture.",
                recommendation = "Verify whether 802.11w Protected Management Frames (PMF) is enforced on affected APs.",
                targetBssid = null,
                provenance = DataProvenance.IMPORTED.name,
                createdAtEpochMs = now
            )
        }
        analysis.pmkids.filter { it.isWpa2PskPmkid }.forEach { pmkid ->
            newFindings += SecurityFindingEntity(
                findingId = "pmkid-${pmkid.bssid}",
                category = "PMKID Exposure",
                severity = FindingSeverity.MEDIUM.name,
                title = "WPA2-PSK PMKID Observed (${pmkid.essid})",
                description = "EAPOL M1 from ${pmkid.bssid} includes a WPA2-PSK PMKID KDE (key descriptor ${pmkid.keyDescriptorVersion}).",
                recommendation = "Use a strong 16+ character passphrase or upgrade to WPA3-SAE.",
                targetBssid = pmkid.bssid,
                provenance = DataProvenance.IMPORTED.name,
                createdAtEpochMs = now
            )
        }
        if (newFindings.isNotEmpty()) {
            findingDao.upsertAll(newFindings)
        }

        artifact
    }

    suspend fun deleteArtifact(artifactId: String) = artifactDao.deleteById(artifactId)

    private fun resolveFileName(uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    }
}
