# Ollama OCR Affinity And Multi LLM Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 支持 Ollama DeepSeek-OCR 节点、让 PaddleOCR 输出 Markdown 格式，并为 OCR 增加文档级模型亲和力；随后把单例 LLM Markdown 配置演进为多 LLM 配置治理，且无 LLM 配置时不影响 OCR 主流程。

**Architecture:** 分成两个独立 change 执行。Change 1 聚焦 OCR：新增 Ollama OCR 协议客户端，将 OCR 结果统一标记为 Markdown，并在 OCR 路由层按 `documentId -> modelKey` 建立运行时亲和力，保证同一文档所有页只使用同类 OCR。Change 2 聚焦 LLM：把现有单例 `LlmMarkdownConfig` 迁移为多配置模型，通过用途、默认项、优先级和健康状态选择可用 LLM；找不到 LLM 时直接透传 OCR Markdown，不失败、不阻塞。

**Tech Stack:** Java 21, Spring Boot 3, MyBatis-Plus, Flyway, Java HTTP Client, JUnit 5, AssertJ, Vue 3, Naive UI, Vitest.

---

## Scope Split

本计划必须按以下顺序拆分执行和提交：

1. Change 1: `Ollama OCR + Paddle Markdown + OCR document affinity`
2. Change 2: `Multi LLM config governance`

不要把两个 change 合到同一个提交里。OCR 节点调度和 LLM 后处理治理属于两个边界，混在一起会导致测试、回滚和 code review 都变重。

## Non-Negotiable Requirements

- Ollama DeepSeek-OCR 默认固定 prompt：

```text
<|grounding|>Convert the document to markdown.
```

- 不开放自由 prompt 输入。DeepSeek-OCR 对换行和标点敏感，后续如需扩展只能做后端预设或前端下拉。
- PaddleOCR 也必须输出 Markdown 格式，即使底层只返回 `rec_texts`，也要稳定包装为 Markdown 文本。
- 同一文档必须使用同类 OCR 解析。亲和力默认锁 `modelKey`，不是锁 `nodeId`。
- 同一个 `modelKey` 下可以使用多个节点并发处理不同页，避免浪费节点并发。
- PDF/Word 多页并发 OCR 后，最终结果必须继续按 `pageNo` 排序聚合，不能乱序。
- 结果抽屉里的 Markdown 内容和 OCR 原内容必须用左右 Tab 切换展示，不能上下堆叠排列。
- 没有任何 LLM 配置时，OCR 主流程不能失败、不能阻塞、不能把文档标失败。
- LLM 健康检查在没有配置时空跑，不刷错误日志。
- 日志不得输出 API Key、图片 base64、OCR 原文、LLM 原文。

---

## Change 1 File Structure

- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OllamaOcrClient.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OllamaOcrResponseMapper.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrDocumentAffinityTracker.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/InMemoryOcrDocumentAffinityTracker.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/ImageOcrResult.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrNativeResponseMapper.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrNodeImageExecutor.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingDependencies.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingService.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrDispatchCoordinator.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrPendingRequest.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrNodeSelector.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/WeightedCapacityOcrNodeSelector.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrHealthClient.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/RoutingOcrHealthClient.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensPaddleOcrAutoConfiguration.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensOcrResourceAutoConfiguration.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrModelResponse.java`
- Modify: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/OcrNodeApiContractTest.java`
- Modify: `doclens-dashboard/src/components/dashboard/DocumentResultDrawer.vue`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OllamaOcrClientTest.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OllamaOcrResponseMapperTest.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrNativeResponseMapperTest.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrDocumentAffinityTrackerTest.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingDocumentAffinityTest.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrNodeImageExecutorTest.java`
- Test: `doclens-dashboard/src/components/dashboard/__tests__/DocumentResultDrawer.test.ts`

---

### Task 1: 支持 Ollama DeepSeek-OCR 协议

**Files:**
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OllamaOcrClient.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OllamaOcrResponseMapper.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrNodeImageExecutor.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensPaddleOcrAutoConfiguration.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OllamaOcrClientTest.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OllamaOcrResponseMapperTest.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrNodeImageExecutorTest.java`

- [x] **Step 1: Write RED client test**

Add `OllamaOcrClientTest` with a local HTTP server. The test must assert the request body contains the fixed prompt, node provider model, a base64 image, and `stream=false`.

