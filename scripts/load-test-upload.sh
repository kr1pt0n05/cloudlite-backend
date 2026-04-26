#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

FILE_COUNT="${FILE_COUNT:-8000}"
FILE_SIZE="${FILE_SIZE:-22K}"
FILES_DIR="${FILES_DIR:-test-files}"
BATCH_SIZE="${BATCH_SIZE:-250}"

OUTPUT_DIR="$FILES_DIR" \
FILE_COUNT="$FILE_COUNT" \
FILE_SIZE="$FILE_SIZE" \
"$SCRIPT_DIR/generate-test-files.sh"

FILES_DIR="$FILES_DIR" \
BATCH_SIZE="$BATCH_SIZE" \
"$SCRIPT_DIR/upload-batch.sh" "$@"
