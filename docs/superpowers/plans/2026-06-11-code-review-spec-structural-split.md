# Code Review Spec Structural Split Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按 `code-review-spec` 拆分 DocLens-j 中超过复杂度与行数限制的源文件，先从生产代码里最高风险的 `BatchProcessingUseCase` 开始。

**Architecture:** 每次只拆一个职责边界，保持对外 API 不变，并用源文件行数测试作为 RED/GREEN 门禁。第一轮把批次编排、文档结果构建、事件构建拆成独立类，确保 `BatchProcessingUseCase.java` 降到 350 行以内。

**Tech Stack:** Java 21、Maven、JUnit 5、AssertJ。

---

### Task 1: 拆分 BatchProcessingUseCase

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCase.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentOcrResultBuilder.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentProcessingEventBuilder.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/architecture/CoreCodeReviewSpecStructureTest.java`

- [x] **Step 1: Write failing structure test**

```java
@Test
void batchProcessingUseCaseStaysWithinCodeReviewSpecLineLimit() throws IOException {
    Path source = CORE_SOURCE_ROOT.resolve("io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCase.java");

    assertThat(Files.readAllLines(source)).hasSizeLessThanOrEqualTo(350);
}
```

- [x] **Step 2: Run RED**

Run: `mvn -pl doclens-core -Dtest=CoreCodeReviewSpecStructureTest test`

Expected: FAIL because `BatchProcessingUseCase.java` currently has 704 lines.

- [x] **Step 3: Extract result builder**

Move OCR result assembly, Markdown post-processing, raw output tracing, warning merge, stage reporter, and Markdown storage writing into `DocumentOcrResultBuilder`.

- [x] **Step 4: Extract event builder**

Move completion events, failed event, batch finished event, callback body and event detail routing into `DocumentProcessingEventBuilder`.

- [x] **Step 5: Keep BatchProcessingUseCase as orchestration only**

Keep batch loading, document task dispatch, persistence, and finish status calculation in the use case.

- [x] **Step 6: Run focused verification**

Run: `mvn -pl doclens-core -Dtest=CoreCodeReviewSpecStructureTest,BatchProcessingUseCaseTest test`

Expected: PASS.

- [x] **Step 7: Run broader verification**

Run: `mvn -pl doclens-core test`

Expected: PASS.

- [x] **Step 8: Commit**

```bash
git add docs/superpowers/plans/2026-06-11-code-review-spec-structural-split.md \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCase.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentOcrResultBuilder.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentProcessingEventBuilder.java \
  doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/architecture/CoreCodeReviewSpecStructureTest.java
git commit -F /tmp/doclens-batch-processing-split-commit.txt
```

### Task 2: 继续拆分下一个生产超限文件

**Candidate Files:**
- `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensProcessingAutoConfiguration.java`
- `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrNode.java`
- `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/DashScopeOnlineOcrClient.java`
- `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrNodeManagementService.java`

- [x] **Step 1: Pick highest-risk next source file**

Run:

```bash
find . \
  -path './.git' -prune -o \
  -path './.worktrees' -prune -o \
  -path './doclens-dashboard/node_modules' -prune -o \
  -path '*/target' -prune -o \
  -path './doclens-server/src/main/resources/static/dashboard' -prune -o \
  -type f \( -name '*.java' -o -name '*.ts' -o -name '*.vue' \) -print0 \
  | xargs -0 wc -l | awk '$1 > 350 {print}' | sort -nr
```

Expected: The next task starts from the highest-risk remaining production file, not tests.

Selected: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensProcessingAutoConfiguration.java`

- [x] **Step 2: Write failing starter structure test**

Add a starter architecture test that asserts `DocLensProcessingAutoConfiguration.java` stays within 350 lines.

- [x] **Step 3: Run RED**

