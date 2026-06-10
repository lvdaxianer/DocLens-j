# Batch Image-Level OCR Scheduling Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace document-serial OCR scheduling with a persistent image-level OCR queue that keeps every OCR node slot busy while preserving each document's page ownership and page order.

**Architecture:** Keep the existing OCR routing stack (`OcrDispatchCoordinator`, node slot acquisition, pending request queue, retry/failover) and move the bottleneck from `BatchProcessingUseCase.processBatch()` document-serial execution to page-task-driven execution. Documents first finish lightweight preparation (type recognition, Word-to-PDF, PDF rendering), then create persistent page tasks keyed by `documentId + pageNo`; OCR workers consume those page tasks globally, and a document-level aggregator finalizes merge/save/LLM only after all pages of that document are completed and re-sorted by `pageNo`.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, Flyway SQL migrations, JUnit 5, AssertJ, Vue 3, TypeScript.

---

## Persistence Rules

### Strongly consistent writes

These writes are part of correctness and crash recovery, so they must be committed immediately:

- page task creation: `QUEUED`
- task claim: `PROCESSING + locked_by + locked_until`
- page OCR success: `page result upsert + page task COMPLETED` in one transaction
- page OCR terminal failure: `FAILED`

### Weakly consistent writes

These writes are display-oriented and may be delayed or batched:

- `ocr_documents.current_page`
- `ocr_documents.progress_percent`
- `ocr_documents.stage` for dashboard-friendly intermediate states
- dashboard aggregate counters
- non-critical event stream enrichments

### Atomic claim rule

Multiple workers must never process the same page concurrently. A worker may start OCR only after a single atomic database update succeeds:

```sql
UPDATE ocr_document_page_tasks
SET
    status = 'PROCESSING',
    locked_by = :workerId,
    locked_until = :lockedUntil,
    started_at = :now,
    updated_at = :now
WHERE
    task_id = :taskId
    AND status = 'QUEUED';
```

Only the worker that updates exactly one row owns the page task.

### Crash recovery rule

If the server crashes during page OCR:

- a page task with no persisted page result must be retried from the whole page
- a page task with an already persisted page result must be repaired to `COMPLETED` instead of re-running OCR

This keeps page retries bounded to the minimum unit: one page, not one document.

## File Structure

**Create**
- `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentPageTask.java`
- `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentPageTaskStatus.java`
- `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentPageTaskRepository.java`
- `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskPreparationService.java`
- `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskExecutionService.java`
- `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskAggregationService.java`
- `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/PreparedDocumentPages.java`
- `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/PageImageRef.java`
- `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentPageResult.java`
- `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentPageResultRepository.java`
- `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/DocumentPageTaskEntity.java`
- `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/DocumentPageResultEntity.java`
- `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/DocumentPageTaskMapper.java`
- `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/DocumentPageResultMapper.java`
- `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageTaskRepository.java`
- `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageResultRepository.java`
- `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/PageTaskWorkerScheduler.java`
- `doclens-spring-boot-starter/src/main/resources/db/migration/V10__doclens_document_page_tasks.sql`
- `doclens-spring-boot-starter/src/main/resources/db/migration/V11__doclens_document_page_results.sql`
- `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskAggregationServiceTest.java`
- `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskPreparationServiceTest.java`
- `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskExecutionServiceTest.java`
- `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageTaskRepositoryTest.java`
- `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageResultRepositoryTest.java`

**Modify**
- `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCase.java`
- `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentJob.java`
- `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/ProcessingStage.java`
- `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/query/application/ProcessingTrackAssembler.java`
- `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/query/application/DashboardStageMetricsAssembler.java`
- `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/DocumentJobEntity.java`
- `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/DocumentJobMapper.java`
- `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentJobRepository.java`
- `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/PdfImageDocumentExtractor.java`
- `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/ImageDocumentExtractor.java`
- `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/WordDocumentExtractor.java`
- `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensProcessingAutoConfiguration.java`
- `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensExtractionAutoConfiguration.java`
- `doclens-dashboard/src/utils/formatters.ts`

---

