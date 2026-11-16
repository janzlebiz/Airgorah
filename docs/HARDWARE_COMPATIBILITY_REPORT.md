# Hardware Compatibility Report — Airgorah Mobile

## 1. Hardware Categories Implemented
1. **Built-in Android Wi-Fi (`BUILT_IN_WIFI`)**: Queries `WifiManager` for Wi-Fi state, 2.4 GHz / 5 GHz / 6 GHz band support, and managed-mode `ScanResult` discovery. Explicitly marks monitor mode, raw capture, and packet injection as `UNSUPPORTED` on unprivileged Android.
2. **External USB-OTG Adapters (`EXTERNAL_USB`)**: Enumerates attached USB devices via `UsbManager`, inspects device/interface classes, handles `PendingIntent` USB permission requests, and safely handles `ACTION_USB_DEVICE_DETACHED` without crashing. Marks monitor mode and raw capture as `CONDITIONAL` (never inferred from chipset/VID/PID).
3. **Unsupported Hardware (`UNSUPPORTED`)**: Gracefully reports missing `android.hardware.wifi` or non-wireless USB devices without crashing.
4. **Simulated Test Hardware (`SIMULATED`)**: Provides `SIM-WLAN-01` for deterministic UI and Deauthentication Lab verification, clearly badged as `SIMULATED`.

## 2. Verification Scope & Limitations
- Automated tests (`HardwareCapabilityManagerTest`) verified capability matrix generation, `SIMULATED` adapter reporting, `packetInjectionStatus == UNSUPPORTED`, and safe `ACTION_USB_DEVICE_DETACHED` broadcast handling under Robolectric.
- Physical Samsung Galaxy + USB-OTG adapter field testing was **not** performed in this cloud build environment and is required on target hardware before claiming physical OTG adapter compatibility.
