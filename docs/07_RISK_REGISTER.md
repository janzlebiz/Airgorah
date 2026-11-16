# 07 — Risk Register

| ID | Risk | Severity | Likelihood | Mitigation |
|---|---|---|---|---|
| **R-01** | Android SDK & NDK not pre-installed in VM environment | High | Confirmed | Install Android SDK command-line tools, platform 35, build-tools, and NDK in user space (`/home/antigravity/android-sdk`) during Phase 02, and configure `local.properties` / `ANDROID_HOME`. |
| **R-02** | `cargo-ndk` / `aarch64-linux-android` target missing | High | Confirmed | Install `aarch64-linux-android` via `rustup target add` and configure NDK `aarch64-linux-android26-clang` linker (or `cargo-ndk`) in Phase 03. |
| **R-03** | Android `WifiManager.startScan()` throttling (4 scans / 2 min) | Medium | High | Implement `ScanResultsCallback`, surface clear scan-cooldown status in UI, and cache results in Room so the app remains responsive offline. |
| **R-04** | Unprivileged Android cannot open raw `AF_PACKET` monitor sockets | High | Confirmed | Explicitly model monitor mode & raw capture as `Conditional` / `Unsupported` on standard Android; provide full PCAP/CAP import & analysis via SAF and clearly document hardware/OS boundaries. |
| **R-05** | USB-OTG device disconnection during inspection | Medium | Medium | Register `BroadcastReceiver` for `ACTION_USB_DEVICE_DETACHED`, close `UsbDeviceConnection` safely, and update `StateFlow` without crashing. |
| **R-06** | Rust panic across JNI boundary crashing the JVM | High | Medium | Wrap all JNI entry points in `std::panic::catch_unwind` and return structured JSON error responses (`JniResult<T>`). |
| **R-07** | Plaintext password leakage in Password Audit | High | Low | Never log, persist in Room/DataStore, or include user-tested passwords in reports; analyze in-memory only and immediately discard. |
| **R-08** | No physical Android device / emulator attached in cloud VM | Medium | High | Run comprehensive JVM unit tests, Robolectric Android framework tests, Room migration tests, Compose UI tests (Robolectric/Roborazzi or Compose test rule), Rust unit tests, and full signed release APK assembly + APK verification (`apkanalyzer` / `apksigner verify`). Clearly document what was verified on JVM vs physical hardware. |
