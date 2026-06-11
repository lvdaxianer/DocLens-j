# Batch Delete And Compact Detail Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在总览页支持批次删除，详情页删除最后一条数据后自动返回上一页，并压缩批次详情展示密度。

**Architecture:** 后端新增批次删除用例和 `DELETE /api/v1/batches/{batchId}`，复用现有文档删除规则，只允许删除已完成、失败或卡死文档组成的批次。前端在总览批次表格通过事件触发批次删除，详情页在删除后检测剩余文档数量并自动返回上一页，轨道与表格样式做紧凑化调整。

**Tech Stack:** Java 21, Spring Boot MockMvc, JUnit 5, Vue 3 `<script setup lang="ts">`, Pinia, Naive UI, Vitest.

---

### Task 1: 后端批次删除 API

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/BatchDeleteUseCase.java`
- Modify: `doclens-api/src/main/java/io/github/lvdaxianer/doclens/j/api/DocLensEngine.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/api/DefaultDocLensEngine.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/query/interfaces/OcrQueryController.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensProcessingAutoConfiguration.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensAutoConfiguration.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentDeleteUseCaseTest.java`
- Test: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/DocLensOcrApiContractTest.java`

- [x] **Step 1: Write failing core test**

Add a test that creates a batch with completed and stalled documents, calls `BatchDeleteUseCase.delete("batch-test")`, and expects all documents, OCR results, events, storage objects and the batch row to be removed.

- [x] **Step 2: Run RED**

Run:

```bash
mvn -pl doclens-core -Dtest=DocumentDeleteUseCaseTest#deleteBatchRemovesAllDeletableDocumentsAndEmptyBatch test
```

Expected: FAIL because `BatchDeleteUseCase` does not exist yet.

- [x] **Step 3: Implement core batch delete**

Create `BatchDeleteUseCase` with full class/method comments. It loads documents by batch id, rejects missing/empty batches, validates all documents through existing `DocumentDeleteUseCase.delete`, and returns the deleted count.

- [x] **Step 4: Add API contract test**

Add a MockMvc test for `DELETE /api/v1/batches/{batchId}` that uploads a completed batch, deletes it, verifies response contains `batch_id`, `status=deleted`, `deleted_documents`, and verifies batch/document queries return 404.

- [x] **Step 5: Wire HTTP API**

Add `DocLensEngine.deleteBatch`, `DefaultDocLensEngine.deleteBatch`, controller mapping and Spring beans.

- [x] **Step 6: Verify and commit**

Run:

```bash
mvn -pl doclens-core -Dtest=DocumentDeleteUseCaseTest test
mvn -pl doclens-server -Dtest=DocLensOcrApiContractTest test
mvn -pl doclens-server -am package -DskipTests
git diff --check
```

Apply `code-review-spec`, fix issues, then commit with Chinese Conventional Commit.

Verification note:
- `mvn -pl doclens-server -Dtest=DocLensOcrApiContractTest#completedBatchCanBeDeletedByBatchEndpoint test` fails when run without `-am` because upstream module classes are missing from the isolated module classpath.
- `mvn -pl doclens-server -am -Dtest=DocLensOcrApiContractTest#completedBatchCanBeDeletedByBatchEndpoint -Dsurefire.failIfNoSpecifiedTests=false test` passes.
- `mvn -pl doclens-core -Dtest=DocumentDeleteUseCaseTest test`, `mvn -pl doclens-server -am package -DskipTests`, and `git diff --check` pass.

### Task 2: 前端批次删除、详情返回和紧凑样式

**Files:**
- Modify: `doclens-dashboard/src/api/dashboard.ts`
- Modify: `doclens-dashboard/src/views/OverviewView.vue`
- Modify: `doclens-dashboard/src/components/dashboard/BatchTable.vue`
- Modify: `doclens-dashboard/src/views/BatchDetailView.vue`
- Modify: `doclens-dashboard/src/components/dashboard/DocumentTrackCards.vue`
- Modify: `doclens-dashboard/src/components/dashboard/ProcessingRail.vue`
- Test: `doclens-dashboard/src/components/dashboard/__tests__/BatchDetailView.delete.test.ts`
- Test: `doclens-dashboard/src/components/dashboard/__tests__/BatchTable.delete.test.ts`

- [ ] **Step 1: Write failing UI tests**

Add tests proving:
- `BatchTable` emits `delete-batch` after confirmation.
- `BatchDetailView` calls router back when deleting leaves no documents in the selected batch.

- [ ] **Step 2: Run RED**

Run:

```bash
cd doclens-dashboard && npm run test:ui -- BatchTable.delete.test.ts BatchDetailView.delete.test.ts
```

Expected: FAIL because the batch delete button/event and no-document navigation do not exist.

- [ ] **Step 3: Implement front-end delete flow**

Add `deleteBatch(batchId)` API, wire OverviewView loading state and refresh behavior, add delete confirmation button to `BatchTable`, and update `BatchDetailView` to navigate back after deleting the last document.

- [ ] **Step 4: Compact detail display**

Reduce detail card padding, rail dot size, rail node min height, table cell padding, progress height and font sizes so content remains unchanged but more rows and stages fit on screen.

- [ ] **Step 5: Verify and commit**

Run:

```bash
cd doclens-dashboard && npm run test:ui
cd doclens-dashboard && npm run build
git diff --check
```

Apply `code-review-spec`, fix issues, then commit with Chinese Conventional Commit.

---

## Self-Review

Spec coverage:
- 总览批次删除、详情删除后返回上一页、内容不变但密度变小均有任务覆盖。

Placeholder scan:
- No TBD or TODO placeholders remain.

Scope check:
- The task is focused on batch/document deletion UI and compact display only; OCR routing, health checks and LLM processing are out of scope.
