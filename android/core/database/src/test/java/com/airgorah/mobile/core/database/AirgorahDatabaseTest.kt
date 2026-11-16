package com.airgorah.mobile.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AirgorahDatabaseTest {
    private lateinit var db: AirgorahDatabase

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AirgorahDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun accessPointAndClientCrudWorks() = runTest {
        val ap = AccessPointEntity(
            bssid = "AA:BB:CC:11:22:33",
            essid = "LabNet",
            channel = 6,
            frequencyMhz = 2437,
            band = "2.4 GHz",
            rssiDbm = -55,
            securityProtocol = "WPA2",
            capabilitiesRaw = "[WPA2-PSK-CCMP]",
            vendor = "TestVendor",
            isHidden = false,
            hasHandshake = false,
            hasPmkid = false,
            firstSeenEpochMs = 1000L,
            lastSeenEpochMs = 2000L,
            provenance = "OBSERVED",
            classification = "Standard (WPA2)"
        )
        db.accessPointDao().upsertAll(listOf(ap))
        assertEquals(1, db.accessPointDao().getAll().size)
        assertEquals("LabNet", db.accessPointDao().getByBssid("AA:BB:CC:11:22:33")?.essid)

        db.accessPointDao().clearAll()
        assertNull(db.accessPointDao().getByBssid("AA:BB:CC:11:22:33"))
    }
}
