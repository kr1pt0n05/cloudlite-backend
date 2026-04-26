#!/usr/bin/env bash
set -euo pipefail

OUTPUT_DIR="${OUTPUT_DIR:-test-files}"
FILE_COUNT="${FILE_COUNT:-8000}"
FILE_SIZE="${FILE_SIZE:-22K}"
CLEAN="${CLEAN:-false}"

if [[ "$CLEAN" == "true" ]]; then
  if [[ -z "$OUTPUT_DIR" || "$OUTPUT_DIR" == "/" ]]; then
    echo "Refusing to clean unsafe OUTPUT_DIR: $OUTPUT_DIR"
    exit 1
  fi

  rm -rf "$OUTPUT_DIR"
fi

mkdir -p "$OUTPUT_DIR"

padding=${#FILE_COUNT}

echo "Generating $FILE_COUNT random files of approximately $FILE_SIZE each in $OUTPUT_DIR"

for i in $(seq -f "%0${padding}g" 1 "$FILE_COUNT"); do
  file="$OUTPUT_DIR/file-$i.bin"
  if [[ -f "$file" ]]; then
    continue
  fi

  dd if=/dev/urandom of="$file" bs="$FILE_SIZE" count=1 status=none

  if ((10#$i % 250 == 0)); then
    echo "Generated $i / $FILE_COUNT files"
  fi
done

echo "Done. Generated data size:"
du -sh "$OUTPUT_DIR"
