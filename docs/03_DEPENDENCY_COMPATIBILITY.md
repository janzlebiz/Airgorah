# 03 — Dependency Compatibility Analysis

## 1. Upstream Rust Dependencies (`Cargo.toml` / `Cargo.lock`)

| Crate | Version | Android NDK (`aarch64-linux-android`) | Action |
|---|---|---|---|
| `serde` | `1` | Compatible (pure Rust) | **Keep** |
| `serde_json` | `1` | Compatible (pure Rust) | **Keep** |
| `libwifi` | `0.5` | Compatible (pure Rust 802.11 parser) | **Keep** |
| `radiotap` | `1` | Compatible (pure Rust radiotap parser) | **Keep** |
| `csv` | `1.4` | Compatible (build-dependency for `vendors.csv`) | **Keep** |
| `lazy_static` | `1` | Compatible (or `std::sync::OnceLock`) | **Keep** |
| `thiserror` | `2` | Compatible (pure Rust) | **Keep** |
| `log` | `0.4` | Compatible (bridge to `android_logger` or JNI) | **Keep** |
| `regex` | `1.13` | Compatible (pure Rust) | **Keep** |
| `chrono` | `0.4` | Compatible (pure Rust) | **Keep** |
| `jni` | `0.21` (New) | Native Android JNI bindings | **Add** |
| `gtk4` | `0.11` | **Incompatible** (requires GTK4/GLib/X11/Wayland) | **Remove** |
| `nix` | `0.31` | Unnecessary (was for `SO_PEERCRED`, `chown`, `geteuid`) | **Remove** |
| `ctrlc` | `3.5` | Unnecessary (process signal handler for Linux daemon) | **Remove** |
| `which` | `8` | Unnecessary (Linux `PATH` CLI lookup) | **Remove** |
| `ureq` | `3` | Unnecessary (no cloud/network calls needed) | **Remove** |
| `toml` | `1` | Unnecessary (replaced by Android DataStore) | **Remove** |
| `env_logger` | `0.11` | Replaced by Android Logcat / structured JNI errors | **Remove** |

## 2. Upstream External System Dependencies
- `iw`, `ip`, `macchanger`, `systemctl`, `pkexec`, `xterm`, `aircrack-ng`, `crunch`: **None** of these are available on standard Android. All required functionality is either implemented in pure Rust (`libwifi`, `radiotap`, `vendors.csv`, `handshake.rs`) or handled via Android SDK APIs (`WifiManager`, `UsbManager`, `PdfDocument`, SAF).

## 3. Target Android Dependencies
- **Kotlin** (1.9.x / 2.0.x), **AGP** (8.x), **Gradle** (8.x)
- **Jetpack Compose BOM** + **Material 3** + **Material Icons Extended** + **Navigation Compose**
- **AndroidX Core KTX**, **Lifecycle ViewModel Compose**, **Activity Compose**
- **Room** (`room-runtime`, `room-ktx`, `room-compiler` via KSP)
- **DataStore** (`datastore-preferences`)
- **WorkManager** (`work-runtime-ktx`)
- **Kotlinx Serialization / Coroutines**
- **Hilt** (or lightweight manual/Hilt DI — Hilt with KSP is standard for Clean Architecture + MVVM)
- **Testing**: `junit`, `kotlinx-coroutines-test`, `room-testing`, `androidx.test.ext:junit`, `compose-ui-test-junit4`, Robolectric (for JVM Android unit tests without an emulator where applicable).