```java
@Test
void sendsDeepSeekOcrGenerateRequestWithFixedMarkdownPrompt() {
    // Arrange: mock server captures request body and returns {"response":"# 发票\n金额：100","done":true}
    // Act: client.recognizeImage(runtimeNode, request)
    // Assert:
    // - path is /api/generate
    // - model is "deepseek-ocr:latest"
    // - prompt is "<|grounding|>Convert the document to markdown."
    // - images has exactly one base64 item
    // - stream is false
}
```

- [x] **Step 2: Run RED**

Run:

```bash
mvn -pl doclens-spring-boot-starter -Dtest=OllamaOcrClientTest test
```

Expected: FAIL because Ollama client does not exist.

- [x] **Step 3: Implement Ollama client and response mapper**

Implementation requirements:

- Endpoint: `http://{host}:{port}/api/generate`
- Model: `runtimeNode.node().providerModel().orElse(runtimeNode.node().modelKey())`
- Prompt constant: `<|grounding|>Convert the document to markdown.`
- Image content encoded with `Base64.getEncoder().encodeToString(request.imageContent())`
- Request timeout uses existing OCR request timeout configuration.
- Response mapper reads `response` as Markdown.
- `ImageOcrResult.rawOutput` includes `ocr_provider=ollama`, `ocr_format=markdown`, `ocr_model=<model>`.
- Empty response throws `IllegalStateException("Ollama OCR returned empty response")`.

- [x] **Step 4: Route Ollama nodes from node executor**

In `PaddleOcrNodeImageExecutor.recognizeByDeployment`, add a narrow branch:

- If `runtimeNode.node().channelKey()` equals `ollama` or `runtimeNode.node().modelKey()` starts with `ollama`, call `OllamaOcrClient`.
- Existing `ONLINE` branch still uses `DashScopeOnlineOcrClient`.
- Existing Paddle branch remains default for `paddle_ocr`.

If the file becomes too broad, rename in a later refactor task. Do not combine broad rename with the first protocol task.

- [x] **Step 5: Run GREEN and broader compile**

Run:

```bash
mvn -pl doclens-spring-boot-starter -Dtest=OllamaOcrClientTest,OllamaOcrResponseMapperTest,PaddleOcrNodeImageExecutorTest test
mvn -pl doclens-spring-boot-starter -DskipTests compile
```

Expected: PASS.

- [x] **Step 6: Commit Task 1**

Run code-review-spec before committing.

Commit subject:

```text
feat(ocr): 支持 Ollama DeepSeek OCR 协议
```

---

### Task 2: 让 PaddleOCR 输出 Markdown 格式

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/ImageOcrResult.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrNativeResponseMapper.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrNativeResponseMapperTest.java`

- [x] **Step 1: Write RED mapper test**

Add a test where Paddle returns:

```json
{
  "errorCode": 0,
  "errorMsg": "Success",
  "result": {
    "ocrResults": [
      {
        "prunedResult": {
          "rec_texts": ["标题", "第一段", "第二段"],
          "rec_scores": [0.99, 0.98, 0.97],
          "rec_boxes": [],
          "rec_polys": []
        }
      }
    ]
  }
}
```

Expected `result.pageText().get(0).get("text")`:

```markdown
标题

第一段

第二段
```

Expected `rawOutput` contains:

```text
ocr_provider=paddle_ocr
ocr_format=markdown
```

- [x] **Step 2: Run RED**

Run:

```bash
mvn -pl doclens-spring-boot-starter -Dtest=PaddleOcrNativeResponseMapperTest test
```

Expected: FAIL because current mapper joins blocks with single newline and does not mark Markdown format.

- [x] **Step 3: Implement deterministic Markdown wrapper**

Implementation requirements:

- Do not infer headings, tables, or list nesting from Paddle boxes.
- Join non-empty `rec_texts` with blank lines.
- Keep confidence, boxes, polygons, and original response in `rawOutput`.
- Add `ImageOcrResult.fromMarkdownBlocks(...)` or equivalent focused factory to avoid changing unrelated callers.
- Empty Paddle output still returns a warning and empty Markdown string.

- [x] **Step 4: Run GREEN and existing result tests**

Run:

```bash
mvn -pl doclens-spring-boot-starter -Dtest=PaddleOcrNativeResponseMapperTest test
mvn -pl doclens-core -Dtest=BatchProcessingUseCaseLlmMarkdownTest test
```

Expected: PASS.

- [x] **Step 5: Commit Task 2**

Commit subject:

```text
feat(ocr): 统一 PaddleOCR Markdown 输出
```

---

### Task 3: 增加文档级 OCR 模型亲和力

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrDocumentAffinityTracker.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/InMemoryOcrDocumentAffinityTracker.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingDependencies.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingService.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrDispatchCoordinator.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrPendingRequest.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensOcrResourceAutoConfiguration.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrDocumentAffinityTrackerTest.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingDocumentAffinityTest.java`

