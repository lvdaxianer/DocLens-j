# DocLens-j 对 Open WebUI 的接口与用户区分契约

本文只定义两件事：

1. DocLens-j 需要给 Open WebUI 提供什么接口。
2. DocLens-j 怎么区分 Open WebUI 的不同用户、文件和知识库来源。

## 1. 提供什么接口

Open WebUI 必须调用独立适配器前缀 `/api/v1/integrations/open-webui/ocr/*`。
DocLens 原生 `/api/v1/*` 接口仍保留给一方调用方使用，不承载 Open WebUI 专属鉴权与 metadata 规则。

### 1.1 创建 OCR 批次

```http
POST /api/v1/integrations/open-webui/ocr/batches
Content-Type: multipart/form-data
```

请求表单：

```text
files=@demo.pdf
metadata={"source":"open-webui","openwebui_user_id":"user_xxx","openwebui_file_id":"file_xxx","openwebui_knowledge_id":"knowledge_xxx","openwebui_request_id":"req_xxx"}
idempotency_key=openwebui:file:file_xxx:hash:abc
pdf_mode=page_image_fallback
```

成功返回：

```json
{
  "batch_id": "batch_xxx",
  "status": "queued",
  "documents": [
    {
      "document_id": "doc_xxx",
      "filename": "demo.pdf",
      "content_type": "application/pdf",
      "status": "queued"
    }
  ],
  "created_at": "2026-06-12T00:00:00Z"
}
```

失败返回：

```json
{
  "code": "BATCH_CREATE_FAILED",
  "message": "创建 OCR 批次失败",
  "details": {}
}
```

### 1.2 查询批次状态

```http
GET /api/v1/integrations/open-webui/ocr/batches/{batchId}
```

处理中返回：

```json
{
  "batch_id": "batch_xxx",
  "status": "processing",
  "total_documents": 1,
  "completed_documents": 0,
  "failed_documents": 0,
  "documents": [
    {
      "document_id": "doc_xxx",
      "filename": "demo.pdf",
      "status": "processing",
      "stage": "OCR",
      "page_count": 10,
      "completed_pages": 4,
      "failed_pages": 0
    }
  ],
  "updated_at": "2026-06-12T00:01:00Z"
}
```

完成返回：

```json
{
  "batch_id": "batch_xxx",
  "status": "completed",
  "total_documents": 1,
  "completed_documents": 1,
  "failed_documents": 0,
  "documents": [
    {
      "document_id": "doc_xxx",
      "filename": "demo.pdf",
      "status": "completed",
      "stage": "SAVE_TEXT",
      "page_count": 10,
      "completed_pages": 10,
      "failed_pages": 0,
      "result_id": "result_xxx"
    }
  ],
  "updated_at": "2026-06-12T00:02:00Z"
}
```

### 1.3 查询文档状态

```http
GET /api/v1/integrations/open-webui/ocr/documents/{documentId}
```

成功返回：

```json
{
  "document_id": "doc_xxx",
  "batch_id": "batch_xxx",
  "filename": "demo.pdf",
  "content_type": "application/pdf",
  "status": "processing",
  "stage": "OCR",
  "page_count": 10,
  "completed_pages": 4,
  "failed_pages": 0,
  "result_id": null,
  "error_code": null,
  "error_message": null,
  "updated_at": "2026-06-12T00:01:00Z"
}
```

失败返回也使用 200 响应体表达文档业务状态：

```json
{
  "document_id": "doc_xxx",
  "batch_id": "batch_xxx",
  "filename": "demo.pdf",
  "status": "failed",
  "stage": "OCR",
  "error_code": "OCR_FAILED",
  "error_message": "OCR 调用失败",
  "updated_at": "2026-06-12T00:02:00Z"
}
```

### 1.4 获取文档 OCR 结果

```http
GET /api/v1/integrations/open-webui/ocr/documents/{documentId}/result
```

成功返回：

```json
{
  "document_id": "doc_xxx",
  "batch_id": "batch_xxx",
  "result_id": "result_xxx",
  "filename": "demo.pdf",
  "content_type": "text/markdown",
  "text": "# OCR Markdown\n\n正文内容",
  "page_text": {
    "1": "第 1 页文本",
    "2": "第 2 页文本"
  },
  "metadata": {
    "source": "open-webui",
    "openwebui_user_id": "user_xxx",
    "openwebui_file_id": "file_xxx",
    "openwebui_knowledge_id": "knowledge_xxx",
    "openwebui_request_id": "req_xxx"
  },
  "created_at": "2026-06-12T00:02:00Z"
}
```

