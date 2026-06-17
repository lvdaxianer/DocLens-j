## Context

`DashboardQueryService.batchDetail(...)` 已经返回 `batch`、`documents`、`events`、`callback_jobs` 等详情数据。`BatchDetailView.vue` 负责路由级组合，顶部已经渲染 `BatchSummaryStrip` 和 OCR/回调相关面板。批次实体本身保存了 `metadata`、`callbackUrl` 和 `idempotencyKey`，但 Dashboard 的 `BatchRow` 类型和行组装器没有暴露这些字段。

## Goals / Non-Goals

**Goals:**
- 在批次详情页显式展示回调地址、幂等值和 meta 信息。
- 保持详情页首屏紧凑，不影响现有摘要条、OCR 路由、回调任务和文档轨道。
- 保持前端展示组件纯展示，数据由父级通过 `selectedBatch.batch` 传入。
- 后端只扩展 Dashboard 详情所需读模型，不改变上传或回调业务流程。

**Non-Goals:**
- 不新增单独的接入信息路由。
- 不支持编辑回调地址、幂等值或 metadata。
- 不改变 `idempotency_key` 允许重复上传并原样透传的语义。
- 不改变 callback job 面板中已有的投递结果展示。

## Decisions

- 后端在 `DashboardRowAssembler.batchRow(...)` 输出中增加三个字段，复用已有 `Batch` 领域对象值。
- 前端扩展 `BatchRow` 类型，保持字段名与 API JSON 一致：`callback_url`、`idempotency_key`、`metadata`。
- 新增 `BatchIntakeInfoPanel.vue` 作为独立展示组件，职责仅为渲染批次接入入参。
- `BatchDetailView.vue` 只负责把 `selectedBatch.batch` 传给新组件，继续保持路由级组合层定位。
- `metadata` 在组件内格式化为稳定 JSON 文本；空对象展示“无 meta 信息”，空字符串展示“未提供”。

## Testing

- 后端增加 Dashboard 批次详情契约测试，先验证详情响应包含三项接入字段。
- 前端增加组件测试，验证回调地址、幂等值、格式化 metadata 和空值占位。
- 运行后端 focused test、前端 focused test，以及相关 broader verification。

## Risks / Trade-offs

- `metadata` 可能较长，面板需要允许换行和滚动，避免撑破详情页。
- 旧批次可能没有回调地址或幂等值，前端必须有稳定空值展示。
