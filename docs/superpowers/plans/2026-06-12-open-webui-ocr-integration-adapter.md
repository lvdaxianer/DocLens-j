# Open WebUI OCR Integration Adapter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Open WebUI 增加独立 OCR integration adapter，让 Open WebUI 通过隔离路径调用 DocLens-j OCR，同时保留用户、文件、知识库来源与内部鉴权契约。

**Architecture:** 新增 server 层 `integration.interfaces` 包，adapter 只做协议转换、内部 token 校验、Open WebUI metadata 校验和响应整形；OCR 创建、查询、结果、事件、重试全部继续调用现有 `DocLensEngine`。原生 `/api/v1/batches` 与 `/api/v1/documents/*` 不承载 Open WebUI 专属规则，后续接 Dify/FastGPT 等只新增自己的 integration adapter。

**Tech Stack:** Java 21, Spring Boot 3, MockMvc, JUnit 5, Jackson, DocLensEngine SDK.

---

## Design Decisions

- Open WebUI 适配路径使用隔离前缀：`/api/v1/integrations/open-webui/ocr`。
- Open WebUI adapter 只做协议适配，不做 OCR 模型、并发、LLM 后处理等业务决策。
- 内部鉴权由 `DocLensOpenWebuiIntegrationProperties` 提供 token，配置键为 `doclens.integrations.open-webui.internal-token`。
- 为兼容本地开发，token 为空时视为未配置，所有 Open WebUI adapter 接口返回 401；生产必须显式配置。
- metadata 必填字段：`source=open-webui`、`openwebui_user_id`、`openwebui_file_id`、`openwebui_knowledge_id`、`openwebui_request_id`。
- 请求头 `X-OpenWebUI-User-Id` 与 metadata 的 `openwebui_user_id` 必须一致，`X-OpenWebUI-Request-Id` 与 metadata 的 request id 必须一致。
- 幂等键格式固定为 `openwebui:file:<openwebui_file_id>:hash:<sha256>`，adapter 校验前缀和 file id 匹配。
- 查询、结果、事件、重试响应按 `docs/integrations/open-webui-ocr-contract.md` 转换字段名；找不到或未完成等业务错误返回契约里的 `code/message/details`。
- adapter 不记录 API token、文件内容、OCR 原文、LLM 原文。

## File Structure

- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/integration/interfaces/OpenWebuiIntegrationProperties.java`
  - 绑定 `doclens.integrations.open-webui.internal-token`。
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/integration/interfaces/OpenWebuiIdentity.java`
  - 保存 Open WebUI 请求头身份。
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/integration/interfaces/OpenWebuiMetadata.java`
  - 保存并校验 Open WebUI metadata。
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/integration/interfaces/OpenWebuiIntegrationException.java`
  - 携带 HTTP 状态与契约错误 code。
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/integration/interfaces/OpenWebuiAuthGuard.java`
  - 校验 Authorization 与请求头身份。
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/integration/interfaces/OpenWebuiMetadataMapper.java`
  - 解析 metadata JSON、校验必填字段和幂等键。
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/integration/interfaces/OpenWebuiResponseMapper.java`
  - 将 DocLens 原生 Map 响应转换为 Open WebUI 契约响应。
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/integration/interfaces/OpenWebuiOcrIntegrationController.java`
  - 暴露 Open WebUI OCR adapter endpoints。
- Create: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/OpenWebuiOcrIntegrationContractTest.java`
  - 覆盖鉴权、创建、查询、结果、事件、重试、健康契约。

---

## Task 1: Open WebUI 鉴权与创建批次适配

**Files:**
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/integration/interfaces/OpenWebuiIntegrationProperties.java`
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/integration/interfaces/OpenWebuiIdentity.java`
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/integration/interfaces/OpenWebuiMetadata.java`
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/integration/interfaces/OpenWebuiIntegrationException.java`
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/integration/interfaces/OpenWebuiAuthGuard.java`
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/integration/interfaces/OpenWebuiMetadataMapper.java`
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/integration/interfaces/OpenWebuiResponseMapper.java`
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/integration/interfaces/OpenWebuiOcrIntegrationController.java`
- Create: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/OpenWebuiOcrIntegrationContractTest.java`

- [x] **Step 1: Write RED unauthorized test**

