# AGENTS.md

## Scope and current state
- This repository is an early-stage Spring Boot backend skeleton; only bootstrap + dev security are implemented in code.
- Do not assume service/repository/controller layers exist yet; planned architecture is documented separately in `docs/CloudLite-Requirements-&-Project-Description.txt`.
- Existing AI convention files (`.github/copilot-instructions.md`, `CLAUDE.md`, etc.) were not found in this repo at scan time.

## Architecture map (what is actually wired)
- App entrypoint: `src/main/java/de/lind3/CloudLite/CloudLiteApplication.java`.
- Active profile defaults to `development` via `src/main/resources/application.yaml`.
- Security for `development`: `src/main/java/de/lind3/CloudLite/security/SecurityConfigDev.java`.
- OAuth2 resource server JWT issuer is read from `spring.security.oauth2.resourceserver.jwt.issuer-uri` and decoded via `JwtDecoders.fromIssuerLocation(...)`.
- Authorization rules currently permit most HTTP methods globally; only `/api/files/**` is explicitly authenticated.

## Runtime dependencies and integration points
- Postgres + Keycloak are local-dev dependencies via `docker-compose.yaml`.
- Compose starts three services: `postgres`, `postgres_keycloak`, `keycloak` (realm import enabled).
- Keycloak realm bootstrap source: `import/development-realm.json` (realm `development`, client `frontend-client`).
- Spring loads env values from `.env` using `spring.config.import: optional:file:../.env[.properties]`.
- Storage base path is configurable with `FILE_STORAGE_PATH` (default `./uploads`) in `application.yaml`.

## Developer workflows (project-specific)
- Start infra first (DB + IdP): `docker compose up -d` in repo root.
- Run backend with Maven Wrapper: `./mvnw spring-boot:run`.
- Run tests: `./mvnw test` (currently only context bootstrap test).
- Default local ports from `.env`: backend `8000`, Postgres `5432`, Keycloak `8080`.
- If auth fails locally, verify Keycloak realm import and `ISSUER_URI`/`KC_PORT` alignment in `.env` + `application.yaml`.

## Code and change conventions for agents
- Keep package base `de.lind3.CloudLite` and place new features under this namespace.
- Treat `docs/CloudLite-Requirements-&-Project-Description.txt` as target design intent, not implemented behavior.
- When adding endpoints, explicitly revisit `SecurityConfigDev` matcher order; broad `permitAll` matchers can shadow stricter rules.
- Prefer env-driven config keys already present in `.env`/`application.yaml` instead of introducing hardcoded values.
- Keep local-only artifacts out of commits (`.env`, `.psql/`, `.psql_kc/`, `export/`, `.uploads/`, `target/`; see `.gitignore`).

## Quick file index
- `pom.xml`
- `docker-compose.yaml`
- `src/main/resources/application.yaml`
- `src/main/java/de/lind3/CloudLite/CloudLiteApplication.java`
- `src/main/java/de/lind3/CloudLite/security/SecurityConfigDev.java`
- `src/test/java/de/lind3/CloudLite/CloudLiteApplicationTests.java`
- `docs/CloudLite-Requirements-&-Project-Description.txt`
- `import/development-realm.json`
- `.env`