### Task 1: Introduce Persistent Page-Task Model

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentPageTask.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentPageTaskStatus.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentPageTaskRepository.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentPageResult.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentPageResultRepository.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/DocumentPageTaskEntity.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/DocumentPageResultEntity.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/DocumentPageTaskMapper.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/DocumentPageResultMapper.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageTaskRepository.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageResultRepository.java`
- Create: `doclens-spring-boot-starter/src/main/resources/db/migration/V10__doclens_document_page_tasks.sql`
- Create: `doclens-spring-boot-starter/src/main/resources/db/migration/V11__doclens_document_page_results.sql`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageTaskRepositoryTest.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageResultRepositoryTest.java`

- [ ] **Step 1: Write the failing repository test**

```java
@Test
void repositoryStoresPageTasksByDocumentAndPageNumber() {
    DocumentPageTask first = DocumentPageTask.create(
            new DocumentPageTaskCreateRequest("task-1", "batch-1", "doc-1", 1, "page://1", now));
    DocumentPageTask second = DocumentPageTask.create(
            new DocumentPageTaskCreateRequest("task-2", "batch-1", "doc-1", 2, "page://2", now));

    repository.saveAll(List.of(first, second));

    assertThat(repository.listByDocumentId("doc-1"))
            .extracting(DocumentPageTask::pageNo)
            .containsExactly(1, 2);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl doclens-spring-boot-starter -Dtest=MybatisPlusDocumentPageTaskRepositoryTest test`

Expected: FAIL because page-task entity, mapper, repository, and migration do not exist yet.

- [ ] **Step 3: Write the migration and domain model**

```sql
CREATE TABLE IF NOT EXISTS ocr_document_page_tasks (
    task_id VARCHAR(80) PRIMARY KEY,
    batch_id VARCHAR(80) NOT NULL,
    document_id VARCHAR(80) NOT NULL,
    page_no INTEGER NOT NULL,
    image_storage_uri VARCHAR(2048) NOT NULL,
    status VARCHAR(40) NOT NULL,
    selected_node_id VARCHAR(80),
    retry_count INTEGER NOT NULL,
    error_code VARCHAR(120),
    error_message TEXT,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (document_id, page_no)
);
```

```sql
CREATE TABLE IF NOT EXISTS ocr_document_page_results (
    document_id VARCHAR(80) NOT NULL,
    page_no INTEGER NOT NULL,
    raw_output TEXT NOT NULL,
    page_text TEXT NOT NULL,
    layout_blocks TEXT NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    warnings TEXT NOT NULL,
    node_id VARCHAR(80),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (document_id, page_no)
);
```

```java
public enum DocumentPageTaskStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED
}
```

```java
public record DocumentPageTask(
        String taskId,
        String batchId,
        String documentId,
        int pageNo,
        String imageStorageUri,
        DocumentPageTaskStatus status,
        Optional<String> selectedNodeId,
        int retryCount,
        Optional<String> errorCode,
        Optional<String> errorMessage,
        Optional<OffsetDateTime> startedAt,
        Optional<OffsetDateTime> completedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) { }
```

```java
public record DocumentPageResult(
        String documentId,
        int pageNo,
        Map<String, Object> rawOutput,
        List<Map<String, Object>> pageText,
        List<Map<String, Object>> layoutBlocks,
        double confidence,
        List<String> warnings,
        Optional<String> nodeId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) { }
```

- [ ] **Step 4: Add repository queries used by the scheduler**

```java
List<DocumentPageTask> listQueued(int limit);
List<DocumentPageTask> listByDocumentId(String documentId);
Optional<DocumentPageTask> findByDocumentIdAndPageNo(String documentId, int pageNo);
long countCompletedByDocumentId(String documentId);
void upsert(DocumentPageResult result);
Optional<DocumentPageResult> findByDocumentIdAndPageNo(String documentId, int pageNo);
List<DocumentPageResult> listByDocumentId(String documentId);
```

- [ ] **Step 5: Run focused repository tests**

Run: `mvn -pl doclens-spring-boot-starter -Dtest=MybatisPlusDocumentPageTaskRepositoryTest test`

