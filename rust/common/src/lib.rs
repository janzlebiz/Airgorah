//! Shared building blocks for Airgorah Mobile's native Rust core.
//!
//! Migrated from `martin-olivier/airgorah` (`crates/common`) with Linux IPC and
//! CLI dependency checks removed for Android compatibility.

pub mod channel;
pub mod handshake;
pub mod types;

pub use types::*;

pub const VERSION: &str = concat!("v", env!("CARGO_PKG_VERSION"));
