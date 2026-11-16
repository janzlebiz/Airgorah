# 02 — Architecture Analysis

## 1. Upstream Architecture (`martin-olivier/airgorah`)
Upstream uses a **two-process privilege-separated architecture**:
- **Unprivileged GUI (`airgorah`)**: GTK4 event loop + `gtk4::glib::timeout_add_local` polling timer. Spawns `pkexec airgorah-agent` and communicates over a Unix domain socket (`/run/airgorah/<uid>-<pid>.sock`) using 4-byte length-prefixed JSON frames (`airgorah_common::ipc`).
- **Privileged Agent (`airgorah-agent`)**: Runs as `root`. Spawns a capture thread (`sniffer.rs`) opening `AF_PACKET` (`SOCK_RAW`) on a monitor-mode interface, writing `LINKTYPE_IEEE802_11_RADIOTAP` records to `/tmp`, and updating global `Mutex<HashMap<String, AP>>` state. A background thread polls `handshake::get_crackables` every 1.5s.
- **External CLI Dependencies**: Shells out to `iw`, `ip`, `macchanger`, `systemctl`, `xterm`, `aircrack-ng`, and `crunch`.

## 2. Target Android Architecture (`Airgorah Mobile`)
Android's security model (SELinux, app sandboxing, no `polkit`, no `AF_PACKET` for unprivileged apps, no `xterm`/`aircrack-ng`) requires a **single-process, layered Clean Architecture + MVVM + JNI Rust Core**:

```
┌──────────────────────────────────────────────────────────┐
│                 Presentation Layer (UI)                  │
│  Jetpack Compose + Material 3 (Dark-first) + Navigation  │
│  ViewModels (StateFlow) for 9+ Feature Modules           │
├──────────────────────────────────────────────────────────┤
│                    Domain Layer                          │
│  Use Cases, Capability Matrix, Defensive Analyzers       │
│  Strict separation: Observed / Imported / Simulated      │
├──────────────────────────────────────────────────────────┤
│                     Data Layer                           │
│  Repositories, Room DB (11 Entities), DataStore, SAF     │
├───────────────────────────┬──────────────────────────────┤
│   Android Platform Layer  │      Native Rust Core (JNI)  │
│  WifiManager / ScanResult │  airgorah-common (types,     │
│  UsbManager (USB-OTG)     │   channel, handshake/PMKID)  │
│  Runtime Permissions      │  airgorah-analysis (PCAP,    │
│  WorkManager / PDF Export │   802.11 frame stats, OUI)   │
│                           │  airgorah-android-bridge     │
└───────────────────────────┴──────────────────────────────┘
```

## 3. Component Migration Classification

| Upstream Component | Classification | Target Location | Rationale |
|---|---|---|---|
| `common/src/types.rs` | **Reused (Adapted)** | `rust/common/src/types.rs` | Core `AP`, `Client`, `Settings` models preserved; attack-state types replaced with defensive/lab equivalents. |
| `common/src/channel.rs` | **Reused** | `rust/common/src/channel.rs` | Pure 2.4/5 GHz channel constants and `is_valid_channel_filter` logic. |
| `common/src/handshake.rs` | **Reused & Extended** | `rust/common/src/handshake.rs` | Pure Rust `PcapReader`, EAPOL M1–M4 classifier, PMKID detector; extended with detailed packet/integrity diagnostics. |
| `agent/src/backend/vendors.rs` + `vendors.csv` + `build.rs` | **Reused** | `rust/analysis/src/vendors.rs` | Pure Rust OUI lookup compiled at build time; zero Linux-specific deps. |
| `agent/src/backend/pcap.rs` | **Reused** | `rust/analysis/src/pcap.rs` | Pure Rust libpcap header/record reader/writer. |
| `agent/src/backend/sniffer.rs` (pure helpers) | **Reused (Partial)** | `rust/analysis/src/frame_parser.rs` | `radiotap` + `libwifi` frame decoding, `classify_privacy`, `essid_of`, `sanitize_ssid`, `is_unicast`, `band_of` reused for PCAP inspection. |
| `agent/src/backend/raw_socket.rs`, `interface.rs`, `scan.rs` | **Replaced** | `android/core/hardware/` & `android/core/network/` | Replaced by Android `WifiManager`, `UsbManager`, and `HardwareCapabilityManager`. |
| `agent/src/backend/deauth.rs`, `pmkid.rs` | **Rewritten (Defensive)** | `android/feature/deauthentication-lab/`, `pmkid/` | Operational frame injection removed; replaced with defensive detection lab, educational simulation, and passive PMKID artifact analysis. |
| `gui/src/backend/decrypt.rs` | **Rewritten (Defensive)** | `android/feature/password-audit/` | External `xterm`/`aircrack-ng`/`crunch` removed; replaced with offline defensive password strength & policy auditor. |
| `gui/src/backend/report.rs` | **Rewritten (Expanded)** | `android/core/reporting/` | Expanded from simple JSON to PDF, JSON, and CSV across 7 report types. |
| `gui/src/frontend/*` (GTK4) | **Replaced** | `android/feature/*` (Compose) | Replaced by Material 3 Jetpack Compose screens. |
| `common/src/ipc.rs`, `deps.rs`, `agent/main.rs`, `server.rs` | **Unsupported / Dropped** | N/A | Linux `polkit` + Unix socket IPC + CLI tool checks are obsolete on Android; replaced by JNI + Android permissions. |
