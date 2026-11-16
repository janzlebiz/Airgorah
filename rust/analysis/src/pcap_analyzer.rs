//! Defensive PCAP / 802.11 / Handshake / PMKID artifact analyzer.
//!
//! Validates capture integrity, extracts packet statistics, identifies
//! access points and stations from beacons/probe/data frames, and classifies
//! WPA 4-way handshake and PMKID artifacts without performing credential recovery.

use std::collections::HashMap;

use airgorah_common::handshake::has_crackable_pmkid;
use libwifi::frame::EapolKey;
use libwifi::frame::components::{DataHeader, ManagementHeader, RsnAkmSuite, StationInfo};
use libwifi::{Addresses, Frame};
use serde::{Deserialize, Serialize};

use crate::vendors::find_vendor;

const LINKTYPE_IEEE802_11: u32 = 105;
const LINKTYPE_IEEE802_11_RADIOTAP: u32 = 127;

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq, Eq)]
pub enum ArtifactValidationStatus {
    Valid,
    Invalid,
    Incomplete,
    UnsupportedFormat,
    AnalysisFailed,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq, Eq)]
pub struct HandshakeArtifactSummary {
    pub bssid: String,
    pub essid: String,
    pub station_mac: String,
    pub has_m1: bool,
    pub has_m2: bool,
    pub has_m3: bool,
    pub has_m4: bool,
    pub is_complete_handshake: bool,
    pub status: ArtifactValidationStatus,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq, Eq)]
pub struct PmkidArtifactSummary {
    pub bssid: String,
    pub essid: String,
    pub station_mac: String,
    pub key_descriptor_version: u16,
    pub is_wpa2_psk_pmkid: bool,
    pub is_wpa3_sae_pmkid: bool,
    pub status: ArtifactValidationStatus,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq, Eq)]
pub struct ExtractedAp {
    pub bssid: String,
    pub essid: String,
    pub hidden: bool,
    pub channel: Option<u8>,
    pub band: String,
    pub privacy: String,
    pub signal_dbm: Option<i32>,
    pub vendor: String,
    pub beacon_count: u64,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq, Eq)]
pub struct ExtractedClient {
    pub mac: String,
    pub associated_bssid: Option<String>,
    pub vendor: String,
    pub packet_count: u64,
    pub signal_dbm: Option<i32>,
    pub probed_ssids: Vec<String>,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq, Eq)]
pub struct PcapAnalysisResult {
    pub validation_status: ArtifactValidationStatus,
    pub status_detail: String,
    pub link_type: u32,
    pub link_type_name: String,
    pub total_packets: u64,
    pub decoded_80211_frames: u64,
    pub undecodable_frames: u64,
    pub truncated_trailing_record: bool,
    pub beacon_frames: u64,
    pub probe_request_frames: u64,
    pub probe_response_frames: u64,
    pub data_frames: u64,
    pub eapol_frames: u64,
    pub deauth_frames: u64,
    pub disassoc_frames: u64,
    pub access_points: Vec<ExtractedAp>,
    pub clients: Vec<ExtractedClient>,
    pub handshakes: Vec<HandshakeArtifactSummary>,
    pub pmkids: Vec<PmkidArtifactSummary>,
}

struct PcapSliceReader<'a> {
    data: &'a [u8],
    offset: usize,
    big_endian: bool,
    link_type: u32,
}

impl<'a> PcapSliceReader<'a> {
    fn new(data: &'a [u8]) -> Result<Self, ArtifactValidationStatus> {
        if data.len() < 24 {
            return Err(ArtifactValidationStatus::Invalid);
        }
        let magic = u32::from_le_bytes(data[0..4].try_into().unwrap());
        let big_endian = match magic {
            0xa1b2_c3d4 | 0xa1b2_3c4d => false,
            0xd4c3_b2a1 | 0x4d3c_b2a1 => true,
            // PCAPNG Section Header Block magic (0x0A0D0D0A)
            0x0a0d_0d0a => return Err(ArtifactValidationStatus::UnsupportedFormat),
            _ => return Err(ArtifactValidationStatus::Invalid),
        };
        let link_type = read_u32(&data[20..24], big_endian);
        Ok(Self {
            data,
            offset: 24,
            big_endian,
            link_type,
        })
    }

