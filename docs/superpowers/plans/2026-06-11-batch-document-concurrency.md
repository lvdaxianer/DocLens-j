# Batch Document Concurrency Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让同一批次内多个文档并发进入 OCR 提取链路，避免 OCR 节点并发空转，同时保持文档归属与页顺序稳定。

**Architecture:** 批次调度线程仍只负责启动批次，批次内部文档处理交给独立的 `doclensDocumentProcessingExecutor`。每个文档生成自己的 `DocumentProcessingResult`，完成后独立事务落库；PDF/Word 内部页级 OCR 继续使用已有页任务与 OCR 请求共享等待池，结果按文档和页序合并。

**Tech Stack:** Java 21, JUnit 5, AssertJ, Spring Boot auto-configuration, `ExecutorService`。

---

### Task 1: 批次内文档并发处理

**Files:**
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseTest.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingDependencies.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCase.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensProcessingAutoConfiguration.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringProperties.java`

- [x] **Step 1: Write the failing test**

Add a test named `processBatchStartsMultipleDocumentsBeforeWaitingForFirstCompletion` in `BatchProcessingUseCaseTest`.
The test creates two queued documents and a blocking extractor. It starts `processBatch` on a worker thread, waits until two documents have entered extraction, then releases both. Current serial code should fail because only one document enters extraction before the first one completes.

- [x] **Step 2: Run test to verify RED**

Run:

```bash
mvn -pl doclens-core -Dtest=BatchProcessingUseCaseTest#processBatchStartsMultipleDocumentsBeforeWaitingForFirstCompletion test
```

Expected: FAIL because the second document does not enter extraction while the first document is blocked.

- [x] **Step 3: Implement minimal GREEN**

Add a document-processing `ExecutorService` to `BatchProcessingDependencies` and submit each queued document to that executor from `BatchProcessingUseCase.processBatch`.

Implementation requirements:
- Preserve queued document order when collecting futures, so final persistence order is deterministic.
- Persist each document immediately when its future is joined.
- If worker execution is interrupted, restore the interrupt flag and fail the current document with enough logging.
- Do not mix this with batch scheduler, OCR request, OCR health, or callback executors.

- [x] **Step 4: Wire Spring configuration**

Inject `@Qualifier("doclensDocumentProcessingExecutor") ExecutorService` into `batchProcessingDependencies`.

Update default document-processing pool from `1/1` to `6/6`, so default runtime can actually run multiple documents in one batch. Users can still override `doclens.thread-pools.document-processing-thread-pool.*`.

- [x] **Step 5: Run focused and broader verification**

Run:

```bash
mvn -pl doclens-core -Dtest=BatchProcessingUseCaseTest test
mvn -pl doclens-spring-boot-starter -DskipTests compile
git diff --check
```

Expected: all pass.

- [x] **Step 6: Apply code-review-spec and commit**

Review the full diff against:
- `/Users/lvdaxianer/.claude/skills/code-review-spec/SKILL.md`
- `/Users/lvdaxianer/.claude/skills/code-review-spec/spec.md`
- relevant `references/*.md`

Then commit with Chinese Conventional Commit:

```bash
git add docs/superpowers/plans/2026-06-11-batch-document-concurrency.md \
  doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseTest.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingDependencies.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCase.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensProcessingAutoConfiguration.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringProperties.java
git commit -F /tmp/doclens-batch-document-concurrency-commit.txt
```

Commit subject:

```text
fix(ocr): 并发处理批次文档
```

Refs: 批次文档并发 OCR

---

## Self-Review

Spec coverage:
- Cross-document concurrency is covered by Task 1 test and implementation.
- Existing page-level OCR ordering remains in the extractor layer and is not changed by this task.
- Dedicated thread pool isolation is covered by Spring wiring to `doclensDocumentProcessingExecutor`.

Placeholder scan:
- No placeholder implementation steps remain.

Type consistency:
- `BatchProcessingDependencies` owns the executor and `BatchProcessingUseCase` consumes the same dependency.