- [x] **Step 1: Write RED affinity tracker tests**

Test cases:

- First successful dispatch binds `documentId -> modelKey`.
- Later pages for same document return the bound model key.
- Completion releases binding.
- Binding is per document, not per batch.

- [x] **Step 2: Write RED routing test**

Create two healthy model groups:

- `paddle_ocr`: nodes `paddle-1`, `paddle-2`
- `ollama_deepseek_ocr`: nodes `ollama-1`, `ollama-2`

Run two concurrent pages for the same document. Assert:

- Both calls use the same `modelKey`.
- Calls may use different `nodeId` values within the same `modelKey`.

Add another test:

- First page binds `paddle_ocr`.
- All Paddle nodes then fail.
- Second page must not fail over to `ollama_deepseek_ocr`.

- [x] **Step 3: Run RED**

Run:

```bash
mvn -pl doclens-core -Dtest=OcrDocumentAffinityTrackerTest,OcrRoutingDocumentAffinityTest test
```

Expected: FAIL because document affinity does not exist.

- [x] **Step 4: Implement affinity tracker**

Interface:

```java
public interface OcrDocumentAffinityTracker {
    Optional<String> boundModelKey(String documentId);
    String bindIfAbsent(String documentId, String modelKey);
    void release(String documentId);
}
```

Implementation:

- `InMemoryOcrDocumentAffinityTracker` uses `ConcurrentHashMap`.
- Use `computeIfAbsent` for atomic first binding.
- Do not persist affinity initially. It is a runtime routing guard for in-flight documents.

- [x] **Step 5: Apply affinity in routing**

Routing rules:

- Before candidate selection, check `affinityTracker.boundModelKey(request.documentId())`.
- If present, candidate nodes must match that model key.
- If absent, route normally.
- After a node is selected successfully, bind the document to selected `node.modelKey()`.
- On retries and queue re-dispatch, carry the bound model key through pending request selection.
- On document-level extraction completion, release affinity. If release cannot be safely called at document boundary immediately, release in `finally` around image/PDF extraction.

- [x] **Step 6: Run GREEN and concurrency tests**

Run:

```bash
mvn -pl doclens-core -Dtest=OcrDocumentAffinityTrackerTest,OcrRoutingDocumentAffinityTest,OcrRoutingRuntimeHitTest test
mvn -pl doclens-spring-boot-starter -Dtest=PdfImageDocumentExtractorTest,ImageDocumentExtractorTest test
```

Expected: PASS.

- [x] **Step 7: Commit Task 3**

Commit subject:

```text
feat(ocr): 增加文档级模型亲和力
```

---

### Task 4: Ollama OCR 健康检查和前端模型展示

**Files:**
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrHealthClient.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/RoutingOcrHealthClient.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrModelResponse.java`
- Modify: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/OcrNodeApiContractTest.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/RoutingOcrHealthClientTest.java`

- [x] **Step 1: Write RED health routing test**

Add test:

- `channelKey=ollama` or `modelKey=ollama_deepseek_ocr` calls Ollama health probe.
- Probe body uses the fixed Markdown prompt and 1x1 image.
- HTTP 200 with parseable response means healthy.

- [x] **Step 2: Run RED**

Run:

```bash
mvn -pl doclens-spring-boot-starter -Dtest=RoutingOcrHealthClientTest test
```

Expected: FAIL because Ollama health routing does not exist.

- [x] **Step 3: Implement health probe**

Rules:

- Paddle still checks `/ocr` and `errorCode == 0`.
- Ollama checks `/api/generate`.
- DashScope keeps online permission probe.
- Health logs include node id, model key, and sanitized error only.