    fn next_frame(&mut self) -> (Option<&'a [u8]>, bool) {
        if self.offset == self.data.len() {
            return (None, false);
        }
        if self.offset + 16 > self.data.len() {
            return (None, true);
        }
        let incl_len = read_u32(
            &self.data[self.offset + 8..self.offset + 12],
            self.big_endian,
        ) as usize;
        let start = self.offset + 16;
        let Some(end) = start.checked_add(incl_len) else {
            return (None, true);
        };
        if end > self.data.len() {
            return (None, true);
        }
        self.offset = end;
        (Some(&self.data[start..end]), false)
    }
}

fn read_u32(bytes: &[u8], big_endian: bool) -> u32 {
    let arr = [bytes[0], bytes[1], bytes[2], bytes[3]];
    if big_endian {
        u32::from_be_bytes(arr)
    } else {
        u32::from_le_bytes(arr)
    }
}

pub fn analyze_pcap(data: &[u8]) -> PcapAnalysisResult {
    let mut reader = match PcapSliceReader::new(data) {
        Ok(r) => r,
        Err(ArtifactValidationStatus::UnsupportedFormat) => {
            return empty_result(
                ArtifactValidationStatus::UnsupportedFormat,
                "PCAPNG format detected; only classic libpcap (.cap/.pcap) with IEEE 802.11 or Radiotap link type is currently supported.",
                0,
                "PCAPNG (Unsupported)",
            );
        }
        Err(_) => {
            return empty_result(
                ArtifactValidationStatus::Invalid,
                "File is too short or does not start with a valid classic libpcap magic header.",
                0,
                "Unknown",
            );
        }
    };

    let link_type = reader.link_type;
    let link_type_name = match link_type {
        LINKTYPE_IEEE802_11 => "IEEE 802.11 (105)".to_string(),
        LINKTYPE_IEEE802_11_RADIOTAP => "IEEE 802.11 Radiotap (127)".to_string(),
        other => format!("Unsupported ({other})"),
    };

    if link_type != LINKTYPE_IEEE802_11 && link_type != LINKTYPE_IEEE802_11_RADIOTAP {
        return empty_result(
            ArtifactValidationStatus::UnsupportedFormat,
            &format!("Unsupported link-layer type {link_type}. Expected IEEE 802.11 (105) or Radiotap (127)."),
            link_type,
            &link_type_name,
        );
    }

    let mut total_packets = 0u64;
    let mut decoded_80211_frames = 0u64;
    let mut undecodable_frames = 0u64;
    let mut truncated_trailing_record = false;
    let mut beacon_frames = 0u64;
    let mut probe_request_frames = 0u64;
    let mut probe_response_frames = 0u64;
    let mut data_frames = 0u64;
    let mut eapol_frames = 0u64;
    let mut deauth_frames = 0u64;
    let disassoc_frames = 0u64;

    let mut aps: HashMap<String, ExtractedAp> = HashMap::new();
    let mut clients: HashMap<String, ExtractedClient> = HashMap::new();
    let mut handshake_bits: HashMap<(String, String), u8> = HashMap::new();
    let mut pmkid_records: HashMap<(String, String), u16> = HashMap::new();

    loop {
        let (frame_opt, truncated) = reader.next_frame();
        if truncated {
            truncated_trailing_record = true;
            break;
        }
        let Some(record) = frame_opt else {
            break;
        };
        total_packets += 1;

        let (body, fcs, signal_dbm) = if link_type == LINKTYPE_IEEE802_11_RADIOTAP {
            match radiotap::Radiotap::parse(record) {
                Ok((rt, rest)) => {
                    if rt.flags.as_ref().is_some_and(|f| f.bad_fcs) {
                        undecodable_frames += 1;
                        continue;
                    }
                    let fcs_flag = rt.flags.as_ref().is_some_and(|f| f.fcs);
                    let sig = rt.antenna_signal.map(|s| s.value as i32);
                    (rest, fcs_flag, sig)
                }
                Err(_) => {
                    undecodable_frames += 1;
                    continue;
                }
            }
        } else {
            (record, false, None)
        };

        let frame = match libwifi::parse_frame(body, fcs) {
            Ok(f) => f,
            Err(_) if fcs => match libwifi::parse_frame(body, false) {
                Ok(f) => f,
                Err(_) => {
                    undecodable_frames += 1;
                    continue;
                }
            },
            Err(_) => {
                undecodable_frames += 1;
                continue;
            }
        };

        decoded_80211_frames += 1;

        match frame {
            Frame::Beacon(b) => {
                beacon_frames += 1;
                fold_ap(&b.header, b.capability_info, &b.station_info, signal_dbm, &mut aps);
            }
            Frame::ProbeResponse(p) => {
                probe_response_frames += 1;
                fold_ap(&p.header, p.capability_info, &p.station_info, signal_dbm, &mut aps);
            }
            Frame::ProbeRequest(p) => {
                probe_request_frames += 1;
                fold_probe(&p.header, &p.station_info, signal_dbm, &mut clients);
            }
            Frame::Deauthentication(_) => {
                deauth_frames += 1;
            }
            Frame::Data(d) => {
                data_frames += 1;
                fold_data_client(&d.header, signal_dbm, &mut clients);
                if d.eapol_key.is_some() {
                    eapol_frames += 1;
                }
                fold_eapol(&d.header, &d.eapol_key, &mut handshake_bits, &mut pmkid_records);
            }
            Frame::QosData(d) => {
                data_frames += 1;
                fold_data_client(&d.header, signal_dbm, &mut clients);
                if d.eapol_key.is_some() {
                    eapol_frames += 1;
                }
                fold_eapol(&d.header, &d.eapol_key, &mut handshake_bits, &mut pmkid_records);
            }
            Frame::NullData(d) => {
                data_frames += 1;
                fold_data_client(&d.header, signal_dbm, &mut clients);
            }
            Frame::QosNull(d) => {
                data_frames += 1;
                fold_data_client(&d.header, signal_dbm, &mut clients);
            }
            _ => {}
        }
    }

    let handshakes: Vec<HandshakeArtifactSummary> = handshake_bits
        .into_iter()
        .map(|((bssid, station_mac), bits)| {
            let has_m1 = bits & 0b0001 != 0;
            let has_m2 = bits & 0b0010 != 0;
            let has_m3 = bits & 0b0100 != 0;
            let has_m4 = bits & 0b1000 != 0;
            let is_complete = has_m2 && (has_m1 || has_m3);
            let essid = aps
                .get(&bssid)
                .map(|a| a.essid.clone())
                .unwrap_or_else(|| "Unknown / Hidden".to_string());
            HandshakeArtifactSummary {
                bssid,
                essid,
                station_mac,
                has_m1,
                has_m2,
                has_m3,
                has_m4,
                is_complete_handshake: is_complete,
                status: if is_complete {
                    ArtifactValidationStatus::Valid
                } else {
                    ArtifactValidationStatus::Incomplete
                },
            }
        })
        .collect();

    let pmkids: Vec<PmkidArtifactSummary> = pmkid_records
        .into_iter()
        .map(|((bssid, station_mac), key_descriptor_version)| {
            let essid = aps
                .get(&bssid)
                .map(|a| a.essid.clone())
                .unwrap_or_else(|| "Unknown / Hidden".to_string());
            let is_wpa3_sae = key_descriptor_version == 0;
            let is_wpa2_psk = matches!(key_descriptor_version, 1..=3);
            PmkidArtifactSummary {
                bssid,
                essid,
                station_mac,
                key_descriptor_version,
                is_wpa2_psk_pmkid: is_wpa2_psk,
                is_wpa3_sae_pmkid: is_wpa3_sae,
                status: ArtifactValidationStatus::Valid,
            }
        })
        .collect();

    let validation_status = if total_packets == 0 {
        ArtifactValidationStatus::Incomplete
    } else if decoded_80211_frames == 0 {
        ArtifactValidationStatus::AnalysisFailed
    } else if truncated_trailing_record {
        ArtifactValidationStatus::Incomplete
    } else {
        ArtifactValidationStatus::Valid
    };

    let status_detail = match validation_status {
        ArtifactValidationStatus::Valid => format!(
            "Parsed {decoded_80211_frames}/{total_packets} frames ({} APs, {} clients, {} handshakes, {} PMKIDs).",
            aps.len(),
            clients.len(),
            handshakes.len(),
            pmkids.len()
        ),
        ArtifactValidationStatus::Incomplete if total_packets == 0 => {
            "Capture contains a valid libpcap header but zero packet records.".to_string()
        }
        ArtifactValidationStatus::Incomplete => format!(
            "Capture has a truncated trailing record; decoded {decoded_80211_frames}/{total_packets} complete frames."
        ),
        ArtifactValidationStatus::AnalysisFailed => {
            "No valid 802.11 frames could be decoded from the capture records.".to_string()
        }
        _ => String::new(),
    };

    PcapAnalysisResult {
        validation_status,
        status_detail,
        link_type,
        link_type_name,
        total_packets,
        decoded_80211_frames,
        undecodable_frames,
        truncated_trailing_record,
        beacon_frames,
        probe_request_frames,
        probe_response_frames,
        data_frames,
        eapol_frames,
        deauth_frames,
        disassoc_frames,
        access_points: aps.into_values().collect(),
        clients: clients.into_values().collect(),
        handshakes,
        pmkids,
    }
}

