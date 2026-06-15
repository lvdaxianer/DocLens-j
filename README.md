# DocLens Java

[English](README-en.md)

DocLens Java 是一个基于 Java 21、Spring Boot 3 和 DDD 边界设计的 OCR
SDK 与 HTTP 服务。它既可以作为 Spring Boot Starter 嵌入宿主系统，也可以
作为独立服务通过 `/api/v1/**` 提供 OCR 批处理能力。

## 适用场景

- 批量上传 PDF、Word、图片、TIFF、Markdown 或 TXT，并统一产出文本结果。
- 将 OCR 能力嵌入现有 Java/Spring Boot 应用。
- 独立部署 OCR 服务，再通过 HTTP API 被业务系统或 OpenWebUI 调用。
- 管理多个 OCR 节点、路由策略、健康状态和 LLM Markdown 后处理配置。

## 模块结构

| 模块 | 职责 | Spring Web 依赖 |
| --- | --- | --- |
| `doclens-api` | 对外 SDK 契约：`DocLensEngine`、DTO、事件 SPI、适配器能力模型 | 否 |
| `doclens-core` | DDD 领域模型、用例、查询服务、默认引擎实现 | 否 |
| `doclens-spring-boot-starter` | Spring Boot 自动装配、MyBatis-Plus 仓储、Flyway、事务、本地存储、默认 OCR 适配器 | 否 |
| `doclens-server` | HTTP 启动类、REST Controller、Actuator 与静态控制台入口 | 是 |

## 快速开始

启动 HTTP 服务：

```bash
mvn -pl doclens-server spring-boot:run
```

推荐的本地开发启动方式：

```bash
./scripts/dev-up.sh
./scripts/dev-status.sh
./scripts/dev-restart.sh
./scripts/dev-down.sh
```

- 控制台地址：`http://127.0.0.1:10002/dashboard/`
- 后端地址：`http://127.0.0.1:10003`
- Actuator 健康检查：`http://127.0.0.1:10003/actuator/health`
- DocLens API 健康检查：`http://127.0.0.1:10003/api/v1/health`

提交一个 OCR 批次：

```bash
curl -X POST http://localhost:10003/api/v1/batches \
  -F 'files=@demo.pdf' \
  -F 'metadata={"bizId":"A-1001","source":"curl"}' \
  -F 'idempotency_key=idem-demo-001' \
  -F 'pdf_mode=page_image_fallback'
```

## 文档导航

- [HTTP API 参考](docs/api.md)：批次、文档、结果、事件、Dashboard、OCR 节点、LLM Markdown 与 OpenWebUI 集成接口。
- [SDK 使用](docs/sdk.md)：Spring Boot Starter 嵌入式接入与纯 Java SDK 边界。
- [配置说明](docs/configuration.md)：`doclens.*` 配置项、密钥环境变量、OCR/LLM/文档处理参数。
- [本地开发](docs/development.md)：开发脚本、端口、日志、测试和构建命令。
- [打包指南](docs/packaging.md)：SDK、Starter 和 HTTP 服务的 Maven 打包方式。
- [OpenWebUI OCR 契约](docs/integrations/open-webui-ocr-contract.md)：OpenWebUI 专用接口、鉴权与 metadata 约定。

## 文档处理流水线

- Markdown/TXT：直接读取文本，不调用 OCR。
- Image/TIFF：调用 OCR 转文字。
- PDF：按页渲染为图片，并发 OCR 后按页码顺序合并文本。
- Word：先通过 LibreOffice 转 PDF，再复用 PDF 流程。
- LLM Markdown：可选后处理；未配置 LLM 时主流程仍返回 OCR 合并文本。

## 技术栈

- Java 21
- Spring Boot 3.5.x
- Maven 多模块
- MyBatis-Plus
- Flyway
- H2 本地/测试数据库
- PostgreSQL 运行时依赖
- Vue 3、TypeScript、Vite、Naive UI、ECharts 控制台

## 验证

```bash
mvn test -q
./scripts/test-dev-scripts.sh
```
