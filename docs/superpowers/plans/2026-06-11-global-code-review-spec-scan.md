# Global Code Review Spec Scan Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 对 DocLens-j 源码执行 `code-review-spec` 全局扫描，并优先修复低风险、可验证、可提交的规范问题。

**Architecture:** 本计划把“全局扫描”和“自动修复”分开处理：扫描覆盖 Java、TypeScript、Vue 源码，但排除 `.worktrees`、`target`、`node_modules` 与生成的 dashboard 静态资源。修复只处理能通过测试锁定且语义明确的问题，复杂结构性违规记录为残留风险，避免一次性大改造成业务回归。

**Tech Stack:** Java 21、Maven、JUnit 5、AssertJ、Vue 3、TypeScript、Vitest、vue-tsc。

---

### Task 1: 消除显式 `return null` 规范违规

**Files:**
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/InMemoryOcrBatchHitTracker.java`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingTestFixtures.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingServiceTest.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/InMemoryOcrBatchHitTrackerSpecTest.java`

- [x] **Step 1: Write the failing test**

```java
@Test
void inMemoryBatchHitTrackerSourceDoesNotReturnNull() throws IOException {
    String source = Files.readString(IN_MEMORY_BATCH_HIT_TRACKER_SOURCE);

    assertThat(source).doesNotContain("return null;");
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `mvn -pl doclens-spring-boot-starter -Dtest=InMemoryOcrBatchHitTrackerSpecTest test`

Expected: FAIL because the production source currently removes a `ConcurrentHashMap` entry by returning `null` from `computeIfPresent`.

- [x] **Step 3: Write minimal implementation**

```java
String key = hitKey(batchId, modelKey, nodeId);
AtomicLong counter = hitCounts.get(key);
if (counter == null) {
    return;
}
long currentValue = counter.decrementAndGet();
if (currentValue <= 0L) {
    counter.set(0L);
}
```

- [x] **Step 4: Run focused tests**

Run: `mvn -pl doclens-spring-boot-starter -Dtest=InMemoryOcrBatchHitTrackerSpecTest test`

Expected: PASS.

- [x] **Step 5: Run broader verification**

Run: `mvn -pl doclens-core,doclens-spring-boot-starter test`

Expected: PASS.

### Task 2: 清理集合容量和测试默认值魔法数字

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingService.java`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseTest.java`
- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrNodeBootstrapperTest.java`
- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/query/infrastructure/OcrDashboardMetricsProviderTest.java`
- Modify: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/OcrGovernanceConfigApiContractTest.java`
- Modify: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrNodeControllerTest.java`

- [x] **Step 1: Remove raw collection constructors**

```java
private static final int TEST_NODE_CAPACITY = 8;
private final Map<String, OcrNode> nodes = new HashMap<>(TEST_NODE_CAPACITY);
```

- [x] **Step 2: Replace stale default magic numbers**

```java
.andExpect(jsonPath("$.probe_interval_seconds")
        .value(OcrHealthGovernance.DEFAULT_PROBE_INTERVAL_SECONDS))
```

- [x] **Step 3: Run focused tests**

Run: `mvn -pl doclens-server -am -Dtest=OcrNodeControllerTest,OcrGovernanceConfigApiContractTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS.

### Task 3: Commit low-risk global fixes

```bash
git add docs/superpowers/plans/2026-06-11-global-code-review-spec-scan.md \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/InMemoryOcrBatchHitTracker.java \
  doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/InMemoryOcrBatchHitTrackerSpecTest.java \
  doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingTestFixtures.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingService.java \
  doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseTest.java \
  doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrNodeBootstrapperTest.java \
  doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/query/infrastructure/OcrDashboardMetricsProviderTest.java \
  doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrNodeControllerTest.java \
  doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/OcrGovernanceConfigApiContractTest.java
git commit -F /tmp/doclens-global-spec-commit.txt
```

### Task 4: 记录全局残留规范风险

**Files:**
- Modify: `docs/superpowers/plans/2026-06-11-global-code-review-spec-scan.md`

- [x] **Step 1: Run bounded global scan**

Run:

```bash
find . \
  -path './.git' -prune -o \
  -path './.worktrees' -prune -o \
  -path './doclens-dashboard/node_modules' -prune -o \
  -path '*/target' -prune -o \
  -path './doclens-server/src/main/resources/static/dashboard' -prune -o \
  -type f \( -name '*.java' -o -name '*.ts' -o -name '*.vue' \) -print | sort
```

Expected: The scan lists 362 source files.

- [x] **Step 2: Capture high-confidence findings**

Run:

```bash
rg -n "System\.out|printStackTrace|console\.(log|warn|error|debug)|debugger" \
  -g '*.java' -g '*.ts' -g '*.vue' \
  -g '!**/target/**' -g '!**/node_modules/**' \
  -g '!**/.worktrees/**' \
  -g '!doclens-server/src/main/resources/static/dashboard/**' .
```

Expected: No direct output/debug statements.

- [x] **Step 3: Capture structural findings**

Run:

```bash
find . \
  -path './.git' -prune -o \
  -path './.worktrees' -prune -o \
  -path './doclens-dashboard/node_modules' -prune -o \
  -path '*/target' -prune -o \
  -path './doclens-server/src/main/resources/static/dashboard' -prune -o \
  -type f \( -name '*.java' -o -name '*.ts' -o -name '*.vue' \) -print0 \
  | xargs -0 wc -l | sort -nr | sed -n '1,80p'
```

Expected: The output records files above the 350-line limit for later focused refactors.

## Scan Notes

- Scope confirmed by user request: global source scan under this repository.
- Exclusions: `.git`, `.worktrees`, `target`, `doclens-dashboard/node_modules`, `doclens-server/src/main/resources/static/dashboard`.
- Safe auto-fix policy: only change violations with a narrow test and no broad behavior refactor.
- Direct output/debug scan result: no production `System.out` / `printStackTrace` / frontend `console.*` / `debugger` findings.
- Raw `return null` scan result: only two guard tests intentionally contain the literal string in assertions/comments.
- Raw `new ArrayList<>()` / `new HashMap<>()` / `new HashSet<>()` scan result: no remaining findings in scoped Java source.
- Deferred risks: many files exceed the strict 350-line file/class rule, and full method/comment-ratio enforcement requires separate focused refactor tasks.
