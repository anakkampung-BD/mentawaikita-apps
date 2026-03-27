#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOCAL_PROPS="${ROOT_DIR}/local.properties"
KEYSTORE_DIR="${ROOT_DIR}/keystore"
KEYSTORE_FILE_DEFAULT="${KEYSTORE_DIR}/obill-upload.jks"

STORE_FILE="${OBILL_PLAYSTORE_STORE_FILE:-$KEYSTORE_FILE_DEFAULT}"
STORE_PASSWORD="${OBILL_PLAYSTORE_STORE_PASSWORD:-}"
KEY_ALIAS="${OBILL_PLAYSTORE_KEY_ALIAS:-obill_upload}"
KEY_PASSWORD="${OBILL_PLAYSTORE_KEY_PASSWORD:-}"

if [[ -z "$STORE_PASSWORD" || -z "$KEY_PASSWORD" ]]; then
  echo "ERROR: Set env vars OBILL_PLAYSTORE_STORE_PASSWORD dan OBILL_PLAYSTORE_KEY_PASSWORD."
  echo "Opsional: OBILL_PLAYSTORE_STORE_FILE, OBILL_PLAYSTORE_KEY_ALIAS"
  exit 1
fi

mkdir -p "$KEYSTORE_DIR"

if [[ ! -f "$STORE_FILE" ]]; then
  echo "Membuat upload keystore baru di: $STORE_FILE"
  keytool -genkeypair \
    -v \
    -keystore "$STORE_FILE" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 9125 \
    -storepass "$STORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -dname "CN=OBill, OU=Android, O=OBill, L=Jakarta, ST=DKI Jakarta, C=ID"
else
  echo "Keystore sudah ada, skip generate: $STORE_FILE"
fi

touch "$LOCAL_PROPS"

upsert_prop() {
  local key="$1"
  local value="$2"
  if rg -n "^${key}=" "$LOCAL_PROPS" >/dev/null 2>&1; then
    python3 - "$LOCAL_PROPS" "$key" "$value" <<'PY'
import re, sys
path, key, value = sys.argv[1], sys.argv[2], sys.argv[3]
with open(path, "r", encoding="utf-8") as f:
    s = f.read()
s = re.sub(rf"^{re.escape(key)}=.*$", f"{key}={value}", s, flags=re.M)
with open(path, "w", encoding="utf-8") as f:
    f.write(s)
PY
  else
    printf "%s=%s\n" "$key" "$value" >> "$LOCAL_PROPS"
  fi
}

upsert_prop "obill.playstore.storeFile" "$STORE_FILE"
upsert_prop "obill.playstore.storePassword" "$STORE_PASSWORD"
upsert_prop "obill.playstore.keyAlias" "$KEY_ALIAS"
upsert_prop "obill.playstore.keyPassword" "$KEY_PASSWORD"

echo "Konfigurasi Play Store signing sudah ditulis ke: $LOCAL_PROPS"
echo "Langkah berikutnya:"
echo "  ./scripts/gradle.sh bundlePlaystoreRelease"
