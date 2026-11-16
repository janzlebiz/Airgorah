package com.airgorah.mobile.core.reporting

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.airgorah.mobile.core.database.AccessPointDao
import com.airgorah.mobile.core.database.CaptureArtifactDao
import com.airgorah.mobile.core.database.LabDao
import com.airgorah.mobile.core.database.ReportDao
import com.airgorah.mobile.core.database.ReportEntity
import com.airgorah.mobile.core.database.SecurityFindingDao
import com.airgorah.mobile.core.database.WirelessClientDao
import com.airgorah.mobile.core.hardware.HardwareCapabilityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class ReportType(val title: String) {
    NETWORK_INVENTORY("Network Inventory"),
    WIRELESS_SECURITY_ASSESSMENT("Wireless Security Assessment"),
    HARDWARE_COMPATIBILITY("Hardware Compatibility"),
    CAPTURE_ANALYSIS("Capture Analysis"),
    PMKID_ARTIFACT_ANALYSIS("PMKID Artifact Analysis"),
    DEAUTH_LAB_SIMULATION("Deauthentication Lab Simulation"),
    PASSWORD_SECURITY_ASSESSMENT("Password Security Assessment")
}

enum class ReportFormat(val ext: String) {
    PDF("pdf"),
    JSON("json"),
    CSV("csv")
}

@Serializable
data class ReportRow(
    val category: String,
    val subject: String,
    val detail: String,
    val provenance: String
)

@Serializable
data class StructuredReportPayload(
    val reportId: String,
    val reportType: String,
    val generatedAtIso: String,
    val provenanceLegend: String = "OBSERVED = live Android Wi-Fi scan | IMPORTED = user PCAP file | SIMULATED = Deauth Lab | INFERRED = derived assessment",
    val itemCount: Int,
    val rows: List<ReportRow>
)