Expected: PASS.

- [x] **Step 6: Commit**

```bash
git add doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentPageTask.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentPageTaskStatus.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentPageTaskRepository.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentPageResult.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentPageResultRepository.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/DocumentPageTaskEntity.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/DocumentPageResultEntity.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/DocumentPageTaskMapper.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/DocumentPageResultMapper.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageTaskRepository.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageResultRepository.java \
  doclens-spring-boot-starter/src/main/resources/db/migration/V10__doclens_document_page_tasks.sql \
  doclens-spring-boot-starter/src/main/resources/db/migration/V11__doclens_document_page_results.sql \
  doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageTaskRepositoryTest.java \
  doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageResultRepositoryTest.java
git commit -m "feat(ocr): 增加页任务与页结果持久化"
```

### Task 1.5: Add Atomic Page-Task Claim and Terminal State Writes

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentPageTask.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentPageTaskRepository.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentPageTaskClaimRequest.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentPageTaskCompletionRequest.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentPageTaskFailureRequest.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/DocumentPageTaskMapper.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageTaskRepository.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageTaskRepositoryTest.java`

- [x] **Step 1: Write failing atomic claim and terminal-state repository tests**

Coverage:
- `tryMarkProcessingOnlyClaimsQueuedTaskOnce`
- `tryMarkProcessingRejectsAlreadyProcessingTask`
- `markCompletedPersistsTerminalSuccessState`
- `markCompletedRejectsWorkerThatDoesNotOwnTask`
- `markFailedPersistsTerminalFailureState`

RED evidence:
`mvn -pl doclens-spring-boot-starter -am -Dtest=MybatisPlusDocumentPageTaskRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false test`
failed because claim/completion/failure request objects and repository methods did not exist.

- [x] **Step 2: Implement database-backed atomic claim**

Implementation uses a single conditional update:

```sql
UPDATE ocr_document_page_tasks
SET status = 'PROCESSING', locked_by = :workerId, locked_until = :lockedUntil
WHERE task_id = :taskId AND status = 'QUEUED';
```

Only `updatedRows == 1` means the worker owns the page.

- [x] **Step 3: Implement immediate terminal state writes**

Implementation adds repository methods for:
- `markCompleted`
- `markFailed`

These methods persist terminal page state immediately so restart recovery and document aggregation do not depend on the in-memory queue.

- [x] **Step 4: Run focused repository tests**

GREEN evidence:
`mvn -pl doclens-spring-boot-starter -am -Dtest=MybatisPlusDocumentPageTaskRepositoryTest,MybatisPlusDocumentPageResultRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false test`
passed with 8 tests.

### Task 2: Split Document Preparation From OCR Execution

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/PreparedDocumentPages.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/PageImageRef.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskPreparationService.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCase.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/PdfImageDocumentExtractor.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/ImageDocumentExtractor.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/WordDocumentExtractor.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskPreparationServiceTest.java`

- [x] **Step 1: Write the failing preparation test**

