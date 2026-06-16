## Why

上传批次失败时，后端已经返回稳定错误体，例如 `{"detail":"duplicate idempotency key"}`，但 Dashboard 上传页只显示 `上传失败：409 Conflict`。用户无法直接看到真正失败原因，尤其是重复幂等键时会误以为上传链路坏了。

## What Changes

Dashboard 上传 API 在非 2xx 响应时读取 JSON 响应体中的 `detail` 字段，并优先把该 detail 作为页面错误提示。若响应体不是 JSON 或没有 detail，则保留现有 HTTP 状态兜底文案。

## Impact

只影响 `doclens-dashboard` 上传失败提示。后端 409 语义、幂等键逻辑、上传表单字段、成功跳转和批次创建流程不变。
