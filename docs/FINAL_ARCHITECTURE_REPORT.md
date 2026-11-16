# Final Architecture Report — Airgorah Mobile

## 1. Overview
**Airgorah Mobile** (`com.airgorah.mobile`) is a native Android Wi-Fi security auditing application built with Kotlin, Jetpack Compose, Material 3, Room, Hilt, Coroutines/StateFlow, and an ARM64 Rust NDK JNI core (`libairgorah_core.so`), preserving the identity, feature organization, and reusable Rust components of `martin-olivier/airgorah`.

## 2. Module Architecture
- **Rust Native Core (`rust/`)**:
  - `rust/common`: Migrated `types.rs`, `channel.rs`, and `handshake.rs` (`libwifi` + `radiotap` 4-way handshake & PMKID parser).
  - `rust/analysis`: Migrated OUI vendor lookup (`vendors.rs` + `vendors.csv`), extended PCAP/802.11 analyzer (`pcap_analyzer.rs`), and defensive password strength auditor (`password_audit.rs`).
  - `rust/android-bridge`: `cdylib` (`libairgorah_core.so`) with `std::panic::catch_unwind` panic containment and typed JSON envelopes.
- **Android Core (`android/core/`)**:
  - `:core:common`: Shared domain enums (`DataProvenance`, `OperationState`, `CapabilityStatus`, `HardwareCategory`, `ArtifactValidationStatus`), frequency/channel mappers, and `NativeBridge`.
  - `:core:database`: Room database (`AirgorahDatabase`) with 11 entities (`AccessPointEntity`, `WirelessClientEntity`, `WirelessAdapterEntity`, `CaptureSessionEntity`, `CaptureArtifactEntity`, `SecurityFindingEntity`, `AuditSessionEntity`, `LabScenarioEntity`, `LabEventEntity`, `ReportEntity`, `ApplicationSettingsEntity`).
  - `:core:permissions`: `PermissionCoordinator` managing `ACCESS_FINE_LOCATION`, `NEARBY_WIFI_DEVICES`, and Location Services checks with user-facing rationales.
  - `:core:hardware`: `HardwareCapabilityManager` separating Built-in Wi-Fi, External USB-OTG (`UsbManager`), Unsupported hardware, and Simulated test hardware, with safe USB attach/detach/permission receivers.
  - `:core:network`: `WifiDiscoveryRepository`, `CaptureImportRepository` (SAF PCAP import -> Rust JNI analysis -> Room), and `SecurityLabRepository`.
  - `:core:reporting`: `ReportGenerator` exporting PDF (`PdfDocument`), JSON, and CSV across 7 report types with explicit provenance tags.
- **Android Application (`android/app/`)**:
  - Material 3 Dark-first UI (`AirgorahTheme`), `MainViewModel` (`StateFlow`), and 9 primary destinations (`Dashboard`, `Networks`, `Clients`, `Monitoring`, `Captures`, `Security Lab` [4 tabs: `Deauth Lab`, `Handshake`, `PMKID`, `Password Audit`], `Hardware`, `Reports`, `Settings`).
