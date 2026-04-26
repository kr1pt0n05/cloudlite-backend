SESSION_ID="your-session-id"
TOKEN="your-jwt-token"
BATCH_SIZE=250

files=(test-files/*)
total=${#files[@]}

for ((i=0; i<total; i+=BATCH_SIZE)); do
  ARGS=()

  for f in "${files[@]:i:BATCH_SIZE}"; do
    ARGS+=(-F "files=@${f}")
  done

  echo "Uploading batch $((i / BATCH_SIZE + 1))..."

  curl -s -o /dev/null -w "HTTP %{http_code}, time %{time_total}s\n" \
    -X POST "http://localhost:8080/api/upload/sessions/$SESSION_ID/files/batch" \
    -H "Authorization: Bearer $TOKEN" \
    "${ARGS[@]}"
done
