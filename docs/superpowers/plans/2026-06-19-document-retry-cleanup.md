# Document Retry Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make manual document retry clear old page tasks and page OCR results so a retried document starts from a clean child state.

**Architecture:** Extend the retry use case with child-row cleanup methods on the page task and page result repositories, then call that cleanup inside the retry transaction before the document is reset and re-scheduled. Add focused repository tests and a retry regression test so the unique-key collision cannot come back unnoticed.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, JUnit 5, AssertJ

---

### Task 1: Retry regression coverage

**Files:**
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentRetryUseCaseTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void retryFailedDocumentClearsPageChildrenBeforeRescheduling() {
    InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
    InMemoryBatchRepository batchRepository = new InMemoryBatchRepository();
    InMemoryOcrEventRepository eventRepository = new InMemoryOcrEventRepository();
    InMemoryDocumentPageTaskRepository pageTaskRepository = new InMemoryDocumentPageTaskRepository();
    DocumentPageTaskExecutionTestDoubles.InMemoryDocumentPageResultRepository pageResultRepository =
            new DocumentPageTaskExecutionTestDoubles.InMemoryDocumentPageResultRepository();
    RecordingBatchProcessingScheduler scheduler = new RecordingBatchProcessingScheduler();
    OffsetDateTime now = OffsetDateTime.now();

    DocumentJob failedDocument = document("doc-failed", 0)
            .startProcessing(now)
            .advanceStage(ProcessingStage.OCR_IMAGES, 2, 5, now.plusSeconds(1))
            .fail(DocLensConstants.ERROR_CODE_OCR_FAILED, "ocr failed", now.plusSeconds(2));
    documentRepository.save(failedDocument);
    batchRepository.save(batch());
    pageTaskRepository.saveAll(List.of(task("page-task-1", "doc-failed", 1), task("page-task-2", "doc-failed", 2)));
    pageResultRepository.upsert(result("doc-failed", 1));
    pageResultRepository.upsert(result("doc-failed", 2));

    DocumentRetryUseCase useCase = new DocumentRetryUseCase(
            new DocumentRetryDependencies(documentRepository, batchRepository, eventRepository, scheduler,
                    new OcrEventFactory(new IdGenerator()), pageTaskRepository, pageResultRepository),
            new InlineTransactionRunner());

    useCase.retry("doc-failed");

    assertThat(pageTaskRepository.listByDocumentId("doc-failed")).isEmpty();
    assertThat(pageResultRepository.listByDocumentId("doc-failed")).isEmpty();
    assertThat(scheduler.scheduledBatchIds).containsExactly("batch-test");
}
```

- [ ] **Step 2: Run the focused test to verify it fails**

Run: `mvn -pl doclens-core -Dtest=DocumentRetryUseCaseTest#retryFailedDocumentClearsPageChildrenBeforeRescheduling test`
Expected: FAIL with missing repository method or missing cleanup behavior

- [ ] **Step 3: Commit**

```bash
git add doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentRetryUseCaseTest.java
git commit -m "test: add retry child-cleanup regression"
```

### Task 2: Retry cleanup plumbing

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentRetryDependencies.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentRetryUseCase.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentPageTaskRepository.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentPageResultRepository.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageTaskRepository.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageResultRepository.java`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/InMemoryDocumentPageTaskRepository.java`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskExecutionTestDoubles.java`
- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageTaskRepositoryTest.java`
- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageResultRepositoryTest.java`

- [ ] **Step 1: Write the minimal implementation**

```java
public interface DocumentPageTaskRepository {
    void deleteByDocumentId(String documentId);
}

public interface DocumentPageResultRepository {
    void deleteByDocumentId(String documentId);
}

public void retry(String documentId) {
    String batchId = transactionRunner.requiredResult(() -> retryWithinTransaction(documentId));
    batchProcessingScheduler.schedule(batchId);
}

private String retryWithinTransaction(String documentId) {
    DocumentJob document = loadDocument(documentId);
    Batch batch = loadBatch(document.batchId());
    validateRetryable(document);
    pageResultRepository.deleteByDocumentId(documentId);
    pageTaskRepository.deleteByDocumentId(documentId);
    DocumentJob retried = document.retry(OffsetDateTime.now());
    documentRepository.update(retried);
    eventRepository.save(eventFactory.create(retryEvent(batch, retried)));
    refreshBatchSummary(batch.batchId());
    return batch.batchId();
}
```

- [ ] **Step 2: Run the focused test to verify it passes**

Run: `mvn -pl doclens-core,doclens-spring-boot-starter -am -Dtest=DocumentRetryUseCaseTest,MybatisPlusDocumentPageTaskRepositoryTest,MybatisPlusDocumentPageResultRepositoryTest test`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentRetryDependencies.java doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentRetryUseCase.java doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentPageTaskRepository.java doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentPageResultRepository.java doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageTaskRepository.java doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageResultRepository.java doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/InMemoryDocumentPageTaskRepository.java doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskExecutionTestDoubles.java doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageTaskRepositoryTest.java doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageResultRepositoryTest.java\n+git commit -m \"fix: clear retry leftovers before rescheduling\"\n+```\n+\n+### Task 3: Broader verification\n+\n+**Files:**
- None\n+
- [ ] **Step 1: Run broader processing tests**

Run: `mvn -pl doclens-core,doclens-spring-boot-starter -am -DskipITs test`
Expected: PASS

- [ ] **Step 2: Check plan coverage and code review rules**

Run: `openspec validate 2026-06-19-document-retry-cleanup --strict`
Expected: PASS

- [ ] **Step 3: Commit the verification evidence if repository files changed**

```bash
git add openspec/changes/2026-06-19-document-retry-cleanup docs/superpowers/plans/2026-06-19-document-retry-cleanup.md
git commit -m "docs: add retry cleanup implementation plan"
```
