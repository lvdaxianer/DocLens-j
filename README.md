# DocLens Java

[English](README_EN.md)

DocLens Java 是一个基于 Java 21、Spring Boot 3 和 DDD 边界设计的 OCR 服务与 SDK 工程。它同时支持两种交付形态：作为 Jar/Starter 嵌入到宿主应用，也可以作为独立 HTTP 服务部署。

## 项目定位

DocLens Java 目标是把 OCR 批处理能力拆成清晰的核心引擎、基础设施适配和 HTTP 入口：

- 嵌入式 SDK：业务系统引入 `doclens-spring-boot-starter` 后直接注入 `DocLensEngine`。
- HTTP 服务：部署 `doclens-server`，通过 `/api/v1/**` 接口提交与查询 OCR 任务。
- DDD 核心：`doclens-core` 不依赖 Spring Web、Servlet、MyBatis 或 MyBatis-Plus。
- 高可用演进：当前保留批处理、任务锁、回调任务、状态字段等 HA 演进基础，后续可拆 Worker/Callback Worker。

## 模块结构

| 模块 | 职责 | 是否依赖 Spring Web |
| --- | --- | --- |
| `doclens-api` | 对外 SDK 契约：`DocLensEngine`、DTO、事件 SPI、适配器能力模型 | 否 |
| `doclens-core` | DDD 领域模型、用例、查询服务、默认引擎实现 | 否 |
| `doclens-spring-boot-starter` | Spring Boot 自动装配、MyBatis-Plus 仓储、Flyway、事务、本地存储、默认 OCR 适配器 | 否 |
| `doclens-server` | HTTP 启动类与 REST Controller | 是 |

## 技术栈

- Java 21
- Spring Boot 3.5.x
- Maven 多模块
- MyBatis-Plus
- Flyway
- H2 本地/测试数据库
- PostgreSQL 运行时依赖
- Spring Boot Actuator

## 快速开始：HTTP 服务

```bash
mvn -pl doclens-server spring-boot:run
```

推荐开发方式：

```bash
./scripts/dev-up.sh
./scripts/dev-status.sh
./scripts/dev-down.sh
```

- 前端开发地址：`http://127.0.0.1:10002/dashboard/`
- 后端健康检查：`http://127.0.0.1:10003/actuator/health`
- 后端会监听 `doclens-server`、`doclens-core`、`doclens-api`、`doclens-spring-boot-starter` 的 `src/main` 与相关 `pom.xml` 变化，变更后自动重新打包并重启
- 运行日志输出到 `var/dev/frontend.log` 与 `var/dev/backend.log`

默认配置使用 H2 文件数据库和本地文件存储：

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./var/db/doclens;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH
doclens:
  storage-root: ./var/storage
  auto-process-on-upload: true
  adapter:
    default-key: paddle_ocr
  paddle-ocr:
    enabled: true
    endpoint: http://127.0.0.1:8080/ocr
    timeout-seconds: 600
    visualize: false
  extraction:
    ocr-concurrency: 1
  pdf-render:
    dpi: 36
    image-format: png
  word-conversion:
    command: /opt/homebrew/bin/soffice
    timeout-seconds: 60
```

本地 PaddleOCR 原生 API 启动方式：

```bash
cd /Users/lvdaxianer/cache/soft/paddleocr-api
./start-native.sh
```

`./start-native.sh` 默认监听 `http://127.0.0.1:8080/ocr`。如果同时启动 DocLens HTTP 服务，请让 DocLens 使用其它端口，例如：

```bash
mvn -pl doclens-server spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

仓库自带开发脚本默认已经把 DocLens 服务固定到 `10003`，避免与本地 OCR 服务的 `8080` 冲突。

文档转文本流水线：

- Markdown/TXT：直接读取文本，不调用 OCR。
- Image/TIFF：调用 OCR 转文字。
- PDF：按页渲染为图片，并发 OCR 后按页码顺序合并文本。
- Word：先通过 LibreOffice 转 PDF，再复用 PDF 流程。
- 最终纯文本会同时保存到数据库和本地对象存储，磁盘文件名格式为 `[文件名称]_[uuid].md`。
- 默认上传后会进入后台单线程处理，创建批次接口先返回任务信息；通过批次、文档和结果查询接口轮询处理状态。

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

## 快速开始：Spring Boot 嵌入式 SDK

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

## 纯 Java SDK 模式

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

## 配置项

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `doclens.storage-root` | `./var/storage` | 本地对象存储目录 |
| `doclens.auto-process-on-upload` | `true` | 上传后是否调度后台进程内处理 |
| `doclens.worker-id` | `local-worker` | 本地 Worker 标识 |
| `doclens.adapter.default-key` | `paddle_ocr` | 默认 OCR 适配器键 |
| `doclens.paddle-ocr.enabled` | `true` | 是否启用 PaddleOCR 原生适配器 |
| `doclens.paddle-ocr.endpoint` | `http://127.0.0.1:8080/ocr` | PaddleOCR 原生 API 地址 |
| `doclens.paddle-ocr.timeout-seconds` | `600` | PaddleOCR 请求超时 |
| `doclens.extraction.ocr-concurrency` | `1` | PDF/Word 多页 OCR 并发数 |
| `doclens.pdf-render.dpi` | `36` | PDF 转图片渲染 DPI |
| `doclens.word-conversion.command` | `/opt/homebrew/bin/soffice` | Word 转 PDF 命令 |
| `doclens.callback.max-retries` | `3` | 回调最大重试次数 |
| `doclens.callback.timeout-seconds` | `10` | 回调超时时间 |

## 开发与测试

```bash
mvn test -q
```

项目包含以下关键测试：

- HTTP 合约测试：验证上传、查询、结果、事件、适配器和健康接口。
- Starter 嵌入式测试：验证宿主 Spring Boot 应用可以注入并调用 `DocLensEngine`。
- Core 架构边界测试：验证 `doclens-core` 不依赖 Spring/Web/MyBatis。
- HTTP 边界测试：验证 Controller 不直接依赖内部用例、领域对象或仓储。

## 打包

打包 SDK Starter：

```bash
mvn -pl doclens-spring-boot-starter -am clean package
```

打包 HTTP 服务：

```bash
mvn -pl doclens-server -am clean package
```

SDK 相关模块会通过 Maven Enforcer 禁止引入 Spring Web 相关依赖。更多说明见 [打包指南](docs/packaging.md)。

## 当前限制

- 自动化测试使用 Stub 适配器，服务端本地默认使用 PaddleOCR 原生 API。
- 当前 Worker 是进程内执行，独立 Worker/Callback Worker 尚未拆分。
- 默认存储为本地文件系统，S3/MinIO 等对象存储适配可通过 `ObjectStorage` 扩展。
- MyBatis-Plus 在测试启动时可能输出默认包扫描 WARN，但现有测试已验证仓储与接口可正常工作。

## 更多文档

- [SDK and HTTP Delivery](docs/sdk-http-delivery.md)
- [打包指南](docs/packaging.md)
- [DDD HA Implementation Plan](docs/superpowers/plans/2026-06-07-doclens-java-ddd-ha-implementation.md)
