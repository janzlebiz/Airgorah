# Test Execution Report — Airgorah Mobile

## 1. Summary
- **Total Tests Executed**: 28
- **Passed**: 28
- **Failed**: 0
- **Skipped**: 0

## 2. Rust Workspace Tests (`cargo test --workspace`)
- **`airgorah-common`** (14 passed, 0 failed):
  - `channel::tests` (5 tests): 2.4/5 GHz channel filter validation, malformed input rejection, band mismatch rejection.
  - `handshake::tests` (9 tests): EAPOL M1–M4 classification, handshake completeness (`M2 && (M1 || M3)`), PMKID detection in M1, WPA3-SAE PMKID exclusion, `PcapReader` truncated record handling.
- **`airgorah-analysis`** (5 passed, 0 failed):
  - `vendors::tests` (1 test): IEEE OUI lookup (`00:00:0C` -> Cisco) and unknown handling.
  - `pcap_analyzer::tests` (2 tests): Invalid/PCAPNG rejection, empty PCAP incomplete status.
  - `password_audit::tests` (2 tests): Weak common password flagging, strong random passphrase rating.
- **`airgorah-android-bridge`** (2 passed, 0 failed):
  - `tests::ok_json_wraps_payload`
  - `tests::panic_containment_returns_structured_error`

## 3. Android / JVM / Robolectric Tests (`gradle testDebugUnitTest`)
- **`CoreModelsAndNativeBridgeTest`** (3 passed, 0 failed): Frequency-to-channel, frequency-to-band, graceful `NativeBridge` failure handling on host JVM.
- **`AirgorahDatabaseTest`** (1 passed, 0 failed): Room in-memory DB CRUD & clearing.
- **`HardwareCapabilityManagerTest`** (1 passed, 0 failed): Simulated hardware reporting, `packetInjectionStatus == UNSUPPORTED`, safe `ACTION_USB_DEVICE_DETACHED` handling.
- **`ReportGeneratorTest`** (1 passed, 0 failed): Offline JSON, CSV, and PDF report generation & persistence.
- **`AirgorahUiTest`** (1 passed, 0 failed): Compose UI rendering of `DashboardScreen` and navigation trigger.
