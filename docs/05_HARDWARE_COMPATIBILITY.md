# 05 — Hardware Compatibility Matrix

## 1. Hardware Classification Model
Airgorah Mobile's Hardware Abstraction Layer (HAL) strictly separates wireless hardware into four categories:

| Category | Examples | Discovery Mechanism | Capabilities | Limitations |
|---|---|---|---|---|
| **A. Built-in Android Wi-Fi** | Samsung Galaxy S/A/Z series (Qualcomm/Broadcom/Exynos) | `WifiManager` + `ConnectivityManager` | Managed-mode AP discovery (`ScanResult`), RSSI, frequency, channel width, security capabilities (WPA2/WPA3/WEP/Open), active link info | No monitor mode, no raw 802.11 frame capture, no packet injection, OS scan throttling |
| **B. External USB-OTG Adapters** | USB Wi-Fi dongles (Realtek RTL8812AU/8814AU, Atheros AR9271, Ralink RT3070, MediaTek MT7612U) | `UsbManager` (`ACTION_USB_DEVICE_ATTACHED` / `deviceList`) | USB VID/PID identification, interface/endpoint descriptor inspection, OTG power/bus diagnostics, driver/capability matrix reporting | Detection via `UsbManager` does **not** imply monitor mode or kernel `nl80211` access in unprivileged Android apps |
| **C. Unsupported Hardware** | Non-Wi-Fi USB devices, devices without `android.hardware.wifi` or `android.hardware.usb.host`, denied USB permission | `PackageManager.hasSystemFeature` + `UsbManager` | Graceful capability report explaining why feature is unavailable | Advanced capture/monitoring controls remain disabled with clear diagnostic message |
| **D. Simulated Test Hardware** | Built-in Mock/Lab Adapter (`SIM-WLAN-01`) | Internal `SimulatedHardwareProvider` | Deterministic testing of UI states, capability matrix, and Deauthentication Lab topology | Clearly badged as `Simulated` in UI and reports; never presented as real radio data |

## 2. Capability Matrix Per Device
Every detected device produces a `HardwareCapabilityMatrix`:
- `canScanManaged`: Boolean (true for Built-in Wi-Fi when permissions & location enabled)
- `canInspectUsbDescriptors`: Boolean (true for USB-OTG when `UsbManager` permission granted)
- `supportsMonitorMode`: `CapabilityStatus` (`SUPPORTED`, `UNSUPPORTED`, `CONDITIONAL_ROOT_OR_DRIVER_REQUIRED`, `SIMULATED`)
- `supportsRawCapture`: `CapabilityStatus`
- `supportsPacketInjection`: `CapabilityStatus` (always `UNSUPPORTED` in Airgorah Mobile)
- `supportedBands`: List (`2.4 GHz`, `5 GHz`, `6 GHz` where reported by `WifiManager.is5GHzBandSupported` / `is6GHzBandSupported`)

## 3. Honesty & Safety Guarantees
- Never infer monitor-mode availability solely from a USB VID/PID or chipset name.
- Never claim real hardware compatibility based solely on mocked tests.
- Handle USB disconnect events (`ACTION_USB_DEVICE_DETACHED`) gracefully without crashing.
