# LLM Markdown Failure Observability And Retry Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让文档结果在发生 `LLM 回退 OCR` 时保存并展示具体失败原因，并为 LLM Markdown 后处理增加默认 3 次重试，只有 3 次都失败后才回退 OCR。

**Architecture:** 复用现有 `OcrResult.rawVendorOutput` 持久化链路，在 LLM 后处理失败时写入脱敏后的失败消息；查询接口将该字段提升为稳定读模型；结果抽屉在回退状态下显示失败原因。LLM Markdown 后处理在 `BatchProcessingUseCase` 内部按固定上限重试，成功则继续走 Markdown 结果，只有最后一次失败才回退 OCR 并持久化最终失败原因。

**Tech Stack:** Java 21, Spring Boot 3.5, Maven, Vue 3, TypeScript, Vite, JUnit 5, AssertJ.

---

## Confirmed Requirements

- 当前 `LLM 回退 OCR` 状态过于粗糙，必须能看到具体失败原因。
- 失败原因不能泄露 API Key 等敏感信息。
- 当前联通性测试已经证明现有配置“此刻可达”，因此这次修复重点是失败可观测性，而不是盲目改请求格式。
- LLM Markdown 后处理必须默认重试 3 次，第 1/2 次失败不能立刻回退。
- 修复范围应尽量小，不改数据库表结构，优先复用现有结果持久化字段。

## Task 1: 持久化并展示 LLM 失败原因

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCase.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/query/application/OcrQueryService.java`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseTest.java`
- Create: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/query/application/OcrQueryServiceTest.java`
- Modify: `doclens-dashboard/src/types/dashboard.ts`
- Modify: `doclens-dashboard/src/components/dashboard/DocumentResultDrawer.vue`

- [ ] **Step 1: Write the failing test**

```text
后端：
1. LLM 后处理失败时，结果持久化中必须保留 llm_error_message。
2. 文档结果查询接口必须返回 llm_error_message。

前端：
1. 在回退状态且存在 llm_error_message 时，抽屉应显示失败原因。
```

- [ ] **Step 2: Run test to verify it fails**

Run:
- `mvn -pl doclens-core -Dtest=BatchProcessingUseCaseTest,OcrQueryServiceTest test`

Expected:
- 断言 llm_error_message 缺失，测试失败。

- [ ] **Step 3: Write minimal implementation**

```text
1. BatchProcessingUseCase 在回退时保存脱敏后的异常消息。
2. OcrQueryService 返回 llm_error_message 字段。
3. 抽屉在回退状态时展示该字段。
```

- [ ] **Step 4: Run test to verify it passes**

Run:
- `mvn -pl doclens-core -Dtest=BatchProcessingUseCaseTest,OcrQueryServiceTest test`
- `mvn -pl doclens-server -Dtest=DocLensOcrApiContractTest test`
- `pnpm --dir doclens-dashboard build`

Expected:
- 测试和构建通过。

- [ ] **Step 5: Commit**

```bash
git add doclens-core doclens-dashboard docs/superpowers/plans/2026-06-10-llm-markdown-failure-observability.md
git commit -F /tmp/llm-markdown-failure-observability.commit
```

## Task 2: LLM Markdown 默认重试 3 次后再回退

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCase.java`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void processBatchKeepsMarkdownWhenLlmSucceedsOnThirdAttempt() {
    InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
    InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
    RetryingMarkdownPostProcessor markdownPostProcessor =
            new RetryingMarkdownPostProcessor(2, "# 修复后 Markdown");
    documentRepository.save(document("doc-1", 0));
    BatchProcessingUseCase useCase = useCase(documentRepository, resultRepository,
            new InMemoryOcrEventRepository(), new InMemoryBatchRepository(),
            new FixedTextExtractor("原始 OCR 文本"), markdownPostProcessor);

    useCase.processBatch("batch-test");

    assertThat(markdownPostProcessor.attempts()).isEqualTo(3);
    assertThat(resultRepository.findByDocumentId("doc-1")).get().satisfies(result -> {
        assertThat(result.finalText()).isEqualTo("# 修复后 Markdown");
        assertThat(result.rawVendorOutput()).containsEntry("llm_markdown_applied", true);
    });
}
```

```java
@Test
void processBatchFallsBackAfterThreeLlmFailures() {
    InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
    InMemoryOcrResultRepository resultRepository = new InMemoryOcrResultRepository();
    CountingFailingMarkdownPostProcessor markdownPostProcessor =
            new CountingFailingMarkdownPostProcessor("llm unavailable");
    documentRepository.save(document("doc-1", 0));
    BatchProcessingUseCase useCase = useCase(documentRepository, resultRepository,
            new InMemoryOcrEventRepository(), new InMemoryBatchRepository(),
            new FixedTextExtractor("原始 OCR 文本"), markdownPostProcessor);

    useCase.processBatch("batch-test");

    assertThat(markdownPostProcessor.attempts()).isEqualTo(3);
    assertThat(resultRepository.findByDocumentId("doc-1")).get().satisfies(result -> {
        assertThat(result.finalText()).isEqualTo("原始 OCR 文本");
        assertThat(result.rawVendorOutput())
                .containsEntry("llm_markdown_applied", false)
                .containsEntry("llm_error_message", "llm unavailable");
    });
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
- `mvn -pl doclens-core -Dtest=BatchProcessingUseCaseTest#processBatchKeepsMarkdownWhenLlmSucceedsOnThirdAttempt,BatchProcessingUseCaseTest#processBatchFallsBackAfterThreeLlmFailures test`

Expected:
- 当前实现只会调用 1 次 `markdownPostProcessor.process(...)` 并立刻回退，因此断言 `attempts()==3` 失败。

- [ ] **Step 3: Write minimal implementation**

```java
private static final int LLM_MARKDOWN_MAX_ATTEMPTS = 3;

private PostProcessedText postProcessMarkdown(DocumentJob document, DocumentTextExtractionResult extracted) {
    RuntimeException lastFailure = null;
    for (int attempt = 1; attempt <= LLM_MARKDOWN_MAX_ATTEMPTS; attempt++) {
        try {
            MarkdownPostProcessingResult result = markdownPostProcessor.process(markdownRequest(document, extracted));
            return new PostProcessedText(result.markdown(),
                    mergeWarnings(extracted.warnings(), result.warnings()),
                    result.markdownApplied(), Optional.empty());
        } catch (RuntimeException ex) {
            lastFailure = ex;
        }
    }
    return new PostProcessedText(extracted.finalText(), failedWarnings(extracted.warnings()),
            false, Optional.ofNullable(lastFailure).map(RuntimeException::getMessage).filter(message -> !message.isBlank()));
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
- `mvn -pl doclens-core -Dtest=BatchProcessingUseCaseTest,OcrQueryServiceTest test`
- `pnpm --dir doclens-dashboard build`

Expected:
- 新增重试测试通过。
- 现有失败原因可观测性测试继续通过。
- 前端构建继续通过。

- [ ] **Step 5: Commit**

```bash
git add doclens-core doclens-dashboard docs/superpowers/plans/2026-06-10-llm-markdown-failure-observability.md
git commit -F /tmp/llm-markdown-retry.commit
```