Add `OpenWebuiOcrIntegrationContractTest` with `@SpringBootTest`, `@AutoConfigureMockMvc`, isolated H2/storage properties, and:

```java
@Test
void openWebuiCreateBatchRejectsMissingInternalToken() throws Exception {
    mockMvc.perform(multipart("/api/v1/integrations/open-webui/ocr/batches")
                    .file(openwebuiFile())
                    .param("metadata", openwebuiMetadata())
                    .param("idempotency_key", openwebuiIdempotencyKey())
                    .param("pdf_mode", "page_image_fallback"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED_INTERNAL_CALLER"))
            .andExpect(jsonPath("$.message").value("unauthorized internal caller"))
            .andExpect(jsonPath("$.details").isMap());
}
```

- [x] **Step 2: Write RED create success test**

Add:

```java
@Test
void openWebuiCreateBatchMapsIdentityMetadataAndFiles() throws Exception {
    MvcResult created = mockMvc.perform(multipart("/api/v1/integrations/open-webui/ocr/batches")
                    .file(openwebuiFile())
                    .header("Authorization", "Bearer test-openwebui-token")
                    .header("X-OpenWebUI-User-Id", "user_123")
                    .header("X-OpenWebUI-User-Email", "user@example.com")
                    .header("X-OpenWebUI-User-Role", "admin")
                    .header("X-OpenWebUI-Request-Id", "req_123")
                    .param("metadata", openwebuiMetadata())
                    .param("idempotency_key", openwebuiIdempotencyKey())
                    .param("pdf_mode", "page_image_fallback"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.batch_id").value(org.hamcrest.Matchers.startsWith("batch_")))
            .andExpect(jsonPath("$.status").value("queued"))
            .andExpect(jsonPath("$.documents[0].document_id").value(org.hamcrest.Matchers.startsWith("doc_")))
            .andExpect(jsonPath("$.documents[0].filename").value("demo.pdf"))
            .andExpect(jsonPath("$.documents[0].content_type").value("application/pdf"))
            .andExpect(jsonPath("$.documents[0].status").value("queued"))
            .andReturn();
    JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
    assertThat(body.get("created_at").asText()).isNotBlank();
}
```

Set `doclens.integrations.open-webui.internal-token=test-openwebui-token` in `DynamicPropertySource`.

- [x] **Step 3: Run RED**

Run:

```bash
mvn -pl doclens-server -am -Dtest=OpenWebuiOcrIntegrationContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because Open WebUI controller and support classes do not exist.

- [x] **Step 4: Implement minimal auth, metadata, create adapter**

Implementation requirements:

- `OpenWebuiIntegrationProperties` is annotated with `@ConfigurationProperties(prefix = "doclens.integrations.open-webui")` and `@Component`.
- `OpenWebuiAuthGuard.requireIdentity(HttpServletRequest request)` checks:
  - `Authorization` equals `Bearer ` + configured token.
  - token must be configured and nonblank.
  - `X-OpenWebUI-User-Id` and `X-OpenWebUI-Request-Id` are nonblank.
- `OpenWebuiMetadataMapper.toRequest(...)` parses JSON into `Map<String,Object>`, validates required fields, validates `source=open-webui`, validates idempotency key starts with `openwebui:file:<openwebui_file_id>:hash:`.
- `OpenWebuiOcrIntegrationController.createBatch(...)` builds `CreateBatchRequest` directly from multipart files and mapped metadata, then calls `DocLensEngine.createBatch`.
- `OpenWebuiResponseMapper.createdBatch(...)` maps native `file_name` to `filename`; for content type infer by filename suffix minimally: `.pdf -> application/pdf`, `.md -> text/markdown`, `.txt -> text/plain`, `.png -> image/png`, `.jpg/.jpeg -> image/jpeg`, otherwise `application/octet-stream`.
- `@ExceptionHandler(OpenWebuiIntegrationException.class)` in the controller returns `Map.of("code", code, "message", message, "details", details)` with the exception status.

- [x] **Step 5: Run GREEN and focused broader verification**

Run:

```bash
mvn -pl doclens-server -am -Dtest=OpenWebuiOcrIntegrationContractTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl doclens-server -am -DskipTests compile
```

Expected: PASS.

- [x] **Step 6: code-review-spec gate**

Check Task 1 diff against canonical code-review-spec:

- New Java classes and methods have Javadoc with `@author lvdaxianerplus` and `@date 2026-06-12`.
- No log prints token, file bytes, OCR text, or metadata secrets.
- No new public method has 6+ parameters.
- No magic strings repeated in multiple classes without constants.
- Controller stays focused; mapping logic lives in mapper classes.

- [x] **Step 7: Mark task complete and commit**

Commit subject:

```text
✨ feat(integration): 支持 Open WebUI 批次创建适配
```

---

## Task 2: Open WebUI 查询、结果、事件、重试、健康响应适配

**Files:**
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/integration/interfaces/OpenWebuiOcrIntegrationController.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/integration/interfaces/OpenWebuiResponseMapper.java`
- Modify: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/OpenWebuiOcrIntegrationContractTest.java`

- [x] **Step 1: Write RED query chain test**

Add one contract test that creates a batch, waits for completion through adapter status endpoint, then asserts:

```java
mockMvc.perform(get("/api/v1/integrations/open-webui/ocr/batches/{batchId}", batchId)
                .header("Authorization", "Bearer test-openwebui-token")
                .header("X-OpenWebUI-User-Id", "user_123")
                .header("X-OpenWebUI-Request-Id", "req_123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.batch_id").value(batchId))
        .andExpect(jsonPath("$.status").value("completed"))
        .andExpect(jsonPath("$.total_documents").value(1))
        .andExpect(jsonPath("$.completed_documents").value(1))
        .andExpect(jsonPath("$.documents[0].filename").value("demo.pdf"));

