# SDK Usage

DocLens Java supports embedded Spring Boot usage and lower-level pure Java
usage. Both modes share the same core engine contracts from `doclens-api`.

## Module Boundary

| Module | Purpose | Spring Web Dependency |
| --- | --- | --- |
| `doclens-api` | Public SDK contract: `DocLensEngine`, DTOs, event SPI | No |
| `doclens-core` | DDD domain, application use cases, query service, engine implementation | No |
| `doclens-spring-boot-starter` | Spring Boot auto-configuration, MyBatis-Plus repositories, Flyway, storage | No |
| `doclens-server` | HTTP application and REST controllers | Yes |

## Embedded Spring Boot Starter

Add the starter in the host application:

```xml
<dependency>
    <groupId>io.github.lvdaxianer</groupId>
    <artifactId>doclens-spring-boot-starter</artifactId>
    <version>0.1.0-beta.1</version>
</dependency>
```

Inject and call `DocLensEngine`:

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

Host applications can replace default infrastructure by defining their own
Spring beans for extension points such as:

- `OcrAdapter`
- `ObjectStorage`
- `DocLensEventSink`
- `TransactionRunner`

## Pure Java SDK Mode

Use `doclens-api` and `doclens-core` when Spring Boot auto-configuration is not
desired:

```xml
<dependency>
    <groupId>io.github.lvdaxianer</groupId>
    <artifactId>doclens-api</artifactId>
    <version>0.1.0-beta.1</version>
</dependency>
<dependency>
    <groupId>io.github.lvdaxianer</groupId>
    <artifactId>doclens-core</artifactId>
    <version>0.1.0-beta.1</version>
</dependency>
```

In this mode, the host application must provide repositories, storage, OCR
adapter, transaction, event sink, and other infrastructure dependencies
explicitly.

## Boundary Rules

- `doclens-api` stays stable and framework-neutral.
- `doclens-core` does not import Spring Web, Servlet/Jakarta Web, MyBatis, or
  MyBatis-Plus.
- `doclens-spring-boot-starter` owns infrastructure adapters and
  auto-configuration.
- `doclens-server` owns HTTP controllers and should keep business logic in the
  engine/core layers.

## Related Docs

- [HTTP API Reference](api.md)
- [Configuration](configuration.md)
- [Packaging Guide](packaging.md)
