# LLM Markdown Orchestration Skip Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow uploads to declare that upstream LLM orchestration already happened, so DocLens skips its own Markdown post-processing and preserves the incoming extracted text.

**Architecture:** Thread a boolean `llmOrchestrated` flag from the dashboard upload form through multipart upload mapping, batch creation, and document job creation. The backend post-processing entry point will inspect the flag before invoking the Markdown post-processor; when the flag is true, it returns the extracted OCR text directly and marks Markdown as not applied. The default path remains unchanged for existing uploads.

**Tech Stack:** Vue 3, TypeScript, Naive UI, Spring Boot, Java 21, JUnit 5, AssertJ, Vitest, Vue Test Utils

---

### Task 1: Add backend coverage for the orchestration skip flag

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentJobCreateRequest.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentJob.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentOcrResultBuilder.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentMarkdownPostProcessingService.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownPostProcessingRequest.java`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseLlmMarkdownTest.java`

- [ ] **Step 1: Write the failing tests**

Add tests proving both behaviors:

```java
@Test
void processBatchSkipsLlmMarkdownWhenDocumentIsAlreadyOrchestrated() {
    InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
    InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
    documentRepository.save(document("doc-1", 0, JsonPayload.empty(), true));
    BatchProcessingUseCase batchUseCase = useCase(BatchProcessingUseCaseConfig.of(documentRepository,
            resultRepository, new FixedTextExtractor("原始 OCR 文本"), new FailingMarkdownPostProcessor()));

    batchUseCase.processBatch("batch-test");

    assertThat(resultRepository.findByDocumentId("doc-1")).get().satisfies(result -> {
        assertThat(result.finalText()).isEqualTo("原始 OCR 文本");
        assertThat(result.warnings()).doesNotContain("llm_markdown_post_processing_failed");
        assertThat(result.rawVendorOutput()).containsEntry("llm_markdown_applied", false);
    });
}
```

```java
@Test
void processBatchStillAppliesLlmMarkdownWhenDocumentIsNotOrchestrated() {
    InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
    InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
    documentRepository.save(document("doc-1", 0, JsonPayload.empty(), false));
    BatchProcessingUseCase batchUseCase = useCase(BatchProcessingUseCaseConfig.of(documentRepository,
            resultRepository, new FixedTextExtractor("原始 OCR 文本"),
            new FixedMarkdownPostProcessor("# 正文\n\n原始 OCR 文本")));

    batchUseCase.processBatch("batch-test");

    assertThat(resultRepository.findByDocumentId("doc-1")).get().satisfies(result -> {
        assertThat(result.finalText()).isEqualTo("# 正文\n\n原始 OCR 文本");
        assertThat(result.rawVendorOutput()).containsEntry("llm_markdown_applied", true);
    });
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd /Users/lvdaxianer/workspace/my/project/DocLens-j && mvn -pl doclens-core -Dtest=BatchProcessingUseCaseLlmMarkdownTest test
```

Expected: FAIL because `llmOrchestrated` is not threaded through the backend and the skip path does not exist yet.

- [ ] **Step 3: Write minimal implementation**

Add the boolean field to request and job objects, preserve it during request mapping, and short-circuit Markdown post-processing when it is true:

```java
public record DocumentJobCreateRequest(..., ChunkStrategy chunkStrategy, boolean llmOrchestrated) {
    public DocumentJobCreateRequest(...) {
        this(..., ChunkStrategy.general(), false);
    }
}
```

```java
public record DocumentJob(..., ChunkStrategy chunkStrategy, boolean llmOrchestrated, ...) {
    public static DocumentJob create(DocumentJobCreateRequest request) {
        return new DocumentJob(..., request.chunkStrategy(), request.llmOrchestrated(), ...);
    }
}
```

```java
DocumentPostProcessedText process(DocumentJob document, DocumentTextExtractionResult extracted) {
    if (document.llmOrchestrated()) {
        return new DocumentPostProcessedText(extracted.finalText(), extracted.warnings(), false, Optional.empty(), Map.of());
    }
    ...existing retry logic...
}
```

```java
private MarkdownPostProcessingRequest markdownRequest(DocumentJob document, DocumentTextExtractionResult extracted) {
    return new MarkdownPostProcessingRequest(document.documentId(), document.fileName(), document.metadata().values(),
            extracted.finalText(), document.chunkStrategy());
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
cd /Users/lvdaxianer/workspace/my/project/DocLens-j && mvn -pl doclens-core -Dtest=BatchProcessingUseCaseLlmMarkdownTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git -C /Users/lvdaxianer/workspace/my/project/DocLens-j add \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentJobCreateRequest.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentJob.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentOcrResultBuilder.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentMarkdownPostProcessingService.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownPostProcessingRequest.java \
  doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseLlmMarkdownTest.java
```

