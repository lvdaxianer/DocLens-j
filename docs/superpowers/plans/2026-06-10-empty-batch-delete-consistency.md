# Empty Batch Delete Consistency Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复删除最后一个文档后批次仍以 `0/0` 成功态出现在总览的问题，让已无数据的批次不再被读模型继续展示。

**Architecture:** 这次修复直接落在数据一致性层，而不是在 Dashboard 做展示遮掩。删除用例在发现批次内已无剩余文档时，直接删除空批次；同时补充核心层与 API 契约测试，验证批次详情与总览都不再暴露这条空数据。

**Tech Stack:** Java 21, Spring Boot 3.5, Maven, MyBatis-Plus, H2, JUnit 5, AssertJ, Spring MockMvc.

---

## File Map

- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/domain/BatchRepository.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentDeleteUseCase.java`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentDeleteUseCaseTest.java`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/query/application/DashboardQueryServiceTest.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/infrastructure/MybatisPlusBatchRepository.java`
- Modify: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/DocLensOcrApiContractTest.java`

### Task 1: 删除最后一个文档时移除空批次

**Files:**
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/query/application/DashboardQueryServiceTest.java`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentDeleteUseCaseTest.java`
- Modify: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/DocLensOcrApiContractTest.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/domain/BatchRepository.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentDeleteUseCase.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/infrastructure/MybatisPlusBatchRepository.java`

- [x] **Step 1: 写失败测试**

```java
@Test
void summaryDoesNotExposeBatchWithoutDocuments() {
    InMemoryBatchRepository batchRepository = new InMemoryBatchRepository(List.of(
            new Batch("batch-empty", BatchStatus.COMPLETED, 0, 0, 0, Optional.empty(), Optional.empty(),
                    "completed", JsonPayload.empty(), Optional.empty(), Optional.empty(), BASE_TIME,
                    BASE_TIME.plusMinutes(30))
    ));
    DashboardQueryService service = new DashboardQueryService(batchRepository,
            new InMemoryDocumentJobRepository(List.of()), new InMemoryOcrEventRepository());

    Map<String, Object> summary = service.summary();

    assertThat(summary.get("recent_batches"))
            .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
            .isEmpty();
}
```

```java
@Test
void deleteLastDocumentRemovesEmptyBatch() {
    // 删除最后一个文档后，批次仓储中不再保留该批次。
}
```

```java
@Test
void deletingLastCompletedDocumentRemovesBatchFromDashboardApis() throws Exception {
    // 调用 DELETE /api/v1/documents/{documentId} 后，
    // GET /api/v1/batches/{batchId} 返回 404，
    // GET /api/v1/dashboard/summary 的 recent_batches 不再包含该 batchId。
}
```

- [x] **Step 2: 运行测试确认 RED**

Run:
`mvn -pl doclens-core -am -Dtest=DashboardQueryServiceTest,DocumentDeleteUseCaseTest -Dsurefire.failIfNoSpecifiedTests=false test`

Run:
`mvn -pl doclens-server -am -Dtest=DocLensOcrApiContractTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected:
- `DashboardQueryServiceTest` 失败，因为当前总览仍返回空批次。
- `DocumentDeleteUseCaseTest` 失败，因为当前删除最后一个文档只会把批次写成 `0/0 completed`。
- `DocLensOcrApiContractTest` 失败，因为当前批次详情和总览仍可查到空批次。

- [x] **Step 3: 写最小实现**

```java
public interface BatchRepository {

    void deleteById(String batchId);
}
```

```java
private void refreshBatchSummary(String batchId) {
    List<DocumentJob> documents = documentRepository.listByBatchId(batchId);
    if (documents.isEmpty()) {
        batchRepository.deleteById(batchId);
    } else {
        batchRepository.updateSummary(batchId, documents.size(), completedCount, failedCount, batchStatus(documents));
    }
}
```

```java
@Override
public void deleteById(String batchId) {
    removeById(batchId);
}
```

- [x] **Step 4: 运行测试确认 GREEN**

Run:
`mvn -pl doclens-core -am -Dtest=DashboardQueryServiceTest,DocumentDeleteUseCaseTest -Dsurefire.failIfNoSpecifiedTests=false test`

Run:
`mvn -pl doclens-server -am -Dtest=DocLensOcrApiContractTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected:
- 目标测试全部通过。
- 删除最后一个文档后，空批次不再出现在 Dashboard 总览和批次详情接口里。

- [x] **Step 5: 执行更宽验证**

Run:
`mvn -pl doclens-core -am -Dtest=DocumentDeleteUseCaseTest,DashboardQueryServiceTest,DocumentRetryUseCaseTest,StaleDocumentRecoveryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`

Run:
`mvn -pl doclens-server -am -Dtest=DocLensOcrApiContractTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected:
- 删除、重试、卡死恢复相关核心测试保持通过。
- API 契约测试保持通过，没有引入批次查询回归。

- [x] **Step 6: 执行 code-review-spec**

Review checklist:
- 只修改本任务相关 diff，没有带入主工作区未提交的在线 OCR 变更。
- 删除逻辑没有在循环中调用数据库或远程服务。
- 空批次删除路径没有吞异常，404 行为与现有 `ResourceNotFoundException` 契约一致。
- 新增仓储接口与测试命名清晰，没有魔法字符串扩散。

- [x] **Step 7: 提交**

```bash
git add docs/superpowers/plans/2026-06-10-empty-batch-delete-consistency.md \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/domain/BatchRepository.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentDeleteUseCase.java \
  doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentDeleteUseCaseTest.java \
  doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/query/application/DashboardQueryServiceTest.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/infrastructure/MybatisPlusBatchRepository.java \
  doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/DocLensOcrApiContractTest.java
git commit
```

Suggested subject:
`fix(batch): 删除空批次残留`
