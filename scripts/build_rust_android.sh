#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ANDROID_SDK="${ANDROID_HOME:-/home/antigravity/android-sdk}"
NDK_DIR="${ANDROID_NDK_HOME:-$(ls -d "$ANDROID_SDK/ndk/"* | sort -V | tail -n 1)}"
NDK_BIN="$NDK_DIR/toolchains/llvm/prebuilt/linux-x86_64/bin"

export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="${NDK_BIN}/aarch64-linux-android26-clang"
export CC_aarch64_linux_android="${NDK_BIN}/aarch64-linux-android26-clang"

cd "$ROOT_DIR/rust"
cargo build --release --target aarch64-linux-android -p airgorah-android-bridge

mkdir -p "$ROOT_DIR/android/app/src/main/jniLibs/arm64-v8a"
cp target/aarch64-linux-android/release/libairgorah_core.so \
   "$ROOT_DIR/android/app/src/main/jniLibs/arm64-v8a/libairgorah_core.so"

echo "Built libairgorah_core.so for arm64-v8a"
