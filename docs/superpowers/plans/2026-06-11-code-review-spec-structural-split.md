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
