#!/usr/bin/env bash
# Jalankan AVD Pixel_6_API34 (sesuai penggunaan Anda: emulator -avd Pixel_6_API34 -gpu on)
# Pastikan $ANDROID_HOME/emulator ada di PATH, atau gunakan path penuh di bawah.
#
# Penggunaan: ./scripts/start-emulator-pixel6.sh
# Biarkan terminal ini terbuka; di terminal lain jalankan ./scripts/dev-emulator.sh atau ./gradlew :app:installDebug

set -euo pipefail

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
EMU_BIN="$ANDROID_HOME/emulator/emulator"

if [[ ! -x "$EMU_BIN" ]]; then
  echo "Tidak menemukan: $EMU_BIN — set ANDROID_HOME ke lokasi Android SDK Anda."
  exit 1
fi

exec "$EMU_BIN" -avd Pixel_6_API34 -gpu on
