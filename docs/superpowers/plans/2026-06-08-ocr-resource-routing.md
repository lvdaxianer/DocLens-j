# OCR Resource Routing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a configurable OCR resource layer that supports multiple integrated OCR models, multiple nodes per model, upload-time routing selection, cross-model load balancing, node health checks, isolated thread pools, retry/failover, and Dashboard visibility into per-node image workload.

**Architecture:** Keep OCR vendors behind registered adapters, persist OCR node configuration in the database, maintain an in-memory runtime node pool for fast scheduling, and route each image OCR request through an `OcrRoutingService`. Upload requests can use system default routing, global load balancing across all healthy OCR nodes, model-scoped load balancing, or a specific node. Node health checks, OCR requests, document processing, and callbacks use isolated thread pools.

**Tech Stack:** Java 21, Spring Boot 3.5, Maven, MyBatis-Plus, Flyway, H2, Jackson, Java `HttpClient`, Vue 3, TypeScript, Pinia, Naive UI, Vite, JUnit 5, AssertJ.

---

## Confirmed Requirements

- OCR 类型只能从系统已经对接的 OCR 模型中选择，页面不能新增未知 OCR 类型。
- 当前已对接 OCR 模型为 `paddle_ocr`，未来可以继续接入其他 OCR。
- 每个 OCR 模型可以配置多个节点，每个节点只填写 `host + port`，不能填写完整 URL。
- 每个 OCR 适配器自己决定固定接口路径，例如 PaddleOCR 使用 `/ocr` 和 `/health`。
- 上传页面不再暴露 `OCR 适配器` 字段；该字段属于底层工程兼容/调试开关，不作为用户可选项。
- 上传页面不再暴露 `PDF 模式` 字段；当前用户上传统一按系统处理策略执行，避免展示未产品化或未完整生效的内部处理模式。
- 上传页面的 OCR 路由方式默认使用全局负载均衡，不再展示“系统默认”作为用户可选路由方式。
- 上传解析文件时可以选择解析路由方式：
  - 全局负载均衡：从所有 OCR 模型的健康节点中负载均衡
  - 指定 OCR：从指定 OCR 模型的健康节点池中负载均衡
  - 指定 OCR + 指定节点：固定走某个具体节点
- `DEFAULT` 路由模式保留为后端兼容和系统内部配置语义，不作为 Dashboard 上传表单选项。
- 全局负载均衡必须支持跨不同 OCR 模型、不同节点调度。
- 指定 OCR 后仍然在该 OCR 的健康池中负载均衡。
- 指定 OCR 后也可以进一步指定具体节点。
- 线程池必须隔离：
  - 文档处理线程池
  - OCR 请求线程池
  - OCR 健康检查线程池
  - 回调线程池
- OCR 节点需要健康检查、失败摘除、恢复检测。
- 单个节点失败后可以重试，默认 3 次；失败后按路由模式决定是否切换候选节点。
- 页面要能看到不同节点当前有多少图片正在解析。
- Dashboard 要展示节点健康、实时负载、请求耗时、失败次数和最近错误。

## Routing Semantics

| 路由模式 | 用户选择 | 候选节点范围 | 默认失败后行为 |
| --- | --- | --- | --- |
| `DEFAULT` | 系统默认 | 使用系统默认配置解析为具体候选范围 | 按默认配置 |
| `GLOBAL_LOAD_BALANCE` | 全局负载均衡 | 所有已启用、参与全局负载均衡、健康且能力匹配的 OCR 节点 | 切换其他健康节点 |
| `MODEL_LOAD_BALANCE` | 指定 OCR | 指定 OCR 模型下已启用且健康的节点 | 切换同模型其他健康节点 |
| `SPECIFIC_NODE` | 指定 OCR + 指定节点 | 指定节点 | 默认不切走；可配置是否回退 |

推荐默认策略：

- `doclens.ocr.default-routing-mode=GLOBAL_LOAD_BALANCE`
- `doclens.ocr.load-balance-strategy=least-inflight`
- `doclens.ocr.request-retry-times=3`
- `doclens.ocr.specific-node-fallback-enabled=false`

## Upload Form Product Decisions

- 元数据必须以 JSON 编辑体验呈现，不能只用普通多行文本框；提交前需要校验 JSON 格式。
- 回调地址表示解析完成后的 HTTP POST 目标地址；回调请求 body 固定为 `{ "meta": {}, "text": {}, "idempotency_key": "" }`。
- 回调 body 中 `meta` 使用上传时传入的元数据，`text` 使用解析后的内容，`idempotency_key` 使用上传时传入的幂等键。
- 如果配置了 LLM Markdown 后处理，回调 body 中 `text` 优先使用 LLM 生成的 Markdown；如果未配置 LLM，则使用 OCR 合并后的纯文本。
- 上传表单面向用户只保留业务可理解字段：文件、元数据 JSON、回调地址、幂等键、OCR 路由方式。
- OCR 节点在页面和上传选择中优先展示添加节点时填写的别名/名称，避免把 `ocr_node_...` 这类内部节点 ID 直接作为主要显示文本。
- `adapter_override` 和 `pdf_mode` 可在 API/后端层继续兼容历史请求，但 Dashboard 上传入口不展示。

## Dashboard Responsive Requirements

- Dashboard 页面需要适配不同分辨率，重点覆盖 `1440 * 960` 视口。
- 上传页、OCR 资源页、节点表格、节点详情抽屉和批次详情页在 `1440 * 960` 下不能出现主体内容横向溢出、关键操作被遮挡或信息密度失衡。
- 表格、详情抽屉和长文本区域应使用内部滚动或合理折行，避免撑开页面布局。

## OCR Node Deployment Requirements

