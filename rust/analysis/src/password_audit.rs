//! Defensive password strength & policy auditor.
//!
//! Evaluates user-supplied passphrases in memory without storing, logging, or
//! transmitting the plaintext password. Provides entropy estimation with
//! documented limitations, common-pattern detection, and WPA2/WPA3 policy advice.

use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq, Eq)]
pub enum StrengthRating {
    VeryWeak,
    Weak,
    Moderate,
    Strong,
    VeryStrong,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct PasswordAuditResult {
    pub length: usize,
    pub wpa_psk_length_valid: bool,
    pub has_lowercase: bool,
    pub has_uppercase: bool,
    pub has_digits: bool,
    pub has_symbols: bool,
    pub estimated_entropy_bits: f64,
    pub entropy_methodology_note: String,
    pub rating: StrengthRating,
    pub warnings: Vec<String>,
    pub recommendations: Vec<String>,
}

const COMMON_WEAK_PATTERNS: &[&str] = &[
    "password", "12345678", "123456789", "qwerty", "admin", "letmein",
    "welcome", "wifi", "wireless", "changeme", "default", "guest", "11111111",
    "00000000", "abcdef",
];

pub fn audit_password(password: &str) -> PasswordAuditResult {
    let length = password.chars().count();
    let wpa_psk_length_valid = (8..=63).contains(&length);

    let has_lowercase = password.chars().any(|c| c.is_ascii_lowercase());
    let has_uppercase = password.chars().any(|c| c.is_ascii_uppercase());
    let has_digits = password.chars().any(|c| c.is_ascii_digit());
    let has_symbols = password
        .chars()
        .any(|c| !c.is_ascii_alphanumeric() && !c.is_control());

    let mut charset_size = 0u32;
    if has_lowercase {
        charset_size += 26;
    }
    if has_uppercase {
        charset_size += 26;
    }
    if has_digits {
        charset_size += 10;
    }
    if has_symbols {
        charset_size += 33;
    }
    if charset_size == 0 && length > 0 {
        charset_size = 26;
    }

    let raw_entropy = if length == 0 || charset_size == 0 {
        0.0
    } else {
        (length as f64) * (charset_size as f64).log2()
    };

    let lower = password.to_ascii_lowercase();
    let mut warnings = Vec::new();
    let mut recommendations = Vec::new();

    if length == 0 {
        warnings.push("Passphrase is empty.".to_string());
    } else if length < 8 {
        warnings.push("WPA2/WPA3-PSK requires at least 8 characters.".to_string());
    } else if length < 12 {
        warnings.push("Passphrase is shorter than 12 characters; vulnerable to offline PBKDF2/GPU guessing if predictable.".to_string());
    }
    if length > 63 {
        warnings.push("WPA2-PSK ASCII passphrases must not exceed 63 characters.".to_string());
    }

    let mut pattern_penalty = 0.0;
    for pat in COMMON_WEAK_PATTERNS {
        if lower.contains(pat) {
            warnings.push(format!("Contains common dictionary/keyboard pattern ('{pat}')."));
            pattern_penalty += 25.0;
            break;
        }
    }

    if is_sequential_or_repeated(password) {
        warnings.push("Contains repeated characters or simple sequential runs.".to_string());
        pattern_penalty += 20.0;
    }

    let class_count = [has_lowercase, has_uppercase, has_digits, has_symbols]
        .into_iter()
        .filter(|&b| b)
        .count();
    if class_count < 3 && length > 0 {
        recommendations.push("Mix uppercase, lowercase, digits, and symbols (or use 4+ random diceware words).".to_string());
    }
    if length < 16 {
        recommendations.push("Use a random passphrase of at least 16 characters for WPA2-PSK networks.".to_string());
    }
    recommendations.push("Enable WPA3-SAE where supported to mitigate offline handshake/PMKID dictionary attacks.".to_string());

    let adjusted_entropy = (raw_entropy - pattern_penalty).max(0.0);
    let rating = if length < 8 || adjusted_entropy < 35.0 {
        StrengthRating::VeryWeak
    } else if adjusted_entropy < 50.0 {
        StrengthRating::Weak
    } else if adjusted_entropy < 70.0 {
        StrengthRating::Moderate
    } else if adjusted_entropy < 90.0 {
        StrengthRating::Strong
    } else {
        StrengthRating::VeryStrong
    };

    PasswordAuditResult {
        length,
        wpa_psk_length_valid,
        has_lowercase,
        has_uppercase,
        has_digits,
        has_symbols,
        estimated_entropy_bits: (adjusted_entropy * 10.0).round() / 10.0,
        entropy_methodology_note: "Shannon-style charset-pool estimate (L * log2(N)) minus heuristic pattern penalties. Assumes uniform random selection; human-chosen words have lower true entropy.".to_string(),
        rating,
        warnings,
        recommendations,
    }
}

fn is_sequential_or_repeated(s: &str) -> bool {
    let bytes = s.as_bytes();
    if bytes.len() < 4 {
        return false;
    }
    let mut repeat_run = 1usize;
    let mut seq_run = 1usize;
    for w in bytes.windows(2) {
        if w[0] == w[1] {
            repeat_run += 1;
            if repeat_run >= 4 {
                return true;
            }
        } else {
            repeat_run = 1;
        }
        if w[1] == w[0].wrapping_add(1) || w[0] == w[1].wrapping_add(1) {
            seq_run += 1;
            if seq_run >= 4 {
                return true;
            }
        } else {
            seq_run = 1;
        }
    }
    false
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn flags_weak_common_password() {
        let res = audit_password("password123");
        assert!(matches!(res.rating, StrengthRating::VeryWeak | StrengthRating::Weak));
        assert!(!res.warnings.is_empty());
    }

    #[test]
    fn rates_long_random_passphrase_strongly() {
        let res = audit_password("k9$Tm!vQ8#zP2@xL7&wR4");
        assert!(matches!(res.rating, StrengthRating::Strong | StrengthRating::VeryStrong));
        assert!(res.wpa_psk_length_valid);
        assert!(res.estimated_entropy_bits > 90.0);
    }
}
