#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8000}"
TOKEN="${TOKEN:-${1:-}}"

if [[ -z "$TOKEN" ]]; then
  echo "TOKEN is required. Usage: TOKEN=<jwt> $0"
  exit 1
fi

curl -sS \
  -X POST "$BASE_URL/api/upload/sessions" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{}"

echo
