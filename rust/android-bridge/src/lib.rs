//! JNI bridge for Airgorah Mobile (`libairgorah_core.so`).
//!
//! All JNI entry points are wrapped in `std::panic::catch_unwind` so a Rust
//! panic never unwinds across the FFI boundary into the JVM. Every response
//! uses a typed JSON envelope: `{ "ok": true, "data": ... }` or
//! `{ "ok": false, "error": "..." }`.

use std::collections::HashMap;
use std::panic::AssertUnwindSafe;

use jni::JNIEnv;
use jni::objects::{JByteArray, JClass, JString};
use jni::sys::jstring;
use serde::Serialize;

#[derive(Serialize)]
struct JniEnvelope<T: Serialize> {
    ok: bool,
    #[serde(skip_serializing_if = "Option::is_none")]
    data: Option<T>,
    #[serde(skip_serializing_if = "Option::is_none")]
    error: Option<String>,
}

fn ok_json<T: Serialize>(data: T) -> String {
    serde_json::to_string(&JniEnvelope {
        ok: true,
        data: Some(data),
        error: None,
    })
    .unwrap_or_else(|e| err_json(&format!("Serialization error: {e}")))
}

fn err_json(msg: &str) -> String {
    serde_json::to_string(&JniEnvelope::<serde_json::Value> {
        ok: false,
        data: None,
        error: Some(msg.to_string()),
    })
    .unwrap_or_else(|_| r#"{"ok":false,"error":"Fatal JSON serialization failure"}"#.to_string())
}

fn run_safe<F>(f: F) -> String
where
    F: FnOnce() -> Result<String, String>,
{
    match std::panic::catch_unwind(AssertUnwindSafe(f)) {
        Ok(Ok(json)) => json,
        Ok(Err(e)) => err_json(&e),
        Err(_) => err_json("Internal native panic contained safely"),
    }
}

fn to_jstring(env: &mut JNIEnv, value: &str) -> jstring {
    env.new_string(value)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

#[derive(Serialize)]
struct CoreVersionInfo {
    version: &'static str,
    arch: &'static str,
    os: &'static str,
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_airgorah_mobile_core_rust_NativeBridge_nativeGetVersion(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let out = run_safe(|| {
        Ok(ok_json(CoreVersionInfo {
            version: airgorah_common::VERSION,
            arch: std::env::consts::ARCH,
            os: std::env::consts::OS,
        }))
    });
    to_jstring(&mut env, &out)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_airgorah_mobile_core_rust_NativeBridge_nativeLookupVendor(
    mut env: JNIEnv,
    _class: JClass,
    mac: JString,
) -> jstring {
    let mac_str: String = match env.get_string(&mac) {
        Ok(s) => s.into(),
        Err(e) => return to_jstring(&mut env, &err_json(&format!("Invalid MAC string: {e}"))),
    };
    let out = run_safe(|| Ok(ok_json(airgorah_analysis::vendors::find_vendor(&mac_str))));
    to_jstring(&mut env, &out)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_airgorah_mobile_core_rust_NativeBridge_nativeLookupVendorsBatch(
    mut env: JNIEnv,
    _class: JClass,
    macs_json: JString,
) -> jstring {
    let json_str: String = match env.get_string(&macs_json) {
        Ok(s) => s.into(),
        Err(e) => return to_jstring(&mut env, &err_json(&format!("Invalid JSON string: {e}"))),
    };
    let out = run_safe(|| {
        let macs: Vec<String> =
            serde_json::from_str(&json_str).map_err(|e| format!("Invalid MAC array JSON: {e}"))?;
        let mut map = HashMap::with_capacity(macs.len());
        for m in macs {
            let vendor = airgorah_analysis::vendors::find_vendor(&m);
            map.insert(m, vendor);
        }
        Ok(ok_json(map))
    });
    to_jstring(&mut env, &out)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_airgorah_mobile_core_rust_NativeBridge_nativeValidateChannelFilter(
    mut env: JNIEnv,
    _class: JClass,
    filter: JString,
    ghz24: jni::sys::jboolean,
    ghz5: jni::sys::jboolean,
) -> jstring {
    let filter_str: String = match env.get_string(&filter) {
        Ok(s) => s.into(),
        Err(e) => return to_jstring(&mut env, &err_json(&format!("Invalid filter string: {e}"))),
    };
    let out = run_safe(|| {
        let valid = airgorah_common::channel::is_valid_channel_filter(
            &filter_str,
            ghz24 != 0,
            ghz5 != 0,
        );
        Ok(ok_json(valid))
    });
    to_jstring(&mut env, &out)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_airgorah_mobile_core_rust_NativeBridge_nativeAnalyzePcapBytes(
    mut env: JNIEnv,
    _class: JClass,
    data: JByteArray,
) -> jstring {
    let bytes = match env.convert_byte_array(data) {
        Ok(b) => b,
        Err(e) => return to_jstring(&mut env, &err_json(&format!("Failed to read byte array: {e}"))),
    };
    let out = run_safe(|| {
        let result = airgorah_analysis::pcap_analyzer::analyze_pcap(&bytes);
        Ok(ok_json(result))
    });
    to_jstring(&mut env, &out)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_airgorah_mobile_core_rust_NativeBridge_nativeAuditPasswordStrength(
    mut env: JNIEnv,
    _class: JClass,
    password_bytes: JByteArray,
) -> jstring {
    let mut bytes = match env.convert_byte_array(password_bytes) {
        Ok(b) => b,
        Err(e) => return to_jstring(&mut env, &err_json(&format!("Failed to read password bytes: {e}"))),
    };
    let out = run_safe(|| {
        let s = std::str::from_utf8(&bytes)
            .map_err(|_| "Password must be valid UTF-8".to_string())?;
        let audit = airgorah_analysis::password_audit::audit_password(s);
        Ok(ok_json(audit))
    });
    // Zeroize password bytes immediately after evaluation
    for b in &mut bytes {
        *b = 0;
    }
    to_jstring(&mut env, &out)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn panic_containment_returns_structured_error() {
        let res = run_safe(|| -> Result<String, String> {
            panic!("simulated failure");
        });
        assert!(res.contains(r#""ok":false"#));
        assert!(res.contains("panic contained"));
    }

    #[test]
    fn ok_json_wraps_payload() {
        let res = ok_json(true);
        assert_eq!(res, r#"{"ok":true,"data":true}"#);
    }
}
