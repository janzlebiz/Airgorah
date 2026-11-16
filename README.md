# Airgorah Mobile

Native Android Wi-Fi Security Auditing Application transformed from [martin-olivier/airgorah](https://github.com/martin-olivier/airgorah).

## Overview
- **Platform**: Native Android (Kotlin, Jetpack Compose, Material 3, Room, Hilt, Coroutines/StateFlow)
- **Native Core**: Rust (`airgorah-common`, `airgorah-analysis`, `airgorah-android-bridge` -> `libairgorah_core.so` for `arm64-v8a`)
- **Primary Target**: Android 8.0+ (API 26–35), ARM64 (`arm64-v8a`), Samsung Galaxy & USB-OTG compatible devices
- **License**: MIT License (Copyright (c) 2026 Martin Olivier)

## Features
1. **Dashboard**: Adapter status, Wi-Fi radio state, network/client/capture metrics, recent findings.
2. **Networks**: Native `WifiManager` discovery with search, protocol filters, sorting, and AP detail inspection.
3. **Clients**: Authorized client inventory with explicit Observed vs Inferred provenance.
4. **Monitoring**: Hardware capability-driven adapter selection, supported bands, and capture history.
5. **Captures**: Storage Access Framework (SAF) PCAP/CAP import with Rust `libwifi`/`radiotap` validation.
6. **Security Lab**:
   - **Deauthentication Lab**: Defensive detection & educational 802.11w PMF simulation (no RF frame transmission).
   - **WPA Handshake**: EAPOL M1–M4 completeness & integrity inspection (no credential cracking).
   - **PMKID**: RSN PMKID KDE identification & WPA2-PSK vs WPA3-SAE classification.
   - **Password Audit**: Offline in-memory passphrase strength & entropy evaluation (never stored or transmitted).
7. **Hardware**: Built-in Wi-Fi, External USB-OTG (`UsbManager`), Unsupported, and Simulated hardware diagnostics.
8. **Reports**: Offline **PDF**, **JSON**, and **CSV** report generation across 7 report types.
9. **Settings**: Preferences, Rust-validated channel filter, and local data management.

## Building
```bash
# 1. Run Rust tests & cross-compile ARM64 JNI library
cd rust && cargo test --workspace
../scripts/build_rust_android.sh

# 2. Run Android unit tests & build signed release APK
cd ../android
gradle testDebugUnitTest assembleRelease
```
