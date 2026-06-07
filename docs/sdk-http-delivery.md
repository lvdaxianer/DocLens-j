# DocLens SDK and HTTP Delivery

DocLens Java supports two delivery modes that share the same core engine.

## Module Boundary

| Module | Purpose | Depends on Spring Web |
| --- | --- | --- |
| `doclens-api` | Public SDK contract: `DocLensEngine`, DTOs, event SPI | No |
| `doclens-core` | DDD domain, application use cases, query service, engine implementation | No |
| `doclens-spring-boot-starter` | Spring Boot auto-configuration, MyBatis-Plus repositories, Flyway, storage | No |
| `doclens-server` | HTTP application and REST controllers | Yes |

## Embedded Spring Boot Usage

Add the starter in the host application:

```xml
<dependency>
    <groupId>io.github.lvdaxianer</groupId>
    <artifactId>doclens-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Inject and call the engine:

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

The host application may replace default beans such as `OcrAdapter`,
`ObjectStorage`, `DocLensEventSink`, or `TransactionRunner` by defining its own
Spring beans.

## Pure Java SDK Usage

Use `doclens-api` and `doclens-core` when the host does not want Spring Boot
auto-configuration. In this mode the host must provide repository, storage,
OCR adapter, transaction, and event sink implementations explicitly.

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

## HTTP Service Usage

Run `doclens-server` when DocLens should be deployed as an independent HTTP
service. REST controllers are thin adapters and call `DocLensEngine`; business
logic remains in `doclens-core`.

```bash
mvn -pl doclens-server spring-boot:run
```

Existing HTTP endpoints remain under `/api/v1/**`.

## Boundary Rules

- `doclens-api` must stay stable and framework-neutral.
- `doclens-core` must not import Spring, Servlet/Jakarta Web, MyBatis, or MyBatis-Plus.
- `doclens-spring-boot-starter` owns infrastructure adapters and auto-configuration.
- `doclens-server` must not call use cases or repositories directly from controllers.
