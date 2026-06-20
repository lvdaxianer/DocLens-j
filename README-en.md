# DocLens Java

[中文](README.md)

DocLens Java is an OCR SDK and HTTP service built with Java 21, Spring Boot 3,
and DDD boundaries. It can be embedded into a host Spring Boot application as a
Starter, or deployed as a standalone service that exposes `/api/v1/**` OCR batch
processing APIs.

## Use Cases

- Upload PDF, Word, image, TIFF, Markdown, or TXT files in batches and produce
  unified text results.
- Embed OCR processing into an existing Java or Spring Boot application.
- Deploy DocLens as an independent HTTP service for business systems or
  OpenWebUI.
- Manage multiple OCR nodes, routing policy, health state, and LLM Markdown
  post-processing configuration.

## Modules

| Module | Responsibility | Spring Web Dependency |
| --- | --- | --- |
| `doclens-api` | Public SDK contract: `DocLensEngine`, DTOs, event SPI, adapter capability model | No |
| `doclens-core` | DDD domain model, use cases, query service, default engine implementation | No |
| `doclens-spring-boot-starter` | Spring Boot auto-configuration, MyBatis-Plus repositories, Flyway, transaction, local storage, default OCR adapter | No |
| `doclens-server` | HTTP application, REST controllers, Actuator, and static dashboard entry | Yes |

## Quick Start

Start the HTTP service:

```bash
mvn -pl doclens-server spring-boot:run
```

Recommended local development workflow:

```bash
./scripts/dev-up.sh
./scripts/dev-status.sh
./scripts/dev-restart.sh
./scripts/dev-down.sh
```

- Dashboard: `http://127.0.0.1:10002/dashboard/`
- Backend: `http://127.0.0.1:10003`
- Actuator health: `http://127.0.0.1:10003/actuator/health`
- DocLens API health: `http://127.0.0.1:10003/api/v1/health`

Create an OCR batch:

```bash
curl -X POST http://localhost:10003/api/v1/batches \
  -F 'files=@demo.pdf' \
  -F 'metadata={"bizId":"A-1001","source":"curl"}' \
  -F 'idempotency_key=idem-demo-001' \
  -F 'pdf_mode=page_image_fallback'
```

## Documentation

- [HTTP API Reference](docs/api.md): batches, documents, results, events,
  dashboard, OCR nodes, LLM Markdown, and OpenWebUI integration endpoints.
- [SDK Usage](docs/sdk.md): embedded Spring Boot Starter usage and pure Java SDK
  boundaries.
- [Configuration](docs/configuration.md): `doclens.*` properties, secret
  environment variables, OCR/LLM/document processing settings.
- [Local Development](docs/development.md): development scripts, ports, logs,
  test commands, and build commands.
- [Technical Delivery](docs/technical-delivery.md): architecture, concurrency,
  high availability, load balancing, recovery, and technical trade-offs.
- [Packaging Guide](docs/packaging_en.md): Maven packaging for SDK, Starter, and
  HTTP service artifacts.
- [OpenWebUI OCR Contract](docs/integrations/open-webui-ocr-contract.md):
  OpenWebUI-specific endpoints, authorization, and metadata conventions.

## Document Pipeline

- Markdown/TXT: read text directly.
- Image/TIFF: run OCR on the image.
- PDF: render pages as images, OCR them concurrently, and merge by page order.
- Word: convert to PDF with LibreOffice, then reuse the PDF flow.
- LLM Markdown: optional post-processing; when no LLM is configured, DocLens
  still returns the merged OCR text.

## Tech Stack

- Java 21
- Spring Boot 3.5.x
- Maven multi-module build
- MyBatis-Plus
- Flyway
- H2 for local/test runtime
- PostgreSQL runtime dependency
- Vue 3, TypeScript, Vite, Naive UI, and ECharts dashboard

## Verification

```bash
mvn test -q
./scripts/test-dev-scripts.sh
```
