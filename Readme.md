# CloudLite Backend

## Generate mapped upload test files

Use `scripts/generate-mapped-upload-files.sh` to create UUID-prefixed test files
and a matching `mapping.json` for the batch upload endpoint.

```bash
./scripts/generate-mapped-upload-files.sh
```

The first positional argument controls the number of files:

```bash
./scripts/generate-mapped-upload-files.sh 250
```

Optional environment variables:

```bash
OUTPUT_DIR=my-upload-files FILE_SIZE_KB=64 ./scripts/generate-mapped-upload-files.sh 100
```

- `OUTPUT_DIR`: output directory, default `mapped-upload-files`
- `FILE_COUNT`: number of files, default `100`; overridden by the first argument when unset
- `FILE_SIZE_KB`: target size per file in KB, default `4`

Each generated upload filename has this format:

```text
<fileId>-<userFileName>
```

Example:

```text
1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed-test-file-001.txt
```

The generated `mapping.json` looks like:

```json
{
  "files": [
    {
      "fileId": "1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed",
      "path": "/Studium"
    }
  ]
}
```

The backend removes the UUID prefix before storing the file, so the example
above is stored as `test-file-001.txt`.
