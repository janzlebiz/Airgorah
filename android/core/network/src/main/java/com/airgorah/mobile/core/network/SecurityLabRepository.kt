package com.airgorah.mobile.core.network

import com.airgorah.mobile.core.common.DataProvenance
import com.airgorah.mobile.core.common.FindingSeverity
import com.airgorah.mobile.core.database.LabDao
import com.airgorah.mobile.core.database.LabEventEntity
import com.airgorah.mobile.core.database.LabScenarioEntity
import com.airgorah.mobile.core.database.SecurityFindingDao
import com.airgorah.mobile.core.database.SecurityFindingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityLabRepository @Inject constructor(
    private val labDao: LabDao,
    private val findingDao: SecurityFindingDao
) {
    suspend fun runDeauthSimulation(pmfEnabled: Boolean) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val scenarioId = UUID.randomUUID().toString()
        val scenario = LabScenarioEntity(
            scenarioId = scenarioId,
            name = if (pmfEnabled) "802.11w PMF Protected Topology" else "Unprotected Legacy WPA2 Topology",
            topologyJson = """{"ap":"AA:BB:CC:11:22:33","clients":["DE:AD:BE:EF:00:01","DE:AD:BE:EF:00:02","DE:AD:BE:EF:00:03"],"pmfEnabled":$pmfEnabled}""",
            pmfEnabled = pmfEnabled,
            description = "Simulated defensive deauthentication detection drill. No real 802.11 frames are transmitted.",
            createdAtEpochMs = now
        )
        labDao.upsertScenario(scenario)

        val events = listOf(
            LabEventEntity(
                eventId = UUID.randomUUID().toString(),
                scenarioId = scenarioId,
                timestampEpochMs = now,
                eventType = "DEAUTH_BURST_SIMULATED",
                sourceMac = "AA:BB:CC:11:22:33 (Spoofed)",
                targetMac = "FF:FF:FF:FF:FF:FF (Broadcast)",
                reasonCode = 7,
                severity = if (pmfEnabled) FindingSeverity.INFO.name else FindingSeverity.HIGH.name,
                isSimulated = true,
                detectionNote = if (pmfEnabled) {
                    "PMF (802.11w) active: Unauthenticated broadcast deauth frames ignored by PMF-capable stations; Security Association Query (SA Query) triggered."
                } else {
                    "PMF disabled: Spike of unprotected Reason 7 deauthentication frames detected; stations vulnerable to forced disconnect."
                }
            ),
            LabEventEntity(
                eventId = UUID.randomUUID().toString(),
                scenarioId = scenarioId,
                timestampEpochMs = now + 150,
                eventType = if (pmfEnabled) "PMF_SA_QUERY_VERIFIED" else "CLIENT_RECONNECT_STORM",
                sourceMac = "DE:AD:BE:EF:00:01",
                targetMac = "AA:BB:CC:11:22:33",
                reasonCode = if (pmfEnabled) 0 else 6,
                severity = if (pmfEnabled) FindingSeverity.INFO.name else FindingSeverity.MEDIUM.name,
                isSimulated = true,
                detectionNote = if (pmfEnabled) {
                    "Station maintained association; WIDS logged spoofed management frame attempt."
                } else {
                    "Multiple clients re-associating within 500ms window — classic indicator of a deauthentication disruption event."
                }
            )
        )
        labDao.insertEvents(events)

        findingDao.upsertAll(
            listOf(
                SecurityFindingEntity(
                    findingId = "lab-$scenarioId",
                    category = "Deauthentication Lab (Simulated)",
                    severity = if (pmfEnabled) FindingSeverity.INFO.name else FindingSeverity.HIGH.name,
                    title = if (pmfEnabled) "Simulated PMF Defense Verified" else "Simulated Deauth Exposure Detected",
                    description = events.first().detectionNote,
                    recommendation = "Enforce 802.11w Protected Management Frames (PMF = Required on WPA3, Capable/Required on WPA2).",
                    targetBssid = "AA:BB:CC:11:22:33",
                    provenance = DataProvenance.SIMULATED.name,
                    createdAtEpochMs = now
                )
            )
        )
    }

    suspend fun clearSimulationEvents() = labDao.clearEvents()
}
