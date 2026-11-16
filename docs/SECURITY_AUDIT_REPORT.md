# Security Audit Report — Airgorah Mobile

## 1. Offensive / Harmful Capability Exclusion
- **No Deauthentication Frame Transmission**: Operational 802.11 frame injection (`deauth.rs`, `raw_socket.rs`) was **not** migrated. The Deauthentication Lab is strictly a defensive educational simulation.
- **No Credential Cracking**: Operational dictionary/brute-force cracking (`aircrack-ng`, `crunch`, `decrypt.rs`) was **not** migrated. Handshake and PMKID modules perform passive artifact validation and protocol classification only.

## 2. Data Protection & Privacy
- **No Plaintext Password Storage**: `PasswordAuditTab` passes a `CharArray` to `NativeBridge.auditPasswordStrength`, which converts to UTF-8 bytes, evaluates strength in Rust, and immediately zeroes both the Kotlin `ByteArray`/`CharArray` and Rust byte buffer. Passwords are never logged, stored in Room/DataStore, or included in reports.
- **Offline Operation**: `AndroidManifest.xml` does **not** request `android.permission.INTERNET`. All scanning, PCAP parsing, OUI lookup, password auditing, and PDF/JSON/CSV reporting operate 100% offline.
- **Backup Disabled**: `android:allowBackup="false"` prevents unintended cloud backup of local audit databases.

## 3. Permissions & JNI Safety
- **Least-Privilege Permissions**: Only `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, and `NEARBY_WIFI_DEVICES` (with `neverForLocation`) are declared, with clear user-facing rationales in `PermissionCoordinator`.
- **JNI Panic Containment**: All 6 JNI functions in `rust/android-bridge/src/lib.rs` are wrapped in `std::panic::catch_unwind` and verified by unit tests (`panic_containment_returns_structured_error`).
- **Release Hardening**: Release build has `isDebuggable = false` and is signed with `airgorah-release.jks`.
