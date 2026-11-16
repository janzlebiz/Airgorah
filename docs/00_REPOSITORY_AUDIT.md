# 00 — Repository Audit: `martin-olivier/airgorah`

## 1. Upstream Metadata
- **Repository**: `https://github.com/martin-olivier/airgorah`
- **Version**: `0.9.0`
- **Rust Edition**: `2024` (workspace resolver `"3"`)
- **License**: MIT License — `Copyright (c) 2026 Martin Olivier`
- **Authors**: Martin Olivier (`martin.olivier@live.fr`)
- **Target Platform (Upstream)**: Linux only (X11/Wayland + `polkit` + `iw` + `ip` + `macchanger` + `systemctl` + `xterm` + `aircrack-ng` + `crunch`)

## 2. Workspace Structure
The upstream Cargo workspace consists of three crates:

| Crate | Path | Binary / Lib | Purpose |
|---|---|---|---|
| `airgorah-common` | `crates/common` | Library | Shared wire types (`AP`, `Client`, `Settings`, `MacMode`, `AttackTarget`, `AttackState`), channel plan validation (`CHANNELS_2_4`, `CHANNELS_5`, `is_valid_channel_filter`), external tool catalog (`deps`), length-prefixed JSON IPC codec (`ipc`), and native PCAP 4-way handshake / PMKID detector (`handshake`). |
| `airgorah-agent` | `crates/agent` | Binary (`airgorah-agent`) | Root-privileged daemon launched via `pkexec`. Owns raw `AF_PACKET` socket capture (`sniffer.rs`, `raw_socket.rs`), `PcapWriter` (`pcap.rs`), OUI vendor table built from `vendors.csv` (`build.rs`, `vendors.rs`), `iw`/`ip`/`macchanger`/`systemctl` execution (`interface.rs`), deauth/PMKID frame injection (`deauth.rs`, `pmkid.rs`), and Unix socket server (`server.rs`). |
| `airgorah` | `crates/gui` | Binary (`airgorah`) | Unprivileged GTK4 (`gtk4-rs v0.11`) desktop GUI. Manages user settings (`~/.config/airgorah/config.toml`), sysfs/iw interface discovery (`iface.rs`), agent lifecycle (`client.rs`), JSON report export (`report.rs`), external `xterm`/`aircrack-ng`/`crunch` invocation (`decrypt.rs`), and GTK4 views (`frontend/`). |

## 3. Packaging, Build & CI Assets
- **`Dockerfile`**: Debian Bookworm + `rust:1.94.1` + `libgtk-4-dev` + `libglib2.0-dev` + `fpm` to build `.deb`, `.rpm`, `.pkg.tar.zst`.
- **`.github/workflows/CI.yml`**: Runs `cargo clippy --all --all-features -- -D warnings`, `cargo fmt --all -- --check`, and builds `x86_64` and `aarch64` Linux packages.
- **`package/.policy`**: Polkit policy (`org.freedesktop.policykit.pkexec.airgorah-agent`) granting `pkexec` root execution of `/usr/bin/airgorah-agent`.
- **`package/.desktop`**: XDG desktop entry (`com.molivier.airgorah`).
- **`.fpm`**: Packaging manifest mapping binaries, icon, desktop entry, policy, README, and LICENSE.
- **`crates/agent/vendors.csv`**: IEEE OUI / MA-M / MA-S MAC prefix -> Vendor Name lookup table compiled into a static `HashMap` via `build.rs`.

## 4. Existing Unit Tests
- `crates/common/src/channel.rs`: 5 unit tests covering empty filter, valid 2.4/5 GHz channels, band mismatch rejection, out-of-plan channel rejection, and malformed input rejection.
- `crates/common/src/handshake.rs`: 9 unit tests covering EAPOL message 1–4 classification, handshake completeness (`M2 && (M1 || M3)`), PMKID detection in M1, WPA3-SAE PMKID exclusion, M1 without PMKID, `get_crackables`, `PcapReader` record iteration, truncated trailing record handling, and invalid PCAP rejection.
- `crates/agent/src/backend/pcap.rs`: 2 unit tests covering `append_records` concatenation and header-only no-op.
- `crates/agent/src/backend/sniffer.rs`: 14 unit tests covering `build_channel_list`, `plan_channel` hopping/parking/wrapping, `sanitize_ssid`, and `essid_of`.

## 5. Key Architectural Observations for Android Migration
1. **Privilege & IPC Split**: Upstream's `polkit` + Unix socket + `airgorah-agent` model is Linux-desktop specific and cannot run on non-rooted Android. Android replaces this with Android runtime permissions, `WifiManager` / `ConnectivityManager`, `UsbManager` (USB-OTG), Storage Access Framework (SAF), and in-process JNI calls to a safe Rust core.
2. **Pure Rust Goldmine**: `airgorah-common` (`handshake.rs`, `channel.rs`, `types.rs`), `pcap.rs`, `vendors.rs` (`vendors.csv`), and the pure 802.11 frame-parsing functions in `sniffer.rs` have zero Linux-kernel or GTK4 dependencies and can be compiled for `aarch64-linux-android` via Android NDK.
3. **Security & Platform Boundaries**: Standard Android does not expose raw `AF_PACKET` monitor-mode sockets or arbitrary 802.11 frame injection without root/custom kernels. Furthermore, Airgorah Mobile must maintain strict separation between **supported** (Android Wi-Fi discovery, PCAP import/analysis, OUI lookup, USB-OTG enumeration, defensive password auditing, reporting), **conditional** (external USB adapter capability diagnostics), and **simulated/defensive** (deauthentication detection lab) capabilities, and must **not** implement operational Wi-Fi disruption or credential cracking.