- [x] **Step 4: Update supported model API**

Expose Ollama OCR as supported model:

```text
modelKey=ollama_deepseek_ocr
displayName=Ollama DeepSeek OCR
defaultPort=11434
providerModel=deepseek-ocr:latest
channelKey=ollama
```

- [x] **Step 5: Run GREEN and API contract tests**

Run:

```bash
mvn -pl doclens-spring-boot-starter -Dtest=RoutingOcrHealthClientTest test
mvn -pl doclens-server -am -Dtest=OcrNodeApiContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

- [x] **Step 6: Commit Task 4**

Commit subject:

```text
feat(ocr): 增加 Ollama OCR 健康探测
```

---

### Task 5: 结果抽屉改为左右 Tab 切换内容

**Files:**
- Modify: `doclens-dashboard/src/components/dashboard/DocumentResultDrawer.vue`
- Test: `doclens-dashboard/src/components/dashboard/__tests__/DocumentResultDrawer.test.ts`

- [x] **Step 1: Write RED UI test**

Add or update a component test for `DocumentResultDrawer`:

- Render a result with both Markdown content and OCR original content.
- Assert two tabs exist: `Markdown 内容` or `OCR 纯文本`, and `OCR 原内容`.
- Assert the default active tab shows final Markdown/OCR display content.
- Switch to `OCR 原内容` tab and assert original OCR text appears.
- Assert both tabs keep their own copy button and character count.
- Assert the drawer no longer renders two stacked `.document-result__text` sections at the same time.

- [x] **Step 2: Run RED**

Run:

```bash
cd doclens-dashboard && npm run test:ui -- DocumentResultDrawer
```

Expected: FAIL because current UI renders Markdown/OCR final content and OCR original content as two vertical sections.

- [x] **Step 3: Implement Tab layout**

Implementation requirements:

- Use Naive UI `NTabs` and `NTabPane`.
- The first tab label is `textTitle`, so LLM applied shows `Markdown 内容`; fallback shows `OCR 纯文本`.
- The second tab label is `OCR 原内容`.
- Each tab pane contains its own header row with character count and copy button.
- Keep `pre` preview styling and existing copy behavior.
- Do not place Markdown content and OCR original content one above another.
- Empty content still shows the existing empty text for that tab.

- [x] **Step 4: Run GREEN and frontend verification**

Run:

```bash
cd doclens-dashboard && npm run test:ui -- DocumentResultDrawer
cd doclens-dashboard && npm run test:ui && npm run build
```

Expected: PASS.

- [x] **Step 5: Commit Task 5**

Commit subject:

```text
feat(dashboard): 结果抽屉支持内容 Tab 切换
```

---

## Change 1 Final Verification

- [ ] **Step 1: Backend focused verification**

Run:

```bash
mvn -pl doclens-core -Dtest=OcrDocumentAffinityTrackerTest,OcrRoutingDocumentAffinityTest,OcrRoutingRuntimeHitTest test
mvn -pl doclens-spring-boot-starter -Dtest=OllamaOcrClientTest,OllamaOcrResponseMapperTest,PaddleOcrNativeResponseMapperTest,PaddleOcrNodeImageExecutorTest,RoutingOcrHealthClientTest test
mvn -pl doclens-server -am -Dtest=OcrNodeApiContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

- [ ] **Step 2: Frontend focused verification**

Run:

```bash
cd doclens-dashboard && npm run test:ui -- DocumentResultDrawer
cd doclens-dashboard && npm run build
```

Expected: PASS.

- [ ] **Step 3: Broader verification**

Run:

```bash
mvn -pl doclens-core,doclens-spring-boot-starter,doclens-server test
git diff --check
```

Expected: PASS.

- [ ] **Step 4: Manual smoke**

Start backend and create an Ollama node:

```json
{
  "model_key": "ollama_deepseek_ocr",
  "deployment_type": "offline",
  "name": "ollama-deepseek-215",
  "host": "10.100.30.215",
  "port": 11434,
  "channel_key": "ollama",
  "provider_model": "deepseek-ocr:latest",
  "enabled": true,
  "participate_global": true,
  "weight": 100,
  "max_concurrency": 4
}
```

Upload a multi-page PDF or image batch and verify:

- Ollama OCR returns Markdown.
- Paddle OCR returns Markdown.
- Same document pages do not mix `paddle_ocr` and `ollama_deepseek_ocr`.
- Final text order follows page number.
- Result drawer shows Markdown/OCR final content and OCR original content as tabs, not as vertical stacked sections.

---

## Change 2 File Structure

- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/LlmUsageType.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/LlmConfigSelector.java`
- Create: `doclens-spring-boot-starter/src/main/resources/db/migration/V10__doclens_multi_llm_config.sql`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/LlmMarkdownConfig.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/LlmMarkdownConfigRepository.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/LlmMarkdownConfigService.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/LlmMarkdownConfigSettings.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/LlmMarkdownConfigEntity.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusLlmMarkdownConfigRepository.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/LlmMarkdownHealthChecker.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ConfigurableMarkdownPostProcessor.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/processing/interfaces/LlmMarkdownConfigController.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/processing/interfaces/LlmMarkdownConfigRequest.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/processing/interfaces/LlmMarkdownConfigResponse.java`
- Modify: `doclens-dashboard/src/components/ocr/LlmMarkdownConfigPanel.vue`
- Modify: `doclens-dashboard/src/composables/useLlmMarkdownConfig.ts`
- Modify: `doclens-dashboard/src/types/llmMarkdownConfig.ts`
- Modify: `doclens-dashboard/src/utils/llmMarkdownConfigRules.ts`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/LlmConfigSelectorTest.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/LlmMarkdownConfigServiceTest.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ConfigurableMarkdownPostProcessorTest.java`
- Test: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/LlmMarkdownConfigApiContractTest.java`
- Test: `doclens-dashboard/src/utils/__tests__/llmMarkdownConfigRules.test.ts`

---

### Task 5: 持久化多 LLM 配置模型

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/LlmUsageType.java`
- Create: `doclens-spring-boot-starter/src/main/resources/db/migration/V10__doclens_multi_llm_config.sql`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/LlmMarkdownConfig.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/LlmMarkdownConfigRepository.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/LlmMarkdownConfigService.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/LlmMarkdownConfigEntity.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusLlmMarkdownConfigRepository.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/LlmMarkdownConfigServiceTest.java`

- [ ] **Step 1: Write RED service tests**

Test cases:

- Can save two configs with different names.
- Exactly one default config per `usageType`.
- Disabling one config does not disable other configs.
- Existing singleton row migrates to one `MARKDOWN_POST_PROCESSING` default config.

- [ ] **Step 2: Run RED**

Run:

```bash
mvn -pl doclens-core -Dtest=LlmMarkdownConfigServiceTest test
```

Expected: FAIL because repository and domain still assume singleton.

- [ ] **Step 3: Implement domain and migration**

New model fields:

```text
id
name
api_type
url
model
credential_ref or encrypted credential
credential_configured
usage_type
enabled
healthy
health_message
last_health_at
priority
is_default
created_at
updated_at
```

Migration rules:

- Preserve current singleton row values.
- Create one row with `usage_type='MARKDOWN_POST_PROCESSING'`.
- Set `is_default=true`.
- Preserve `enabled`, `healthy`, `health_message`, `last_health_at`.

- [ ] **Step 4: Run GREEN**

Run:

```bash
mvn -pl doclens-core -Dtest=LlmMarkdownConfigServiceTest test
mvn -pl doclens-spring-boot-starter -DskipTests compile
```

Expected: PASS.

- [ ] **Step 5: Commit Task 5**

Commit subject:

```text
feat(llm): 支持多 LLM 配置持久化
```

---