```java
@Test
void preparationCreatesOrderedPageTasksWithoutRunningOcr() {
    PreparedDocumentPages prepared = service.prepare(document);

    assertThat(prepared.documentId()).isEqualTo("doc-1");
    assertThat(prepared.pageImages()).extracting(PageImageRef::pageNo).containsExactly(1, 2, 3);
    assertThat(pageTaskRepository.listByDocumentId("doc-1"))
            .extracting(DocumentPageTask::status)
            .containsOnly(DocumentPageTaskStatus.QUEUED);
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `mvn -pl doclens-core -Dtest=DocumentPageTaskPreparationServiceTest test`

Expected: FAIL because preparation service and page-image result objects do not exist.

RED evidence:
`mvn -pl doclens-core -Dtest=DocumentPageTaskPreparationServiceTest test`
failed because `PageImageRef`, `PreparedDocumentPages`, `PageImagePreparation`,
`DocumentPageTaskPreparationService`, and `OCR_QUEUED` did not exist.

- [x] **Step 3: Refactor extraction flow to stop before OCR**

```java
public record PreparedDocumentPages(
        String batchId,
        String documentId,
        String fileName,
        List<PageImageRef> pageImages
) { }
```

```java
public record PageImageRef(int pageNo, String imageStorageUri) { }
```

```java
public PreparedDocumentPages prepare(DocumentJob document) {
    DocumentJob preparing = document.startPreparation(now);
    documentRepository.update(preparing);
    List<PageImageRef> pageImages = pageImagePreparation.prepare(document);
    pageTaskRepository.saveAll(tasksFor(document, pageImages));
    documentRepository.update(preparing.markOcrQueued(pageImages.size(), now));
    return new PreparedDocumentPages(document.batchId(), document.documentId(), document.fileName(), pageImages);
}
```

Implemented so far:
- Added `PageImageRef` and `PreparedDocumentPages`.
- Added `PageImagePreparation` abstraction for page-image preparation.
- Added `DocumentPageTaskPreparationService`.
- Added `ProcessingStage.OCR_QUEUED` and `DocumentJob.markOcrQueued(...)`.
- Added `IdGenerator.newPageTaskId()`.

GREEN evidence:
`mvn -pl doclens-core -am -Dtest=DocumentPageTaskPreparationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
passed with 1 test.

- [x] **Step 4: Change batch entrypoint to enqueue work instead of OCR whole documents**

```java
public void processBatch(String batchId) {
    List<DocumentJob> documents = queuedDocuments(batchId);
    documents.forEach(document -> preparationService.prepare(document));
}
```

Implemented:
- `BatchProcessingUseCase` now enqueues `IMAGE`, `PDF`, and `WORD` documents through
  `DocumentPageTaskPreparationService`, marks documents as `PROCESSING + OCR_QUEUED`,
  and leaves `TEXT` / `MARKDOWN` on the existing synchronous text path.
- Added `DefaultPageImagePreparation` and Spring beans so image documents reuse their
  original storage URI, while PDF / Word documents are rendered into page images.
- Registered `DocumentPageTaskMapper` and `MybatisPlusDocumentPageTaskRepository` in
  the embedded starter auto-configuration so the page-task preparation bean starts.

RED evidence:
`mvn -pl doclens-spring-boot-starter -am -Dtest=DefaultPageImagePreparationTest -Dsurefire.failIfNoSpecifiedTests=false test`
failed because image preparation read object storage before returning the original
image URI.

GREEN evidence:
`mvn -pl doclens-spring-boot-starter -am -Dtest=DefaultPageImagePreparationTest -Dsurefire.failIfNoSpecifiedTests=false test`
passed with 1 test after image preparation skipped storage reads.

- [x] **Step 5: Run focused preparation tests**

Run: `mvn -pl doclens-core -Dtest=DocumentPageTaskPreparationServiceTest test`

Expected: PASS.

Focused verification:
`mvn -pl doclens-core,doclens-spring-boot-starter -am -Dtest=BatchProcessingUseCaseTest,DefaultPageImagePreparationTest,DocLensProcessingAutoConfigurationTest,DocLensStarterEmbeddedTest -Dsurefire.failIfNoSpecifiedTests=false test`
passed with the relevant core and starter tests.

Broader verification:
`mvn -pl doclens-core,doclens-spring-boot-starter -am test`
passed with core 81 tests and starter 78 tests.

- [x] **Step 6: Commit**

```bash
git add doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/PreparedDocumentPages.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/PageImageRef.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskPreparationService.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCase.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/PdfImageDocumentExtractor.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/ImageDocumentExtractor.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/extraction/WordDocumentExtractor.java \
  doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskPreparationServiceTest.java
git commit -m "refactor(ocr): 拆分文档预处理与图片调度"
```