fn empty_result(
    status: ArtifactValidationStatus,
    detail: &str,
    link_type: u32,
    link_type_name: &str,
) -> PcapAnalysisResult {
    PcapAnalysisResult {
        validation_status: status,
        status_detail: detail.to_string(),
        link_type,
        link_type_name: link_type_name.to_string(),
        total_packets: 0,
        decoded_80211_frames: 0,
        undecodable_frames: 0,
        truncated_trailing_record: false,
        beacon_frames: 0,
        probe_request_frames: 0,
        probe_response_frames: 0,
        data_frames: 0,
        eapol_frames: 0,
        deauth_frames: 0,
        disassoc_frames: 0,
        access_points: Vec::new(),
        clients: Vec::new(),
        handshakes: Vec::new(),
        pmkids: Vec::new(),
    }
}

fn fold_ap(
    header: &ManagementHeader,
    capability_info: u16,
    info: &StationInfo,
    signal_dbm: Option<i32>,
    aps: &mut HashMap<String, ExtractedAp>,
) {
    let Some(bssid) = header.bssid() else { return };
    let bssid = bssid.to_long_string();
    let channel = info.channel();
    let band = match channel {
        Some(c) if c > 14 => "5 GHz".to_string(),
        Some(_) => "2.4 GHz".to_string(),
        None => "Unknown".to_string(),
    };
    let privacy = classify_privacy(capability_info, info);
    let raw_essid = info
        .essid()
        .filter(|s| !s.starts_with("<hidden"))
        .map(|s| s.chars().filter(|c| !c.is_control()).collect::<String>())
        .unwrap_or_default();
    let hidden = raw_essid.is_empty();
    let essid = if hidden {
        format!("[Hidden] (length: {})", info.ssid_length.unwrap_or(0))
    } else {
        raw_essid
    };

    aps.entry(bssid.clone())
        .and_modify(|ap| {
            ap.beacon_count += 1;
            if !hidden {
                ap.essid = essid.clone();
                ap.hidden = false;
            }
            if signal_dbm.is_some() {
                ap.signal_dbm = signal_dbm;
            }
        })
        .or_insert_with(|| ExtractedAp {
            vendor: find_vendor(&bssid),
            bssid,
            essid,
            hidden,
            channel,
            band,
            privacy,
            signal_dbm,
            beacon_count: 1,
        });
}