- 添加 OCR 节点时需要先区分节点部署类型：离线节点、在线节点。
- 离线节点表示用户自建或内网部署的 OCR 服务；当前已对接的 `paddle_ocr` 属于离线节点。
- 离线节点表单只需要填写节点别名、OCR 类型、IP/Host、端口，以及是否启用、是否参与全局负载均衡等调度配置。
- 离线节点不能让用户填写完整接口 URL；接口路径仍由适配器固定约定，例如 PaddleOCR 的 `/ocr` 和 `/health`。
- 在线节点表示调用云厂商托管 OCR/视觉模型；添加在线节点时先选择渠道，再输入模型名称和 API Key 环境变量名。
- 当前在线渠道需要支持阿里百炼，调用方式使用 DashScope OpenAI compatible chat completions API：
  - endpoint: `https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions`
  - header: `Authorization: Bearer ${DASHSCOPE_API_KEY}`
  - header: `Content-Type: application/json`
  - model 示例：`qwen-vl-ocr-2025-11-20`
  - message 内容包含图片 URL 或图片内容，以及固定提示词：`请仅输出图像中的文本内容。`
- 在线节点 API Key 属于敏感信息，服务端运行时只从环境变量读取；页面只能展示环境变量名、是否已配置或更新时间。
- 编辑在线节点时填写的是环境变量名，不在前端输入或保存真实 API Key。
- API Key 不能出现在前端响应、Dashboard 列表、详情页、日志、错误提示或 OCR 调用记录中。
- 在线节点的健康检查需要至少校验渠道配置、模型配置和密钥可用性；失败时记录脱敏后的错误原因。
- 在线节点和离线节点都可以作为 OCR 路由候选资源，是否参与全局负载均衡由节点配置决定。

## Optional LLM Markdown Formatting Step

- 文档 OCR 解析完成后允许配置一个可选 LLM 后处理步骤。
- LLM 后处理步骤的目标是将 OCR 得到的纯文本转换为 Markdown，不负责摘要、改写、补充内容或事实纠错。
- 如果用户配置了 LLM，则在 OCR 文本合并后调用 LLM 生成 Markdown。
- 如果用户没有配置 LLM，则直接返回 OCR 合并后的纯文本，不额外生成 Markdown 结构。
- LLM 配置需要填写 URL、模型名称和 API Key 环境变量名。
- API Key 为可选运行时依赖：在线模型通常需要在服务启动环境中配置对应环境变量；离线/内网模型可以使用空环境变量名。
- LLM API Key 属于敏感信息，服务端运行时只从环境变量读取；前端和配置表不保存真实 API Key。
- LLM API Key 不允许出现在前端响应、Dashboard 页面、日志、错误提示或调用记录中。
- LLM 输出需要保留原始 OCR text 作为可追溯数据，Markdown 作为结构化结果保存或返回。
- LLM 后处理失败时不能吞异常，需要记录错误；失败策略可以配置为返回原始 OCR text 或标记批次失败，默认建议回退到原始 OCR text 并记录告警。

推荐 LLM Markdown 转换 System Prompt：

```text
你是一个严谨的文档结构化编辑器。你的任务是将 OCR 解析得到的纯文本整理为 Markdown 文档。

必须遵守：
1. 只基于输入文本进行格式整理，不得新增、猜测、扩写、总结或删除正文信息。
2. 保留原文语言、数字、单位、金额、日期、编号、人名、公司名、专业术语和标点含义。
3. 可以修复明显由 OCR 造成的错误换行、断行、空格和段落粘连，但不得改变原文语义。
4. Markdown 标题最多只能使用三级：#、##、###。禁止使用 #### 或更深层级。
5. 只有在文本中明显存在标题、章节、条款层级时才使用标题；无法判断时使用普通段落。
6. 对明显的列表、编号、条款、表格进行 Markdown 结构化。
7. 对无法可靠还原成表格的内容，不要强行制作表格，保留为段落或列表。
8. 如果某一行是否为标题、表格或正文无法判断，请优先保守处理为普通正文。
9. 如果发现疑似 OCR 错字，不要自行纠正，除非它只是明显的空格、换行或字符粘连问题。
10. 不要输出解释、备注、处理说明或代码块标记。
11. 最终只输出 Markdown 内容。
```

推荐 LLM Markdown 转换 User Prompt：

```text
请将下面的 OCR 纯文本转换为 Markdown。

转换目标：
- 保留完整内容和原始语义
- 优化段落、标题、列表、表格结构
- 标题层级最多三级
- 不进行摘要，不补充输入中不存在的信息
- 无法确定结构时按普通正文保守处理

元数据：
{{meta_json}}

OCR 文本：
{{ocr_text}}
```

## File Structure

### Core Domain / Application

- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrModelDefinition.java`
  - 描述已支持 OCR 模型、能力、路径约定和是否参与默认调度。
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrModelRegistry.java`
  - 提供已支持 OCR 模型查询和校验。
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrRoutingMode.java`
  - 定义 `DEFAULT`、`GLOBAL_LOAD_BALANCE`、`MODEL_LOAD_BALANCE`、`SPECIFIC_NODE`。
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrRoutePolicy.java`
  - 批次级 OCR 路由策略值对象。
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrNode.java`
  - OCR 节点领域对象。
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrNodeStatus.java`
  - 定义 `UP`、`DOWN`、`RECOVERING`、`DISABLED`。
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrNodeRepository.java`
  - OCR 节点仓储接口。
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrNodeMetrics.java`
  - 节点运行指标：解析中图片数、排队图片数、今日完成、成功、失败、平均耗时、P95。
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrNodeCall.java`
  - 图片级 OCR 调用记录。
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrNodeCallRepository.java`
  - 图片级调用记录仓储接口。
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrNodeManagementService.java`
  - 节点增删改查、启停、手动测试。
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingService.java`
  - OCR 图片请求统一调度入口。
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrNodeSelector.java`
  - 节点选择接口。
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/LeastInflightOcrNodeSelector.java`
  - 基于 `inflight_images` 的负载均衡实现。
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrAdapter.java`
  - 保持统一 OCR 识别接口，补充模型 key 约束。
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/ImageOcrRequest.java`
  - 增加路由策略、批次 ID、文档 ID、页码。
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/ImageOcrResult.java`
  - 增加命中模型、命中节点、耗时、重试次数。
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/application/CreateBatchCommand.java`
  - 增加 `OcrRoutePolicy ocrRoutePolicy`。
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/api/CreateBatchRequest.java`
  - 增加上传时 OCR 路由字段。
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/api/DefaultDocLensEngine.java`
  - 将 API 请求中的 OCR 路由参数传入 `CreateBatchCommand`。
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentJob.java`
  - 持久化文档使用的 OCR 路由策略快照。
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentJobCreateRequest.java`
  - 增加 OCR 路由策略。
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/query/application/DashboardQueryService.java`
  - 增加 OCR 节点健康和节点图片负载读模型。
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/query/application/OcrQueryService.java`
  - 文档结果中展示 OCR 路由和命中节点信息。

### Spring Boot Starter / Infrastructure

- Create: `doclens-spring-boot-starter/src/main/resources/db/migration/V3__doclens_ocr_resource_routing.sql`
  - 新增 OCR 节点表、OCR 调用记录表、文档路由策略字段。
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrNodeEntity.java`
  - OCR 节点数据库实体。
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrNodeMapper.java`
  - MyBatis-Plus mapper。
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/MybatisPlusOcrNodeRepository.java`
  - OCR 节点仓储实现。
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrNodeCallEntity.java`
  - OCR 图片调用记录实体。
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrNodeCallMapper.java`
  - OCR 图片调用记录 mapper。
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/MybatisPlusOcrNodeCallRepository.java`
  - OCR 图片调用记录仓储实现。
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrRuntimeNodePool.java`
  - 运行时节点缓存和 inflight 计数。
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrRuntimeNode.java`
  - 运行时节点对象，包含并发计数和延迟窗口。
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthChecker.java`
  - 定时健康检查。
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensOcrThreadPoolAutoConfiguration.java`
  - OCR 相关线程池隔离配置。
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensOcrResourceAutoConfiguration.java`
  - OCR 节点、路由、健康检查相关 Bean 自动配置。
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrNativeClient.java`
  - 从固定 endpoint 改为接收运行时节点，按节点拼接 `/ocr`。
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrNativeAdapter.java`
  - 通过 `OcrRoutingService` 或节点执行上下文调用。
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensPaddleOcrAutoConfiguration.java`
  - 注册 `paddle_ocr` 模型定义和默认 bootstrap 节点。
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringProperties.java`
  - 增加 OCR 路由、节点、线程池配置。
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/shared/config/DocLensProperties.java`
  - 增加 OCR 统一配置对象。
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/DocumentJobEntity.java`
  - 增加 OCR 路由字段。
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/DocumentJobMapper.java`
  - 映射 OCR 路由字段。
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/DocumentJobMapper.xml` if mapper XML exists.
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentJobRepository.java`
  - 映射领域对象和实体新增字段。
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/ImageDocumentExtractor.java`
  - 使用路由策略调用 OCR。
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/PdfImageDocumentExtractor.java`
  - 每张图片 OCR 都进入统一路由服务。
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/WordDocumentExtractor.java`
  - Word 转 PDF 后沿用同一批次 OCR 路由策略。

### Server Interfaces

- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrModelController.java`
  - 查询已支持 OCR 模型。
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrNodeController.java`
  - 节点增删改查、启停、测试。
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrNodeRequest.java`
  - 新增/编辑节点请求。
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrNodeResponse.java`
  - 节点响应 DTO。
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/interfaces/CreateBatchForm.java`
  - 增加上传路由字段。
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/interfaces/CreateBatchRequestMapper.java`
  - 从 multipart 表单读取 OCR 路由字段。
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/dashboard/interfaces/DashboardController.java`
  - 增加 OCR 节点负载和线程池指标接口。
- Modify: `doclens-server/src/main/resources/application.yml`
  - 新增 OCR 路由、线程池、bootstrap 节点默认配置。

### Dashboard Frontend

- Create: `doclens-dashboard/src/views/OcrResourcesView.vue`
  - OCR 资源配置页面。
- Create: `doclens-dashboard/src/components/ocr/OcrModelList.vue`
  - 已支持 OCR 模型列表。
- Create: `doclens-dashboard/src/components/ocr/OcrNodeTable.vue`
  - OCR 节点表格，展示节点实时图片负载。
- Create: `doclens-dashboard/src/components/ocr/OcrNodeFormDrawer.vue`
  - 新增/编辑节点抽屉。
- Create: `doclens-dashboard/src/components/ocr/OcrNodeDetailDrawer.vue`
  - 节点详情：当前解析图片、请求记录、重试记录。
- Create: `doclens-dashboard/src/components/upload/OcrRoutingSelector.vue`
  - 上传页 OCR 路由选择组件。
- Create: `doclens-dashboard/src/api/ocrResources.ts`
  - OCR 模型和节点 API。
- Create: `doclens-dashboard/src/types/ocrResources.ts`
  - OCR 模型、节点、节点指标、路由模式类型。
- Modify: `doclens-dashboard/src/components/dashboard/UploadDropzone.vue`
  - 接入 OCR 路由选择器。