### Task 6: 增加 LLM 选择器和无配置降级

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/LlmConfigSelector.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ConfigurableMarkdownPostProcessor.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/LlmMarkdownHealthChecker.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/LlmConfigSelectorTest.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ConfigurableMarkdownPostProcessorTest.java`

- [ ] **Step 1: Write RED selector tests**

Test cases:

- Select default enabled healthy config for `MARKDOWN_POST_PROCESSING`.
- If default unhealthy, select next enabled healthy config by lower `priority`.
- If no enabled healthy config exists, return empty.
- If no config exists, return empty without exception.

- [ ] **Step 2: Write RED post processor test**

Test:

- Repository has no LLM config.
- `ConfigurableMarkdownPostProcessor.process("OCR markdown")` returns original content.
- Result has `llm_markdown_applied=false`.
- Result reason is `no_available_llm_config`.
- No exception is thrown.

- [ ] **Step 3: Run RED**

Run:

```bash
mvn -pl doclens-core -Dtest=LlmConfigSelectorTest test
mvn -pl doclens-spring-boot-starter -Dtest=ConfigurableMarkdownPostProcessorTest test
```

Expected: FAIL.

- [ ] **Step 4: Implement selector and graceful fallback**

Rules:

- `selector.select(LlmUsageType.MARKDOWN_POST_PROCESSING)` returns `Optional<LlmMarkdownConfig>`.
- `ConfigurableMarkdownPostProcessor` treats empty selection as no-op success.
- No LLM config must not affect OCR, batch status, document status, or result persistence.
- Health checker list is empty when no configs exist and must not log warnings.

- [ ] **Step 5: Run GREEN**

Run:

```bash
mvn -pl doclens-core -Dtest=LlmConfigSelectorTest test
mvn -pl doclens-spring-boot-starter -Dtest=ConfigurableMarkdownPostProcessorTest test
```

Expected: PASS.

- [ ] **Step 6: Commit Task 6**

Commit subject:

```text
feat(llm): 增加配置选择和无配置降级
```

---

### Task 7: 多 LLM 配置 API

**Files:**
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/processing/interfaces/LlmMarkdownConfigController.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/processing/interfaces/LlmMarkdownConfigRequest.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/processing/interfaces/LlmMarkdownConfigResponse.java`
- Test: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/LlmMarkdownConfigApiContractTest.java`

- [ ] **Step 1: Write RED API contract tests**

Endpoints:

```text
GET    /api/v1/llm-markdown-config
POST   /api/v1/llm-markdown-config
PUT    /api/v1/llm-markdown-config/{id}
PATCH  /api/v1/llm-markdown-config/{id}/enabled
PATCH  /api/v1/llm-markdown-config/{id}/default
DELETE /api/v1/llm-markdown-config/{id}
POST   /api/v1/llm-markdown-config/test
```

Backward compatibility:

- Existing `GET /api/v1/llm-markdown-config` may return a list or a compatibility object only if frontend is updated in the same task.
- Prefer a list response for new UI.

- [ ] **Step 2: Run RED**

Run:

```bash
mvn -pl doclens-server -am -Dtest=LlmMarkdownConfigApiContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL.

- [ ] **Step 3: Implement API**

Rules:

- Delete default config is allowed only if no documents depend on it, or require setting a new default first.
- Setting a default clears other defaults under same `usageType`.
- Empty list returns `[]` with HTTP 200.
- Test endpoint can test unsaved config.

- [ ] **Step 4: Run GREEN**

Run:

```bash
mvn -pl doclens-server -am -Dtest=LlmMarkdownConfigApiContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

- [ ] **Step 5: Commit Task 7**

Commit subject:

```text
feat(llm): 提供多配置管理接口
```

---

### Task 8: 多 LLM 配置前端页面

**Files:**
- Modify: `doclens-dashboard/src/components/ocr/LlmMarkdownConfigPanel.vue`
- Modify: `doclens-dashboard/src/composables/useLlmMarkdownConfig.ts`
- Modify: `doclens-dashboard/src/types/llmMarkdownConfig.ts`
- Modify: `doclens-dashboard/src/utils/llmMarkdownConfigRules.ts`
- Test: `doclens-dashboard/src/utils/__tests__/llmMarkdownConfigRules.test.ts`

- [ ] **Step 1: Write RED frontend utility tests**

Test cases:

- Empty config list shows no blocking error state.
- Create payload includes `name`, `usage_type`, `priority`, `is_default`, `enabled`.
- Full endpoint URL is accepted without educational endpoint hints.
- Pause one config does not change other configs in local state.

- [ ] **Step 2: Run RED**

Run:

```bash
cd doclens-dashboard && npm run test:utils
```

Expected: FAIL.

- [ ] **Step 3: Implement UI**

Page behavior:

- Show list columns: name, usage type, API type, model, endpoint, enabled, healthy, default, last health time.
- Actions: add, edit, test, pause, resume, set default, delete.
- Empty state: "暂无 LLM 配置，OCR 可正常运行。"
- Do not show prompts like "OpenAI compatible 填到 /v1、Anthropic 填到 /anthropic".
- Keep placeholder minimal: "请输入完整接口地址".

- [ ] **Step 4: Run GREEN and build**

Run:

```bash
cd doclens-dashboard && npm run test:utils && npm run test:ui && npm run build
```

Expected: PASS.

- [ ] **Step 5: Commit Task 8**

Commit subject:

```text
feat(dashboard): 支持多 LLM 配置管理
```

---

## Change 2 Final Verification

- [ ] **Step 1: Backend verification**

Run:

```bash
mvn -pl doclens-core -Dtest=LlmMarkdownConfigServiceTest,LlmConfigSelectorTest test
mvn -pl doclens-spring-boot-starter -Dtest=ConfigurableMarkdownPostProcessorTest,LlmMarkdownHealthCheckerTest test
mvn -pl doclens-server -am -Dtest=LlmMarkdownConfigApiContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