Run: `mvn -pl doclens-spring-boot-starter -am -Dtest=StarterCodeReviewSpecStructureTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: FAIL because `DocLensProcessingAutoConfiguration.java` currently has 650 lines.

- [x] **Step 4: Extract LLM Markdown auto-configuration**

Move LLM Markdown service, tester, health checker, scheduler and Markdown post processor beans into
`DocLensLlmMarkdownAutoConfiguration`.

- [x] **Step 5: Extract stale document recovery auto-configuration**

Move stale document recovery service, dependencies, scheduler executor, scheduler and runner into
`DocLensStaleDocumentRecoveryAutoConfiguration`.

- [x] **Step 6: Keep DocLensProcessingAutoConfiguration focused**

Keep batch processing, create batch, retry/delete and processing executor beans in the original class.

- [x] **Step 7: Register extracted auto-configurations**

Add extracted auto-configuration classes to `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

- [x] **Step 8: Run focused verification**

Run: `mvn -pl doclens-spring-boot-starter -am -Dtest=StarterCodeReviewSpecStructureTest,DocLensProcessingAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS.

- [x] **Step 9: Run broader verification**

Run: `mvn -pl doclens-spring-boot-starter -am -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS.

- [x] **Step 10: Commit**

Create an atomic Chinese Conventional Commit for the starter auto-configuration split.

### Task 3: 拆分 OcrNode 领域对象

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrNode.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrNodeNormalization.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/architecture/CoreCodeReviewSpecStructureTest.java`

- [x] **Step 1: Write failing structure test**

Add `OcrNode.java` to the core structure test line-limit list.

- [x] **Step 2: Run RED**

Run: `mvn -pl doclens-core -am -Dtest=CoreCodeReviewSpecStructureTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: FAIL because `OcrNode.java` currently has 471 lines.

- [x] **Step 3: Extract node normalization**

Move host, port, online channel, provider model, optional text and positive number normalization into
`OcrNodeNormalization`.

- [x] **Step 4: Keep OcrNode focused on domain state**

Keep factory methods, status transitions, and immutable node reconstruction in `OcrNode`.

- [x] **Step 5: Run focused verification**

Run: `mvn -pl doclens-core -am -Dtest=CoreCodeReviewSpecStructureTest,OcrNodeTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS.

- [x] **Step 6: Run broader verification**

Run: `mvn -pl doclens-core -am -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS.

- [x] **Step 7: Commit**

Create an atomic Chinese Conventional Commit for the OcrNode domain object split.

### Task 4: 拆分 DashScopeOnlineOcrClient

**Files:**
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/DashScopeOnlineOcrClient.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/DashScopeOcrPayloadFactory.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/DashScopeOcrResponseMapper.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/DashScopeOcrResponseSanitizer.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/autoconfigure/StarterCodeReviewSpecStructureTest.java`

- [x] **Step 1: Write failing structure test**

Add `DashScopeOnlineOcrClient.java` to the starter structure test line-limit list.

- [x] **Step 2: Run RED**

Run: `mvn -pl doclens-spring-boot-starter -am -Dtest=StarterCodeReviewSpecStructureTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: FAIL because `DashScopeOnlineOcrClient.java` currently has 434 lines.

- [x] **Step 3: Extract payload factory**

Move request body, multimodal message content, MIME inference, data URL and permission probe request into
`DashScopeOcrPayloadFactory`.

- [x] **Step 4: Extract response mapper and sanitizer**

Move compatible response mapping and raw output conversion into `DashScopeOcrResponseMapper`; move response
body sanitizing and truncation into `DashScopeOcrResponseSanitizer`.

- [x] **Step 5: Keep client focused on HTTP orchestration**

Keep request sending, permission probing, logging and exception handling in `DashScopeOnlineOcrClient`.

- [x] **Step 6: Run focused verification**

Run: `mvn -pl doclens-spring-boot-starter -am -Dtest=StarterCodeReviewSpecStructureTest,DashScopeOnlineOcrClientTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS.

- [x] **Step 7: Run broader verification**

Run: `mvn -pl doclens-spring-boot-starter -am -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS.