- Modify: `doclens-dashboard/src/types/upload.ts`
  - 上传参数增加 OCR 路由字段。
- Modify: `doclens-dashboard/src/api/upload.ts`
  - multipart 表单追加 OCR 路由字段。
- Modify: `doclens-dashboard/src/router/index.ts`
  - 增加 OCR 资源配置路由。
- Modify: `doclens-dashboard/src/App.vue`
  - 增加 OCR 资源配置菜单。
- Modify: `doclens-dashboard/src/views/OverviewView.vue`
  - 总览展示 OCR 节点健康和繁忙程度。
- Modify: `doclens-dashboard/src/views/BatchDetailView.vue`
  - 批次详情展示 OCR 路由策略和命中节点。

---

## Database Design

### `doclens_ocr_nodes`

```sql
CREATE TABLE doclens_ocr_nodes (
    id VARCHAR(64) PRIMARY KEY,
    deployment_type VARCHAR(32) NOT NULL,
    model_key VARCHAR(64) NOT NULL,
    channel_key VARCHAR(64),
    provider_model VARCHAR(128),
    name VARCHAR(128) NOT NULL,
    host VARCHAR(255) NOT NULL,
    port INT NOT NULL,
    credential_ref VARCHAR(255),
    credential_configured BOOLEAN NOT NULL,
    enabled BOOLEAN NOT NULL,
    participate_global BOOLEAN NOT NULL,
    weight INT NOT NULL,
    max_concurrency INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    failure_count BIGINT NOT NULL,
    success_count BIGINT NOT NULL,
    avg_latency_ms BIGINT NOT NULL,
    p95_latency_ms BIGINT NOT NULL,
    last_health_at TIMESTAMP,
    last_success_at TIMESTAMP,
    last_failure_at TIMESTAMP,
    last_error VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_doclens_ocr_nodes_model_host_port UNIQUE (model_key, host, port)
);
```

说明：

- `deployment_type` 用于区分 `OFFLINE` 与 `ONLINE`。
- 离线节点使用 `host + port`，例如当前 `paddle_ocr`。
- 在线节点使用 `channel_key + provider_model + credential_ref`，例如 `aliyun_bailian + qwen-vl-ocr-2025-11-20`。
- `credential_ref` 存储环境变量名，不能存储真实 API Key。
- `credential_configured` 用于前端展示环境变量名是否已经配置。

### `doclens_ocr_node_calls`

```sql
CREATE TABLE doclens_ocr_node_calls (
    id VARCHAR(64) PRIMARY KEY,
    batch_id VARCHAR(64) NOT NULL,
    document_id VARCHAR(64) NOT NULL,
    page_no INT NOT NULL,
    model_key VARCHAR(64) NOT NULL,
    node_id VARCHAR(64) NOT NULL,
    routing_mode VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    retry_count INT NOT NULL,
    elapsed_ms BIGINT NOT NULL,
    error_code VARCHAR(128),
    error_message VARCHAR(1000),
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP
);
```

### `ocr_document_jobs` or existing document job table changes

Add columns to the existing document job table:

```sql
ALTER TABLE document_jobs ADD COLUMN ocr_routing_mode VARCHAR(64);
ALTER TABLE document_jobs ADD COLUMN ocr_model_key VARCHAR(64);
ALTER TABLE document_jobs ADD COLUMN ocr_node_id VARCHAR(64);
ALTER TABLE document_jobs ADD COLUMN ocr_load_balance_strategy VARCHAR(64);
```

Use the actual table name from `V1__doclens_ocr_schema.sql` during implementation. Do not guess the table name when writing the migration.

---

## API Design

### Query Supported OCR Models

`GET /api/v1/ocr-models`

Response:

```json
{
  "items": [
    {
      "model_key": "paddle_ocr",
      "name": "PaddleOCR",
      "description": "PaddleOCR native-compatible HTTP API",
      "supported_inputs": ["image"],
      "ocr_path": "/ocr",
      "health_path": "/health",
      "node_count": 2,
      "healthy_node_count": 1,
      "enabled_node_count": 2
    }
  ]
}
```

### Query OCR Nodes

`GET /api/v1/ocr-models/{modelKey}/nodes`

Response:

```json
{
  "items": [
    {
      "id": "node_215",
      "model_key": "paddle_ocr",
      "name": "paddle-215",
      "host": "10.100.30.215",
      "port": 8080,
      "enabled": true,
      "participate_global": true,
      "weight": 100,
      "max_concurrency": 4,
      "status": "UP",
      "inflight_images": 12,
      "queued_images": 0,
      "processed_images_today": 340,
      "success_images": 337,
      "failed_images": 3,
      "avg_latency_ms": 1800,
      "p95_latency_ms": 4200,
      "last_health_at": "2026-06-08T17:00:00+08:00",
      "last_error": ""
    }
  ]
}
```

### Create OCR Node

`POST /api/v1/ocr-models/{modelKey}/nodes`

Request:

```json
{
  "name": "paddle-215",
  "host": "10.100.30.215",
  "port": 8080,
  "enabled": true,
  "participate_global": true,
  "weight": 100,
  "max_concurrency": 4
}
```

Validation:

- `modelKey` must exist in `OcrModelRegistry`.
- `host` must not include scheme, path, query, or fragment.
- `port` must be between 1 and 65535.
- `(model_key, host, port)` must be unique.
- `max_concurrency` must be greater than 0.
- `weight` must be greater than 0.

### Upload Batch with OCR Routing

`POST /api/v1/batches`

Multipart fields:

- `files`
- `metadata`
- `callbackUrl`
- `idempotencyKey`
- `adapterOverride` existing field if retained
- `ocrRoutingMode`
- `ocrModelKey`
- `ocrNodeId`
- `ocrLoadBalanceStrategy`

Examples:

Global load balancing:

```text
ocrRoutingMode=GLOBAL_LOAD_BALANCE
ocrModelKey=
ocrNodeId=
ocrLoadBalanceStrategy=least-inflight
```

Model-scoped load balancing:

```text
ocrRoutingMode=MODEL_LOAD_BALANCE
ocrModelKey=paddle_ocr
ocrNodeId=
ocrLoadBalanceStrategy=least-inflight
```

Specific node:

```text
ocrRoutingMode=SPECIFIC_NODE
ocrModelKey=paddle_ocr
ocrNodeId=node_215
ocrLoadBalanceStrategy=
```

---

## Implementation Tasks

### Task 1: Add OCR Routing Domain Models

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrRoutingMode.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrNodeStatus.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrRoutePolicy.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrModelDefinition.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrModelRegistry.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrRoutePolicyTest.java`

- [ ] **Step 1: Write failing route policy tests**

Create tests:

```java
@Test
void globalLoadBalanceDoesNotRequireModelOrNode() {
    OcrRoutePolicy policy = OcrRoutePolicy.globalLoadBalance("least-inflight");

    assertThat(policy.routingMode()).isEqualTo(OcrRoutingMode.GLOBAL_LOAD_BALANCE);
    assertThat(policy.modelKey()).isEmpty();
    assertThat(policy.nodeId()).isEmpty();
    assertThat(policy.loadBalanceStrategy()).contains("least-inflight");
}

