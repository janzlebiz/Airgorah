# 06 — Migration Plan

## 1. Target Repository Structure (`airgorah-mobile/`)
```
airgorah-mobile/
├── android/
│   ├── app/                        # Application entry, DI graph, MainActivity, NavHost
│   ├── core/
│   │   ├── common/                 # Shared Kotlin models, Result/UiState, Coroutines dispatchers
│   │   ├── database/               # Room DB, DAOs, 11 Entities, Migrations
│   │   ├── network/                # WifiManager scanner, ScanResult mapper
│   │   ├── hardware/               # HAL, UsbManager OTG discovery, CapabilityMatrix
│   │   ├── permissions/            # Runtime & USB permission coordinator
│   │   └── reporting/              # PDF, JSON, CSV report generators
│   └── feature/
│       ├── dashboard/              # Security dashboard & system status
│       ├── discovery/              # Live Wi-Fi scan orchestration
│       ├── accesspoints/           # Networks list, search/filter/sort, AP details
│       ├── clients/                # Authorized client inventory (observed vs inferred)
│       ├── monitoring/             # Adapter selection, capabilities, channel plan, session history
│       ├── captures/               # PCAP browser, SAF import, metadata, stats, export
│       ├── handshake/              # WPA 4-way handshake artifact validation & inspection
│       ├── pmkid/                  # PMKID artifact identification & analysis
│       ├── deauthentication-lab/   # Defensive deauth detection & educational simulation
│       ├── password-audit/         # Defensive password strength & policy auditor
│       ├── hardware/               # USB-OTG & built-in adapter diagnostics
│       ├── reports/                # Report builder & export UI
│       └── settings/               # DataStore preferences & data management
├── rust/
│   ├── Cargo.toml                  # Workspace: common, analysis, android-bridge
│   ├── common/                     # Migrated airgorah-common (types, channel, handshake)
│   ├── analysis/                   # Migrated vendors.rs/vendors.csv, pcap.rs, frame_parser, password_audit
│   └── android-bridge/             # cdylib JNI bridge with panic containment & typed JSON/FFI
├── docs/                           # Phase 00-11 documentation & reports
├── scripts/                        # Rust NDK build & verification scripts
├── tests/                          # Cross-layer test fixtures (sample .cap/.pcap files)
├── LICENSE                         # Preserved MIT License (Copyright 2026 Martin Olivier)
└── README.md
```

## 2. Sequential Execution Phases
- **Phase 00**: Repository Audit & Migration Plan *(Current Phase)*
- **Phase 01**: Architecture Finalization (Data models, Room schema, JNI contracts)
- **Phase 02**: Android Foundation (Gradle, SDK/NDK setup, Compose Material 3 dark-first theme, 9-destination Navigation)
- **Phase 03**: Rust Integration (`rust/common`, `rust/analysis`, `rust/android-bridge`, `aarch64-linux-android` build + unit tests)
- **Phase 04**: Hardware Layer (`WifiManager`, `UsbManager` OTG, `HardwareCapabilityManager`, disconnect handling)
- **Phase 05**: Core Functionality (Room persistence, AP discovery, AP details, Client inventory)
- **Phase 06**: Monitoring & Artifacts (Monitoring screen, SAF PCAP import, Handshake & PMKID defensive analysis)
- **Phase 07**: Security Lab (Deauthentication Lab simulation & defensive detection, Password Audit)
- **Phase 08**: Reports (PDF, JSON, CSV generators & export)
- **Phase 09**: Security Hardening (Permissions audit, no plaintext password storage, input validation)
- **Phase 10**: Verification (Rust tests, JUnit, Robolectric/instrumentation, Compose UI tests)
- **Phase 11**: APK Release & Release Gate (Signed non-debuggable release APK + all Gate reports)
