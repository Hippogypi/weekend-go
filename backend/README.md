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
