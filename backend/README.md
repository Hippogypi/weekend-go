# weekend-go backend

Spring Boot backend service for weekend-go.

## Requirements

- JDK 17 or newer
- Maven is optional when using `mvnw` / `mvnw.cmd`; the wrapper script downloads Maven 3.9.9 when no local `mvn` is found.

## Local Configuration

Default configuration lives in `src/main/resources/application.yml`.

Copy `src/main/resources/application-local.example.yml` to `src/main/resources/application-local.yml` for local-only overrides. Do not commit real database passwords or API keys.

Useful environment variables:

- `AMAP_API_KEY`
- `DB_USERNAME`
- `DB_PASSWORD`

## Commands

On Windows PowerShell:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

On Unix-like shells:

```sh
./mvnw test
./mvnw spring-boot:run
```

Health check:

```text
GET /api/health
```