未完成返回：

```json
{
  "code": "DOCUMENT_NOT_COMPLETED",
  "message": "文档尚未完成 OCR",
  "details": {
    "document_id": "doc_xxx",
    "status": "processing"
  }
}
```

失败返回：

```json
{
  "code": "DOCUMENT_FAILED",
  "message": "文档 OCR 失败",
  "details": {
    "document_id": "doc_xxx",
    "error_code": "OCR_FAILED",
    "error_message": "OCR 调用失败"
  }
}
```

### 1.5 查询批次事件

```http
GET /api/v1/integrations/open-webui/ocr/batches/{batchId}/events
```

成功返回：

```json
{
  "batch_id": "batch_xxx",
  "events": [
    {
      "event_id": "event_xxx",
      "document_id": "doc_xxx",
      "type": "DOCUMENT_STARTED",
      "stage": "OCR",
      "message": "文档开始 OCR",
      "created_at": "2026-06-12T00:00:10Z"
    }
  ]
}
```

### 1.6 重试文档 OCR

```http
POST /api/v1/integrations/open-webui/ocr/documents/{documentId}/retry
```

成功返回：

```json
{
  "document_id": "doc_xxx",
  "batch_id": "batch_xxx",
  "status": "queued",
  "stage": "QUEUED",
  "error_code": null,
  "error_message": null,
  "updated_at": "2026-06-12T00:03:00Z"
}
```

### 1.7 健康检查

```http
GET /api/v1/integrations/open-webui/ocr/health
```

成功返回：

```json
{
  "status": "UP",
  "service": "doclens-j",
  "time": "2026-06-12T00:00:00Z"
}
```

## 2. 怎么区分用户

### 2.1 请求头

Open WebUI 调 DocLens-j 时必须是服务端到服务端调用，并携带：

```http
Authorization: Bearer <DOCLENS_INTERNAL_TOKEN>
X-OpenWebUI-User-Id: user_xxx
X-OpenWebUI-User-Email: user@example.com
X-OpenWebUI-User-Role: admin
X-OpenWebUI-Request-Id: req_xxx
```

DocLens-j 必须先校验：

```text
Authorization == Bearer <DOCLENS_INTERNAL_TOKEN>
```

未授权返回：

```json
{
  "code": "UNAUTHORIZED_INTERNAL_CALLER",
  "message": "unauthorized internal caller",
  "details": {}
}
```

### 2.2 metadata 用户字段

Open WebUI 创建 OCR 批次时，`metadata` 必须包含：

```json
{
  "source": "open-webui",
  "openwebui_user_id": "user_xxx",
  "openwebui_file_id": "file_xxx",
  "openwebui_knowledge_id": "knowledge_xxx",
  "openwebui_request_id": "req_xxx"
}
```

DocLens-j 使用这些字段做归属记录和排障定位。

### 2.3 idempotency_key 规则

DocLens-j 使用 `idempotency_key` 区分同一个 Open WebUI 文件的重复提交。

格式：

```text
openwebui:file:<openwebui_file_id>:hash:<sha256>
```

示例：

```text
openwebui:file:file_abc123:hash:9f86d081...
```

当 Open WebUI 需要对账某个文件时，可以直接通过该幂等键回查批次：

```http
GET /api/v1/batches/by-idempotency-key/{idempotencyKey}
```

该查询接口只读，返回的 batch snapshot 会包含 `documents` 数组，便于 Open WebUI
在回调丢失、重启恢复或人工重试时继续对账。

### 2.4 查询归属

DocLens-j 返回 batch、document、result 时，需要带回原始 metadata：

```json
{
  "metadata": {
    "source": "open-webui",
    "openwebui_user_id": "user_xxx",
    "openwebui_file_id": "file_xxx",
    "openwebui_knowledge_id": "knowledge_xxx",
    "openwebui_request_id": "req_xxx"
  }
}
```

Open WebUI 通过这些字段把 DocLens-j 的 batch/document/result 映射回自己的用户、文件和知识库。
