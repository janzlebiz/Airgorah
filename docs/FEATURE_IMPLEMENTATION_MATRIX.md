# Feature Implementation Matrix — Airgorah Mobile

| Original Feature | Android Implementation | Status | Verification |
|---|---|---|---|
| **Wi-Fi Discovery** | `WifiDiscoveryRepository` (`WifiManager` + `ScanResultsCallback`) + Rust OUI enrichment + Room persistence + Search/Filter/Sort/Details | Implemented | Unit & UI Tested |
| **Client Discovery** | `ClientsScreen` + `WirelessClientEntity` populated from imported PCAP frames with explicit `OBSERVED` vs `INFERRED` provenance | Implemented | Unit & DB Tested |
| **Monitor Mode** | `HardwareCapabilityManager` + `MonitoringScreen` reporting actual Android/driver capability without inferring from chipset name | Implemented (Conditional) | Unit Tested (Mocked/Robolectric) |
| **Channel Management** | Rust `channel.rs` (`is_valid_channel_filter`) via JNI + `frequencyToChannel` + Settings channel filter validation | Implemented | Rust & JVM Unit Tested |
| **Deauthentication** | `SecurityLabScreen` (`Deauth Lab` tab) + `SecurityLabRepository`: simulated topology, clients, deauth events, timeline, 802.11w PMF defense. **No RF frame transmission.** | Implemented (Defensive/Simulated) | Unit Tested |
| **Handshake Capture** | `CapturesScreen` + `SecurityLabScreen` (`Handshake` tab) + Rust `handshake.rs`/`pcap_analyzer.rs`: SAF PCAP import, M1–M4 flags, completeness, integrity status. **No cracking.** | Implemented | Rust Unit Tested |
| **PMKID** | `SecurityLabScreen` (`PMKID` tab) + Rust `pcap_analyzer.rs`: RSN PMKID KDE extraction, WPA2-PSK vs WPA3-SAE classification. **No active solicitation/cracking.** | Implemented | Rust Unit Tested |
| **Password Testing** | `SecurityLabScreen` (`Password Audit` tab) + Rust `password_audit.rs`: in-memory strength, entropy with methodology note, pattern warnings, policy advice. **Zero plaintext storage.** | Implemented (Defensive) | Rust Unit Tested |
| **Reports** | `ReportGenerator` (`:core:reporting`) + `ReportsScreen`: PDF, JSON, CSV across 7 report types with provenance legend | Implemented | Robolectric Unit Tested |
| **Hardware / USB-OTG** | `HardwareCapabilityManager` + `HardwareScreen`: Built-in, External USB-OTG, Unsupported, Simulated + USB permission & safe detach | Implemented | Robolectric Unit Tested |
| **Settings & Data** | `SettingsScreen` + `ApplicationSettingsEntity` + Clear Local Data | Implemented | DB Tested |