@Singleton
class ReportGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accessPointDao: AccessPointDao,
    private val clientDao: WirelessClientDao,
    private val artifactDao: CaptureArtifactDao,
    private val findingDao: SecurityFindingDao,
    private val labDao: LabDao,
    private val reportDao: ReportDao,
    private val hardwareManager: HardwareCapabilityManager
) {
    private val json = Json { prettyPrint = true }

    suspend fun generateReport(
        type: ReportType,
        format: ReportFormat,
        extraNote: String? = null
    ): Result<ReportEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val reportId = UUID.randomUUID().toString()
            val isoTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            val rows = buildRows(type, extraNote)
            val payload = StructuredReportPayload(
                reportId = reportId,
                reportType = type.title,
                generatedAtIso = isoTime,
                itemCount = rows.size,
                rows = rows
            )

            val reportsDir = File(context.filesDir, "reports").apply { mkdirs() }
            val safeName = type.name.lowercase(Locale.US)
            val outFile = File(reportsDir, "airgorah_${safeName}_${System.currentTimeMillis()}.${format.ext}")

            when (format) {
                ReportFormat.JSON -> outFile.writeText(json.encodeToString(payload))
                ReportFormat.CSV -> outFile.writeText(renderCsv(payload))
                ReportFormat.PDF -> renderPdf(payload, outFile)
            }

            val entity = ReportEntity(
                reportId = reportId,
                reportType = type.title,
                format = format.name,
                title = "${type.title} (${format.name})",
                fileUri = outFile.absolutePath,
                createdAtEpochMs = System.currentTimeMillis(),
                itemCount = rows.size
            )
            reportDao.upsert(entity)
            entity
        }
    }

    private suspend fun buildRows(type: ReportType, extraNote: String?): List<ReportRow> {
        return when (type) {
            ReportType.NETWORK_INVENTORY -> accessPointDao.getAll().map {
                ReportRow("AccessPoint", "${it.essid} (${it.bssid})", "Ch ${it.channel} | ${it.band} | ${it.securityProtocol} | ${it.rssiDbm} dBm | ${it.vendor}", it.provenance)
            }
            ReportType.WIRELESS_SECURITY_ASSESSMENT -> findingDao.getAll().map {
                ReportRow(it.category, "[${it.severity}] ${it.title}", "${it.description} Recommendation: ${it.recommendation}", it.provenance)
            }
            ReportType.HARDWARE_COMPATIBILITY -> hardwareManager.adapters.value.map {
                ReportRow(it.category.name, it.displayName, "Scan=${it.canScanManaged}, Monitor=${it.monitorModeStatus}, Raw=${it.rawCaptureStatus} — ${it.diagnosticMessage}", if (it.category.name == "SIMULATED") "SIMULATED" else "OBSERVED")
            }
            ReportType.CAPTURE_ANALYSIS -> artifactDao.getAll().map {
                ReportRow("Capture", it.fileName, "Status=${it.validationStatus} | Packets=${it.packetCount} | APs=${it.bssidCount} | Handshakes=${it.handshakeCount} | PMKIDs=${it.pmkidCount}", "IMPORTED")
            }
            ReportType.PMKID_ARTIFACT_ANALYSIS -> artifactDao.getAll().filter { it.pmkidCount > 0 }.map {
                ReportRow("PMKID", it.fileName, "${it.pmkidCount} PMKID artifact(s) identified. ${it.statusDetail}", "IMPORTED")
            }
            ReportType.DEAUTH_LAB_SIMULATION -> labDao.getAllEvents().map {
                ReportRow(it.eventType, "${it.sourceMac} -> ${it.targetMac}", "Reason=${it.reasonCode} | ${it.detectionNote}", "SIMULATED")
            }
            ReportType.PASSWORD_SECURITY_ASSESSMENT -> listOf(
                ReportRow(
                    "PasswordAudit",
                    "Defensive Policy Assessment (No Plaintext Stored)",
                    extraNote ?: "Password strength evaluation completed in memory. Plaintext passphrases are never logged or exported.",
                    "INFERRED"
                )
            )
        }
    }

    private fun renderCsv(payload: StructuredReportPayload): String = buildString {
        appendLine("report_type,generated_at,category,subject,detail,provenance")
        payload.rows.forEach { r ->
            appendLine(
                listOf(payload.reportType, payload.generatedAtIso, r.category, r.subject, r.detail, r.provenance)
                    .joinToString(",") { csvEscape(it) }
            )
        }
    }

    private fun csvEscape(value: String): String =
        "\"" + value.replace("\"", "\"\"") + "\""

    private fun renderPdf(payload: StructuredReportPayload, outFile: File) {
        val renderedByAndroidPdf = runCatching {
            val doc = PdfDocument()
            try {
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                var page = doc.startPage(pageInfo)
                var canvas: Canvas = page.canvas
                val titlePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 16f
                    isFakeBoldText = true
                }
                val bodyPaint = Paint().apply {
                    color = Color.DKGRAY
                    textSize = 10f
                }

                var y = 48f
                canvas.drawText("Airgorah Mobile — ${payload.reportType}", 36f, y, titlePaint)
                y += 20f
                canvas.drawText("Generated: ${payload.generatedAtIso} | Items: ${payload.itemCount}", 36f, y, bodyPaint)
                y += 16f
                canvas.drawText("Provenance: ${payload.provenanceLegend}", 36f, y, bodyPaint)
                y += 24f

                payload.rows.forEachIndexed { idx, row ->
                    if (y > 790f) {
                        doc.finishPage(page)
                        page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, idx + 2).create())
                        canvas = page.canvas
                        y = 48f
                    }
                    canvas.drawText("[${row.provenance}] ${row.category}: ${row.subject}".take(92), 36f, y, bodyPaint)
                    y += 14f
                    canvas.drawText("   ${row.detail}".take(96), 36f, y, bodyPaint)
                    y += 18f
                }

                doc.finishPage(page)
                FileOutputStream(outFile).use { doc.writeTo(it) }
                outFile.length() > 0L
            } finally {
                doc.close()
            }
        }.getOrDefault(false)

        if (!renderedByAndroidPdf) {
            // Pure-Kotlin minimal PDF 1.4 fallback (used when native libandroid_runtime PdfDocument is absent, e.g., Robolectric)
            val textLines = buildList {
                add("Airgorah Mobile - ${payload.reportType}")
                add("Generated: ${payload.generatedAtIso} | Items: ${payload.itemCount}")
                payload.rows.forEach { add("[${it.provenance}] ${it.category}: ${it.subject} - ${it.detail}") }
            }.joinToString("\\n") { it.replace("(", "[").replace(")", "]") }
            val streamContent = "BT /F1 10 Tf 36 780 Td ($textLines) Tj ET"
            val pdf = "%PDF-1.4\n1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj\n" +
                "2 0 obj<< /Type /Pages /Kids [3 0 R] /Count 1 >>endobj\n" +
                "3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>endobj\n" +
                "4 0 obj<< /Length ${streamContent.length} >>stream\n$streamContent\nendstream endobj\n" +
                "5 0 obj<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>endobj\n" +
                "trailer<< /Root 1 0 R >>\n%%EOF\n"
            outFile.writeText(pdf)
        }
    }
}
