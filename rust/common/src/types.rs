//! Core wireless auditing data types for Airgorah Mobile.
//!
//! Preserves the original `AP`, `Client`, and `Settings` structures from
//! `martin-olivier/airgorah`, adapted for defensive mobile auditing.

use serde::{Deserialize, Serialize};
use std::collections::HashMap;

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq, Eq)]
pub enum DataProvenance {
    Observed,
    Imported,
    Simulated,
    Inferred,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq, Eq)]
pub struct AP {
    pub essid: String,
    pub bssid: String,
    pub band: String,
    pub channel: String,
    pub power: String,
    pub privacy: String,
    pub hidden: bool,
    pub handshake: bool,
    pub pmkid: bool,
    pub saved_handshake: Option<String>,
    pub first_time_seen: String,
    pub last_time_seen: String,
    pub clients: HashMap<String, Client>,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq, Eq)]
pub struct Client {
    pub mac: String,
    pub packets: String,
    pub power: String,
    pub first_time_seen: String,
    pub last_time_seen: String,
    pub vendor: String,
    pub probes: String,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq, Eq)]
pub struct Settings {
    pub display_hidden_ap: bool,
    pub auto_enrich_oui: bool,
}

impl Default for Settings {
    fn default() -> Self {
        Self {
            display_hidden_ap: true,
            auto_enrich_oui: true,
        }
    }
}