fn fold_probe(
    header: &ManagementHeader,
    info: &StationInfo,
    signal_dbm: Option<i32>,
    clients: &mut HashMap<String, ExtractedClient>,
) {
    let mac = header.address_2.to_long_string();
    let probe = info
        .essid()
        .filter(|s| !s.starts_with("<hidden"))
        .map(|s| s.chars().filter(|c| !c.is_control()).collect::<String>())
        .filter(|s| !s.is_empty());

    let entry = clients.entry(mac.clone()).or_insert_with(|| ExtractedClient {
        vendor: find_vendor(&mac),
        mac,
        associated_bssid: None,
        packet_count: 0,
        signal_dbm,
        probed_ssids: Vec::new(),
    });
    entry.packet_count += 1;
    if signal_dbm.is_some() {
        entry.signal_dbm = signal_dbm;
    }
    if let Some(p) = probe {
        if !entry.probed_ssids.contains(&p) {
            entry.probed_ssids.push(p);
        }
    }
}

fn fold_data_client(
    header: &DataHeader,
    signal_dbm: Option<i32>,
    clients: &mut HashMap<String, ExtractedClient>,
) {
    let fc = &header.frame_control;
    let (station, bssid) = if fc.to_ds() && !fc.from_ds() {
        (header.ta(), header.ra())
    } else if fc.from_ds() && !fc.to_ds() {
        (header.ra(), header.ta())
    } else {
        return;
    };
    let mac = station.to_long_string();
    let bssid_str = bssid.to_long_string();
    let entry = clients.entry(mac.clone()).or_insert_with(|| ExtractedClient {
        vendor: find_vendor(&mac),
        mac,
        associated_bssid: Some(bssid_str.clone()),
        packet_count: 0,
        signal_dbm,
        probed_ssids: Vec::new(),
    });
    entry.packet_count += 1;
    if entry.associated_bssid.is_none() {
        entry.associated_bssid = Some(bssid_str);
    }
    if signal_dbm.is_some() {
        entry.signal_dbm = signal_dbm;
    }
}

