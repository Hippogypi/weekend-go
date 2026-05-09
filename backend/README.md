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

### Local MySQL

For a real local MySQL connection, create `src/main/resources/application-local.yml` from the example file. This file is ignored by Git and must not contain committed credentials.

```powershell
Copy-Item src/main/resources/application-local.example.yml src/main/resources/application-local.yml
$env:DB_USERNAME="weekend_go"
$env:DB_PASSWORD="<local-password>"
```

Set `DB_USERNAME` and `DB_PASSWORD` in every shell that starts the backend or runs local-profile tests. If `DB_PASSWORD` is not set, the example fallback is `change-me`, so MySQL authentication is expected to fail unless that is the real local password.

With the local profile enabled, the backend connects to `jdbc:mysql://localhost:3306/weekend_go` and uses the JDBC repositories instead of the in-memory fallback.

Smoke verification used for `local-database-setup`:

```powershell
$env:DB_USERNAME="weekend_go"
$env:DB_PASSWORD="<local-password>"
.\mvnw.cmd "-Dtest=AuthControllerTest" "-Dspring.profiles.active=local" test
```

Expected result: `AuthControllerTest` starts with profile `local`, Hikari opens a MySQL Connector/J connection, and the register/login/auth tests pass.

### Amap Web Service

The backend uses the Amap `Web服务` key for server-side REST API calls, such as geocoding, reverse geocoding, POI search, and administrative district lookup.

Local options:

1. Put the key in `src/main/resources/application-local.yml`:

```yml
weekend-go:
  amap:
    api-key: your-amap-web-service-key
```

2. Or set the environment variable:

```powershell
$env:AMAP_API_KEY="your-amap-web-service-key"
```

When using `application-local.yml`, start the app with the `local` profile:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

The Amap `Web服务` key checks the public outbound IP, not `127.0.0.1`. If Amap returns `INVALID_USER_IP`, update the IP whitelist in the Amap console to the current public outbound IP.

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
