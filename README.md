# DocLens Java

DocLens Java 是一个基于 Java 21、Spring Boot 3 和 DDD 边界设计的 OCR 服务与 SDK 工程。它同时支持两种交付形态：作为 Jar/Starter 嵌入到宿主应用，也可以作为独立 HTTP 服务部署。

DocLens Java is an OCR SDK and service built with Java 21, Spring Boot 3, and DDD boundaries. It supports two delivery modes: embedded Jar/Starter integration and standalone HTTP service deployment.

## 中文说明

### 项目定位

DocLens Java 目标是把 OCR 批处理能力拆成清晰的核心引擎、基础设施适配和 HTTP 入口：

- 嵌入式 SDK：业务系统引入 `doclens-spring-boot-starter` 后直接注入 `DocLensEngine`。
- HTTP 服务：部署 `doclens-server`，通过 `/api/v1/**` 接口提交与查询 OCR 任务。
- DDD 核心：`doclens-core` 不依赖 Spring Web、Servlet、MyBatis 或 MyBatis-Plus。
- 高可用演进：当前保留批处理、任务锁、回调任务、状态字段等 HA 演进基础，后续可拆 Worker/Callback Worker。

### 模块结构

| 模块 | 职责 | 是否依赖 Spring Web |
| --- | --- | --- |
| `doclens-api` | 对外 SDK 契约：`DocLensEngine`、DTO、事件 SPI、适配器能力模型 | 否 |
| `doclens-core` | DDD 领域模型、用例、查询服务、默认引擎实现 | 否 |
| `doclens-spring-boot-starter` | Spring Boot 自动装配、MyBatis-Plus 仓储、Flyway、事务、本地存储、默认 OCR 适配器 | 否 |
| `doclens-server` | HTTP 启动类与 REST Controller | 是 |

### 技术栈

- Java 21
- Spring Boot 3.5.x
- Maven 多模块
- MyBatis-Plus
- Flyway
- H2 本地/测试数据库
- PostgreSQL 运行时依赖
- Spring Boot Actuator

### 快速开始：HTTP 服务

```bash
mvn -pl doclens-server spring-boot:run
```

默认配置使用 H2 文件数据库和本地文件存储：

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./var/db/doclens;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH
doclens:
  storage-root: ./var/storage
  auto-process-on-upload: true
```

提交 OCR 批次：

```bash
curl -X POST http://localhost:8080/api/v1/batches \
  -F 'files=@demo.pdf' \
  -F 'metadata={"bizId":"A-1001","source":"curl"}' \
  -F 'idempotency_key=idem-demo-001' \
  -F 'pdf_mode=page_image_fallback'
```

常用 HTTP 接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/v1/batches` | 创建 OCR 批次 |
| `GET` | `/api/v1/batches/{batchId}` | 查询批次状态 |
| `GET` | `/api/v1/documents/{documentId}` | 查询文档状态 |
| `GET` | `/api/v1/documents/{documentId}/result` | 查询 OCR 结果 |
| `GET` | `/api/v1/batches/{batchId}/events` | 查询批次事件 |
| `GET` | `/api/v1/adapters` | 查询 OCR 适配器能力 |
| `GET` | `/api/v1/health` | 服务健康检查 |

### 快速开始：Spring Boot 嵌入式 SDK

宿主应用引入 starter：