mockMvc.perform(get("/api/v1/integrations/open-webui/ocr/documents/{documentId}", documentId)
                .header("Authorization", "Bearer test-openwebui-token")
                .header("X-OpenWebUI-User-Id", "user_123")
                .header("X-OpenWebUI-Request-Id", "req_123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.document_id").value(documentId))
        .andExpect(jsonPath("$.filename").value("demo.pdf"))
        .andExpect(jsonPath("$.content_type").value("application/pdf"))
        .andExpect(jsonPath("$.page_count").value(1))
        .andExpect(jsonPath("$.completed_pages").value(1));
```

- [x] **Step 2: Write RED result/events/retry/health tests**

Add assertions:

```java
mockMvc.perform(get("/api/v1/integrations/open-webui/ocr/documents/{documentId}/result", documentId)
                .header("Authorization", "Bearer test-openwebui-token")
                .header("X-OpenWebUI-User-Id", "user_123")
                .header("X-OpenWebUI-Request-Id", "req_123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content_type").value("text/markdown"))
        .andExpect(jsonPath("$.text").value("# Demo\ncontent"))
        .andExpect(jsonPath("$.metadata.openwebui_user_id").value("user_123"));

mockMvc.perform(get("/api/v1/integrations/open-webui/ocr/batches/{batchId}/events", batchId)
                .header("Authorization", "Bearer test-openwebui-token")
                .header("X-OpenWebUI-User-Id", "user_123")
                .header("X-OpenWebUI-Request-Id", "req_123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.events").isArray());

mockMvc.perform(get("/api/v1/integrations/open-webui/ocr/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.service").value("doclens-j"));
```

For retry, insert or create a failed document using existing support if needed and assert adapter returns `status=queued`, `stage=QUEUED`, null error fields.

- [x] **Step 3: Run RED**

Run:

```bash
mvn -pl doclens-server -am -Dtest=OpenWebuiOcrIntegrationContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because only create endpoint exists.

- [x] **Step 4: Implement adapter endpoints**

Add endpoints under `/api/v1/integrations/open-webui/ocr`:

- `GET /batches/{batchId}` -> `responseMapper.batch(engine.getBatch(batchId))`
- `GET /documents/{documentId}` -> `responseMapper.document(engine.getDocument(documentId))`
- `GET /documents/{documentId}/result` -> `responseMapper.result(engine.getDocumentResult(documentId), engine.getDocument(documentId))`
- `GET /batches/{batchId}/events` -> `responseMapper.events(engine.getEvents(batchId))`
- `POST /documents/{documentId}/retry` -> `responseMapper.retry(engine.retryDocument(documentId))`
- `GET /health` -> `Map.of("status","UP","service","doclens-j","time", OffsetDateTime.now().toString())`

Response mapping rules:

