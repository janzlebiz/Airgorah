# Phase 01 — Finalized Architecture, Data Models & JNI Contracts

## 1. Module Dependency Graph (Android)
- `:core:common` — Pure Kotlin models (`DataProvenance`, `CapabilityStatus`, `OperationState`, `UiText`, dispatchers). Zero Android framework dependencies where possible.
- `:core:database` -> `:core:common` — Room Database (`AirgorahDatabase`), 11 Entities, DAOs, TypeConverters, explicit Migration strategy.
- `:core:permissions` -> `:core:common` — Runtime & USB permission state machine and rationale definitions.
- `:core:hardware` -> `:core:common`, `:core:permissions` — `HardwareCapabilityManager`, `WifiHardwareProbe`, `UsbOtgAdapterProbe`, `SimulatedHardwareProvider`, disconnect receiver.
- `:core:network` -> `:core:common`, `:core:database`, `:core:hardware` — `WifiDiscoveryManager` (`WifiManager` + `ScanResultsCallback`), frequency-to-channel mapper, OUI enrichment via Rust JNI.
- `:core:reporting` -> `:core:common`, `:core:database` — Native `PdfDocument` generator, JSON serializer, CSV exporter.
- `:feature:*` -> `:core:common`, `:core:database`, `:core:hardware`, `:core:network`, `:core:permissions`, `:core:reporting`.
- `:app` -> all `:core:*` and `:feature:*` modules, Hilt DI root, `MainActivity`, Material 3 Dark-first `AirgorahTheme`, 9-destination Navigation + sub-routes.

## 2. Room Database Schema (11 Entities)
1. `AccessPointEntity`: `bssid` (PK), `essid`, `channel`, `frequencyMhz`, `band`, `rssiDbm`, `securityProtocol`, `vendor`, `isHidden`, `hasHandshake`, `hasPmkid`, `firstSeenEpochMs`, `lastSeenEpochMs`, `provenance` (`OBSERVED`, `IMPORTED`, `SIMULATED`, `INFERRED`), `classification`
2. `WirelessClientEntity`: `mac` (PK), `associatedBssid`, `vendor`, `packetCount`, `rssiDbm`, `probedSsids`, `firstSeenEpochMs`, `lastSeenEpochMs`, `provenance`, `connectionState`
3. `WirelessAdapterEntity`: `adapterId` (PK), `name`, `category` (`BUILT_IN`, `EXTERNAL_USB`, `UNSUPPORTED`, `SIMULATED`), `vendorId`, `productId`, `driverInfo`, `supports24Ghz`, `supports5Ghz`, `supports6Ghz`, `monitorModeStatus`, `rawCaptureStatus`, `isConnected`, `lastSeenEpochMs`
4. `CaptureSessionEntity`: `sessionId` (PK), `adapterId`, `startedAtEpochMs`, `endedAtEpochMs`, `channelPlan`, `status`, `packetCount`, `storageBytes`, `notes`
5. `CaptureArtifactEntity`: `artifactId` (PK), `fileName`, `uriString`, `fileSizeBytes`, `linkType`, `packetCount`, `bssidCount`, `handshakeCount`, `pmkidCount`, `validationStatus` (`VALID`, `INVALID`, `INCOMPLETE`, `UNSUPPORTED_FORMAT`, `ANALYSIS_FAILED`), `statusDetail`, `importedAtEpochMs`
6. `SecurityFindingEntity`: `findingId` (PK), `category`, `severity` (`INFO`, `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), `title`, `description`, `recommendation`, `targetBssid`, `provenance`, `createdAtEpochMs`
7. `AuditSessionEntity`: `auditId` (PK), `auditType`, `startedAtEpochMs`, `completedAtEpochMs`, `summary`, `findingsCount`
8. `LabScenarioEntity`: `scenarioId` (PK), `name`, `topologyJson`, `pmfEnabled`, `description`, `createdAtEpochMs`
9. `LabEventEntity`: `eventId` (PK), `scenarioId`, `timestampEpochMs`, `eventType`, `sourceMac`, `targetMac`, `reasonCode`, `severity`, `isSimulated`, `detectionNote`
10. `ReportEntity`: `reportId` (PK), `reportType`, `format` (`PDF`, `JSON`, `CSV`), `title`, `fileUri`, `createdAtEpochMs`, `itemCount`
11. `ApplicationSettingsEntity`: `id` (PK = 1), `displayHiddenAps`, `autoEnrichOui`, `default24Ghz`, `default5Ghz`, `channelFilter`, `pmfSimulationDefault`, `lastUpdatedEpochMs`

*(DataStore Preferences also mirrors lightweight UI settings for instant reactive startup.)*

## 3. Native Rust Core & JNI Contracts (`libairgorah_core.so`)
All JNI functions live in `com.airgorah.mobile.core.rust.NativeBridge` and map to `rust/android-bridge/src/lib.rs`.
Every function wraps execution in `std::panic::catch_unwind` and returns a JSON envelope:
```json
{ "ok": true, "data": { ... } }
// or
{ "ok": false, "error": "structured message" }
```

### JNI Methods:
1. `nativeGetVersion(): String` — Returns Rust core version + ABI diagnostic info.
2. `nativeLookupVendor(mac: String): String` — Offline OUI lookup from compiled `vendors.csv` (`VENDORS` map).
3. `nativeLookupVendorsBatch(macsJson: String): String` — Batch MAC -> Vendor map for scan results.
4. `nativeValidateChannelFilter(filter: String, ghz24: Boolean, ghz5: Boolean): String` — Reuses `airgorah_common::channel::is_valid_channel_filter`.
5. `nativeAnalyzePcapBytes(data: ByteArray): String` — Parses classic libpcap (LE/BE, link types 105 & 127), decodes 802.11 frames with `radiotap` + `libwifi`, classifies EAPOL M1–M4, detects RSN PMKID KDEs (excluding WPA3-SAE key descriptor 0), extracts discovered APs/clients/statistics, and returns structured `PcapAnalysisResult` with `validationStatus` (`VALID`, `INVALID`, `INCOMPLETE`, `UNSUPPORTED_FORMAT`, `ANALYSIS_FAILED`).
6. `nativeAuditPasswordStrength(passwordBytes: ByteArray): String` — Defensive in-memory password strength, entropy estimation (with documented limitations), character-class & common-pattern checks; zeroes buffer immediately after evaluation and never returns or logs the password.
