# Release Gate Report — Airgorah Mobile

## 1. Release Artifact Metadata
- **APK Path**: `android/app/build/outputs/apk/release/app-release.apk`
- **APK Size**: ~15 MB (includes `lib/arm64-v8a/libairgorah_core.so`, 2,905,224 bytes)
- **Package ID**: `com.airgorah.mobile`
- **Version Name**: `0.9.0-mobile.1`
- **Version Code**: `1`
- **Minimum Android Version**: API 26 (Android 8.0)
- **Target SDK**: API 35 (Android 15)
- **Supported ABIs**: `arm64-v8a`
- **Signed**: Yes — verified with `apksigner verify` (APK Signature Scheme v2, 1 signer)
- **Debuggable**: `false` (`isDebuggable = false` in release buildType)
- **Test Totals**: 28 passed, 0 failed (21 Rust + 7 Android/Robolectric)

## 2. Release Gate Checklist
- [x] **No startup crashes / UI rendering**: Verified via `AirgorahUiTest` and Hilt + Compose compilation.
- [x] **All 9 navigation destinations + 4 Security Lab tabs implemented**: Dashboard, Networks, Clients, Monitoring, Captures, Security Lab (Deauth Lab, Handshake, PMKID, Password Audit), Hardware, Reports, Settings.
- [x] **Rust library compiled & packaged**: `libairgorah_core.so` (`aarch64-linux-android`) verified inside `app-release.apk`.
- [x] **JNI panic containment**: Verified via Rust unit test.
- [x] **Permissions & Offline operation**: No `INTERNET` permission; least-privilege Wi-Fi/Location/USB permissions with rationales.
- [x] **USB disconnection safety**: Verified via `HardwareCapabilityManagerTest`.
- [x] **Reports export**: PDF, JSON, CSV verified via `ReportGeneratorTest`.
- [x] **Signed & non-debuggable release APK**: Verified via `apksigner verify` and `build.gradle.kts`.

## 3. Known Limitations (Honest Verification Scope)
- **Physical Device / Emulator Execution**: No physical Android smartphone (e.g., Samsung Galaxy) or USB-OTG wireless adapter was attached in this cloud VM. Installation (`adb install`), live `WifiManager` RF scanning, and physical USB-OTG attach/detach must be verified on target hardware before field deployment.
- **Monitor Mode**: Standard non-rooted Android does not expose raw `AF_PACKET` monitor sockets to unprivileged APKs; live monitor-mode capture is marked `UNSUPPORTED` (built-in) / `CONDITIONAL` (external USB-OTG), and offline PCAP/CAP import is provided for 802.11 frame, handshake, and PMKID analysis.
