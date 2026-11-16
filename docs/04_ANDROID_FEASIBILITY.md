# 04 — Android Feasibility Analysis

## 1. Platform Constraints (Non-Rooted Android, API 26–35)
1. **Wi-Fi Discovery (`WifiManager`)**:
   - **Supported**: `WifiManager.getScanResults()`, `WifiManager.startScan()` (throttled on Android 9+: 4 scans per 2 minutes for foreground apps, `WifiManager.ScanResultsCallback` on API 30+), `WifiInfo` / `ConnectivityManager` for active connection metadata.
   - **Permissions Required**: `ACCESS_FINE_LOCATION` (API 26–32), `NEARBY_WIFI_DEVICES` (API 33+, with `neverForLocation` or combined with `ACCESS_FINE_LOCATION` for BSSID/SSID access), `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`.
   - **Samsung Galaxy Specifics**: Samsung One UI enforces background scan throttling and requires Location Services to be enabled globally on the device for `getScanResults()` to return non-empty results.
2. **Raw Packet Capture & Monitor Mode**:
   - **Built-in Android Wi-Fi**: Standard Android Wi-Fi drivers (Broadcom/Qualcomm/Exynos) do **not** expose `AF_PACKET` `SOCK_RAW` monitor mode or radiotap injection to unprivileged apps. `NET_ADMIN` / `NET_RAW` capabilities are stripped from regular APK processes by SELinux and kernel UID/GID checks.
   - **External USB-OTG Adapters**: Android's `UsbManager` (`android.hardware.usb.host`) allows unprivileged apps to enumerate USB devices, request user permission (`PendingIntent`), and open a `UsbDeviceConnection` to claim interfaces and perform bulk/interrupt transfers. However, raw 802.11 monitor mode over USB-OTG requires either a kernel driver (`mac80211` + `nl80211` — unavailable to unprivileged apps without root) or a userspace USB driver/firmware bridge, and **must never be assumed** simply because a USB device is attached.
3. **PCAP / Handshake / PMKID Analysis**:
   - **Fully Feasible**: Android Storage Access Framework (`ACTION_OPEN_DOCUMENT`) allows users to import `.cap` / `.pcap` files from device storage or external OTG drives. The Rust core (`libwifi` + `radiotap` + `PcapReader`) parses these offline in-process via JNI with zero special privileges.
4. **Defensive Security Lab & Password Auditing**:
   - **Fully Feasible**: Offline educational/defensive deauthentication simulation and defensive password strength auditing run natively in Kotlin + Rust without network or root privileges.

## 2. Build & Toolchain Feasibility in Current Environment
- **Available**: OpenJDK 17 (`17.0.21`), `rustc 1.99.1`, `cargo 1.99.1`, `git`, `gh`.
- **To Provision in Phase 02/03 (User Space)**:
  - Android SDK Command-Line Tools (`sdkmanager`, `platform-tools`, `build-tools;35.0.0`, `platforms;android-35`)
  - Android NDK (e.g., `ndk;27.x` or `26.x`)
  - Rust target `aarch64-linux-android` (`rustup target add aarch64-linux-android`) + `cargo-ndk` (or direct NDK clang linker configuration)
  - Gradle wrapper (`./gradlew`) for reproducible offline/CI APK builds.

## 3. Min / Target SDK Decision
- **Minimum SDK**: `26` (Android 8.0 Oreo) — supports `java.time`, modern `NotificationChannel`, `UsbManager` host APIs, and Jetpack Compose + Material 3 without legacy compat bloat.
- **Target SDK**: `35` (Android 15) — current stable supported Android target, with proper `NEARBY_WIFI_DEVICES` (API 33+) and foreground service type declarations if needed.
- **Primary ABI**: `arm64-v8a` (`aarch64-linux-android`).
