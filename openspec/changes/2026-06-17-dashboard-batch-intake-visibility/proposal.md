## Why

运维人员在排查第三方上传、回调投递和幂等透传问题时，需要同时看到批次创建时传入的 `callback_url`、`idempotency_key` 和 `metadata`。当前批次详情页只显示处理进度、OCR 路由和回调投递任务，缺少这组三方接入入参，排查时还要改查数据库或调用底层接口。

## What Changes

在 Dashboard 批次详情能力中补充批次接入信息：

- Dashboard 批次详情接口的 `batch` 对象携带 `callback_url`、`idempotency_key` 和 `metadata`。
- 批次详情页在摘要条下方展示一个紧凑的“接入信息”面板。
- 面板以可扫读字段展示回调地址和幂等值，并用等宽 JSON 区域展示 meta 信息。
- 空值展示稳定占位，避免没有回调或没有 meta 的批次出现布局跳动。

## Impact

影响 Dashboard 批次详情读模型和前端批次详情展示；不改变上传、回调投递、幂等语义、数据库结构或外部上传接口。
