# Document OCR Route, Retry, Delete, and Recovery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修正批次详情页 OCR 路由面板的统计口径，让用户可以看到“当前文件实际分配到哪些节点”；同时补齐文档级 `重试`、`删除`、`卡死检测` 与 `重启恢复` 能力，解决后端重启后文档长时间卡在处理中且无法自助恢复的问题。

**Architecture:** 将现有“批次级 OCR 调度命中统计”和“文档级最终 OCR 分配统计”拆分为两套明确的读模型；在处理域中新增文档级操作用例与状态流转；通过文档进度心跳、stale 扫描器和恢复策略，把进程内异步处理中断后的文档从无限卡住改为可识别、可恢复、可人工干预。

**Tech Stack:** Java 21, Spring Boot 3.5, Maven, MyBatis-Plus, Flyway, H2, Vue 3, TypeScript, Pinia, Naive UI, Vite, JUnit 5, AssertJ, Vitest.

---

## Confirmed Requirements

- 批次详情页当前的 `OCR 路由` 面板不能再用批次级聚合结果冒充“当前文件的实际分配结果”。
- 用户需要看到当前文件的图片或页，最终由哪些 OCR 节点处理，各节点分别处理了多少张。
- 现有批次级 OCR 调度命中信息仍有保留价值，但必须明确改名并调整文案，避免误导。
- 文档列表行操作需要新增 `重试` 与 `删除`。
- 后端重启或处理中断后，不能让文档无限停留在 `处理中`、`OCR 图片解析中` 或 `LLM 排版中`。
- 系统需要具备卡死检测能力，至少能识别长时间无进展的文档并允许恢复。
- 第一版优先支持单实例场景，不强行引入复杂的分布式抢占与多 worker 协调机制。
- 第一版删除能力优先覆盖 `FAILED`、`COMPLETED`、`STALLED` 文档；`PROCESSING` 文档采用保守策略，优先支持“取消并删除”或显式禁用。
- 所有任务必须按 `plan -> RED -> GREEN -> broader verification -> code-review-spec -> commit` 顺序逐个完成。

## Problem Statement

### 1. OCR 路由统计语义错误

- 当前批次详情页中的 OCR 路由面板展示的是批次级 OCR 调度命中统计。
- 该统计同时聚合了历史 OCR 调用记录和运行中命中数据。
- 当同一批次中有多个文档、同一图片发生重试、或节点故障转移时，面板会显示比“当前文件实际使用节点数”更多的节点卡片。
- 用户看到的是当前文件，因此自然会把该面板理解为“这个文件被分到了几个节点”，造成明显认知偏差。

### 2. 文档缺少自助恢复操作

- 当前文档列表缺少文档级 `重试` 和 `删除` 操作。
- 一旦文档失败、卡死或因后端重启中断，用户无法单独处理该文档，只能刷新等待或重新发起整批处理。

### 3. 宕机重启后处理链路缺少恢复机制

- 批次处理当前基于进程内异步线程池执行。
- 当服务进程重启时，线程池中的处理中任务会直接丢失。
- 现有实现中缺少“启动扫描 stale 文档并恢复”的机制。
- 因此文档可能永久停留在处理中状态，且界面仍显示处理中进度。

## Product Decisions

### OCR 路由面板拆分

- 保留现有批次级调度命中视图，但改名为 `批次 OCR 调度命中`。
- 新增 `当前文件实际分配` 视图，展示当前文件最终成功由哪些节点处理。
- 如果当前文件尚未完成 OCR，`当前文件实际分配` 显示明确空态，不猜测、不混用运行中统计。

### 文档恢复策略

- 第一版先支持 `FAILED`、`STALLED` 文档的显式 `重试`。
- 第一版卡死恢复默认策略为：检测 stale 文档后标记为 `STALLED`，由用户手动点击 `重试` 恢复。
- 可选增强方案为：应用启动时自动对 stale 文档尝试一次恢复，但该能力放在后续任务，并通过恢复次数上限保护。

### 删除策略

- `COMPLETED`、`FAILED`、`STALLED` 文档允许直接删除。
- `PROCESSING` 文档第一版不做危险硬删，优先支持：
  - 明确禁用删除并给出提示；或
  - 标记 `CANCEL_REQUESTED` 后等待安全点清理。
