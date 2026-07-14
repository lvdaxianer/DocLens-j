# Local Development

This document covers the local development workflow for DocLens Java and the
Vue dashboard.

## Prerequisites

- Java 21
- Maven
- Node.js and npm for `doclens-dashboard`
- Docker or Docker Compose for the local PostgreSQL service
- Optional LibreOffice command for Word conversion
- Optional PaddleOCR, Ollama, or online OCR service for end-to-end OCR testing

## Start And Stop Services

Recommended commands from the repository root:

```bash
./scripts/dev-up.sh
./scripts/dev-status.sh
./scripts/dev-restart.sh
./scripts/dev-down.sh
```

The helper scripts start PostgreSQL, the dashboard, and the backend. They write
PID files under `var/dev`, keep logs in the same directory, and use the local
PostgreSQL datasource by default.

| Service | URL | Log |
| --- | --- | --- |
| PostgreSQL | `127.0.0.1:15432` | Docker Compose logs |
| Dashboard | `http://127.0.0.1:10002/dashboard/` | `var/dev/frontend.log` |
| Backend | `http://127.0.0.1:10003` | `var/dev/backend.log` |

The dashboard proxies `/api` requests to the backend at
`http://127.0.0.1:10003`.

## Run Backend Only

```bash
mvn -pl doclens-server spring-boot:run
```

If you run the backend without `./scripts/dev-up.sh`, start PostgreSQL first:

```bash
docker compose up -d postgres
```

## Run Dashboard Only

```bash
cd doclens-dashboard
npm install
npm run dev
```

## Testing

Run all Maven tests:

```bash
mvn test -q
```

The Java tests use PostgreSQL Testcontainers, so Docker or a compatible
container runtime must be available.

Run development-script and documentation checks:

```bash
./scripts/test-dev-scripts.sh
```

Run dashboard tests when working in the frontend:

```bash
cd doclens-dashboard
npm test
```

## Build

Package the HTTP service:

```bash
mvn -pl doclens-server -am clean package
```

Package the Spring Boot Starter and dependencies:

```bash
mvn -pl doclens-spring-boot-starter -am clean package
```

See [Packaging Guide](packaging.md) for the full artifact list.

## Useful Local OCR Notes

For a local PaddleOCR native API, start the OCR service first and point DocLens
to its endpoint:

```bash
export DOCLENS_PADDLE_OCR_ENDPOINT=http://127.0.0.1:8080/ocr
mvn -pl doclens-server spring-boot:run
```

Ollama OCR nodes use the `/api/generate` protocol. Prefer `png`, `jpg`, or
`jpeg` input, send raw base64 without a data URL prefix, and keep a generous
timeout for CPU-based OCR models.
