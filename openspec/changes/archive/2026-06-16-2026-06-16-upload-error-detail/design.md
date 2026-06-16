## Design

在 `doclens-dashboard/src/api/upload.ts` 内新增小型错误解析函数：

- 非成功响应先读取 `response.text()`，避免 JSON 解析异常覆盖原始 HTTP 错误。
- 尝试将响应体解析为对象，若存在非空字符串 `detail`，抛出该 detail。
- 解析失败、空 body 或无 detail 时，继续抛出现有 `上传失败：<status> <statusText>`。

状态流保持不变：`uploadBatch` 抛出的 `Error.message` 继续由 `useUploadStore.submitBatch` 写入 `uploadState.error`，页面现有 `NAlert` 直接展示。
