# CloudLite Backend

CloudLite Backend is a Spring Boot service for CloudLite, a lightweight
self-hosted cloud storage project inspired by Nextcloud-style file sync, built
mainly for learning and experimentation.

The service provides the central API for authenticated desktop clients,
storing file and folder metadata, uploading file content, and exposing changes
for synchronization.

[![CloudLite Swagger API view](docs/screenshot_swagger.png)](docs/)

## Scope

- [x] Provide a Spring Boot backend API with PostgreSQL persistence.
- [x] Authenticate requests through Keycloak using OAuth2 bearer JWTs.
- [x] Create and browse folder hierarchies.
- [x] Stream mapped batch uploads to user-specific local filesystem paths.
- [x] Store upload metadata and SHA-256 checksums in PostgreSQL.
- [x] Expose changelog entries for client synchronization.
- [ ] Complete file browsing, download, rename, move, and delete operations.
- [ ] Complete folder rename, move, and delete operations.
- [ ] Add an object-storage implementation backed by RustFS.

CloudLite frontend repository: [cloudlite-frontend](https://github.com/kr1pt0n05/cloudlite-frontend)

## Architecture

- Spring Boot provides the HTTP API and application runtime.
- Spring Security configures the backend as an OAuth2 resource server.
- Keycloak provides the development realm and issues JWT access tokens for the
  `frontend-client` OAuth2/OpenID Connect client.
- PostgreSQL stores users, folder trees, file metadata, and synchronization
  changelogs.
- `FileStorageService` separates content storage from metadata persistence.
- The current `LocalFileStorageService` streams content to paths beneath
  `FILE_STORAGE_PATH`, prefixed by the authenticated user's JWT subject.
- Docker Compose starts the application database, a separate Keycloak database,
  and Keycloak with the imported `development` realm.

### Storage Direction

The current development implementation is filesystem based: PostgreSQL is the
source of truth for metadata, while file bytes are written under the configured
local storage directory (`./.uploads` by default).

A later object-based implementation is planned through the existing storage
abstraction, using [RustFS](https://rustfs.com/) as an S3-compatible,
MinIO-alternative object store. This keeps the API and metadata model separate
from the physical file storage backend.

## Setup

### Prerequisites

Install the following before starting:

- Java 25.
- Docker with Docker Compose.
- A POSIX-compatible shell for the Maven Wrapper (`./mvnw`).

The development stack uses these local endpoints by default:

- Backend API: `http://localhost:8000`
- Keycloak realm: `http://localhost:8080/realms/development`
- PostgreSQL: `localhost:5432`

### Run the Application

1. Clone the repository and enter the backend directory:

   ```bash
   git clone https://github.com/kr1pt0n05/cloudlite-backend.git
   cd cloudlite-backend
   ```

2. Create a local `.env` file for Docker Compose and Spring Boot:

   ```dotenv
   PORT=8000
   ALLOWED_ORIGIN=http://localhost:4200

   POSTGRES_HOST=localhost
   POSTGRES_PORT=5432
   POSTGRES_USER=cloudlite
   POSTGRES_PASSWORD=cloudlite
   POSTGRES_DB=cloudlite

   KC_PORT=8080
   KC_USERNAME=admin
   KC_PASSWORD=admin
   KC_POSTGRES_DB=keycloak
   KC_POSTGRES_USER=keycloak
   KC_POSTGRES_PASSWORD=keycloak
   KEYCLOAK_FRONTEND_CLIENT_ID=frontend-client

   ISSUER_URI=http://localhost:8080/realms/development
   FILE_STORAGE_PATH=./.uploads
   JPA_HIBERNATE_DDL_AUTO=create-drop
   ```

3. Start PostgreSQL and Keycloak. The Keycloak container imports
   `import/development-realm.json`, including the `frontend-client` client:

   ```bash
   docker compose up -d
   ```

4. Load the same local configuration into the shell and start the backend:

   ```bash
   set -a
   . ./.env
   set +a
   ./mvnw spring-boot:run
   ```

   This explicitly supplies the repository `.env` values when Maven is run
   from the repository root; `application.yaml` currently imports
   `../.env` as its optional file-based configuration location.

5. Run the CloudLite frontend or another OAuth2 client against the imported
   Keycloak realm. Protected API requests must include the issued bearer access
   token.

Development note: `JPA_HIBERNATE_DDL_AUTO=create-drop` recreates the application
schema when the backend starts, so local metadata is not retained between runs.

## Authentication

In development, CloudLite uses Keycloak and OAuth2/OpenID Connect:

- The frontend authenticates with the public `frontend-client` client.
- Keycloak issues a JWT access token from the `development` realm.
- The backend validates the bearer JWT through its configured issuer URI.
- `/api/folders/**`, `/api/upload/**`, and `/api/changelogs/**` require an
  authenticated token.

## Commands

```bash
docker compose up -d      # Start PostgreSQL and Keycloak
set -a; . ./.env; set +a  # Load backend environment configuration
./mvnw spring-boot:run    # Run the backend at http://localhost:8000
./mvnw test               # Run the backend tests
docker compose down       # Stop local infrastructure
```
