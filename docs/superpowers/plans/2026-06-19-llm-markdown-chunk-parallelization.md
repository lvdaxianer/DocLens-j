# LLM Markdown Chunk Parallelization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Parallelize Markdown chunk execution so chunks from one document and
chunks across multiple documents can be consumed concurrently, while preserving
round-robin LLM config selection, per-config limits, retries, and ordered merge
semantics.

**Architecture:** Keep the public synchronous Markdown post-processing contract,
but move concurrency inside the chunk execution layer. Each chunk becomes an
independent work item submitted to a shared executor; each work item selects an
eligible LLM config at execution time, acquires that config's permit, retries on
failure, and falls back to the original chunk text if retries are exhausted. A
joiner waits for all chunk futures, sorts them by `chunkIndex`, and concatenates
the per-chunk results back into the final document text.

**Tech Stack:** Java 21, JUnit 5, AssertJ, Spring Boot auto-configuration,
existing DocLens core and starter modules.

---

### Task 1: Shared chunk executor test coverage

**Files:**
- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ChunkedMarkdownPostProcessorTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void processesChunksInParallelAcrossASharedExecutorAndMergesByChunkOrder() {
    // Arrange a recording delegate that blocks two chunk requests so we can
    // observe overlap and verify the final join order.
    // Expected final markdown: "chunk-0\n\nchunk-1\n\nchunk-2".
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl doclens-spring-boot-starter -Dtest=ChunkedMarkdownPostProcessorTest#processesChunksInParallelAcrossASharedExecutorAndMergesByChunkOrder test -DskipITs`

Expected: FAIL because the current `ChunkedMarkdownPostProcessor` processes
chunks sequentially and cannot observe overlap.

- [ ] **Step 3: Write minimal implementation**

No implementation yet. This task is test-only and should remain red until the
shared chunk executor exists.

- [ ] **Step 4: Run test to verify it passes**

Not applicable yet.

- [ ] **Step 5: Commit**

```bash
git add doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ChunkedMarkdownPostProcessorTest.java
git commit -m "test: add chunk-parallel markdown coverage"
```

### Task 2: Multi-document shared pool test coverage

**Files:**
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseTest.java`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseTestSupport.java` if the test needs a new chunk-aware helper

- [ ] **Step 1: Write the failing test**

```java
@Test
void processBatchCanFeedMultipleDocumentsIntoTheSameChunkPool() {
    // Arrange two documents whose chunk work both block on the same shared pool.
    // Assert the second document begins chunk work before the first finishes.
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl doclens-core -Dtest=BatchProcessingUseCaseTest#processBatchCanFeedMultipleDocumentsIntoTheSameChunkPool test -DskipITs`

Expected: FAIL because the current Markdown post-processing path is still
sequential at the chunk level.

- [ ] **Step 3: Write minimal implementation**

No implementation yet. This task is test-only and should remain red until the
chunk executor is introduced.

- [ ] **Step 4: Run test to verify it passes**

Not applicable yet.

- [ ] **Step 5: Commit**

```bash
git add doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseTest.java
git commit -m "test: add multi-document chunk pool coverage"
```

### Task 3: Parallel chunk execution implementation

**Files:**
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ChunkedMarkdownPostProcessor.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ConfigurableMarkdownPostProcessor.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownPostProcessingResult.java` if result metadata needs a helper for chunk fallback visibility
- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ChunkedMarkdownPostProcessorTest.java`
- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ConfigurableMarkdownPostProcessorTest.java`

- [ ] **Step 1: Implement the shared chunk coordinator**

```java
// Sketch:
// 1. plan chunks
// 2. submit each chunk to an ExecutorService
// 3. inside each chunk task, call the existing runtime processor
// 4. collect futures, sort by chunkIndex, and merge results
// 5. on exhausted retries, substitute the original chunk text only for that chunk
```

- [ ] **Step 2: Run focused tests and confirm RED becomes GREEN**

Run:
`mvn -pl doclens-spring-boot-starter -Dtest=ChunkedMarkdownPostProcessorTest,ConfigurableMarkdownPostProcessorTest test -DskipITs`

Expected: PASS after the coordinator is implemented.

- [ ] **Step 3: Verify merged chunk fallback semantics**

```java
@Test
void failedChunkFallsBackToItsOriginalTextWhileOtherChunksStillMerge() {
    // Arrange one chunk to fail permanently and the rest to succeed.
    // Assert only the failed chunk is replaced by its original OCR text.
}
```

- [ ] **Step 4: Run the broader backend tests**

Run: `mvn -pl doclens-core,doclens-spring-boot-starter test -DskipITs`

Expected: PASS with the new parallel chunk behavior.

- [ ] **Step 5: Commit**

```bash
git add doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ChunkedMarkdownPostProcessor.java \
        doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ConfigurableMarkdownPostProcessor.java \
        doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownPostProcessingResult.java \
        doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ChunkedMarkdownPostProcessorTest.java \
        doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ConfigurableMarkdownPostProcessorTest.java
git commit -m "feat: parallelize markdown chunk processing"
```

### Task 4: Validation and audit

**Files:**
- Modify: `openspec/changes/2026-06-19-llm-markdown-chunk-parallelization/tasks.md`

- [ ] **Step 1: Validate the OpenSpec change**

Run: `openspec validate 2026-06-19-llm-markdown-chunk-parallelization --strict`

Expected: PASS with no schema or task coverage issues.

- [ ] **Step 2: Run plan-implementation consistency audit**

Check that every requirement in `specs/llm-markdown-parallel-chunking/spec.md`
has a corresponding implementation task and test task.

- [ ] **Step 3: Mark completed tasks**

Update exactly one completed checkbox immediately after each gate passes.

- [ ] **Step 4: Archive and final commit**

Archive the completed OpenSpec change after all tasks pass, then commit the
archive update if repository files changed.
