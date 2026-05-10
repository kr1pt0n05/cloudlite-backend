# CloudLite Backend

CloudLite Backend is a Spring Boot service for CloudLite, a lightweight
self-hosted cloud storage project inspired by Nextcloud-style file sync, built
mainly for learning and experimentation.

The backend provides the central API for authenticated users, file and folder
metadata, batch uploads, local filesystem storage, and changelog-based
synchronization with the CloudLite desktop client.

## Scope

This project currently focuses on the backend storage API, local development
security, folder hierarchy management, mapped batch file uploads, metadata
persistence, and changelog entries that clients can use to synchronize local
state.

The current storage implementation keeps file metadata in PostgreSQL and file
content on the local filesystem. A future storage model may add
content-addressed blobs with content-based chunking to support more efficient
deduplication and sync behavior.

CloudLite frontend repository: TODO: add frontend repository URL.

## Architecture

- Spring Boot provides the HTTP API and application runtime.
- Spring Security runs the development OAuth2 resource server integration.
- Keycloak OAuth2 is used for authentication in local development.
- PostgreSQL stores users, folders, files, and changelog metadata.
- The local filesystem stores uploaded file content under the configured
  storage base path.
- File, folder, upload, changelog, and user packages separate the main backend
  responsibilities.
- Docker Compose starts the local PostgreSQL and Keycloak dependencies.

## Commands

```bash
docker compose up -d
./mvnw spring-boot:run
./mvnw test
```

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