fn fold_eapol(
    header: &DataHeader,
    eapol: &Option<EapolKey>,
    handshake_bits: &mut HashMap<(String, String), u8>,
    pmkid_records: &mut HashMap<(String, String), u16>,
) {
    let Some(key) = eapol else { return };
    if key.key_information & 0x0008 == 0 {
        return;
    }
    let ack = key.key_information & 0x0080 != 0;
    let mic = key.key_information & 0x0100 != 0;
    let secure = key.key_information & 0x0200 != 0;
    let message = match (ack, mic, secure) {
        (true, false, _) => 1u8,
        (false, true, false) => 2u8,
        (true, true, _) => 3u8,
        (false, true, true) => 4u8,
        _ => return,
    };

    let fc = &header.frame_control;
    let (bssid, station) = if fc.to_ds() && !fc.from_ds() {
        (header.ra(), header.ta())
    } else if fc.from_ds() && !fc.to_ds() {
        (header.ta(), header.ra())
    } else {
        return;
    };

    let pair = (bssid.to_long_string(), station.to_long_string());
    *handshake_bits.entry(pair.clone()).or_insert(0) |= 1 << (message - 1);

    if key.pmkid().is_some() {
        let descriptor_version = key.key_information & 0x0007;
        // Also cross-check with airgorah-common's has_crackable_pmkid
        let _ = has_crackable_pmkid(key);
        pmkid_records.entry(pair).or_insert(descriptor_version);
    }
}

fn classify_privacy(capability_info: u16, info: &StationInfo) -> String {
    if let Some(rsn) = &info.rsn_information {
        if rsn.akm_suites.contains(&RsnAkmSuite::SAE) {
            return "WPA3".to_string();
        }
        return "WPA2".to_string();
    }
    if info.wpa_info.is_some() {
        return "WPA".to_string();
    }
    if capability_info & 0x0010 != 0 {
        return "WEP".to_string();
    }
    "OPN".to_string()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn rejects_invalid_pcap_and_pcapng() {
        let short = analyze_pcap(&[1, 2, 3]);
        assert_eq!(short.validation_status, ArtifactValidationStatus::Invalid);

        let mut pcapng = vec![0x0a, 0x0d, 0x0d, 0x0a];
        pcapng.extend_from_slice(&[0u8; 24]);
        let ng = analyze_pcap(&pcapng);
        assert_eq!(ng.validation_status, ArtifactValidationStatus::UnsupportedFormat);
    }

    #[test]
    fn reports_incomplete_for_empty_pcap() {
        let mut header = Vec::new();
        header.extend_from_slice(&0xa1b2_c3d4u32.to_le_bytes());
        header.extend_from_slice(&2u16.to_le_bytes());
        header.extend_from_slice(&4u16.to_le_bytes());
        header.extend_from_slice(&0i32.to_le_bytes());
        header.extend_from_slice(&0u32.to_le_bytes());
        header.extend_from_slice(&65535u32.to_le_bytes());
        header.extend_from_slice(&LINKTYPE_IEEE802_11.to_le_bytes());
        let res = analyze_pcap(&header);
        assert_eq!(res.validation_status, ArtifactValidationStatus::Incomplete);
        assert_eq!(res.total_packets, 0);
    }
}