- [ ] **Step 2: Frontend verification**

Run:

```bash
cd doclens-dashboard && npm run test:utils && npm run test:ui && npm run build
```

Expected: PASS.

- [ ] **Step 3: No LLM config smoke**

Use an empty LLM config table and process an OCR document.

Expected:

- Document reaches `COMPLETED`.
- OCR Markdown is persisted.
- Result metadata includes `llm_markdown_applied=false`.
- Reason is `no_available_llm_config`.
- Dashboard shows normal OCR result and no global error.

- [ ] **Step 4: Multi LLM smoke**

Create two `MARKDOWN_POST_PROCESSING` configs:

- config A: default, disabled
- config B: enabled, healthy, priority lower number

Expected:

- Selector uses config B.
- Pausing config B causes OCR Markdown passthrough if no other healthy config exists.
- No document fails because LLM is unavailable.

---

## Final Audit And Review

- [ ] **Step 1: Full backend test**

Run:

```bash
mvn -pl doclens-core,doclens-spring-boot-starter,doclens-server test
```

Expected: PASS.

- [ ] **Step 2: Full frontend test and build**

Run:

```bash
cd doclens-dashboard && npm run test:utils && npm run test:ui && npm run build
```

Expected: PASS.

- [ ] **Step 3: Static diff verification**

Run:

```bash
git diff --check
```

Expected: PASS.

- [ ] **Step 4: code-review-spec gate**

Review the full diff against:

- `/Users/lvdaxianer/.claude/skills/code-review-spec/SKILL.md`
- `/Users/lvdaxianer/.claude/skills/code-review-spec/spec.md`
- Relevant files under `/Users/lvdaxianer/.claude/skills/code-review-spec/references`

Must verify:

- New classes and public methods have required author/date comments.
- Branch comments satisfy current project convention.
- No new 6+ parameter methods unless unavoidable and justified.
- No secrets or base64 image bodies in logs.
- Thread pools are not reused across health, OCR request, document processing, and frontend callbacks.
- Error paths preserve OCR completion when LLM config is absent.

- [ ] **Step 5: Final manual restart smoke**

Run backend and frontend, then verify:

```bash
curl http://localhost:10003/actuator/health
curl http://localhost:10002/api/v1/llm-markdown-config
```

Expected:

- Backend health is `UP`.
- LLM config API returns `200` even if list is empty.
- Frontend dashboard loads at `http://localhost:10002/dashboard/`.

---

## Self-Review

Spec coverage:

- Ollama OCR protocol is covered by Task 1.
- Fixed DeepSeek-OCR prompt is covered by Task 1 and Task 4.
- Paddle Markdown output is covered by Task 2.
- OCR document affinity is covered by Task 3.
- Ollama health check and supported model exposure are covered by Task 4.
- Multi LLM persistence, selection, API, and UI are covered by Tasks 5 through 8.
- No LLM config fallback is covered by Task 6 and Change 2 smoke tests.

Placeholder scan:

- No `TBD`, `TODO`, or open-ended "add appropriate handling" steps remain.

Type consistency:

- `modelKey` is the OCR affinity key across routing, pending queue, and tests.
- `usageType` is the LLM selection key across persistence, selector, API, and UI.
- `MARKDOWN_POST_PROCESSING` is the first supported LLM usage type and maps to existing Markdown post-processing behavior.
