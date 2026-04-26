#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8000}"
FILES_DIR="${FILES_DIR:-test-files}"
BATCH_SIZE="${BATCH_SIZE:-250}"
CONNECT_TIMEOUT="${CONNECT_TIMEOUT:-10}"
MAX_TIME="${MAX_TIME:-300}"
SESSION_ID="${SESSION_ID:-${1:-}}"
TOKEN="${TOKEN:-${2:-}}"

if [[ -z "$SESSION_ID" ]]; then
  echo "SESSION_ID is required. Usage: SESSION_ID=<uuid> TOKEN=<jwt> $0"
  exit 1
fi

if [[ -z "$TOKEN" ]]; then
  echo "TOKEN is required. Usage: SESSION_ID=<uuid> TOKEN=<jwt> $0"
  exit 1
fi

if [[ ! -d "$FILES_DIR" ]]; then
  echo "Files directory not found: $FILES_DIR"
  echo "Run scripts/generate-test-files.sh first."
  exit 1
fi

mapfile -t files < <(find "$FILES_DIR" -maxdepth 1 -type f -name '*.bin' | sort)
total=${#files[@]}

if ((total == 0)); then
  echo "No .bin files found in $FILES_DIR"
  exit 1
fi

echo "Uploading $total files from $FILES_DIR to session $SESSION_ID"
echo "Batch size: $BATCH_SIZE"
echo "Backend: $BASE_URL"
echo "Curl connect timeout: ${CONNECT_TIMEOUT}s"
echo "Curl per-batch max time: ${MAX_TIME}s"

start_epoch=$(date +%s)
start_human=$(date -Is)
response_file=$(mktemp)
trap 'rm -f "$response_file"' EXIT

echo "Started at $start_human"

for ((i=0; i<total; i+=BATCH_SIZE)); do
  ARGS=()

  for f in "${files[@]:i:BATCH_SIZE}"; do
    ARGS+=(-F "files=@${f}")
  done

  batch_number=$((i / BATCH_SIZE + 1))
  batch_end=$((i + BATCH_SIZE))
  if ((batch_end > total)); then
    batch_end=$total
  fi

  batch_start_epoch=$(date +%s)
  echo "Uploading batch $batch_number ($((i + 1))-$batch_end of $total) at $(date -Is)..."

  set +e
  curl_output=$(curl -sS -o "$response_file" -w "%{http_code} %{time_total}" \
    --connect-timeout "$CONNECT_TIMEOUT" \
    --max-time "$MAX_TIME" \
    -X POST "$BASE_URL/api/upload/sessions/$SESSION_ID/files/batch" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Expect:" \
    "${ARGS[@]}")
  curl_exit=$?
  set -e

  if ((curl_exit != 0)); then
    echo "Batch $batch_number failed before receiving a complete HTTP response. curl exit: $curl_exit"
    if [[ -s "$response_file" ]]; then
      cat "$response_file"
      echo
    fi
    exit 1
  fi

  read -r http_code time_total <<< "$curl_output"

  if [[ "$http_code" != "201" ]]; then
    echo "Batch $batch_number failed with HTTP $http_code"
    cat "$response_file"
    echo
    exit 1
  fi

  batch_elapsed=$(( $(date +%s) - batch_start_epoch ))
  echo "Batch $batch_number completed: HTTP $http_code, curl=${time_total}s, wall=${batch_elapsed}s"
done

elapsed=$(( $(date +%s) - start_epoch ))
echo "Uploaded $total files in ${elapsed}s"
echo "Finished at $(date -Is)"
