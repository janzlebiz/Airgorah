package com.airgorah.mobile.core.reporting

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.airgorah.mobile.core.database.AirgorahDatabase
import com.airgorah.mobile.core.hardware.HardwareCapabilityManager
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ReportGeneratorTest {
    private lateinit var db: AirgorahDatabase
    private lateinit var generator: ReportGenerator

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AirgorahDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val hw = HardwareCapabilityManager(ctx, db.wirelessAdapterDao())
        hw.refreshHardware()
        generator = ReportGenerator(
            ctx,
            db.accessPointDao(),
            db.wirelessClientDao(),
            db.captureArtifactDao(),
            db.securityFindingDao(),
            db.labDao(),
            db.reportDao(),
            hw
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun generatesJsonCsvAndPdfReports() = runTest {
        val jsonRes = generator.generateReport(ReportType.HARDWARE_COMPATIBILITY, ReportFormat.JSON)
        assertTrue(jsonRes.isSuccess)
        val jsonFile = File(jsonRes.getOrThrow().fileUri)
        assertTrue(jsonFile.exists())
        assertTrue(jsonFile.readText().contains("Hardware Compatibility"))

        val csvRes = generator.generateReport(ReportType.HARDWARE_COMPATIBILITY, ReportFormat.CSV)
        assertTrue(csvRes.isSuccess)
        assertTrue(File(csvRes.getOrThrow().fileUri).readText().contains("report_type"))

        val pdfRes = generator.generateReport(ReportType.HARDWARE_COMPATIBILITY, ReportFormat.PDF)
        assertTrue(pdfRes.isSuccess)
        assertTrue(File(pdfRes.getOrThrow().fileUri).length() > 0)
    }
}