- 实际采用哪一种，以最小风险实现为准。

## Target User Experience

### 批次详情页

- 用户在当前文档卡片中可以看到：
  - 当前文件实际分配到的 OCR 节点
  - 每个节点最终处理的图片数或页数
- 用户在批次级区域可以看到：
  - 该批次整体的 OCR 调度命中分布
  - 明确说明该统计可能包含重试、故障转移和运行中请求

### 文档列表行操作

- `重试`：仅在允许状态下显示或可点击。
- `删除`：仅在允许状态下显示或可点击。
- 触发操作后，界面展示 loading 与结果反馈，并刷新当前批次详情。

### 卡死文档

- 长时间无进展的文档从 `处理中` 转为 `已卡住` 或等价状态展示。
- 界面明确告诉用户该文档可执行 `重试`。

## Backend Design

### A. OCR 路由统计读模型拆分

#### 现有问题

- `hitNodesByBatch(batchId)` 当前统计的是批次级 OCR 命中次数，不区分“尝试过”和“最终完成”。
- 统计结果混入运行时命中快照后，更适合作为运维视角，而非文档视角。

#### 改造方向

- 保留现有批次级统计接口或字段，但改名为更准确的语义，例如：
  - `batch_dispatch_hit_nodes`
- 新增文档级最终分配统计，例如：
  - `document_final_hit_nodes`

#### 文档级统计口径

- 仅统计当前文档最终成功的 OCR 调用。
- 不按失败尝试次数累计。
- 不把其他文档数据混入当前文档视角。
- 如果存在一张图片多次尝试，最终仅归属到成功节点。

#### 可能的数据要求

- OCR 调用记录需要至少可关联：
  - `batchId`
  - `documentId`
  - `nodeId`
  - `modelKey`
  - `callStatus`
- 如果当前记录无法稳定区分“文档内同一图片”的最终成功归属，需要补最小必要字段。

### B. 文档级重试能力

#### 能力目标

- 对 `FAILED`、`STALLED` 文档提供文档级重试。
- 重试后重新进入处理流程，不影响同批次中已完成文档。

#### 推荐实现

- 新增 `DocumentRetryUseCase` 或等价应用服务。
- 增加文档操作接口：
  - `POST /documents/{documentId}/retry`
- 允许重试的状态：
  - `FAILED`
  - `STALLED`
- 重试时执行：
  - 重置错误信息
  - 重置阶段与进度
  - 记录事件日志
  - 重新加入处理队列

#### 调度建议

- 如果当前只能整批调度，需避免把已完成文档再次执行。
- 推荐补充单文档处理入口，避免重试时整批重复执行。

### C. 文档级删除能力

#### 能力目标

- 删除指定文档及其关联 OCR 结果、事件、对象存储文件。
- 删除后批次统计和列表状态保持一致。

#### 推荐实现

- 新增 `DocumentDeleteUseCase` 或等价应用服务。
- 增加操作接口：
  - `DELETE /documents/{documentId}`
- 第一版允许删除的状态：
  - `COMPLETED`
  - `FAILED`
  - `STALLED`
- 删除时清理：
  - 文档任务记录
  - OCR 结果
  - OCR 事件
  - Markdown 结果文件
  - 原始上传文件或按现有存储策略决定是否保留

#### 处理中删除策略

- 第一版不建议直接对 `PROCESSING` 文档硬删。
- 如果实现取消链路，则增加：
  - `CANCEL_REQUESTED`
  - `CANCELLED`
- 如果本轮不做取消链路，则前端对处理中删除置灰并显示说明。

### D. 卡死检测与重启恢复

#### stale 判定基础

- 在文档任务记录上维护 `lastProgressAt` 或复用可靠的更新时间字段。
- 每次阶段推进、页数推进、OCR 完成、Markdown 完成时刷新该时间。
- 配置 stale 阈值，例如 `5 分钟无进展`。

#### 检测策略

- 应用启动时扫描：
  - `PROCESSING` 且超过 stale 阈值的文档
- 定时任务持续扫描：
  - 长时间无进展的处理中文档

#### 第一版恢复策略