```xml
<dependency>
    <groupId>io.github.lvdaxianer</groupId>
    <artifactId>doclens-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

注入并调用统一引擎：

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

宿主应用可以通过自定义 Spring Bean 替换默认实现，例如 `OcrAdapter`、`ObjectStorage`、`DocLensEventSink` 或 `TransactionRunner`。

### 纯 Java SDK 模式

如果宿主应用不使用 Spring Boot 自动装配，可以只引入：

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

这种模式下，宿主需要自行提供仓储、存储、OCR 适配器、事务、事件 Sink 等实现。

### 配置项

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `doclens.storage-root` | `./var/storage` | 本地对象存储目录 |
| `doclens.auto-process-on-upload` | `true` | 上传后是否立即在进程内处理 |
| `doclens.worker-id` | `local-worker` | 本地 Worker 标识 |
| `doclens.callback.max-retries` | `3` | 回调最大重试次数 |
| `doclens.callback.timeout-seconds` | `10` | 回调超时时间 |

### 开发与测试

```bash
mvn test -q
```

项目包含以下关键测试：

- HTTP 合约测试：验证上传、查询、结果、事件、适配器和健康接口。
- Starter 嵌入式测试：验证宿主 Spring Boot 应用可以注入并调用 `DocLensEngine`。
- Core 架构边界测试：验证 `doclens-core` 不依赖 Spring/Web/MyBatis。
- HTTP 边界测试：验证 Controller 不直接依赖内部用例、领域对象或仓储。

### 当前限制

- 默认 OCR 适配器仍是 Stub 实现，用于打通垂直切片。
- 当前 Worker 是进程内执行，独立 Worker/Callback Worker 尚未拆分。
- 默认存储为本地文件系统，S3/MinIO 等对象存储适配可通过 `ObjectStorage` 扩展。
- MyBatis-Plus 在测试启动时可能输出默认包扫描 WARN，但现有测试已验证仓储与接口可正常工作。

### 更多文档

- [SDK and HTTP Delivery](docs/sdk-http-delivery.md)
- [DDD HA Implementation Plan](docs/superpowers/plans/2026-06-07-doclens-java-ddd-ha-implementation.md)

---

## English

### What Is DocLens Java?

DocLens Java separates OCR batch processing into a stable SDK contract, a framework-neutral core, Spring Boot infrastructure adapters, and an optional HTTP server.

- Embedded SDK: add `doclens-spring-boot-starter` and inject `DocLensEngine`.
- HTTP service: run `doclens-server` and use `/api/v1/**` endpoints.
- DDD core: `doclens-core` does not depend on Spring Web, Servlet, MyBatis, or MyBatis-Plus.
- HA-ready evolution: schema and domain fields keep room for worker locking, callback jobs, retries, and future process split.

### Modules

| Module | Responsibility | Depends on Spring Web |
| --- | --- | --- |
| `doclens-api` | Public SDK contract: `DocLensEngine`, DTOs, event SPI, adapter capability model | No |
| `doclens-core` | DDD domain model, use cases, query service, default engine implementation | No |
| `doclens-spring-boot-starter` | Spring Boot auto-configuration, MyBatis-Plus repositories, Flyway, transaction, local storage, default OCR adapter | No |
| `doclens-server` | HTTP application and REST controllers | Yes |

### Tech Stack

- Java 21
- Spring Boot 3.5.x
- Maven multi-module build
- MyBatis-Plus
- Flyway
- H2 for local/test runtime
- PostgreSQL runtime dependency
- Spring Boot Actuator

### Quick Start: HTTP Service

```bash
mvn -pl doclens-server spring-boot:run
```

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

### Quick Start: Embedded Spring Boot SDK

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

### Pure Java SDK Mode

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

### Configuration

| Property | Default | Description |
| --- | --- | --- |
| `doclens.storage-root` | `./var/storage` | Local object storage root |
| `doclens.auto-process-on-upload` | `true` | Process uploaded batches in-process |
| `doclens.worker-id` | `local-worker` | Local worker identifier |
| `doclens.callback.max-retries` | `3` | Maximum callback retries |
| `doclens.callback.timeout-seconds` | `10` | Callback timeout in seconds |

### Development

Run all tests:

```bash
mvn test -q
```

Important test coverage:

- HTTP contract tests for upload, query, result, events, adapters, and health endpoints.
- Embedded starter test proving a host Spring Boot app can inject and call `DocLensEngine`.
- Core architecture test preventing Spring/Web/MyBatis dependencies from entering `doclens-core`.
- HTTP boundary test preventing controllers from depending directly on internal use cases, domains, or repositories.

### Current Limitations

- The default OCR adapter is a Stub implementation for vertical-slice verification.
- Worker execution is currently in-process; standalone Worker and Callback Worker processes are future work.
- Local filesystem storage is the default; S3/MinIO can be added through `ObjectStorage`.
- MyBatis-Plus may log a default mapper-scan warning during tests, while repository and API contract tests still pass.

### More Documentation

- [SDK and HTTP Delivery](docs/sdk-http-delivery.md)
- [DDD HA Implementation Plan](docs/superpowers/plans/2026-06-07-doclens-java-ddd-ha-implementation.md)
