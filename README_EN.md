# DocLens Java

[中文](README.md)

DocLens Java is an OCR SDK and service built with Java 21, Spring Boot 3, and DDD boundaries. It supports two delivery modes: embedded Jar/Starter integration and standalone HTTP service deployment.

## What Is DocLens Java?

DocLens Java separates OCR batch processing into a stable SDK contract, a framework-neutral core, Spring Boot infrastructure adapters, and an optional HTTP server.

- Embedded SDK: add `doclens-spring-boot-starter` and inject `DocLensEngine`.
- HTTP service: run `doclens-server` and use `/api/v1/**` endpoints.
- DDD core: `doclens-core` does not depend on Spring Web, Servlet, MyBatis, or MyBatis-Plus.
- HA-ready evolution: schema and domain fields keep room for worker locking, callback jobs, retries, and future process split.

## Modules

| Module | Responsibility | Depends on Spring Web |
| --- | --- | --- |
| `doclens-api` | Public SDK contract: `DocLensEngine`, DTOs, event SPI, adapter capability model | No |
| `doclens-core` | DDD domain model, use cases, query service, default engine implementation | No |
| `doclens-spring-boot-starter` | Spring Boot auto-configuration, MyBatis-Plus repositories, Flyway, transaction, local storage, default OCR adapter | No |
| `doclens-server` | HTTP application and REST controllers | Yes |

## Tech Stack

- Java 21
- Spring Boot 3.5.x
- Maven multi-module build
- MyBatis-Plus
- Flyway
- H2 for local/test runtime
- PostgreSQL runtime dependency
- Spring Boot Actuator

## Quick Start: HTTP Service

```bash
mvn -pl doclens-server spring-boot:run
```

For local PaddleOCR native API integration, start PaddleOCR first:

```bash
cd /Users/lvdaxianer/cache/soft/paddleocr-api
./start-native.sh
```

The PaddleOCR native service listens on `http://127.0.0.1:8080/ocr`. Run DocLens on another port when both services are local:

```bash
mvn -pl doclens-server spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Document text pipeline:

- Markdown/TXT: read text directly.
- Image/TIFF: OCR the image.
- PDF: render pages to images, OCR concurrently, then merge by page order.
- Word: convert to PDF with LibreOffice, then reuse the PDF flow.
- Final plain text is saved in the database and as `[file-name]_[uuid].md` in local object storage.
- Uploads are scheduled on a background single-thread in-process worker by default; poll batch, document, and result endpoints for completion.

Create a batch:

```bash
curl -X POST http://localhost:8080/api/v1/batches \
  -F 'files=@demo.pdf' \
  -F 'metadata={"bizId":"A-1001","source":"curl"}' \
  -F 'idempotency_key=idem-demo-001' \
  -F 'pdf_mode=page_image_fallback'
```

Main endpoints:

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/batches` | Create an OCR batch |
| `GET` | `/api/v1/batches/{batchId}` | Get batch status |
| `GET` | `/api/v1/documents/{documentId}` | Get document status |
| `GET` | `/api/v1/documents/{documentId}/result` | Get OCR result |
| `GET` | `/api/v1/batches/{batchId}/events` | Get batch events |
| `GET` | `/api/v1/adapters` | List adapter capabilities |
| `GET` | `/api/v1/health` | Health check |

## Quick Start: Embedded Spring Boot SDK

Add the starter:

```xml
<dependency>
    <groupId>io.github.lvdaxianer</groupId>
    <artifactId>doclens-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Inject `DocLensEngine`:

```java
@Service
public class HostOcrService {

    private final DocLensEngine docLensEngine;

    public HostOcrService(DocLensEngine docLensEngine) {
        this.docLensEngine = docLensEngine;
    }

    public Map<String, Object> submit(byte[] content) {
        CreateBatchRequest request = new CreateBatchRequest(
                List.of(new DocumentInput("demo.pdf", content)),
                Map.of("source", "host-app"),
                "",
                "idem-host-001",
                "",
                "page_image_fallback"
        );
        return docLensEngine.createBatch(request);
    }
}
```

Host applications can override defaults by defining Spring beans such as `OcrAdapter`, `ObjectStorage`, `DocLensEventSink`, or `TransactionRunner`.

## Pure Java SDK Mode

Use `doclens-api` and `doclens-core` when Spring Boot auto-configuration is not desired. In this mode the host application must provide repository, storage, OCR adapter, transaction, and event sink implementations.

```xml
<dependency>
    <groupId>io.github.lvdaxianer</groupId>
    <artifactId>doclens-api</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>io.github.lvdaxianer</groupId>
    <artifactId>doclens-core</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Configuration

| Property | Default | Description |
| --- | --- | --- |
| `doclens.storage-root` | `./var/storage` | Local object storage root |
| `doclens.auto-process-on-upload` | `true` | Schedule uploaded batches on the in-process background worker |
| `doclens.worker-id` | `local-worker` | Local worker identifier |
| `doclens.adapter.default-key` | `paddle_ocr` | Default OCR adapter key |
| `doclens.paddle-ocr.enabled` | `true` | Enable PaddleOCR native adapter |
| `doclens.paddle-ocr.endpoint` | `http://127.0.0.1:8080/ocr` | PaddleOCR native API endpoint |
| `doclens.paddle-ocr.timeout-seconds` | `600` | PaddleOCR request timeout |
| `doclens.extraction.ocr-concurrency` | `1` | OCR concurrency for PDF/Word pages |
| `doclens.pdf-render.dpi` | `36` | PDF page render DPI |
| `doclens.word-conversion.command` | `/opt/homebrew/bin/soffice` | Word-to-PDF command |
| `doclens.callback.max-retries` | `3` | Maximum callback retries |
| `doclens.callback.timeout-seconds` | `10` | Callback timeout in seconds |

## Development

Run all tests:

```bash
mvn test -q
```

Important test coverage:

- HTTP contract tests for upload, query, result, events, adapters, and health endpoints.
- Embedded starter test proving a host Spring Boot app can inject and call `DocLensEngine`.
- Core architecture test preventing Spring/Web/MyBatis dependencies from entering `doclens-core`.
- HTTP boundary test preventing controllers from depending directly on internal use cases, domains, or repositories.

## Packaging

Package SDK Starter:

```bash
mvn -pl doclens-spring-boot-starter -am clean package
```

Package HTTP service:

```bash
mvn -pl doclens-server -am clean package
```

SDK-related modules are protected by Maven Enforcer rules that ban Spring Web dependencies. See [Packaging Guide](docs/packaging_en.md) for details.

## Current Limitations

- Automated tests use the Stub adapter; local server defaults to the PaddleOCR native API.
- Worker execution is currently in-process; standalone Worker and Callback Worker processes are future work.
- Local filesystem storage is the default; S3/MinIO can be added through `ObjectStorage`.
- MyBatis-Plus may log a default mapper-scan warning during tests, while repository and API contract tests still pass.

## More Documentation

- [SDK and HTTP Delivery](docs/sdk-http-delivery.md)
- [Packaging Guide](docs/packaging_en.md)
- [DDD HA Implementation Plan](docs/superpowers/plans/2026-06-07-doclens-java-ddd-ha-implementation.md)
