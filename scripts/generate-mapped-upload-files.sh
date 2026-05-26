#!/usr/bin/env bash
set -euo pipefail

OUTPUT_DIR="${OUTPUT_DIR:-mapped-upload-files}"
FILE_COUNT="${FILE_COUNT:-${1:-100}}"
FILE_SIZE_KB="${FILE_SIZE_KB:-4}"

PATHS=(
  "/Studium"
  "/Studium/Wintersemester 24"
  "/Studium/Sommersemester 25"
  "/Arbeit"
  "/Workspace"
  "/Workspace/Python"
  "/Workspace/Python/Learning  123"
)

mkdir -p "$OUTPUT_DIR"
target_size_bytes=$((FILE_SIZE_KB * 1024))

mapping='{"files":['

for i in $(seq 1 "$FILE_COUNT"); do
  file_id="$(uuidgen 2>/dev/null || cat /proc/sys/kernel/random/uuid)"
  user_file_name="$(printf "test-file-%03d.txt" "$i")"
  upload_file_name="${file_id}-${user_file_name}"
  path="${PATHS[$(((i - 1) % ${#PATHS[@]}))]}"

  file_path="$OUTPUT_DIR/$upload_file_name"

  {
    printf 'Generated test file %03d\n' "$i"
    printf 'fileId=%s\n' "$file_id"
    printf 'userFileName=%s\n' "$user_file_name"
    printf 'targetPath=%s\n' "$path"
  } > "$file_path"

  current_size="$(wc -c < "$file_path")"
  if ((target_size_bytes > current_size)); then
    remaining_bytes=$((target_size_bytes - current_size))
    head -c "$remaining_bytes" /dev/urandom >> "$file_path"
  fi

  if ((i > 1)); then
    mapping+=","
  fi
  mapping+="{\"fileId\":\"$file_id\",\"path\":\"$path\"}"
done

mapping+=']}'
printf '%s\n' "$mapping" > "$OUTPUT_DIR/mapping.json"

echo "Generated $FILE_COUNT files in: $OUTPUT_DIR"
echo "Target file size: ${FILE_SIZE_KB}KB"
echo "Mapping written to: $OUTPUT_DIR/mapping.json"
echo
cat "$OUTPUT_DIR/mapping.json"
