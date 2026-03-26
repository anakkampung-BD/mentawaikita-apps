#!/usr/bin/env bash
# Wrapper Gradle untuk memastikan environment konsisten:
# - JAVA_HOME = JDK 17
# - ANDROID_HOME = sdk.dir dari local.properties (fallback ke default macOS)
#
# Contoh:
#   ./scripts/gradle.sh :app:clean
#   ./scripts/gradle.sh :app:publishReleaseApkToRepo

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# Pakai JDK 17 jika JAVA_HOME belum ada/invalid.
if [[ -z "${JAVA_HOME:-}" || ! -d "${JAVA_HOME:-}" ]]; then
  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
  fi
fi

if [[ -z "${JAVA_HOME:-}" || ! -d "${JAVA_HOME:-}" ]]; then
  for candidate in \
    "/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home" \
    "/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home"; do
    if [[ -d "$candidate" ]]; then
      export JAVA_HOME="$candidate"
      break
    fi
  done
fi

if [[ -z "${JAVA_HOME:-}" || ! -d "${JAVA_HOME:-}" ]]; then
  echo "JAVA_HOME JDK 17 tidak ditemukan. Install JDK 17 dulu."
  exit 1
fi

# Prioritas SDK:
# 1) sdk.dir dari local.properties
# 2) ANDROID_HOME environment
# 3) fallback default macOS
if [[ -f "$ROOT/local.properties" ]]; then
  SDK_FROM_LOCAL="$(awk -F= '/^sdk.dir=/{print $2}' "$ROOT/local.properties" | tr -d '\r' || true)"
else
  SDK_FROM_LOCAL=""
fi

if [[ -n "${SDK_FROM_LOCAL:-}" && -d "${SDK_FROM_LOCAL:-}" ]]; then
  export ANDROID_HOME="$SDK_FROM_LOCAL"
else
  export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
fi
export ANDROID_SDK_ROOT="$ANDROID_HOME"

exec "$ROOT/gradlew" "$@"