- [x] **Step 8: Commit**

Create an atomic Chinese Conventional Commit for the DashScope online OCR client split.

### Task 5: 拆分 OcrNodeManagementService

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrNodeManagementService.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrNodeSettingsRequestFactory.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/architecture/CoreCodeReviewSpecStructureTest.java`

- [x] **Step 1: Write failing structure test**

Add `OcrNodeManagementService.java` to the core structure test line-limit list.

- [x] **Step 2: Run RED**

Run: `mvn -pl doclens-core -am -Dtest=CoreCodeReviewSpecStructureTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: FAIL because `OcrNodeManagementService.java` currently has 408 lines.

- [x] **Step 3: Extract settings request factory**

Move deployment type defaults, endpoint/online/scheduling defaults, API key validation and
`OcrNodeCreateRequest` construction into `OcrNodeSettingsRequestFactory`.

- [x] **Step 4: Keep service focused on application orchestration**

Keep model support checks, uniqueness checks, repository writes and node pool refresh in
`OcrNodeManagementService`.

- [x] **Step 5: Run focused verification**

Run: `mvn -pl doclens-core -am -Dtest=CoreCodeReviewSpecStructureTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS.

- [x] **Step 6: Run broader verification**

Run: `mvn -pl doclens-core -am -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS.

- [x] **Step 7: Commit**

Create an atomic Chinese Conventional Commit for the OCR node management service split.

### Task 6: 拆分 OcrHealthChecker

**Files:**
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthChecker.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthNodeStateUpdater.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/autoconfigure/StarterCodeReviewSpecStructureTest.java`

- [x] **Step 1: Write failing structure test**

Add `OcrHealthChecker.java` to the starter structure test line-limit list.

- [x] **Step 2: Run RED**

Run: `mvn -pl doclens-spring-boot-starter -am -Dtest=StarterCodeReviewSpecStructureTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: FAIL because `OcrHealthChecker.java` currently has 401 lines.

- [x] **Step 3: Extract node state updater**

Move health success/failure state calculation, recovery threshold checks, circuit window updates and health timestamp
replacement into `OcrHealthNodeStateUpdater`.

- [x] **Step 4: Keep checker focused on probing and persistence**

Keep task submission, health client calls, probe window checks, logging and repository updates in `OcrHealthChecker`.

- [x] **Step 5: Run focused verification**

Run: `mvn -pl doclens-spring-boot-starter -am -Dtest=StarterCodeReviewSpecStructureTest,OcrHealthCheckerTest,OcrHealthCheckerLifecycleTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS.

- [x] **Step 6: Run broader verification**

Run: `mvn -pl doclens-spring-boot-starter -am -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS.

- [x] **Step 7: Commit**

Create an atomic Chinese Conventional Commit for the OCR health checker split.

### Task 7: 拆分 OcrDashboardMetricsProvider 边缘超限

**Files:**
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/query/infrastructure/OcrDashboardMetricsProvider.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/query/infrastructure/OcrDashboardHitNodeKey.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/autoconfigure/StarterCodeReviewSpecStructureTest.java`

- [x] **Step 1: Write failing structure test**

Add `OcrDashboardMetricsProvider.java` to the starter structure test line-limit list.

- [x] **Step 2: Run RED**

Run: `mvn -pl doclens-spring-boot-starter -am -Dtest=StarterCodeReviewSpecStructureTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: FAIL because `OcrDashboardMetricsProvider.java` currently has 351 lines.

- [x] **Step 3: Extract hit node key**

Move the private hit node aggregation key into `OcrDashboardHitNodeKey`.

- [x] **Step 4: Run focused verification**

Run: `mvn -pl doclens-spring-boot-starter -am -Dtest=StarterCodeReviewSpecStructureTest,OcrDashboardMetricsProviderTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS.

- [x] **Step 5: Run broader verification**

Run: `mvn -pl doclens-spring-boot-starter -am -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS.

- [x] **Step 6: Commit**

