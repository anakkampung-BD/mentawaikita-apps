#!/usr/bin/env bash
# OBill: build + install debug + launch MainActivity ke emulator/perangkat, lalu ulang
# otomatis setiap ada perubahan sumber (Gradle :app:installDebugAndRun --continuous).
#
# Penggunaan (dari root repo):
#   ./scripts/dev-emulator.sh
#
# Prasyarat:
#   - Emulator atau perangkat nyala; `adb devices` menampilkan status "device"
#     (contoh AVD: ./scripts/start-emulator-pixel6.sh — Pixel_6_API34 -gpu on)
#   - JDK 17+ untuk Gradle (set JAVA_HOME jika perlu)
#   - ANDROID_HOME menunjuk ke Android SDK (default: ~/Library/Android/sdk di macOS)
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# JDK 17 disarankan untuk AGP 8.x (override dengan JAVA_HOME)
if [[ -z "${JAVA_HOME:-}" ]]; then
  for candidate in \
    "/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home" \
    "/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home"; do
    if [[ -d "$candidate" ]]; then
      export JAVA_HOME="$candidate"
      break
    fi
  done
fi

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"

if [[ ! -x "$ROOT/gradlew" ]]; then
  echo "gradlew tidak ditemukan di $ROOT"
  exit 1
fi

if [[ ! -x "$ADB" ]]; then
  echo "adb tidak ditemukan: $ADB (set ANDROID_HOME)"
  exit 1
fi

if ! "$ADB" devices 2>/dev/null | grep -E '^\S+\s+device\s*$' -q; then
  echo "Tidak ada emulator/perangkat dengan status 'device'."
  echo "Nyalakan emulator standalone lalu pastikan: $ADB devices"
  exit 1
fi

echo "OBill dev loop: installDebug + launch + continuous (Ctrl+C berhenti)"
echo "JAVA_HOME=${JAVA_HOME:-<default>}"
echo "ANDROID_HOME=$ANDROID_HOME"
echo ""

exec "$ROOT/gradlew" :app:installDebugAndRun --continuous
