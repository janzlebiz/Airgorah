//! Offline IEEE OUI / MA-M / MA-S vendor resolution.
//!
//! Migrated from `martin-olivier/airgorah` (`crates/agent/src/backend/vendors.rs`).
//! The OUI table is generated at build time from `vendors.csv` via `build.rs`.

use std::collections::HashMap;
use std::sync::{Mutex, OnceLock};

static VENDORS_CSV: &str = include_str!("../vendors.csv");
static VENDORS_MAP: OnceLock<HashMap<&'static str, &'static str>> = OnceLock::new();
static VENDORS_CACHE: OnceLock<Mutex<HashMap<String, String>>> = OnceLock::new();

fn vendors_map() -> &'static HashMap<&'static str, &'static str> {
    VENDORS_MAP.get_or_init(|| {
        let mut map = HashMap::with_capacity(36000);
        for (idx, line) in VENDORS_CSV.lines().enumerate() {
            if idx == 0 || line.is_empty() {
                continue;
            }
            if let Some((prefix, rest)) = line.split_once(',') {
                let vendor = rest.trim().trim_matches('"');
                map.insert(prefix.trim(), vendor);
            }
        }
        map
    })
}

/// Normalize a MAC address to uppercase colon-separated form before lookup.
fn normalize_mac(mac: &str) -> String {
    mac.trim().to_ascii_uppercase().replace('-', ":")
}

/// Resolve a MAC address to a vendor name, or `"Unknown"`.
pub fn find_vendor(mac: &str) -> String {
    let normalized = normalize_mac(mac);
    if normalized.is_empty() {
        return "Unknown".to_string();
    }

    let cache_lock = VENDORS_CACHE.get_or_init(|| Mutex::new(HashMap::new()));
    if let Ok(cache) = cache_lock.lock() {
        if let Some(vendor) = cache.get(&normalized) {
            return vendor.clone();
        }
    }

    let map = vendors_map();
    let mut prefix = normalized.get(..13).unwrap_or(&normalized).to_string();
    let mut vendor = String::from("Unknown");

    while !prefix.is_empty() {
        if let Some(item) = map.get(prefix.as_str()) {
            vendor = (*item).to_string();
            break;
        }
        prefix.pop();
    }

    if let Ok(mut cache) = cache_lock.lock() {
        cache.insert(normalized, vendor.clone());
    }

    vendor
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn resolves_known_mac_prefix_and_handles_unknown() {
        // 00:00:0C is Cisco Systems in IEEE OUI registry
        let cisco = find_vendor("00:00:0c:11:22:33");
        assert_ne!(cisco, "Unknown");
        assert!(cisco.to_ascii_lowercase().contains("cisco"));

        assert_eq!(find_vendor(""), "Unknown");
    }
}
