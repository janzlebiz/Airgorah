package com.airgorah.mobile.core.hardware

import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.airgorah.mobile.core.common.CapabilityStatus
import com.airgorah.mobile.core.common.HardwareCategory
import com.airgorah.mobile.core.database.AirgorahDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HardwareCapabilityManagerTest {
    private lateinit var ctx: Context
    private lateinit var db: AirgorahDatabase
    private lateinit var manager: HardwareCapabilityManager

    @Before
    fun setup() {
        ctx = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(ctx, AirgorahDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        manager = HardwareCapabilityManager(ctx, db.wirelessAdapterDao())
    }

    @After
    fun teardown() {
        manager.unregisterUsbReceiver()
        db.close()
    }

    @Test
    fun reportsSimulatedAndHandlesUsbDisconnectSafely() {
        manager.registerUsbReceiver()
        val list = manager.refreshHardware()
        assertTrue(list.any { it.category == HardwareCategory.SIMULATED })
        assertTrue(list.all { it.packetInjectionStatus == CapabilityStatus.UNSUPPORTED })

        // Simulate USB detach broadcast without attached device — must not crash
        ctx.sendBroadcast(Intent(UsbManager.ACTION_USB_DEVICE_DETACHED))
        assertEquals("builtin-wlan0", manager.selectedAdapterId.value)
    }
}
