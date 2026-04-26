mkdir -p test-files

for i in $(seq -w 1 8000); do
  dd if=/dev/urandom of="test-files/file-$i.bin" bs=22K count=1 status=none
done