- `total_documents = total_files`, `completed_documents = completed_files`, `failed_documents = failed_files`.
- Batch documents can be absent in native batch response; if unavailable, return `documents=[]` for batch status.
- Document `stage` is uppercase, `page_count=total_pages`, `completed_pages=current_page` when completed or processing, `failed_pages=1` only when status failed.
- Result `text = result.finalText`, `page_text` maps page list index to strings when native page text is a list; otherwise stringify values by page key.
- Metadata is copied from document native metadata.
- ResourceNotFoundException is allowed to use existing global 404 for now unless Open WebUI contract requires specific code in a later task.

- [x] **Step 5: Run GREEN and broader server contracts**

Run:

```bash
mvn -pl doclens-server -am -Dtest=OpenWebuiOcrIntegrationContractTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl doclens-server -am -Dtest=DocLensOcrApiContractTest,DocLensOcrUploadApiContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

- [x] **Step 6: code-review-spec gate**

Check Task 2 diff:

- Response mapper methods are small and documented.
- No loop does remote/database calls; all engine calls happen outside loops.
- Open WebUI headers are required on protected endpoints except `/health`.
- No sensitive content in logs.

- [x] **Step 7: Mark task complete and commit**

Commit subject:

```text
✨ feat(integration): 补齐 Open WebUI 查询适配
```

---

## Task 3: 文档契约对齐与最终验证

**Files:**
- Modify: `docs/integrations/open-webui-ocr-contract.md`
- Modify: `docs/superpowers/plans/2026-06-12-open-webui-ocr-integration-adapter.md`

- [ ] **Step 1: Update contract path section**

Update the integration contract to document adapter-prefixed paths while preserving the original native endpoint semantics note:

```text
Open WebUI should call /api/v1/integrations/open-webui/ocr/*.
DocLens native /api/v1/* endpoints remain available for first-party callers.
```

- [ ] **Step 2: Run documentation diff check**

Run:

```bash
git diff -- docs/integrations/open-webui-ocr-contract.md docs/superpowers/plans/2026-06-12-open-webui-ocr-integration-adapter.md
git diff --check
```

Expected: PASS.

- [ ] **Step 3: Full verification**

Run:

```bash
mvn -pl doclens-server -am test
git diff --check
```

Expected: PASS.

- [ ] **Step 4: Manual smoke with running dev services**

Run:

```bash
./scripts/dev-up.sh
curl --silent --fail http://127.0.0.1:10003/api/v1/integrations/open-webui/ocr/health
curl --silent --fail -X POST http://127.0.0.1:10003/api/v1/integrations/open-webui/ocr/batches
```

Expected:

- Health returns `status=UP`.
- Missing token create call returns 401 JSON with `UNAUTHORIZED_INTERNAL_CALLER`.

- [ ] **Step 5: code-review-spec final gate**

Review full change:

- Integration adapter is isolated from native API.
- Token and user headers are checked before state-changing Open WebUI calls.
- Metadata and idempotency key rules match contract.
- Contract doc and tests agree on endpoint prefix.
- No unrelated dirty files are staged.

- [ ] **Step 6: Mark task complete and commit**

Commit subject:

```text
📝 docs(integration): 对齐 Open WebUI 适配契约
```

---

## Final Audit

- [ ] **Step 1: Checklist consistency**

Confirm every task checkbox is complete or explicitly documented as not applicable.

- [ ] **Step 2: Final status check**

Run:

```bash
git status --short
git log --oneline -5
```

Expected:

- Only pre-existing unrelated dirty files remain outside the committed Open WebUI adapter work.
- Latest commits include planning commit and task commits.

## Self-Review

Spec coverage:

- Open WebUI create batch endpoint is covered by Task 1.
- Internal bearer token and user/request headers are covered by Task 1.
- metadata and idempotency rules are covered by Task 1.
- Batch/document/result/events/retry/health endpoints are covered by Task 2.
- Adapter isolation and path contract are covered by Tasks 1 through 3.

Placeholder scan:

- No `TBD`, `TODO`, or unspecified handler step remains.

Type consistency:

- Open WebUI fields use `openwebui_*` in metadata and `X-OpenWebUI-*` in headers.
- Adapter paths consistently use `/api/v1/integrations/open-webui/ocr`.
- Native DocLens response maps remain `Map<String,Object>` from `DocLensEngine` and are converted only in `OpenWebuiResponseMapper`.