@Test
void modelLoadBalanceRequiresModelKey() {
    assertThatThrownBy(() -> OcrRoutePolicy.modelLoadBalance("", "least-inflight"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ocr model key is required");
}

@Test
void specificNodeRequiresModelAndNode() {
    assertThatThrownBy(() -> OcrRoutePolicy.specificNode("paddle_ocr", ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ocr node id is required");
}
```

- [ ] **Step 2: Run test and verify it fails**

Run:

```bash
mvn -pl doclens-core -am -Dtest=OcrRoutePolicyTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation fails because classes do not exist.

- [ ] **Step 3: Implement enums and value object**

Implement:

```java
public enum OcrRoutingMode {
    DEFAULT,
    GLOBAL_LOAD_BALANCE,
    MODEL_LOAD_BALANCE,
    SPECIFIC_NODE
}
```

```java
public enum OcrNodeStatus {
    UP,
    DOWN,
    RECOVERING,
    DISABLED
}
```

`OcrRoutePolicy` fields:

- `OcrRoutingMode routingMode`
- `Optional<String> modelKey`
- `Optional<String> nodeId`
- `Optional<String> loadBalanceStrategy`

Factory methods:

- `defaultPolicy()`
- `globalLoadBalance(String strategy)`
- `modelLoadBalance(String modelKey, String strategy)`
- `specificNode(String modelKey, String nodeId)`

- [ ] **Step 4: Run focused test**

Run:

```bash
mvn -pl doclens-core -am -Dtest=OcrRoutePolicyTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

### Task 2: Add OCR Node and Call Domain Repositories

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrNode.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrNodeMetrics.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrNodeRepository.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrNodeCall.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrNodeCallRepository.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrNodeTest.java`

- [ ] **Step 1: Write failing node validation tests**

Test cases:

```java
@Test
void createNodeRejectsInvalidPort() {
    assertThatThrownBy(() -> OcrNode.create("node_1", "paddle_ocr", "paddle-1",
            "10.100.30.215", 0, true, true, 100, 4, BASE_TIME))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("port");
}

@Test
void disabledNodeUsesDisabledStatus() {
    OcrNode node = OcrNode.create("node_1", "paddle_ocr", "paddle-1",
            "10.100.30.215", 8080, false, true, 100, 4, BASE_TIME);

    assertThat(node.status()).isEqualTo(OcrNodeStatus.DISABLED);
}
```

- [ ] **Step 2: Implement domain objects**

`OcrNode` must include:

- `id`
- `modelKey`
- `name`
- `host`
- `port`
- `enabled`
- `participateGlobal`
- `weight`
- `maxConcurrency`
- `status`
- `failureCount`
- `successCount`
- `avgLatencyMs`
- `p95LatencyMs`
- `lastHealthAt`
- `lastSuccessAt`
- `lastFailureAt`
- `lastError`
- `createdAt`
- `updatedAt`

`OcrNodeMetrics` must include:

- `inflightImages`
- `queuedImages`
- `processedImagesToday`
- `successImages`
- `failedImages`
- `avgLatencyMs`
- `p95LatencyMs`
- `lastRequestAt`
- `lastError`

- [ ] **Step 3: Run focused test**

Run:

```bash
mvn -pl doclens-core -am -Dtest=OcrNodeTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

### Task 3: Add Database Migration and Infrastructure Mappers

**Files:**
- Create: `doclens-spring-boot-starter/src/main/resources/db/migration/V3__doclens_ocr_resource_routing.sql`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrNodeEntity.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrNodeMapper.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/MybatisPlusOcrNodeRepository.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrNodeCallEntity.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrNodeCallMapper.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/MybatisPlusOcrNodeCallRepository.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/MybatisPlusOcrNodeRepositoryTest.java`

- [ ] **Step 1: Read existing migration before writing V3**

Run:

```bash
sed -n '1,260p' doclens-spring-boot-starter/src/main/resources/db/migration/V1__doclens_ocr_schema.sql
sed -n '1,220p' doclens-spring-boot-starter/src/main/resources/db/migration/V2__doclens_final_text.sql
```

Confirm the actual document job table name and column naming style.

- [ ] **Step 2: Write failing repository integration test**

Use H2 and Flyway. Test:

- save node
- find by id
- list by model key
- reject duplicate model + host + port through database constraint

- [ ] **Step 3: Write migration**

Create:

- `doclens_ocr_nodes`
- `doclens_ocr_node_calls`
- new columns on existing document job table for route policy

Use `VARCHAR`, `BIGINT`, `BOOLEAN`, and `TIMESTAMP` consistently with prior migrations.

- [ ] **Step 4: Implement entities and repositories**

Repository methods:

- `save(OcrNode node)`
- `update(OcrNode node)`
- `findById(String nodeId)`
- `listByModelKey(String modelKey)`
- `listEnabled()`
- `listAll()`
- `findByModelHostPort(String modelKey, String host, int port)`

- [ ] **Step 5: Run starter tests**

Run:

```bash
mvn -pl doclens-spring-boot-starter -am test
```

Expected: PASS.

### Task 4: Add OCR Thread Pool Isolation

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/shared/config/DocLensProperties.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringProperties.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensOcrThreadPoolAutoConfiguration.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/shared/infrastructure/NamedThreadPoolFactory.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensOcrThreadPoolAutoConfigurationTest.java`

- [ ] **Step 1: Add thread pool properties**

Add property records:

- `documentProcessingThreadPool`
- `ocrRequestThreadPool`
- `ocrHealthThreadPool`
- `callbackThreadPool`

Each contains:

- `coreSize`
- `maxSize`
- `queueCapacity`
- `keepAliveSeconds`
- `threadNamePrefix`

- [ ] **Step 2: Implement isolated executors**

Create beans:

- `doclensDocumentProcessingExecutor`
- `doclensOcrRequestExecutor`
- `doclensOcrHealthExecutor`
- `doclensCallbackExecutor`

Use named `ThreadPoolExecutor`; do not use default `@Async` executor.

- [ ] **Step 3: Add tests**

Assert bean names exist and thread names start with:

- `doclens-document-processing-`
- `doclens-ocr-request-`
- `doclens-ocr-health-`
- `doclens-callback-`

- [ ] **Step 4: Run focused test**

Run:

```bash
mvn -pl doclens-spring-boot-starter -am -Dtest=DocLensOcrThreadPoolAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

### Task 5: Implement Runtime Node Pool and Least-Inflight Selection

**Files:**
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrRuntimeNode.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrRuntimeNodePool.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/LeastInflightOcrNodeSelector.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/adapter/application/LeastInflightOcrNodeSelectorTest.java`

- [ ] **Step 1: Write selector tests**

Cases:

- ignores `DOWN`
- ignores `DISABLED`
- global mode includes multiple models
- model mode includes only one model
- chooses node with lowest `inflightImages`
- tie-breaker chooses lower average latency

- [ ] **Step 2: Implement runtime pool**

Runtime pool responsibilities:

- load enabled nodes from repository at startup
- refresh after node CRUD changes
- increment inflight before OCR request
- decrement inflight in `finally`
- expose snapshot for Dashboard

- [ ] **Step 3: Implement selector**

Selector method:

```java
Optional<OcrRuntimeNode> select(OcrRoutePolicy policy, List<OcrRuntimeNode> nodes);
```

Selection order:

1. enabled
2. `UP`
3. route policy candidate filter
4. below `maxConcurrency`
5. lowest `inflightImages`
6. lowest `avgLatencyMs`
7. stable node id order

- [ ] **Step 4: Run focused tests**

Run:

```bash
mvn -pl doclens-core -am -Dtest=LeastInflightOcrNodeSelectorTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

### Task 6: Refactor PaddleOCR Client to Runtime Nodes

**Files:**
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrNativeClient.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrNativeAdapter.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrNativeClientTest.java`

- [ ] **Step 1: Update tests**

Add assertion that node `10.100.30.215:8080` builds:

- OCR URI: `http://10.100.30.215:8080/ocr`
- health URI: `http://10.100.30.215:8080/health`

- [ ] **Step 2: Change client method signature**

From:

```java
JsonNode recognizeImage(byte[] imageContent)
```

To:

```java
JsonNode recognizeImage(OcrRuntimeNode node, byte[] imageContent)
```

Do not log base64. Log only:

- model key
- node id
- host
- port
- file byte length
- elapsed ms
- HTTP status

- [ ] **Step 3: Run tests**

Run:

```bash
mvn -pl doclens-spring-boot-starter -am -Dtest=PaddleOcrNativeClientTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

### Task 7: Implement OCR Routing Service with Retry and Failover

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingService.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRouteExecutionResult.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRouteExecutionException.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingServiceTest.java`

- [ ] **Step 1: Write retry tests**

Cases:

- same node retries 3 times
- after node retry exhaustion, global mode selects another healthy node
- model mode fails over only within same model
- specific node does not fail over by default
- all candidates failed throws `OcrRouteExecutionException`

- [ ] **Step 2: Implement service**

Flow:

1. Resolve policy.
2. Select candidate node.
3. Increment `inflightImages`.
4. Call adapter client.
5. On success record call success and decrement inflight.
6. On failure retry same node up to configured count.
7. If exhausted, mark node failure and select next candidate when policy allows.
8. If no candidates remain, throw explicit OCR failure.

- [ ] **Step 3: Add call records**

Each image OCR request writes one `OcrNodeCall` with:

- `batchId`
- `documentId`
- `pageNo`
- `modelKey`
- `nodeId`
- `routingMode`
- `status`
- `retryCount`
- `elapsedMs`
- `errorCode`
- `errorMessage`
- `startedAt`
- `finishedAt`

- [ ] **Step 4: Run focused tests**

Run:

```bash
mvn -pl doclens-core -am -Dtest=OcrRoutingServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

### Task 8: Implement Health Checker and Recovery Flow

**Files:**
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthChecker.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthClient.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthCheckerTest.java`

- [ ] **Step 1: Write health flow tests**

Cases:

- `UP` node with consecutive health failures becomes `DOWN`
- `DOWN` node with first successful health check becomes `RECOVERING`
- `RECOVERING` node with configured success threshold becomes `UP`
- `DISABLED` node is not checked
- health checks run through `doclensOcrHealthExecutor`

- [ ] **Step 2: Implement health client**

For PaddleOCR:

```text
GET http://{host}:{port}/health
```

Success:

- HTTP 2xx

Failure:

- timeout
- connection failure
- non-2xx

- [ ] **Step 3: Implement checker**

Use configurable:

- `healthCheckIntervalSeconds`
- `healthCheckTimeoutSeconds`
- `healthFailureThreshold`
- `recoverySuccessThreshold`

- [ ] **Step 4: Run tests**

Run:

```bash
mvn -pl doclens-spring-boot-starter -am -Dtest=OcrHealthCheckerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

### Task 9: Persist Upload-Time OCR Route Policy

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/api/CreateBatchRequest.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/api/DefaultDocLensEngine.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/application/CreateBatchCommand.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/application/CreateBatchUseCase.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentJob.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentJobCreateRequest.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/interfaces/CreateBatchForm.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/interfaces/CreateBatchRequestMapper.java`
- Modify: `doclens-dashboard/src/types/upload.ts`
- Modify: `doclens-dashboard/src/api/upload.ts`
- Test: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/DocLensOcrApiContractTest.java`

- [ ] **Step 1: Add contract test**

Multipart upload with:

```text
ocrRoutingMode=MODEL_LOAD_BALANCE
ocrModelKey=paddle_ocr
ocrLoadBalanceStrategy=least-inflight
```

Assert created document stores:

- `ocr_routing_mode=MODEL_LOAD_BALANCE`
- `ocr_model_key=paddle_ocr`
- `ocr_load_balance_strategy=least-inflight`

- [ ] **Step 2: Implement request mapping**

Default missing fields to:

- `DEFAULT` routing mode
- empty model key
- empty node id
- configured default strategy

- [ ] **Step 3: Persist into document job**

Each document in the batch stores the same route policy snapshot.

- [ ] **Step 4: Run server tests**

Run:

```bash
mvn -pl doclens-server -am test
```

Expected: PASS.

### Task 10: Add OCR Node Management APIs

**Files:**
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrModelController.java`
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrNodeController.java`
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrNodeRequest.java`
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrNodeResponse.java`
- Test: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/OcrNodeApiContractTest.java`

- [ ] **Step 1: Add API contract tests**

Cover:

- list supported models
- create PaddleOCR node
- reject unknown model key
- reject duplicate host/port under same model
- enable/disable node
- manually test node

- [ ] **Step 2: Implement controllers**

Endpoints:

- `GET /api/v1/ocr-models`
- `GET /api/v1/ocr-models/{modelKey}/nodes`
- `POST /api/v1/ocr-models/{modelKey}/nodes`
- `PUT /api/v1/ocr-nodes/{nodeId}`
- `PATCH /api/v1/ocr-nodes/{nodeId}/enabled`
- `DELETE /api/v1/ocr-nodes/{nodeId}`
- `POST /api/v1/ocr-nodes/{nodeId}/test`

- [ ] **Step 3: Run contract tests**

Run:

```bash
mvn -pl doclens-server -am -Dtest=OcrNodeApiContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

### Task 11: Add Dashboard OCR Resource Page

**Files:**
- Create: `doclens-dashboard/src/views/OcrResourcesView.vue`
- Create: `doclens-dashboard/src/components/ocr/OcrModelList.vue`
- Create: `doclens-dashboard/src/components/ocr/OcrNodeTable.vue`
- Create: `doclens-dashboard/src/components/ocr/OcrNodeFormDrawer.vue`
- Create: `doclens-dashboard/src/components/ocr/OcrNodeDetailDrawer.vue`
- Create: `doclens-dashboard/src/api/ocrResources.ts`
- Create: `doclens-dashboard/src/types/ocrResources.ts`
- Modify: `doclens-dashboard/src/router/index.ts`
- Modify: `doclens-dashboard/src/App.vue`

- [ ] **Step 1: Define TypeScript types**

Types:

- `OcrModel`
- `OcrNode`
- `OcrNodeMetrics`
- `OcrNodeCall`
- `OcrNodeStatus`

- [ ] **Step 2: Build OCR model list**

Show:

- model name
- model key
- node count
- healthy node count
- unhealthy node count

- [ ] **Step 3: Build node table**

Columns:

- OCR
- node name
- host:port
- status
- enabled
- participate global
- inflight images
- queued images
- processed today
- success images
- failed images
- avg latency
- p95 latency
- last health time
- actions

- [ ] **Step 4: Build node form**

Fields:

- OCR model select from supported models
- node name
- host
- port
- enabled switch
- participate global switch
- weight
- max concurrency

- [ ] **Step 5: Build node detail drawer**

Sections:

- current inflight image calls
- recent calls
- retry count
- last errors

- [ ] **Step 6: Run frontend build**

Run:

```bash
cd doclens-dashboard
npm run build
```

Expected: PASS.

### Task 12: Add Upload OCR Routing Selector

**Files:**
- Create: `doclens-dashboard/src/components/upload/OcrRoutingSelector.vue`
- Modify: `doclens-dashboard/src/components/dashboard/UploadDropzone.vue`
- Modify: `doclens-dashboard/src/types/upload.ts`
- Modify: `doclens-dashboard/src/api/upload.ts`

- [ ] **Step 1: Add selector UI**

Control layout:

- radio group: system default / global load balancing / specified OCR / specified node
- model select visible for specified OCR and specified node
- node select visible for specified node
- strategy select visible for global and model load balancing

- [ ] **Step 2: Validation**

Before submit:

- specified OCR requires model key
- specified node requires model key and node id
- node select only shows nodes under selected model
- disabled nodes are marked and cannot be selected

- [ ] **Step 3: Submit multipart fields**

Append:

- `ocrRoutingMode`
- `ocrModelKey`
- `ocrNodeId`
- `ocrLoadBalanceStrategy`

- [ ] **Step 4: Run frontend build**

Run:

```bash
cd doclens-dashboard
npm run build
```

Expected: PASS.

### Task 13: Add Dashboard OCR Metrics and Thread Pool Metrics

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/query/application/DashboardQueryService.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/dashboard/interfaces/DashboardController.java`
- Modify: `doclens-dashboard/src/views/OverviewView.vue`
- Modify: `doclens-dashboard/src/views/BatchDetailView.vue`
- Modify: `doclens-dashboard/src/types/dashboard.ts`

- [ ] **Step 1: Add backend read model**

Expose:

- healthy node count
- down node count
- recovering node count
- global inflight images
- busiest node
- OCR request executor active count
- OCR request executor queue size
- OCR health executor active count
- OCR health executor queue size

- [ ] **Step 2: Add Dashboard overview cards**

Cards:

- OCR 健康节点
- OCR 异常节点
- OCR 解析中图片
- OCR 请求队列

- [ ] **Step 3: Add batch detail route policy display**

Show:

- routing mode
- model key
- node id if specified
- actual hit nodes from calls

- [ ] **Step 4: Run backend and frontend checks**

Run:

```bash
mvn -pl doclens-server -am test
cd doclens-dashboard && npm run build
```

Expected: PASS.

### Task 14: Bootstrap Existing PaddleOCR Server Configuration

**Files:**
- Modify: `doclens-server/src/main/resources/application.yml`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensPaddleOcrAutoConfiguration.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrNodeBootstrapper.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrNodeBootstrapperTest.java`

- [ ] **Step 1: Add bootstrap config**

Use server OCR as default:

```yaml
doclens:
  ocr:
    default-routing-mode: GLOBAL_LOAD_BALANCE
    load-balance-strategy: least-inflight
    request-retry-times: 3
    health-check-interval-seconds: 30
    health-check-timeout-seconds: 5
    health-failure-threshold: 3
    recovery-success-threshold: 2
    specific-node-fallback-enabled: false
  paddle-ocr:
    bootstrap-nodes:
      - name: paddle-215
        host: 10.100.30.215
        port: 8080
        enabled: true
        participate-global: true
        weight: 100
        max-concurrency: 4
```

- [ ] **Step 2: Implement bootstrapper**

Behavior:

- if database has no `paddle_ocr` nodes, insert bootstrap nodes
- if database already has nodes, do not overwrite page configuration

- [ ] **Step 3: Run focused test**

Run:

```bash
mvn -pl doclens-spring-boot-starter -am -Dtest=OcrNodeBootstrapperTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

### Task 15: End-to-End Verification

**Files:**
- No new files required.

- [ ] **Step 1: Run full backend tests**

Run:

```bash
mvn -pl doclens-server -am test
```

Expected: PASS.

- [ ] **Step 2: Run frontend build**

Run:

```bash
cd doclens-dashboard
npm run build
```

Expected: PASS.

- [ ] **Step 3: Start backend**

Use the project’s current preferred server port. If occupied, choose a different non-special port.

Expected:

- `/api/v1/ocr-models` returns `paddle_ocr`
- OCR node page can load nodes
- Upload page can submit routing fields

- [ ] **Step 4: Real PaddleOCR smoke test**

Use `10.100.30.215:8080`:

- create node if bootstrap did not insert it
- run manual test
- upload one small image
- verify node `inflight_images` increments during OCR and decrements after completion
- verify node call record is created
- verify batch detail shows routing mode and hit node

---

## Self-Review Checklist

- [ ] OCR 类型只能来自 registered models.
- [ ] Page can configure multiple nodes per supported OCR.
- [ ] Upload supports global load balancing across different OCR models and nodes.
- [ ] Upload supports model-scoped load balancing.
- [ ] Upload supports model + specific node.
- [ ] Specific node does not silently fail over unless explicitly configured.
- [ ] Thread pools are isolated and named.
- [ ] Health checker does not share OCR request executor.
- [ ] Node status transitions are visible.
- [ ] Node inflight image count is visible.
- [ ] OCR call records include batch, document, page, model, node, retries, status, and elapsed time.
- [ ] Dashboard can identify busy, slow, and failing OCR nodes.
- [ ] Backend tests do not require the real PaddleOCR server except the final smoke test.
- [ ] Frontend build updates static Dashboard assets only when frontend code changes.

## Open Implementation Notes

- Use `paddle_ocr` as the canonical model key for PaddleOCR. Keep current adapter key compatibility by mapping old default adapter key to `paddle_ocr` during bootstrap if needed.
- Avoid database queries on every image OCR selection. Use `OcrRuntimeNodePool` snapshots and refresh after configuration mutations and health checks.
- Do not log image base64, request body, or raw OCR payload beyond safe summaries.
- Keep page-configured host/port validation strict to reduce SSRF risk.
- If the project table names differ from this plan, update migration names based on existing Flyway SQL before implementation.
