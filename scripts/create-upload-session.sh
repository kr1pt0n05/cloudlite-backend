#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8000}"
TARGET_FOLDER_ID="${TARGET_FOLDER_ID:-${1:-}}"
TOKEN="${TOKEN:-${2:-}}"

if [[ -z "$TARGET_FOLDER_ID" ]]; then
  echo "TARGET_FOLDER_ID is required. Usage: TARGET_FOLDER_ID=<uuid> TOKEN=<jwt> $0"
  exit 1
fi

if [[ -z "$TOKEN" ]]; then
  echo "TOKEN is required. Usage: TARGET_FOLDER_ID=<uuid> TOKEN=<jwt> $0"
  exit 1
fi

curl -sS \
  -X POST "$BASE_URL/api/upload/sessions" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"targetFolderId\":\"$TARGET_FOLDER_ID\"}"

echo
