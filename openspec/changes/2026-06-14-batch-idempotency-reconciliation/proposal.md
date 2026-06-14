## Why

OpenWebUI（也就是你这里说的 KnowSpace）需要按上传时传入的 `idempotency_key`
主动回查 DocLens-j 的批次和文档状态，避免回调丢失、任务卡死或服务
重启后文件一直停留在处理中。

## What Changes

在现有 OCR 查询层新增 `GET /api/v1/batches/by-idempotency-key/{idempotencyKey}`，
复用 `BatchRepository.findByIdempotencyKey(...)` 和 `DocumentJobRepository.listByBatchId(...)`
返回一份可直接用于对账的批次快照。

该接口只读，不修改任何任务状态；找不到批次时返回稳定的 404 载荷。

## Capabilities

### New Capabilities
- `batch-idempotency-reconciliation`: provides a batch snapshot lookup by `idempotency_key`.

### Modified Capabilities
- `service-heartbeat`: remains the liveness probe already implemented.

## Impact

Backend query controller/service code, contract tests, and API documentation for
batch reconciliation.