- 将 stale 文档转为 `STALLED`
- 记录 `stalledReason`
- 写入文档事件，说明原因可能为：
  - 服务重启
  - OCR 调用长时间无响应
  - LLM 后处理长时间无响应

#### 后续增强策略

- 启动时自动重试一次 stale 文档
- 增加 `recoveryAttemptCount`
- 达到阈值后停止自动恢复，改为人工重试

## Frontend Design

### BatchDetailView

- 将当前 `OCR 路由` 区块拆分为：
  - `当前文件实际分配`
  - `批次 OCR 调度命中`
- 在批次级区块下增加说明文案：
  - 包含重试、故障转移和运行中请求，仅反映批次调度情况
- 当前文件未完成 OCR 时，显示明确空态和提示

### Document Row Actions

- 在文档列表行新增：
  - `重试`
  - `删除`
- 操作按钮显隐依赖文档状态
- 所有操作均需有 loading 态与失败提示

### Document Status Presentation

- 为 `STALLED` 增加状态文案和样式
- 明确与 `FAILED` 区分：
  - `FAILED` 表示处理已结束且失败
  - `STALLED` 表示处理中断或长时间无进展，可重试

## Data Model Changes

### Document Status

- 评估并新增以下状态中的必要项：
  - `STALLED`
  - `CANCEL_REQUESTED`
  - `CANCELLED`

### Document Job Fields

- 评估并新增以下字段中的必要项：
  - `last_progress_at`
  - `retry_count`
  - `recovery_attempt_count`
  - `stalled_reason`
  - `cancel_requested_at`

### Document Events

- 补充事件类型：
  - `DOCUMENT_RETRY_REQUESTED`
  - `DOCUMENT_STALLED`
  - `DOCUMENT_RECOVERED`
  - `DOCUMENT_DELETE_REQUESTED`
  - `DOCUMENT_DELETED`

## API Surface

### Query APIs

- 继续返回批次级 OCR 调度命中字段
- 新增当前文档最终分配字段
- 如有必要，新增单独查询接口：
  - `GET /dashboard/documents/{documentId}/ocr-route`

### Command APIs

- `POST /documents/{documentId}/retry`
- `DELETE /documents/{documentId}`
- 可选：
  - `POST /documents/{documentId}/cancel`

## File Structure

### Core Domain / Application

- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/query/application/DashboardQueryService.java`
  - 拆分批次级命中统计和文档级最终分配统计。
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/query/application/DashboardRowAssembler.java`
  - 组装新的路由展示字段。
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentStatus.java`
  - 增加 `STALLED` 及可选取消状态。
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentJob.java`
  - 增加卡死、重试、删除相关状态流转与时间字段支持。
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentRetryUseCase.java`
  - 文档级重试用例。
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentDeleteUseCase.java`
  - 文档级删除用例。
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/StaleDocumentRecoveryService.java`
  - stale 扫描与恢复协调。
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCase.java`
  - 在关键阶段推进时刷新文档进度心跳。
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingService.java`
  - 确认调用记录包含文档级最终分配统计所需字段。

### Spring Boot Starter / Infrastructure

- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/query/infrastructure/OcrDashboardMetricsProvider.java`
  - 保留批次级调度命中统计，补充文档级最终分配统计实现。