### Task 2: Thread the upload flag through the dashboard and request mapper

**Files:**
- Modify: `doclens-dashboard/src/types/upload.ts`
- Modify: `doclens-dashboard/src/utils/uploadFormRules.ts`
- Modify: `doclens-dashboard/src/components/dashboard/UploadDropzone.vue`
- Modify: `doclens-dashboard/src/components/upload/UploadAdvancedOptions.vue`
- Modify: `doclens-dashboard/src/api/upload.ts`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/interfaces/CreateBatchRequestMapper.java`
- Modify: `doclens-api/src/main/java/io/github/lvdaxianer/doclens/j/api/CreateBatchRequest.java`

- [ ] **Step 1: Write the failing test**

Add a multipart mapping test that proves the upload flag is preserved:

```java
@Test
void toRequestPreservesLlMorchestratedFlag() {
    MockMultipartFile file = new MockMultipartFile("files", "a.txt", "text/plain", "x".getBytes());
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter("metadata", "{}");
    request.setParameter("llmOrchestrated", "true");

    CreateBatchRequest mapped = mapper.toRequest(List.of(file), request);

    assertThat(mapped.llmOrchestrated()).isTrue();
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd /Users/lvdaxianer/workspace/my/project/DocLens-j && mvn -pl doclens-server -Dtest=CreateBatchRequestMapperChunkStrategyTest test
```

Expected: FAIL because the mapper and request type do not yet carry `llmOrchestrated`.

- [ ] **Step 3: Write minimal implementation**

Add the field to the dashboard upload types and form data builder, then map the multipart field on the server:

```ts
export interface UploadBatchOptions {
  ...
  llmOrchestrated: boolean
}
```

```ts
appendOptional(formData, 'llmOrchestrated', String(options.llmOrchestrated))
```

```java
private static final String LLM_ORCHESTRATED_PARAM = "llmOrchestrated";

return new CreateBatchRequest(..., form.chunkStrategy(), form.llmOrchestrated(), ...);
```

```java
public record CreateBatchRequest(..., String chunkStrategy, boolean llmOrchestrated, ...) {
    public CreateBatchRequest(...) {
        this(..., null, false, ...);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
cd /Users/lvdaxianer/workspace/my/project/DocLens-j && mvn -pl doclens-server -Dtest=CreateBatchRequestMapperChunkStrategyTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git -C /Users/lvdaxianer/workspace/my/project/DocLens-j add \
  doclens-dashboard/src/types/upload.ts \
  doclens-dashboard/src/utils/uploadFormRules.ts \
  doclens-dashboard/src/components/dashboard/UploadDropzone.vue \
  doclens-dashboard/src/components/upload/UploadAdvancedOptions.vue \
  doclens-dashboard/src/api/upload.ts \
  doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/interfaces/CreateBatchRequestMapper.java \
  doclens-api/src/main/java/io/github/lvdaxianer/doclens/j/api/CreateBatchRequest.java
```

### Task 3: Verify the change and finalize the plan

**Files:**
- Create: `openspec/changes/llm-markdown-orchestration-skip/.openspec.yaml`
- Create: `openspec/changes/llm-markdown-orchestration-skip/proposal.md`
- Create: `openspec/changes/llm-markdown-orchestration-skip/design.md`
- Create: `openspec/changes/llm-markdown-orchestration-skip/tasks.md`
- Create: `openspec/changes/llm-markdown-orchestration-skip/specs/llm-markdown-orchestration-skip/spec.md`
- Create: `docs/superpowers/plans/2026-06-19-llm-markdown-orchestration-skip.md`

- [x] **Step 1: Draft the OpenSpec change**

Write the proposal, design, tasks, and spec to capture the skip semantics and default behavior.

- [x] **Step 2: Validate the OpenSpec change**

Run:

```bash
cd /Users/lvdaxianer/workspace/my/project/DocLens-j && openspec validate llm-markdown-orchestration-skip --strict
```

Expected: PASS.

- [ ] **Step 3: Review the plan for placeholders and consistency**

Check that all task steps contain concrete file paths, commands, and code samples, and that every spec requirement maps to a task.

- [ ] **Step 4: Commit the plan artifact**

Create a Chinese Conventional Commit with the plan and OpenSpec artifacts before implementation begins.