Create an atomic Chinese Conventional Commit for the dashboard metrics provider line-limit cleanup.

### Task 8: 拆分 BatchProcessingUseCaseTest 测试职责

**Files:**
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseTest.java`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/architecture/CoreCodeReviewSpecStructureTest.java`
- Create: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseLlmMarkdownTest.java`
- Create: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseTestSupport.java`
- Create: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseRepositories.java`
- Create: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseEventRepositories.java`
- Create: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseExtractors.java`
- Create: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseMarkdownProcessors.java`
- Create: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseInfrastructure.java`

- [x] **Step 1: Write failing structure test**

Add core test sources touched by this split to `CoreCodeReviewSpecStructureTest`.

- [x] **Step 2: Run RED**

Run: `mvn -pl doclens-core -am -Dtest=CoreCodeReviewSpecStructureTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: FAIL because `BatchProcessingUseCaseTest.java` currently has more than 350 lines.

- [x] **Step 3: Extract shared test support**

Move test factories, in-memory repositories, extractors, Markdown processors and stub infrastructure into focused
package-private test support classes.

- [x] **Step 4: Split LLM Markdown scenarios**

Move LLM Markdown post-processing tests into `BatchProcessingUseCaseLlmMarkdownTest`.

- [x] **Step 5: Run focused verification**

Run: `mvn -pl doclens-core -am -Dtest=CoreCodeReviewSpecStructureTest,BatchProcessingUseCaseTest,BatchProcessingUseCaseLlmMarkdownTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS.

- [x] **Step 6: Run broader verification**

Run: `mvn -pl doclens-core -am -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS.

- [x] **Step 7: Commit**

Create an atomic Chinese Conventional Commit for the batch processing use case test split.

### Task 9: 拆分 DocumentDeleteUseCaseTest 测试职责

**Files:**
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentDeleteUseCaseTest.java`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/architecture/CoreCodeReviewSpecStructureTest.java`
- Create: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchDeleteUseCaseTest.java`
- Create: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentDeleteUseCaseTestSupport.java`
- Create: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentDeleteUseCaseRepositories.java`
- Create: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentDeleteUseCaseInfrastructure.java`
- Create: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DeleteUseCaseDocumentJobRepository.java`
- Create: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DeleteUseCaseBatchRepository.java`
- Create: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DeleteUseCaseOcrResultRepository.java`
- Create: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DeleteUseCaseOcrEventRepository.java`

- [x] **Step 1: Write failing structure test**

Add `DocumentDeleteUseCaseTest.java` to `CoreCodeReviewSpecStructureTest.TOUCHED_PROCESSING_TESTS`.

- [x] **Step 2: Run RED**

Run: `mvn -pl doclens-core -am -Dtest=CoreCodeReviewSpecStructureTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: FAIL because `DocumentDeleteUseCaseTest.java` currently has more than 350 lines.

- [x] **Step 3: Split batch deletion scenarios**

Move `deleteBatchRemovesAllDeletableDocumentsAndEmptyBatch` and
`deleteBatchRejectsNonDeletableDocumentsWithoutPartialDelete` into `BatchDeleteUseCaseTest`.

- [x] **Step 4: Extract shared support builders**

Move shared document, stalled document, batch, result, event and use case factories into
`DocumentDeleteUseCaseTestSupport`.

- [x] **Step 5: Extract in-memory test doubles**

Move repositories into `DocumentDeleteUseCaseRepositories`, and object storage plus transaction runner into
`DocumentDeleteUseCaseInfrastructure`.

- [x] **Step 6: Run focused verification**

Run: `mvn -pl doclens-core -am -Dtest=CoreCodeReviewSpecStructureTest,DocumentDeleteUseCaseTest,BatchDeleteUseCaseTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS.

- [x] **Step 7: Run broader verification**

Run: `mvn -pl doclens-core -am -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS.

- [ ] **Step 8: Commit**

Create an atomic Chinese Conventional Commit for the document deletion use case test split.