### Task 3: Execute Page Tasks Through Global OCR Workers

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskExecutionService.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/PageTaskWorkerScheduler.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensProcessingAutoConfiguration.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensExtractionAutoConfiguration.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskExecutionServiceTest.java`

- [x] **Step 1: Write the failing worker test**

```java
@Test
void workerConsumesQueuedPagesAcrossDocumentsAndPreservesTaskIdentity() {
    repository.saveAll(List.of(task("doc-1", 1), task("doc-2", 1), task("doc-1", 2)));

    worker.runOnce();

    assertThat(executedRequests)
            .extracting(ImageOcrRequest::documentId, ImageOcrRequest::pageNo)
            .containsExactlyInAnyOrder(tuple("doc-1", 1), tuple("doc-2", 1), tuple("doc-1", 2));
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `mvn -pl doclens-core -Dtest=DocumentPageTaskExecutionServiceTest test`

Expected: FAIL because there is no page-task worker service yet.

Evidence: RED covered by `DocumentPageTaskExecutionServiceTest`, including cross-document page identity and
non-blocking task submission expectations before the execution service existed.

- [x] **Step 3: Implement global page-task execution**

```java
public void runOnce() {
    pageTaskRepository.listQueued(workerBatchSize).forEach(task -> {
        if (!pageTaskRepository.tryMarkProcessing(task.taskId(), workerId, lockedUntil, now)) {
            return;
        }
        executor.submit(() -> execute(task));
    });
}
```

```java
private void execute(DocumentPageTask task) {
    ImageOcrRequest request = requestFactory.from(task);
    ImageOcrResult result = routingService.recognize(request, document(task.documentId()).ocrRoutePolicy()).result();
    transactionRunner.requiredVoid(() -> {
        pageResultRepository.upsert(DocumentPageResult.from(task, result, resultNodeId, now));
        pageTaskRepository.update(task.markCompleted(resultNodeId, now));
    });
    aggregationService.recordSuccess(task, resultNodeId);
}
```

- [x] **Step 4: Reuse existing dispatch coordinator instead of reimplementing slot logic**

```java
OcrRouteExecutionResult routeResult = routingService.recognize(request, document.ocrRoutePolicy());
```

This step explicitly keeps `OcrDispatchCoordinator`, `OcrPendingRequestQueue`, node slot acquisition, and node failover intact.

- [x] **Step 5: Run focused execution tests**

Run: `mvn -pl doclens-core -Dtest=DocumentPageTaskExecutionServiceTest test`

Expected: PASS.

Evidence:
- `mvn -pl doclens-core -am -Dtest=DocumentPageTaskExecutionServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl doclens-core,doclens-spring-boot-starter -am -Dtest=DocumentPageTaskExecutionServiceTest,DocLensProcessingAutoConfigurationTest,DocLensStarterEmbeddedTest -Dsurefire.failIfNoSpecifiedTests=false test`

- [x] **Step 6: Commit**

```bash
git add doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskExecutionService.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/PageTaskWorkerScheduler.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensProcessingAutoConfiguration.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensExtractionAutoConfiguration.java \
  doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskExecutionServiceTest.java
git commit -m "feat(ocr): 增加图片级全局调度执行器"
```

### Task 4: Aggregate Results By Document and Preserve Page Order

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskAggregationService.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentJob.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/ProcessingStage.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskAggregationServiceTest.java`

- [x] **Step 1: Write the failing aggregation test**

```java
@Test
void aggregationFinalizesDocumentOnlyAfterAllPagesArriveAndSortsByPageNo() {
    pageResultRepository.upsert(pageResult("doc-1", 2, "second"));
    pageResultRepository.upsert(pageResult("doc-1", 1, "first"));
    service.recordSuccess(task("doc-1", 2), "node-b");
    service.recordSuccess(task("doc-1", 1), "node-a");

    OcrResult result = resultRepository.findByDocumentId("doc-1").orElseThrow();

    assertThat(result.pageText())
            .extracting(page -> page.get("pageNo"))
            .containsExactly(1, 2);
    assertThat(result.finalText()).contains("first").contains("second");
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `mvn -pl doclens-core -Dtest=DocumentPageTaskAggregationServiceTest test`

Expected: FAIL because aggregation service and new stage transitions do not exist.

Evidence:
- `mvn -pl doclens-core -am -Dtest=DocumentPageTaskAggregationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- RED observed for duplicate completion idempotency: `expected: 1 but was: 2`
- RED observed for listener isolation: completed page was incorrectly changed to `FAILED`

- [x] **Step 3: Add document stages for image-level flow**

```java
public enum ProcessingStage {
    QUEUED,
    PREPARING_PAGES,
    OCR_QUEUED,
    OCR_IMAGES,
    MERGE_TEXT,
    SAVE_TEXT,
    COMPLETED,
    FAILED
}
```

```java
public DocumentJob markOcrQueued(int totalPages, OffsetDateTime now) {
    return withState(DocumentStatus.PROCESSING, ProcessingStage.OCR_QUEUED, 0, 0, totalPages,
            resultId, Optional.empty(), Optional.empty(), now);
}
```

- [x] **Step 4: Finalize only when all page tasks are completed**

```java
public void recordSuccess(DocumentPageTask task, ImageOcrResult result) {
    long completed = pageTaskRepository.countCompletedByDocumentId(task.documentId());
    DocumentJob document = documentRepository.findById(task.documentId()).orElseThrow();
    documentRepository.update(document.markPageCompleted((int) completed, document.totalPages(), now));
    if (completed == document.totalPages()) {
        completeDocument(task.documentId());
    }
}
```

```java
private void completeDocument(String documentId) {
    List<ImageOcrResult> pageResults = pageResultRepository.listByDocumentId(documentId).stream()
            .map(DocumentPageResult::toImageOcrResult)
            .toList();
    DocumentTextExtractionResult extracted = DocumentTextExtractionResult.fromPageResults(
            documentId, document.fileName(), pageResults, List.of());
    // continue with merge, LLM markdown, save result, emit events
}
```

- [x] **Step 5: Run focused aggregation tests**

Run: `mvn -pl doclens-core -Dtest=DocumentPageTaskAggregationServiceTest test`

Expected: PASS.

Evidence:
- `mvn -pl doclens-core -am -Dtest=DocumentPageTaskAggregationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl doclens-core -am -Dtest=DocumentPageTaskExecutionServiceTest,DocumentPageTaskAggregationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl doclens-core,doclens-spring-boot-starter -am -Dtest=DocumentPageTaskAggregationServiceTest,DocumentPageTaskExecutionServiceTest,DocLensProcessingAutoConfigurationTest,DocLensStarterEmbeddedTest -Dsurefire.failIfNoSpecifiedTests=false test`

- [x] **Step 6: Commit**

```bash
git add doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskAggregationService.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentJob.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/ProcessingStage.java \
  doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskAggregationServiceTest.java
git commit -m "feat(ocr): 按文档聚合页结果并保持页序"
```

### Task 5: Recovery, Retry, and Restart Safety

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskExecutionService.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskAggregationService.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCase.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentJob.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskExecutionServiceTest.java`

- [x] **Step 1: Write the failing retry/resume test**

```java
@Test
void failedPageTaskRetriesWithoutDuplicatingFinalDocumentOutput() {
    executionService.execute(task("doc-1", 1).markFailed("OCR_TIMEOUT"));
    executionService.execute(task("doc-1", 1).resetForRetry());

    assertThat(pageTaskRepository.listByDocumentId("doc-1"))
            .filteredOn(task -> task.pageNo() == 1)
            .hasSize(1);
    assertThat(resultRepository.findByDocumentId("doc-1")).isPresent();
}

@Test
void recoveryMarksTaskCompletedWhenPageResultWasCommittedBeforeCrash() {
    pageResultRepository.upsert(pageResult("doc-1", 1, "done"));
    pageTaskRepository.update(task("doc-1", 1).markProcessing("worker-a", expiredLock, now));

    recoveryService.recoverExpiredTasks();

    assertThat(pageTaskRepository.findByDocumentIdAndPageNo("doc-1", 1))
            .get()
            .extracting(DocumentPageTask::status)
            .isEqualTo(DocumentPageTaskStatus.COMPLETED);
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `mvn -pl doclens-core -Dtest=DocumentPageTaskExecutionServiceTest test`

Expected: FAIL because retry/idempotency semantics are incomplete.

Evidence:
- `mvn -pl doclens-core -am -Dtest=DocumentPageTaskRecoveryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- RED observed: `DocumentPageTaskRecoveryService` and `DocumentPageTaskRecoveryDependencies` did not exist.

- [x] **Step 3: Add idempotent retry rules**

```java
UNIQUE (document_id, page_no)
```

```java
public DocumentPageTask resetForRetry(OffsetDateTime now) {
    return new DocumentPageTask(taskId, batchId, documentId, pageNo, imageStorageUri,
            DocumentPageTaskStatus.QUEUED, Optional.empty(), retryCount + 1,
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), createdAt, now);
}
```

- [x] **Step 4: Add restart recovery**

```java
public void recoverStuckTasks() {
    pageTaskRepository.listProcessingExpired(expireBefore).forEach(task -> {
        if (pageResultRepository.findByDocumentIdAndPageNo(task.documentId(), task.pageNo()).isPresent()) {
            pageTaskRepository.update(task.markCompleted(task.selectedNodeId().orElse(""), now));
        } else {
            pageTaskRepository.update(task.resetForRetry(now));
        }
    });
}
```

This keeps page tasks persistent across process restarts and prevents “lost” pages from leaving a document forever half-done.

- [x] **Step 5: Run focused retry/recovery tests**

Run: `mvn -pl doclens-core -Dtest=DocumentPageTaskExecutionServiceTest,DocumentPageTaskAggregationServiceTest test`

Expected: PASS.

Evidence:
- `mvn -pl doclens-core -am -Dtest=DocumentPageTaskRecoveryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl doclens-spring-boot-starter -am -Dtest=MybatisPlusDocumentPageTaskRepositoryTest,MybatisPlusDocumentPageResultRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl doclens-core,doclens-spring-boot-starter -am -Dtest=DocumentPageTaskRecoveryServiceTest,DocumentPageTaskExecutionServiceTest,DocumentPageTaskAggregationServiceTest,MybatisPlusDocumentPageTaskRepositoryTest,MybatisPlusDocumentPageResultRepositoryTest,DocLensProcessingAutoConfigurationTest,DocLensStarterEmbeddedTest -Dsurefire.failIfNoSpecifiedTests=false test`

- [ ] **Step 6: Commit**

```bash
git add doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskExecutionService.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskAggregationService.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCase.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/DocumentJob.java \
  doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskExecutionServiceTest.java
git commit -m "fix(ocr): 增强页任务重试与重启恢复"
```

Evidence:
- `git diff --check`
- `mvn -pl doclens-core -am -Dtest=DocumentPageTaskRecoveryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl doclens-core,doclens-spring-boot-starter -am -Dtest=DocumentPageTaskRecoveryServiceTest,DocumentPageTaskExecutionServiceTest,DocumentPageTaskAggregationServiceTest,MybatisPlusDocumentPageTaskRepositoryTest,MybatisPlusDocumentPageResultRepositoryTest,DocLensProcessingAutoConfigurationTest,DocLensStarterEmbeddedTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `git commit -F /tmp/doclens-task5-commit.txt`

### Task 6: Update Query Models and Dashboard Labels

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/query/application/ProcessingTrackAssembler.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/query/application/DashboardStageMetricsAssembler.java`
- Modify: `doclens-dashboard/src/utils/formatters.ts`
- Test: `doclens-dashboard/src/utils/__tests__/formatters.test.ts`

- [x] **Step 1: Write the failing formatter/query test**

```ts
test('stage label distinguishes preparing pages and queued OCR work', () => {
  expect(stageLabel('preparing_pages')).toBe('预处理中')
  expect(stageLabel('ocr_queued')).toBe('OCR排队中')
})
```

```java
@Test
void dashboardCountsOcrQueuedSeparatelyFromGeneralQueued() {
    assertThat(assembler.stageStatusCounts(List.of(document(ProcessingStage.OCR_QUEUED))))
            .anySatisfy(row -> assertThat(row.get("stage")).isEqualTo("ocr_queued"));
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `mvn -pl doclens-core -Dtest=DashboardStageMetricsAssemblerTest test`

Run: `npm --prefix doclens-dashboard test -- formatters`

Expected: FAIL because the new stage labels do not exist yet.

Evidence:
- `mvn -pl doclens-core -am -Dtest=DashboardQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `npm --prefix doclens-dashboard run test:utils -- formatters`
- RED observed: backend had no `ocr_queued` row; frontend still showed `queued` as `待解析`
  and excluded `ocr_queued` from image progress stages.

- [x] **Step 3: Update backend query labels and frontend labels**

```ts
const labels: Record<string, string> = {
  queued: '待调度',
  preparing_pages: '预处理中',
  ocr_queued: 'OCR排队中',
  ocr_images: 'OCR图片解析中',
  merge_text: '文本合并中',
  save_text: 'LLM排版中',
  completed: '解析完成'
}
```

- [x] **Step 4: Update progress track semantics**

```java
return normalizedStage == ProcessingStage.OCR_QUEUED
        || normalizedStage == ProcessingStage.OCR_IMAGES
        || normalizedStage == ProcessingStage.MERGE_TEXT
        || normalizedStage == ProcessingStage.SAVE_TEXT
        || normalizedStage == ProcessingStage.COMPLETED;
```

- [x] **Step 5: Run focused UI/query tests**

Run: `mvn -pl doclens-core -Dtest=DashboardStageMetricsAssemblerTest test`

Run: `npm --prefix doclens-dashboard test -- formatters`

Expected: PASS.

Evidence:
- `mvn -pl doclens-core -am -Dtest=DashboardQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `npm --prefix doclens-dashboard run test:utils -- formatters`
- `npm --prefix doclens-dashboard test`
- `mvn -pl doclens-core -am -Dtest=DashboardQueryServiceTest,OcrQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `npm --prefix doclens-dashboard run build`

- [x] **Step 6: Commit**

```bash
git add doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/query/application/ProcessingTrackAssembler.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/query/application/DashboardStageMetricsAssembler.java \
  doclens-dashboard/src/utils/formatters.ts \
  doclens-dashboard/src/utils/__tests__/formatters.test.ts
git commit -m "feat(dashboard): 细化OCR图片级调度状态"
```

Evidence:
- `git diff --check`
- `git commit -F /tmp/doclens-task6-commit.txt`

### Task 7: Broader Verification and Throughput Regression Checks

**Files:**
- Modify: `docs/superpowers/plans/2026-06-10-batch-image-level-ocr-scheduling.md`

- [ ] **Step 1: Run backend module tests**

Run: `mvn -pl doclens-core,doclens-spring-boot-starter,doclens-server -am test`

Expected: PASS.

- [ ] **Step 2: Run dashboard tests and build**

Run: `npm --prefix doclens-dashboard test`

Run: `npm --prefix doclens-dashboard run build`

Expected: PASS.

- [ ] **Step 3: Manual throughput check**

Run:

```bash
curl -sS http://127.0.0.1:10003/api/v1/dashboard/ocr-health | jq '.thread_pools, .ocr_metrics'
```

Expected:
- `global_inflight_images` climbs toward the sum of healthy node slots.
- documents from the same batch can be simultaneously in `OCR排队中` and `OCR图片解析中`.
- final text for each document remains sorted by `pageNo`.

- [ ] **Step 4: Manual correctness check**

Verify on a batch with 5 mixed files:
- no duplicate `document_id + page_no` rows,
- no page order inversion in `ocr_results.page_text`,
- deleting one failed document does not affect other documents still aggregating.

- [ ] **Step 5: Commit any final verification-only notes if needed**

```bash
git add docs/superpowers/plans/2026-06-10-batch-image-level-ocr-scheduling.md
git commit -m "docs(ocr): 补充图片级调度验证说明"
```

---

## Self-Review

**Spec coverage:** This plan covers image-level queueing, slot saturation, per-document ownership, per-page ordering, retry/recovery, and dashboard state refinement. No uncovered requirement remains from the current discussion.

**Placeholder scan:** No `TODO`/`TBD` placeholders remain; each task includes file paths, tests, and runnable commands.

**Type consistency:** The plan consistently uses `DocumentPageTask`, `DocumentPageTaskStatus`, `DocumentPageTaskRepository`, `PreparedDocumentPages`, `PageImageRef`, and `OCR_QUEUED`.
