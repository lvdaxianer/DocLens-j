# LLM Result and Page Task Observability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Dashboard LLM status messaging accurate for applied vs configured behavior, and surface the real reason page tasks fail so operators can tell lock/state mismatches from OCR failures.

**Architecture:** Keep the result-status fix in the Dashboard read-model utilities and drawer UI so the current result payload continues to drive the display. For page-task failures, keep the write path unchanged but enrich the repository error message with the task's current state and lock metadata so the existing exception explains whether the update failed because the task was not PROCESSING, the worker did not own the lock, or the lock was stale.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, Vue 3, TypeScript, Vitest/node:test, Maven/Gradle project test slices.

---

### Task 1: Make LLM result status text describe application state instead of configuration state

**Files:**
- Modify: `doclens-dashboard/src/utils/llmResultDisplayRules.ts`
- Modify: `doclens-dashboard/src/components/dashboard/DocumentResultDrawer.vue`
- Modify: `doclens-dashboard/src/utils/__tests__/llmResultDisplayRules.test.ts`
- Modify: `doclens-dashboard/src/components/dashboard/__tests__/DocumentResultDrawer.test.ts`

- [ ] **Step 1: Write the failing test**

```ts
import test from 'node:test'
import assert from 'node:assert/strict'
import { llmPostProcessingStatus, llmStageDescription } from '../llmResultDisplayRules.ts'

test('llm result display says not applied when markdown was configured but not used', () => {
  const result = {
    finalText: 'OCR 文本',
    llmMarkdownApplied: false,
    warnings: ['no_available_llm_config']
  }

  assert.equal(llmPostProcessingStatus(result), 'LLM 未应用')
  assert.equal(llmStageDescription(result), 'LLM 已配置但本次结果未应用，返回 OCR 纯文本')
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `pnpm vitest doclens-dashboard/src/utils/__tests__/llmResultDisplayRules.test.ts -t "not applied"`
Expected: FAIL because the current implementation still returns `LLM 未启用` / configuration-centric text.

- [ ] **Step 3: Write minimal implementation**

```ts
export function llmPostProcessingStatus(result: DocumentResultDisplayPayload): string {
  if (isLlmMarkdownApplied(result)) {
    return 'LLM 已排版'
  } else if (isLlmMarkdownFallback(result)) {
    return 'LLM 回退 OCR'
  } else if (result.rawVendorOutput?.llm_markdown_applied !== undefined || result.llmMarkdownApplied !== undefined) {
    return 'LLM 未应用'
  } else {
    return 'LLM 未启用'
  }
}

export function llmStageDescription(result: DocumentResultDisplayPayload): string {
  if (isLlmMarkdownApplied(result)) {
    return '已输出 Markdown 结构化结果'
  } else if (isLlmMarkdownFallback(result)) {
    return 'LLM 排版失败，已回退 OCR 纯文本'
  } else if (result.rawVendorOutput?.llm_markdown_applied !== undefined || result.llmMarkdownApplied !== undefined) {
    return 'LLM 已配置但本次结果未应用，返回 OCR 纯文本'
  } else {
    return '未配置 LLM 后处理，返回 OCR 纯文本'
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `pnpm vitest doclens-dashboard/src/utils/__tests__/llmResultDisplayRules.test.ts`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add doclens-dashboard/src/utils/llmResultDisplayRules.ts doclens-dashboard/src/components/dashboard/DocumentResultDrawer.vue doclens-dashboard/src/utils/__tests__/llmResultDisplayRules.test.ts doclens-dashboard/src/components/dashboard/__tests__/DocumentResultDrawer.test.ts
git commit -m "🐛 fix(dashboard): clarify llm result status"
```

### Task 2: Surface page-task completion failure context in the repository exception

**Files:**
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageTaskRepository.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/DocumentPageTaskMapper.java`
- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageTaskRepositoryTest.java`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskExecutionServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
assertThatThrownBy(() -> repository.markCompleted(completeRequest("task-9", "worker-b")))
    .isInstanceOf(IllegalStateException.class)
    .hasMessageContaining("task-9")
    .hasMessageContaining("PROCESSING")
    .hasMessageContaining("worker-a")
    .hasMessageContaining("worker-b");
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl doclens-spring-boot-starter -Dtest=MybatisPlusDocumentPageTaskRepositoryTest test`
Expected: FAIL because the current exception only says `page task complete state update failed: <taskId>`.

- [ ] **Step 3: Write minimal implementation**

```java
private void ensureUpdated(int updatedRows, String taskId, String operation) {
  if (updatedRows != UPDATED_ONE_ROW) {
    DocumentPageTaskEntity entity = getBaseMapper().selectById(taskId);
    throw new IllegalStateException("page task %s state update failed: taskId=%s, status=%s, lockedBy=%s, lockedUntil=%s"
        .formatted(operation, taskId,
            entity == null ? "missing" : entity.getStatus(),
            entity == null ? "missing" : String.valueOf(entity.getLockedBy()),
            entity == null ? "missing" : String.valueOf(entity.getLockedUntil())));
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl doclens-spring-boot-starter -Dtest=MybatisPlusDocumentPageTaskRepositoryTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageTaskRepository.java doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/DocumentPageTaskMapper.java doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageTaskRepositoryTest.java doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskExecutionServiceTest.java
git commit -m "🐛 fix(processing): expose page task completion context"
```

### Task 3: Verify the dashboard and backend regression slices

**Files:**
- Test: `doclens-dashboard/src/utils/__tests__/llmResultDisplayRules.test.ts`
- Test: `doclens-dashboard/src/components/dashboard/__tests__/DocumentResultDrawer.test.ts`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageTaskRepositoryTest.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskExecutionServiceTest.java`

- [ ] **Step 1: Run the focused dashboard tests**

Run: `pnpm vitest doclens-dashboard/src/utils/__tests__/llmResultDisplayRules.test.ts doclens-dashboard/src/components/dashboard/__tests__/DocumentResultDrawer.test.ts`
Expected: PASS.

- [ ] **Step 2: Run the focused backend repository tests**

Run: `mvn -pl doclens-spring-boot-starter -Dtest=MybatisPlusDocumentPageTaskRepositoryTest test`
Expected: PASS.

- [ ] **Step 3: Run the focused backend execution tests**

Run: `mvn -pl doclens-core -Dtest=DocumentPageTaskExecutionServiceTest test`
Expected: PASS.

- [ ] **Step 4: Run the broader relevant slice**

Run: `mvn -pl doclens-core,doclens-spring-boot-starter test -DskipITs`
Expected: PASS for the touched processing area.

- [ ] **Step 5: Commit**

```bash
git add doclens-dashboard/src/utils/__tests__/llmResultDisplayRules.test.ts doclens-dashboard/src/components/dashboard/__tests__/DocumentResultDrawer.test.ts doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusDocumentPageTaskRepositoryTest.java doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentPageTaskExecutionServiceTest.java
git commit -m "✅ test(processing): cover llm status and task failure context"
```
