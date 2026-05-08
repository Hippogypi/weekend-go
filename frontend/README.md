# weekend-go frontend

Vue frontend for weekend-go.

## Requirements

- Node.js 20 or newer is recommended.
- npm is used by the current lockfile.

## Local Configuration

Copy `.env.example` to `.env.local` for local-only overrides. Do not commit real API keys.

Useful variables:

- `VITE_API_BASE_URL`: backend API base URL, for example `http://127.0.0.1:8080`.
- `VITE_AMAP_JS_API_KEY`: Amap `Web端(JS API)` key.
- `VITE_AMAP_SECURITY_JS_CODE`: Amap JS API security code.

For local Amap testing, open the frontend with `127.0.0.1`:

```text
http://127.0.0.1:5173
```

If the Amap key whitelist only contains `127.0.0.1`, using `localhost` may fail whitelist validation.

## Commands

```powershell
npm install
npm run dev
npm run test
npm run build
```
