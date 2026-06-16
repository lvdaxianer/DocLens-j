## Why

OpenWebUI（也就是你这里说的 KnowSpace）会在上传时传入 `idempotency_key`，
但这个字段属于第三方系统自己的幂等判断依据。DocLens-j 只负责保存并在
OCR 完成回调中原样透传，不能因为同一个 `idempotency_key` 已经出现过就拒绝
新的上传。

当前实现同时在应用层和数据库层把 `idempotency_key` 当成唯一键，重复上传会
返回 `409 duplicate idempotency key`。这会阻断第三方系统自己的幂等处理，
也会让用户误以为 OCR 上传失败。

## What Changes

调整 `idempotency_key` 的产品语义：

- 上传创建批次时允许重复的 `idempotency_key`。
- DocLens-j 保存收到的 `idempotency_key`，并在完成回调 payload 里原样透传。
- 移除数据库对 `ocr_batches.idempotency_key` 的唯一约束。
- 现有 `GET /api/v1/batches/by-idempotency-key/{idempotencyKey}` 只作为兼容性
  回查接口保留；如果存在多条匹配记录，返回最新一条批次快照，不再表示该键唯一。

## Capabilities

### New Capabilities
- `batch-idempotency-reconciliation`: provides a best-effort batch snapshot lookup by `idempotency_key`.

### Modified Capabilities
- `callback-delivery`: callback payloads preserve the uploaded `idempotency_key`.
- `dashboard-upload-errors`: structured upload errors remain visible, but duplicate
  `idempotency_key` is no longer a DocLens-j upload conflict.

## Impact

Backend ingestion, persistence migration, query compatibility behavior, callback
payload assertions, dashboard copy/tests, and API/integration documentation.
