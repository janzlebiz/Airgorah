package com.airgorah.mobile.core.common

import com.airgorah.mobile.core.rust.NativeBridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class CoreModelsAndNativeBridgeTest {
    @Test
    fun frequencyToChannelMaps24And5Ghz() {
        assertEquals(1, frequencyToChannel(2412))
        assertEquals(6, frequencyToChannel(2437))
        assertEquals(14, frequencyToChannel(2484))
        assertEquals(36, frequencyToChannel(5180))
        assertEquals(149, frequencyToChannel(5745))
        assertNull(frequencyToChannel(900))
    }

    @Test
    fun frequencyToBandMapsCorrectly() {
        assertEquals("2.4 GHz", frequencyToBand(2437))
        assertEquals("5 GHz", frequencyToBand(5180))
        assertEquals("6 GHz", frequencyToBand(5955))
        assertEquals("Unknown", frequencyToBand(100))
    }

    @Test
    fun nativeBridgeFailsGracefullyOnHostJvmWithoutSo() {
        // On host JVM without Android .so, NativeBridge should not throw uncaught exceptions
        val res = NativeBridge.lookupVendor("00:00:0C:11:22:33")
        assertFalse(NativeBridge.isLoaded)
        assert(res.isFailure)
    }
}