- Modify: `doclens-spring-boot-starter/src/main/resources/db/migration/*.sql`
  - 增加文档状态、进度心跳和恢复字段所需迁移。
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/*`
  - 仓储实体与映射增加新字段支持。
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/StaleDocumentRecoveryScheduler.java`
  - 启动扫描与定时扫描实现。

### Server / Interfaces

- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/*`
  - 增加文档重试、删除相关接口。
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/*`
  - 返回新的批次详情读模型字段。

### Frontend

- Modify: `doclens-dashboard/src/views/BatchDetailView.vue`
  - 拆分和重命名 OCR 路由展示区域。
- Modify: `doclens-dashboard/src/components/*`
  - 补充文档级分配展示组件与操作按钮。
- Modify: `doclens-dashboard/src/api/dashboard.ts`
  - 对接新的查询字段与命令接口。
- Modify: `doclens-dashboard/src/types/*`
  - 增加 `STALLED`、操作权限与新的 OCR 路由读模型类型。

---

## Task 1: 修正 OCR 路由统计口径并拆分展示

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/query/application/DashboardQueryService.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/query/infrastructure/OcrDashboardMetricsProvider.java`
- Modify: `doclens-dashboard/src/views/BatchDetailView.vue`
- Modify: `doclens-dashboard/src/api/dashboard.ts`

- [ ] **Step 1: Write the failing test**

```text
后端：
1. 为 Dashboard 查询补单测：
   - 同一批次包含多个文档时，当前文档分配统计不能串到其他文档。
   - 同一图片发生重试或故障转移时，最终分配只统计成功节点，不统计失败尝试次数。
2. 保留批次级统计单测，但断言字段语义调整为“调度命中”。

前端：
1. 组件单测断言页面同时展示：
   - 当前文件实际分配
   - 批次 OCR 调度命中
2. 当前文件未完成 OCR 时显示空态提示。
```

- [ ] **Step 2: Run test to verify it fails**

Run:
- `mvn -pl doclens-core,doclens-spring-boot-starter -Dtest=... test`
- `pnpm --dir doclens-dashboard test -- --run ...`

Expected:
- 当前文档分配统计缺失或语义错误，测试失败。

- [ ] **Step 3: Write minimal implementation**

```text
1. 后端新增文档级最终分配统计字段。
2. 前端拆分面板并调整文案。
3. 批次级面板增加说明文字，避免误导。
```

- [ ] **Step 4: Run test to verify it passes**

Run:
- `mvn -pl doclens-core,doclens-spring-boot-starter test`
- `pnpm --dir doclens-dashboard test -- --run`

Expected:
- 相关测试通过。

- [ ] **Step 5: Commit**

```bash
git add doclens-core doclens-spring-boot-starter doclens-dashboard
git commit -F /tmp/document-ocr-route-task1.commit
```

## Task 2: 增加文档级重试能力

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentRetryUseCase.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentJob.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/*`
- Modify: `doclens-dashboard/src/api/dashboard.ts`
- Modify: `doclens-dashboard/src/views/BatchDetailView.vue`

- [ ] **Step 1: Write the failing test**

```text
后端：
1. `FAILED` 文档可重试。
2. `STALLED` 文档可重试。
3. `COMPLETED` 文档不可重试。
4. 重试后应重置阶段、进度和错误信息，并记录事件。

前端：
1. 允许状态展示 `重试` 按钮。
2. 点击后调用接口并刷新列表。
```

- [ ] **Step 2: Run test to verify it fails**

Run:
- `mvn -pl doclens-core,doclens-server -Dtest=... test`
- `pnpm --dir doclens-dashboard test -- --run ...`

Expected:
- 缺少重试入口和状态流转，测试失败。

- [ ] **Step 3: Write minimal implementation**

```text
1. 增加文档重试用例和接口。
2. 新增或补足单文档重新入队能力。
3. 前端增加 `重试` 按钮与调用逻辑。
```

- [ ] **Step 4: Run test to verify it passes**

Run:
- `mvn -pl doclens-core,doclens-server test`
- `pnpm --dir doclens-dashboard test -- --run`

Expected:
- 相关测试通过。

- [ ] **Step 5: Commit**

```bash
git add doclens-core doclens-server doclens-dashboard
git commit -F /tmp/document-ocr-route-task2.commit
```

## Task 3: 增加文档级删除能力

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentDeleteUseCase.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentJob.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/*`
- Modify: `doclens-dashboard/src/views/BatchDetailView.vue`

- [ ] **Step 1: Write the failing test**

```text
后端：
1. `FAILED`、`COMPLETED`、`STALLED` 文档允许删除。
2. 删除后应同步清理 OCR 结果、事件及关联存储。
3. 删除后批次统计与文档列表应保持一致。
4. `PROCESSING` 文档第一版若不支持删除，应明确拒绝并返回可读错误。

前端：
1. 允许状态展示 `删除` 按钮。
2. 删除前有确认交互。
3. 删除成功后刷新详情。
```

- [ ] **Step 2: Run test to verify it fails**

Run:
- `mvn -pl doclens-core,doclens-server -Dtest=... test`
- `pnpm --dir doclens-dashboard test -- --run ...`

Expected:
- 缺少删除能力或删除后状态不一致，测试失败。

- [ ] **Step 3: Write minimal implementation**

```text
1. 增加文档删除用例和接口。
2. 清理关联结果与事件。
3. 前端增加 `删除` 按钮和确认交互。
```

- [ ] **Step 4: Run test to verify it passes**

Run:
- `mvn -pl doclens-core,doclens-server test`
- `pnpm --dir doclens-dashboard test -- --run`

Expected:
- 相关测试通过。

- [ ] **Step 5: Commit**

```bash
git add doclens-core doclens-server doclens-dashboard
git commit -F /tmp/document-ocr-route-task3.commit
```

## Task 4: 增加卡死检测与重启恢复

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/StaleDocumentRecoveryService.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCase.java`
- Modify: `doclens-spring-boot-starter/src/main/resources/db/migration/*.sql`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/StaleDocumentRecoveryScheduler.java`
- Modify: `doclens-dashboard/src/types/*`
- Modify: `doclens-dashboard/src/views/BatchDetailView.vue`

- [ ] **Step 1: Write the failing test**

```text
后端：
1. 处理中且超过 stale 阈值的文档应被识别为 stale。
2. stale 文档启动扫描后应转为 `STALLED`。
3. 定时扫描应处理新增 stale 文档。
4. 阶段推进时应更新 `lastProgressAt`。

前端：
1. `STALLED` 状态应有明确文案。
2. `STALLED` 文档应可执行 `重试`。
```

- [ ] **Step 2: Run test to verify it fails**

Run:
- `mvn -pl doclens-core,doclens-spring-boot-starter -Dtest=... test`
- `pnpm --dir doclens-dashboard test -- --run ...`

Expected:
- 缺少 stale 检测和状态展示，测试失败。

- [ ] **Step 3: Write minimal implementation**

```text
1. 增加进度心跳字段与刷新逻辑。
2. 实现启动扫描和定时扫描。
3. stale 文档转为 `STALLED` 并记录事件。
4. 前端展示 `STALLED` 状态和重试入口。
```

- [ ] **Step 4: Run test to verify it passes**

Run:
- `mvn -pl doclens-core,doclens-spring-boot-starter test`
- `pnpm --dir doclens-dashboard test -- --run`

Expected:
- 相关测试通过。

- [ ] **Step 5: Commit**

```bash
git add doclens-core doclens-spring-boot-starter doclens-dashboard
git commit -F /tmp/document-ocr-route-task4.commit
```

---

## Verification Matrix

- 后端单元测试：
  - Dashboard 查询聚合
  - 文档状态流转
  - 重试与删除用例
  - stale 检测与恢复
- 后端集成测试：
  - 数据库迁移
  - 接口状态码与返回值
  - 删除后批次统计一致性
- 前端单元测试：
  - OCR 路由展示
  - 行操作按钮显隐
  - `STALLED` 状态展示
- 手工验证：
  - 上传 Word，确认 2 张图不会再显示成 3 个最终分配节点
  - 上传 PDF，中途重启后端，确认文档最终转为 `STALLED` 或可恢复状态
  - 点击 `重试` 后重新处理成功
  - 删除已完成或失败文档后列表与统计同步更新

## Risks and Mitigations

- 风险：OCR 调用记录不足以支撑文档级最终分配统计。
  - 缓解：优先确认现有调用记录字段；如不足，先补最小必要字段，再实现聚合。
- 风险：当前处理链路以批次为单位，文档级重试可能误触发整批重跑。
  - 缓解：优先补单文档处理入口或在批次处理中跳过非目标文档。
- 风险：删除操作破坏处理一致性。
  - 缓解：第一版仅允许安全状态删除，处理中删除保守处理。
- 风险：自动恢复在未来多实例部署下引发重复处理。
  - 缓解：第一版以 `STALLED + 手动重试` 为主，自动恢复作为受控增强。

## Out of Scope for This Plan

- 多实例分布式 worker 的抢占锁、lease、幂等恢复协调。
- 对处理中任务的强制中断和立即删除。
- 通用任务队列中间件替换当前进程内异步模型。
- 跨批次批量重试、批量删除和批量恢复控制台。
