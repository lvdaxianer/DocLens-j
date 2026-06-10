# Model Heartbeat Pool Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 OCR 模型与 LLM 模型建立统一、隔离、可持久化、可观测的心跳检测与运行时健康池，确保心跳线程不受业务线程池阻塞影响，并在心跳失败或请求连续失败后及时摘除模型。

**Architecture:** 在现有 `OcrHealthChecker`、`OcrHealthCheckScheduler`、`OcrRuntimeNodePool` 和 `LlmMarkdownHealthChecker` 基础上抽象统一的模型健康目标、探针、运行时心跳池与 DB 同步器。心跳 worker 使用独立线程池和独立调度线程池，每个目标串行探测，默认 5 秒周期，失败后 1 秒间隔重试 3 次；请求链路连续失败 3 次后通过同一状态入口标记目标不可用。DB 是持久化事实来源，内存池是请求路由实时依据，双向同步通过 `lastHeartbeatAt/updatedAt` 做新鲜度保护。

**Tech Stack:** Java 21, Spring Boot 3, MyBatis-Plus, Flyway, JUnit 5, AssertJ, Mockito, Vue dashboard existing APIs.

---

## Scope And Current Context

当前仓库已有以下基础能力，应优先复用：

- OCR 节点健康检查：`doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthChecker.java`
- OCR 周期调度：`doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthCheckScheduler.java`
- OCR 运行时节点池：`doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrRuntimeNodePool.java`
- OCR 请求重试与故障转移：`doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingService.java`
- LLM 健康检查：`doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/LlmMarkdownHealthChecker.java`
- LLM 周期调度：`doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/LlmMarkdownHealthCheckScheduler.java`
- 线程池配置：`doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensOcrThreadPoolAutoConfiguration.java`
- OCR 治理配置：`doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrHealthGovernance.java`

本计划不做 dashboard 大改，只保证后端状态可查询、可持久化、可用于路由。前端展示可在后续单独计划中接入更细的失败类型和指标。

## File Structure

新增或修改文件职责如下：

- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/domain/ModelHealthStatus.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/domain/ModelHealthFailureType.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/domain/ModelHealthTargetType.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/domain/ModelHealthTargetId.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/domain/ModelHealthSnapshot.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/domain/ModelHealthState.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/domain/ModelHealthRepository.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/application/ModelHeartbeatPool.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/application/InMemoryModelHeartbeatPool.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/application/ModelHeartbeatProbe.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/application/ModelHeartbeatProbeResult.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/application/ModelHeartbeatWorker.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/application/ModelHeartbeatWorkerSettings.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/application/ModelHeartbeatSyncService.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/health/infrastructure/ModelHealthEntity.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/health/infrastructure/ModelHealthMapper.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/health/infrastructure/MybatisPlusModelHealthRepository.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/health/infrastructure/OcrNodeHeartbeatProbe.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/health/infrastructure/LlmMarkdownHeartbeatProbe.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/health/infrastructure/ModelHeartbeatScheduler.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensModelHeartbeatAutoConfiguration.java`
- Create: `doclens-spring-boot-starter/src/main/resources/db/migration/V9__doclens_model_health.sql`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringProperties.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensOcrThreadPoolAutoConfiguration.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingDependencies.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingService.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensOcrResourceAutoConfiguration.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/LlmMarkdownHealthChecker.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/health/domain/ModelHealthStateTest.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/health/application/InMemoryModelHeartbeatPoolTest.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/health/application/ModelHeartbeatWorkerTest.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/health/application/ModelHeartbeatSyncServiceTest.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingServiceTest.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/health/infrastructure/MybatisPlusModelHealthRepositoryTest.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensModelHeartbeatAutoConfigurationTest.java`

## Implementation Rules

- 心跳 worker 必须使用独立线程池，不复用文档处理、OCR 请求、回调或默认 `@Async` 线程池。
- 单个模型目标的心跳必须串行执行，禁止同一目标并发探测。
- 默认探测间隔 `5s`，失败重试间隔 `1s`，连续失败阈值 `3`，均可配置。
- 心跳成功后立即更新内存心跳池，并异步持久化 DB。
- 心跳失败 3 次后立即更新内存心跳池为 `DOWN`，并异步持久化 DB。
- 请求链路连续失败 3 次后通过同一心跳池入口标记目标失败，不直接绕过状态机写 DB。
- DB 到心跳池同步默认 `2s` 一次；同步时只允许较新的 DB 状态覆盖较旧内存状态。
- 启动时允许从 DB 恢复状态，但超过 TTL 的 `UP` 必须降级为 `UNKNOWN`，并触发一次立即探测。
- 手动禁用 `DISABLED` 不参与心跳、不参与路由。
- 日志不得输出 API Key、图片内容、OCR 原文、LLM 原文。

---

### Task 1: 定义统一模型健康领域对象

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/domain/ModelHealthStatus.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/domain/ModelHealthFailureType.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/domain/ModelHealthTargetType.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/domain/ModelHealthTargetId.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/domain/ModelHealthSnapshot.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/domain/ModelHealthState.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/health/domain/ModelHealthStateTest.java`

- [ ] **Step 1: Write the failing tests**

Add these tests:

```java
package io.github.lvdaxianer.doclens.j.health.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ModelHealthStateTest {

    @Test
    void successMovesUnknownToUpAndClearsFailureFields() {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-10T10:00:00+08:00");
        ModelHealthState state = ModelHealthState.initial(target());

        ModelHealthState updated = state.markSuccess(now);

        assertThat(updated.status()).isEqualTo(ModelHealthStatus.UP);
        assertThat(updated.consecutiveFailures()).isZero();
        assertThat(updated.lastHeartbeatAt()).contains(now);
        assertThat(updated.lastSuccessAt()).contains(now);
        assertThat(updated.lastFailureAt()).isEmpty();
        assertThat(updated.lastError()).isEmpty();
    }

    @Test
    void thirdFailureMovesUpToDownAndRecordsFailureType() {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-10T10:00:00+08:00");
        ModelHealthState state = ModelHealthState.initial(target()).markSuccess(now.minusSeconds(5));

        ModelHealthState once = state.markFailure(ModelHealthFailureType.TIMEOUT, "timeout", now.minusSeconds(2), 3);
        ModelHealthState twice = once.markFailure(ModelHealthFailureType.TIMEOUT, "timeout", now.minusSeconds(1), 3);
        ModelHealthState third = twice.markFailure(ModelHealthFailureType.TIMEOUT, "timeout", now, 3);

        assertThat(third.status()).isEqualTo(ModelHealthStatus.DOWN);
        assertThat(third.consecutiveFailures()).isEqualTo(3);
        assertThat(third.lastFailureType()).contains(ModelHealthFailureType.TIMEOUT);
        assertThat(third.lastError()).contains("timeout");
    }

    @Test
    void downMovesToRecoveringBeforeReturningUp() {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-10T10:00:00+08:00");
        ModelHealthState down = ModelHealthState.initial(target())
                .markFailure(ModelHealthFailureType.CONNECTION_REFUSED, "refused", now.minusSeconds(3), 1);

        ModelHealthState recovering = down.markSuccess(now.minusSeconds(1));
        ModelHealthState up = recovering.markRecoverySuccess(now, 2);

        assertThat(recovering.status()).isEqualTo(ModelHealthStatus.RECOVERING);
        assertThat(up.status()).isEqualTo(ModelHealthStatus.UP);
    }

    @Test
    void staleUpSnapshotBecomesUnknownOnRestore() {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-10T10:00:00+08:00");
        ModelHealthState state = ModelHealthState.restore(snapshot(ModelHealthStatus.UP, now.minusSeconds(30)),
                now, 20);

        assertThat(state.status()).isEqualTo(ModelHealthStatus.UNKNOWN);
    }

    private ModelHealthTargetId target() {
        return new ModelHealthTargetId(ModelHealthTargetType.OCR_NODE, "paddle_ocr", "paddle-1");
    }

    private ModelHealthSnapshot snapshot(ModelHealthStatus status, OffsetDateTime lastHeartbeatAt) {
        return new ModelHealthSnapshot(target(), status, 0L, 0L, Optional.of(lastHeartbeatAt),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), lastHeartbeatAt);
    }
}
```

- [ ] **Step 2: Run test to verify RED**

Run:

```bash
mvn -pl doclens-core -Dtest=ModelHealthStateTest test
```

Expected: FAIL because `ModelHealthState` and related types do not exist.

- [ ] **Step 3: Implement minimal domain code**

Implement:

- `ModelHealthStatus` with `UNKNOWN`, `UP`, `RECOVERING`, `DOWN`, `DISABLED`
- `ModelHealthFailureType` with `TIMEOUT`, `CONNECTION_REFUSED`, `HTTP_5XX`, `AUTH_FAILED`, `RATE_LIMITED`, `BAD_RESPONSE`, `REQUEST_FAILED`, `UNKNOWN`
- `ModelHealthTargetType` with `OCR_NODE`, `LLM_MARKDOWN`
- `ModelHealthTargetId` record containing `targetType`, `modelKey`, `targetId`
- `ModelHealthSnapshot` immutable record for persisted/runtime snapshots
- `ModelHealthState` immutable record with:
  - `initial(ModelHealthTargetId targetId)`
  - `restore(ModelHealthSnapshot snapshot, OffsetDateTime now, int staleAfterSeconds)`
  - `markSuccess(OffsetDateTime now)`
  - `markRecoverySuccess(OffsetDateTime now, int recoverySuccessThreshold)`
  - `markFailure(ModelHealthFailureType failureType, String error, OffsetDateTime now, int failureThreshold)`
  - `snapshot()`

State behavior:

- `UNKNOWN + success -> UP`
- `UP + failure below threshold -> UP`
- `failureCount >= threshold -> DOWN`
- `DOWN + success -> RECOVERING`
- `RECOVERING + successCount >= recoverySuccessThreshold -> UP`
- `DISABLED` ignores success/failure and remains `DISABLED`
- Restored `UP` with stale heartbeat becomes `UNKNOWN`

- [ ] **Step 4: Run test to verify GREEN**

Run:

```bash
mvn -pl doclens-core -Dtest=ModelHealthStateTest test
```

Expected: PASS.

- [ ] **Step 5: Broader verification**

Run:

```bash
mvn -pl doclens-core test
```

Expected: PASS.

- [ ] **Step 6: code-review-spec gate**

Review only this task diff against:

- `/Users/lvdaxianer/.claude/skills/code-review-spec/SKILL.md`
- `/Users/lvdaxianer/.claude/skills/code-review-spec/spec.md`
- `/Users/lvdaxianer/.claude/skills/code-review-spec/references/*.md`

Fix any violations in method comments, class comments, branch handling, null safety, parameter counts, constants, collection capacity, exception handling, logging, and thread pool rules.

- [ ] **Step 7: Commit**

```bash
git add doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/domain \
        doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/health/domain/ModelHealthStateTest.java
git commit -F /tmp/model-health-domain.commit
```

Commit message:

```text
feat(health): 新增模型健康状态领域模型

新增统一模型健康状态、失败类型、目标标识与快照对象，
为 OCR 节点和 LLM 模型共享心跳治理能力提供领域基础。

该变更只新增核心领域对象，不接入现有路由和调度流程，
因此不会改变当前 OCR 或 LLM 请求行为。

Refs: 模型心跳池
```

---

### Task 2: 实现线程安全的内存心跳池

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/domain/ModelHealthRepository.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/application/ModelHeartbeatPool.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/application/InMemoryModelHeartbeatPool.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/health/application/InMemoryModelHeartbeatPoolTest.java`

- [ ] **Step 1: Write the failing tests**

Add tests:

```java
package io.github.lvdaxianer.doclens.j.health.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthFailureType;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthSnapshot;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthStatus;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthTargetId;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthTargetType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InMemoryModelHeartbeatPoolTest {

    @Test
    void markSuccessUpdatesPoolImmediately() {
        InMemoryModelHeartbeatPool pool = new InMemoryModelHeartbeatPool(() -> now(), 3, 2, 20);
        ModelHealthTargetId target = target();

        pool.register(List.of(target));
        pool.markSuccess(target);

        assertThat(pool.find(target)).get()
                .extracting(ModelHealthSnapshot::status)
                .isEqualTo(ModelHealthStatus.UP);
    }

    @Test
    void thirdRequestFailureMarksTargetDown() {
        InMemoryModelHeartbeatPool pool = new InMemoryModelHeartbeatPool(() -> now(), 3, 2, 20);
        ModelHealthTargetId target = target();

        pool.register(List.of(target));
        pool.markRequestFailure(target, ModelHealthFailureType.REQUEST_FAILED, "ocr failed");
        pool.markRequestFailure(target, ModelHealthFailureType.REQUEST_FAILED, "ocr failed");
        pool.markRequestFailure(target, ModelHealthFailureType.REQUEST_FAILED, "ocr failed");

        assertThat(pool.find(target)).get()
                .extracting(ModelHealthSnapshot::status)
                .isEqualTo(ModelHealthStatus.DOWN);
    }

    @Test
    void olderRepositorySnapshotDoesNotOverrideFreshRuntimeState() {
        InMemoryModelHeartbeatPool pool = new InMemoryModelHeartbeatPool(() -> now(), 3, 2, 20);
        ModelHealthTargetId target = target();

        pool.register(List.of(target));
        pool.markSuccess(target);
        pool.mergeFromRepository(List.of(snapshot(target, ModelHealthStatus.DOWN, now().minusSeconds(5))));

        assertThat(pool.find(target)).get()
                .extracting(ModelHealthSnapshot::status)
                .isEqualTo(ModelHealthStatus.UP);
    }

    private ModelHealthTargetId target() {
        return new ModelHealthTargetId(ModelHealthTargetType.OCR_NODE, "paddle_ocr", "paddle-1");
    }

    private OffsetDateTime now() {
        return OffsetDateTime.parse("2026-06-10T10:00:00+08:00");
    }

    private ModelHealthSnapshot snapshot(
            ModelHealthTargetId target,
            ModelHealthStatus status,
            OffsetDateTime updatedAt
    ) {
        return new ModelHealthSnapshot(target, status, 0L, 0L, Optional.of(updatedAt),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), updatedAt);
    }
}
```

- [ ] **Step 2: Run test to verify RED**

Run:

```bash
mvn -pl doclens-core -Dtest=InMemoryModelHeartbeatPoolTest test
```

Expected: FAIL because `InMemoryModelHeartbeatPool` does not exist.

- [ ] **Step 3: Implement minimal pool**

Implement `ModelHeartbeatPool` methods:

- `register(List<ModelHealthTargetId> targets)`
- `find(ModelHealthTargetId targetId)`
- `snapshot()`
- `markSuccess(ModelHealthTargetId targetId)`
- `markHeartbeatFailure(ModelHealthTargetId targetId, ModelHealthFailureType failureType, String message)`
- `markRequestFailure(ModelHealthTargetId targetId, ModelHealthFailureType failureType, String message)`
- `mergeFromRepository(List<ModelHealthSnapshot> snapshots)`

Implement `InMemoryModelHeartbeatPool` using `ConcurrentHashMap<ModelHealthTargetId, ModelHealthState>`.

Implementation requirements:

- Use `compute` or `computeIfPresent` for atomic state updates.
- Do not return `null`; use `Optional`.
- `mergeFromRepository` must compare `snapshot.updatedAt()` with current state `updatedAt` and keep the newer state.
- Initialize collection capacities from input size.

- [ ] **Step 4: Run test to verify GREEN**

Run:

```bash
mvn -pl doclens-core -Dtest=InMemoryModelHeartbeatPoolTest test
```

Expected: PASS.

- [ ] **Step 5: Broader verification**

Run:

```bash
mvn -pl doclens-core test
```

Expected: PASS.

- [ ] **Step 6: code-review-spec gate**

Run manual review against the full task diff. Pay special attention to concurrent updates, Optional returns, method length, if-else comments, and collection capacities.

- [ ] **Step 7: Commit**

```bash
git add doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health \
        doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/health/application/InMemoryModelHeartbeatPoolTest.java
git commit -F /tmp/model-heartbeat-pool.commit
```

Commit message:

```text
feat(health): 新增线程安全模型心跳池

新增进程内模型心跳池，支持模型注册、成功标记、心跳失败
标记、请求失败标记和 DB 快照合并。

心跳池使用原子更新保护连续失败计数，并通过更新时间避免
旧 DB 状态覆盖较新的运行时状态。

Refs: 模型心跳池
```

---

### Task 3: 持久化模型健康状态

**Files:**
- Create: `doclens-spring-boot-starter/src/main/resources/db/migration/V9__doclens_model_health.sql`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/health/infrastructure/ModelHealthEntity.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/health/infrastructure/ModelHealthMapper.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/health/infrastructure/MybatisPlusModelHealthRepository.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/health/infrastructure/MybatisPlusModelHealthRepositoryTest.java`

- [ ] **Step 1: Write the failing repository test**

Add tests:

```java
package io.github.lvdaxianer.doclens.j.health.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthSnapshot;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthStatus;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthTargetId;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthTargetType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MybatisPlusModelHealthRepositoryTest {

    @Autowired
    private MybatisPlusModelHealthRepository repository;

    @Test
    void upsertAllPersistsAndReloadsHealthSnapshots() {
        ModelHealthTargetId target = new ModelHealthTargetId(ModelHealthTargetType.OCR_NODE, "paddle_ocr", "paddle-1");
        OffsetDateTime now = OffsetDateTime.parse("2026-06-10T10:00:00+08:00");
        ModelHealthSnapshot snapshot = new ModelHealthSnapshot(target, ModelHealthStatus.DOWN, 3L, 0L,
                Optional.of(now), Optional.empty(), Optional.of(now), Optional.of("timeout"),
                Optional.empty(), now);

        repository.upsertAll(List.of(snapshot));

        assertThat(repository.listAll()).singleElement()
                .satisfies(saved -> {
                    assertThat(saved.targetId()).isEqualTo(target);
                    assertThat(saved.status()).isEqualTo(ModelHealthStatus.DOWN);
                    assertThat(saved.consecutiveFailures()).isEqualTo(3L);
                    assertThat(saved.lastError()).contains("timeout");
                });
    }
}
```

- [ ] **Step 2: Run test to verify RED**

Run:

```bash
mvn -pl doclens-spring-boot-starter -Dtest=MybatisPlusModelHealthRepositoryTest test
```

Expected: FAIL because migration, entity, and repository do not exist.

- [ ] **Step 3: Add migration**

Create `V9__doclens_model_health.sql`:

```sql
CREATE TABLE IF NOT EXISTS doclens_model_health (
    target_type VARCHAR(64) NOT NULL,
    model_key VARCHAR(128) NOT NULL,
    target_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    consecutive_failures BIGINT NOT NULL DEFAULT 0,
    consecutive_successes BIGINT NOT NULL DEFAULT 0,
    last_heartbeat_at TIMESTAMP WITH TIME ZONE,
    last_success_at TIMESTAMP WITH TIME ZONE,
    last_failure_at TIMESTAMP WITH TIME ZONE,
    last_failure_type VARCHAR(64),
    last_error VARCHAR(1024),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (target_type, model_key, target_id)
);

CREATE INDEX IF NOT EXISTS idx_doclens_model_health_status
    ON doclens_model_health(status);

CREATE INDEX IF NOT EXISTS idx_doclens_model_health_updated_at
    ON doclens_model_health(updated_at);
```

- [ ] **Step 4: Implement entity, mapper, repository**

Implement repository methods:

- `List<ModelHealthSnapshot> listAll()`
- `void upsertAll(List<ModelHealthSnapshot> snapshots)`

Implementation requirements:

- Return `List.of()` for empty results.
- `upsertAll` returns immediately for empty input.
- Batch save/update snapshots, no per-row remote or DB call inside loops when MyBatis-Plus batch APIs are available.
- Truncate `lastError` to `1024` characters before persistence.

- [ ] **Step 5: Run test to verify GREEN**

Run:

```bash
mvn -pl doclens-spring-boot-starter -Dtest=MybatisPlusModelHealthRepositoryTest test
```

Expected: PASS.

- [ ] **Step 6: Broader verification**

Run:

```bash
mvn -pl doclens-spring-boot-starter test
```

Expected: PASS.

- [ ] **Step 7: code-review-spec gate**

Review persistence diff for DB batching, sensitive data logging, string constants, null safety, and comments.

- [ ] **Step 8: Commit**

```bash
git add doclens-spring-boot-starter/src/main/resources/db/migration/V9__doclens_model_health.sql \
        doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/health/infrastructure \
        doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/health/infrastructure/MybatisPlusModelHealthRepositoryTest.java
git commit -F /tmp/model-health-persistence.commit
```

Commit message:

```text
feat(health): 持久化模型心跳状态

新增模型健康状态表和 MyBatis-Plus 仓储，支持心跳池状态
批量写入与启动恢复读取。

持久化字段覆盖状态、连续成功失败次数、最近心跳时间、
失败类型和错误摘要，便于服务重启后恢复健康状态。

Refs: 模型心跳池
```

---

### Task 4: 实现 DB 与心跳池异步同步

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/application/ModelHeartbeatSyncService.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/health/application/ModelHeartbeatSyncServiceTest.java`

- [ ] **Step 1: Write the failing sync tests**

Add tests:

```java
package io.github.lvdaxianer.doclens.j.health.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthRepository;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthSnapshot;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthStatus;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthTargetId;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthTargetType;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ModelHeartbeatSyncServiceTest {

    @Test
    void flushWritesCurrentPoolSnapshotToRepository() {
        InMemoryModelHeartbeatPool pool = new InMemoryModelHeartbeatPool(() -> now(), 3, 2, 20);
        InMemoryRepository repository = new InMemoryRepository();
        ModelHeartbeatSyncService service = new ModelHeartbeatSyncService(pool, repository);

        pool.register(List.of(target()));
        pool.markSuccess(target());
        service.flushToRepository();

        assertThat(repository.saved).singleElement()
                .extracting(ModelHealthSnapshot::status)
                .isEqualTo(ModelHealthStatus.UP);
    }

    @Test
    void refreshMergesRepositorySnapshotIntoPool() {
        InMemoryModelHeartbeatPool pool = new InMemoryModelHeartbeatPool(() -> now(), 3, 2, 20);
        InMemoryRepository repository = new InMemoryRepository();
        ModelHeartbeatSyncService service = new ModelHeartbeatSyncService(pool, repository);
        repository.saved = List.of(snapshot(ModelHealthStatus.DOWN, now()));

        service.refreshFromRepository();

        assertThat(pool.find(target())).get()
                .extracting(ModelHealthSnapshot::status)
                .isEqualTo(ModelHealthStatus.DOWN);
    }

    private ModelHealthTargetId target() {
        return new ModelHealthTargetId(ModelHealthTargetType.OCR_NODE, "paddle_ocr", "paddle-1");
    }

    private OffsetDateTime now() {
        return OffsetDateTime.parse("2026-06-10T10:00:00+08:00");
    }

    private ModelHealthSnapshot snapshot(ModelHealthStatus status, OffsetDateTime updatedAt) {
        return new ModelHealthSnapshot(target(), status, 0L, 0L, Optional.of(updatedAt),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), updatedAt);
    }

    private static class InMemoryRepository implements ModelHealthRepository {
        private List<ModelHealthSnapshot> saved = new ArrayList<>(0);

        @Override
        public List<ModelHealthSnapshot> listAll() {
            return saved;
        }

        @Override
        public void upsertAll(List<ModelHealthSnapshot> snapshots) {
            saved = List.copyOf(snapshots);
        }
    }
}
```

- [ ] **Step 2: Run test to verify RED**

Run:

```bash
mvn -pl doclens-core -Dtest=ModelHeartbeatSyncServiceTest test
```

Expected: FAIL because `ModelHeartbeatSyncService` does not exist.

- [ ] **Step 3: Implement sync service**

Implement:

- `flushToRepository()` calls `repository.upsertAll(pool.snapshot())`
- `refreshFromRepository()` calls `pool.mergeFromRepository(repository.listAll())`

No scheduler in core; scheduler is Spring infrastructure in Task 7.

- [ ] **Step 4: Run test to verify GREEN**

Run:

```bash
mvn -pl doclens-core -Dtest=ModelHeartbeatSyncServiceTest test
```

Expected: PASS.

- [ ] **Step 5: Broader verification**

Run:

```bash
mvn -pl doclens-core test
```

Expected: PASS.

- [ ] **Step 6: code-review-spec gate**

Review sync service for null safety, exception boundaries, and method size.

- [ ] **Step 7: Commit**

```bash
git add doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/application/ModelHeartbeatSyncService.java \
        doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/health/application/ModelHeartbeatSyncServiceTest.java
git commit -F /tmp/model-heartbeat-sync.commit
```

Commit message:

```text
feat(health): 新增模型心跳池同步服务

新增心跳池与持久化仓储之间的双向同步服务，支持运行时
状态批量写入 DB，也支持从 DB 快照刷新内存心跳池。

同步服务保持核心层无 Spring 调度依赖，便于后续用独立
调度线程池控制同步频率。

Refs: 模型心跳池
```

---

### Task 5: 实现单目标串行心跳 worker

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/application/ModelHeartbeatProbe.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/application/ModelHeartbeatProbeResult.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/application/ModelHeartbeatWorker.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/application/ModelHeartbeatWorkerSettings.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/health/application/ModelHeartbeatWorkerTest.java`

- [ ] **Step 1: Write failing worker tests**

Add tests:

```java
package io.github.lvdaxianer.doclens.j.health.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthFailureType;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthStatus;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthTargetId;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthTargetType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ModelHeartbeatWorkerTest {

    @Test
    void successfulProbeMarksPoolUpImmediately() {
        InMemoryModelHeartbeatPool pool = new InMemoryModelHeartbeatPool(() -> now(), 3, 2, 20);
        pool.register(List.of(target()));
        ModelHeartbeatWorker worker = new ModelHeartbeatWorker(target(), successfulProbe(), pool, settings());

        worker.runOnce();

        assertThat(pool.find(target())).get()
                .extracting(snapshot -> snapshot.status())
                .isEqualTo(ModelHealthStatus.UP);
    }

    @Test
    void threeProbeFailuresMarkPoolDown() {
        InMemoryModelHeartbeatPool pool = new InMemoryModelHeartbeatPool(() -> now(), 3, 2, 20);
        pool.register(List.of(target()));
        ModelHeartbeatWorker worker = new ModelHeartbeatWorker(target(), failingProbe(), pool, settings());

        worker.runOnce();

        assertThat(pool.find(target())).get()
                .extracting(snapshot -> snapshot.status())
                .isEqualTo(ModelHealthStatus.DOWN);
    }

    @Test
    void thirdProbeAttemptCanRecoverWithoutMarkingDown() {
        InMemoryModelHeartbeatPool pool = new InMemoryModelHeartbeatPool(() -> now(), 3, 2, 20);
        pool.register(List.of(target()));
        AtomicInteger attempts = new AtomicInteger(0);
        ModelHeartbeatWorker worker = new ModelHeartbeatWorker(target(), () -> {
            if (attempts.incrementAndGet() < 3) {
                return ModelHeartbeatProbeResult.failure(ModelHealthFailureType.TIMEOUT, "timeout");
            } else {
                return ModelHeartbeatProbeResult.success();
            }
        }, pool, settings());

        worker.runOnce();

        assertThat(attempts).hasValue(3);
        assertThat(pool.find(target())).get()
                .extracting(snapshot -> snapshot.status())
                .isEqualTo(ModelHealthStatus.UP);
    }

    private ModelHeartbeatProbe successfulProbe() {
        return ModelHeartbeatProbeResult::success;
    }

    private ModelHeartbeatProbe failingProbe() {
        return () -> ModelHeartbeatProbeResult.failure(ModelHealthFailureType.TIMEOUT, "timeout");
    }

    private ModelHeartbeatWorkerSettings settings() {
        return new ModelHeartbeatWorkerSettings(5, 1, 3);
    }

    private ModelHealthTargetId target() {
        return new ModelHealthTargetId(ModelHealthTargetType.OCR_NODE, "paddle_ocr", "paddle-1");
    }

    private OffsetDateTime now() {
        return OffsetDateTime.parse("2026-06-10T10:00:00+08:00");
    }
}
```

- [ ] **Step 2: Run test to verify RED**

Run:

```bash
mvn -pl doclens-core -Dtest=ModelHeartbeatWorkerTest test
```

Expected: FAIL because worker classes do not exist.

- [ ] **Step 3: Implement worker**

Implement behavior:

- `runOnce()` invokes probe up to `maxAttempts`.
- On first success, call `pool.markSuccess(targetId)` immediately and stop.
- On failure, call `pool.markHeartbeatFailure(...)`.
- Between failed attempts, sleep `retryIntervalSeconds`.
- If interrupted, restore interrupt flag and mark failure with `UNKNOWN`.
- Worker itself does not schedule the next run; Spring scheduler handles that in Task 7.

- [ ] **Step 4: Run test to verify GREEN**

Run:

```bash
mvn -pl doclens-core -Dtest=ModelHeartbeatWorkerTest test
```

Expected: PASS.

- [ ] **Step 5: Broader verification**

Run:

```bash
mvn -pl doclens-core test
```

Expected: PASS.

- [ ] **Step 6: code-review-spec gate**

Review worker for thread interruption, magic numbers, method size, and branch comments.

- [ ] **Step 7: Commit**

```bash
git add doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/health/application \
        doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/health/application/ModelHeartbeatWorkerTest.java
git commit -F /tmp/model-heartbeat-worker.commit
```

Commit message:

```text
feat(health): 新增单模型串行心跳 worker

新增模型心跳探针结果和单目标 worker，支持默认三次探测、
失败后一秒重试、成功立即更新心跳池。

worker 只负责单次串行探测，不承担调度职责，便于后续用
独立线程池隔离心跳任务。

Refs: 模型心跳池
```

---

### Task 6: 接入 OCR 和 LLM 心跳探针

**Files:**
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/health/infrastructure/OcrNodeHeartbeatProbe.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/health/infrastructure/LlmMarkdownHeartbeatProbe.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/LlmMarkdownHealthChecker.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/health/infrastructure/OcrNodeHeartbeatProbeTest.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/health/infrastructure/LlmMarkdownHeartbeatProbeTest.java`

- [ ] **Step 1: Write OCR probe failing test**

Test behavior:

- Given an OCR node and `OcrHealthClient` returns true.
- `OcrNodeHeartbeatProbe.probe()` returns success.
- Given client throws or returns false.
- probe returns failure with `TIMEOUT`, `CONNECTION_REFUSED`, `HTTP_5XX`, or `UNKNOWN` according to exception type.

Run:

```bash
mvn -pl doclens-spring-boot-starter -Dtest=OcrNodeHeartbeatProbeTest test
```

Expected: FAIL because probe does not exist.

- [ ] **Step 2: Write LLM probe failing test**

Test behavior:

- Enabled configured LLM with tester response healthy returns success.
- Disabled or unconfigured LLM returns failure `BAD_RESPONSE` with non-sensitive message.
- Tester exception returns failure `UNKNOWN` or mapped failure type.

Run:

```bash
mvn -pl doclens-spring-boot-starter -Dtest=LlmMarkdownHeartbeatProbeTest test
```

Expected: FAIL because probe does not exist.

- [ ] **Step 3: Implement OCR probe**

Implementation:

- Constructor accepts `OcrNode` and `OcrHealthClient`.
- Calls `healthClient.isHealthy(node)`.
- Maps `true -> success`.
- Maps `false -> BAD_RESPONSE`.
- Maps timeout exceptions to `TIMEOUT`.
- Maps connection exceptions to `CONNECTION_REFUSED`.
- Does not persist state directly.

- [ ] **Step 4: Implement LLM probe**

Implementation:

- Constructor accepts `LlmMarkdownConfigRepository` and `LlmMarkdownConfigTester`.
- Reads current config.
- Disabled or missing config returns failure with `BAD_RESPONSE`.
- Calls tester using `LlmMarkdownConfigSettings`.
- Does not expose credential value in logs or result message.
- Does not persist state directly.

- [ ] **Step 5: Run focused tests**

Run:

```bash
mvn -pl doclens-spring-boot-starter -Dtest=OcrNodeHeartbeatProbeTest,LlmMarkdownHeartbeatProbeTest test
```

Expected: PASS.

- [ ] **Step 6: Broader verification**

Run:

```bash
mvn -pl doclens-spring-boot-starter test
```

Expected: PASS.

- [ ] **Step 7: code-review-spec gate**

Review for exception handling, secret redaction, remote call boundaries, comments, and method length.

- [ ] **Step 8: Commit**

```bash
git add doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/health/infrastructure \
        doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/health/infrastructure
git commit -F /tmp/model-heartbeat-probes.commit
```

Commit message:

```text
feat(health): 接入 OCR 与 LLM 心跳探针

新增 OCR 节点和 LLM Markdown 心跳探针，将底层健康检查结果
适配为统一模型心跳结果。

探针只负责探测和错误类型归一化，不直接修改 DB 或运行时池，
避免健康状态更新入口分散。

Refs: 模型心跳池
```

---

### Task 7: 配置独立心跳线程池与调度器

**Files:**
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringProperties.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensOcrThreadPoolAutoConfiguration.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/health/infrastructure/ModelHeartbeatScheduler.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensModelHeartbeatAutoConfiguration.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensModelHeartbeatAutoConfigurationTest.java`

- [ ] **Step 1: Write failing autoconfiguration test**

Test behavior:

- Context contains `doclensModelHeartbeatExecutor`.
- Context contains `doclensModelHeartbeatSchedulerExecutor`.
- Thread name prefixes are `doclens-model-heartbeat-` and `doclens-model-heartbeat-scheduler-`.
- Defaults are interval `5`, retry interval `1`, attempts `3`, db sync interval `2`.

Run:

```bash
mvn -pl doclens-spring-boot-starter -Dtest=DocLensModelHeartbeatAutoConfigurationTest test
```

Expected: FAIL because configuration does not exist.

- [ ] **Step 2: Add properties**

Add nested `ModelHeartbeatProperties`:

- `enabled`
- `intervalSeconds`
- `retryIntervalSeconds`
- `attempts`
- `dbSyncIntervalSeconds`
- `staleAfterSeconds`

Defaults:

- `enabled = true`
- `intervalSeconds = 5`
- `retryIntervalSeconds = 1`
- `attempts = 3`
- `dbSyncIntervalSeconds = 2`
- `staleAfterSeconds = 20`

Add thread pool property:

- `modelHeartbeatThreadPool`

Default:

- core size `2`
- max size `8`
- queue capacity `100`
- keep alive `60`
- prefix `doclens-model-heartbeat-`

- [ ] **Step 3: Implement scheduler**

`ModelHeartbeatScheduler` responsibilities:

- On start, load OCR enabled nodes and LLM enabled config as targets.
- Register targets in `ModelHeartbeatPool`.
- Schedule one run per target with independent worker submission.
- Never run two concurrent workers for the same target.
- Schedule DB refresh and DB flush every `dbSyncIntervalSeconds`.
- On shutdown, stop scheduling without marking targets DOWN.

- [ ] **Step 4: Wire auto configuration**

Create beans:

- `ModelHeartbeatPool`
- `ModelHeartbeatSyncService`
- `ModelHeartbeatScheduler`
- `doclensModelHeartbeatExecutor`
- `doclensModelHeartbeatSchedulerExecutor`
- `ApplicationRunner` to start scheduler

Use `@ConditionalOnMissingBean` and existing repository/client beans.

- [ ] **Step 5: Run focused test**

Run:

```bash
mvn -pl doclens-spring-boot-starter -Dtest=DocLensModelHeartbeatAutoConfigurationTest test
```

Expected: PASS.

- [ ] **Step 6: Broader verification**

Run:

```bash
mvn -pl doclens-spring-boot-starter test
```

Expected: PASS.

- [ ] **Step 7: code-review-spec gate**

Review specifically for thread pool isolation, meaningful names, lifecycle shutdown, queue capacity, and no use of default async executors.

- [ ] **Step 8: Commit**

```bash
git add doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringProperties.java \
        doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensOcrThreadPoolAutoConfiguration.java \
        doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensModelHeartbeatAutoConfiguration.java \
        doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/health/infrastructure/ModelHeartbeatScheduler.java \
        doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensModelHeartbeatAutoConfigurationTest.java
git commit -F /tmp/model-heartbeat-scheduler.commit
```

Commit message:

```text
feat(health): 新增模型心跳独立调度

新增模型心跳独立线程池、调度线程池与自动配置，支持按模型
目标串行探测，并定期同步 DB 与运行时心跳池。

心跳调度与 OCR 请求、文档处理、回调线程池完全隔离，避免业务
线程池拥塞导致健康检测失效。

Refs: 模型心跳池
```

---

### Task 8: OCR 请求连续失败后联动心跳池

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingDependencies.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingService.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensOcrResourceAutoConfiguration.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingServiceTest.java`

- [ ] **Step 1: Add failing routing test**

Add test:

```java
@Test
void requestFailuresMarkHeartbeatPoolAfterConfiguredAttempts() {
    TrackingHeartbeatPool heartbeatPool = new TrackingHeartbeatPool();
    TestContext context = contextWithHeartbeatPool(List.of(node("paddle-1", "paddle_ocr")),
            failingExecutor(), heartbeatPool);

    assertThatThrownBy(() -> context.service.recognize(request(), OcrRoutePolicy.specificNode("paddle_ocr", "paddle-1")))
            .isInstanceOf(OcrRouteExecutionException.class);

    assertThat(heartbeatPool.requestFailureCount("paddle-1")).isEqualTo(3);
    assertThat(heartbeatPool.lastStatus("paddle-1")).isEqualTo(ModelHealthStatus.DOWN);
}
```

Add `TrackingHeartbeatPool` test helper in the same test file.

- [ ] **Step 2: Run test to verify RED**

Run:

```bash
mvn -pl doclens-core -Dtest=OcrRoutingServiceTest#requestFailuresMarkHeartbeatPoolAfterConfiguredAttempts test
```

Expected: FAIL because routing service does not accept or update heartbeat pool.

- [ ] **Step 3: Modify dependencies**

Add optional `ModelHeartbeatPool` to `OcrRoutingDependencies`.

Provide a no-op implementation or optional adapter for tests and configurations that do not have the heartbeat feature enabled.

- [ ] **Step 4: Update routing failure handling**

In `tryOnce` catch branch:

- Map OCR request exception to `ModelHealthFailureType`.
- Call `heartbeatPool.markRequestFailure(targetId, failureType, sanitizedMessage)`.
- Target id is `OCR_NODE + node.modelKey() + node.nodeId()`.
- Do not mark model down directly outside the pool.

On success:

- Call `heartbeatPool.markSuccess(targetId)` after successful OCR.

- [ ] **Step 5: Wire Spring configuration**

In OCR resource auto configuration, pass `ModelHeartbeatPool` into routing dependencies when available.

- [ ] **Step 6: Run focused test**

Run:

```bash
mvn -pl doclens-core -Dtest=OcrRoutingServiceTest#requestFailuresMarkHeartbeatPoolAfterConfiguredAttempts test
```

Expected: PASS.

- [ ] **Step 7: Broader verification**

Run:

```bash
mvn -pl doclens-core test
mvn -pl doclens-spring-boot-starter test
```

Expected: PASS.

- [ ] **Step 8: code-review-spec gate**

Review for sensitive logging, null safety, parameter count in dependencies, and duplicated exception mapping.

- [ ] **Step 9: Commit**

```bash
git add doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application \
        doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingServiceTest.java \
        doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensOcrResourceAutoConfiguration.java
git commit -F /tmp/ocr-request-heartbeat-failure.commit
```

Commit message:

```text
feat(health): OCR 请求失败联动心跳池

OCR 请求链路在节点调用成功时刷新模型心跳状态，在连续请求
失败达到阈值后通过心跳池标记节点不可用。

该变更复用统一状态入口，避免请求失败和周期心跳各自维护
不同健康状态。

Refs: 模型心跳池
```

---

### Task 9: LLM 请求失败后联动心跳池

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingDependencies.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCase.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensProcessingAutoConfiguration.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseTest.java`

- [ ] **Step 1: Add failing LLM request failure test**

Add test:

```java
@Test
void llmMarkdownFailuresMarkHeartbeatPoolAfterThreeAttempts() {
    TrackingHeartbeatPool heartbeatPool = new TrackingHeartbeatPool();
    CountingFailingMarkdownPostProcessor markdownPostProcessor =
            new CountingFailingMarkdownPostProcessor("llm unavailable");
    TestContext context = contextWithHeartbeatPool(new InMemoryBatchRepository(),
            new FixedTextExtractor("原始 OCR 文本"), markdownPostProcessor, heartbeatPool);

    context.useCase.process(documentId());

    assertThat(markdownPostProcessor.attempts()).isEqualTo(3);
    assertThat(heartbeatPool.requestFailureCount("default")).isEqualTo(3);
    assertThat(heartbeatPool.lastStatus("default")).isEqualTo(ModelHealthStatus.DOWN);
}
```

- [ ] **Step 2: Run test to verify RED**

Run:

```bash
mvn -pl doclens-core -Dtest=BatchProcessingUseCaseTest#llmMarkdownFailuresMarkHeartbeatPoolAfterThreeAttempts test
```

Expected: FAIL because batch processing does not update heartbeat pool.

- [ ] **Step 3: Add heartbeat pool dependency**

Add optional/no-op `ModelHeartbeatPool` to `BatchProcessingDependencies`.

- [ ] **Step 4: Update LLM post-processing retry path**

When LLM Markdown succeeds:

- `pool.markSuccess(LLM_MARKDOWN/default/default)`

When each LLM Markdown attempt fails:

- `pool.markRequestFailure(LLM_MARKDOWN/default/default, REQUEST_FAILED, sanitizedMessage)`

Do not include prompt, OCR text, markdown output, or API key in error message.

- [ ] **Step 5: Wire Spring configuration**

Pass `ModelHeartbeatPool` from processing auto configuration.

- [ ] **Step 6: Run focused test**

Run:

```bash
mvn -pl doclens-core -Dtest=BatchProcessingUseCaseTest#llmMarkdownFailuresMarkHeartbeatPoolAfterThreeAttempts test
```

Expected: PASS.

- [ ] **Step 7: Broader verification**

Run:

```bash
mvn -pl doclens-core test
mvn -pl doclens-spring-boot-starter test
```

Expected: PASS.

- [ ] **Step 8: code-review-spec gate**

Review for sensitive data leakage, retry behavior, duplicate logic, and dependency parameter count.

- [ ] **Step 9: Commit**

```bash
git add doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application \
        doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseTest.java \
        doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensProcessingAutoConfiguration.java
git commit -F /tmp/llm-request-heartbeat-failure.commit
```

Commit message:

```text
feat(health): LLM 请求失败联动心跳池

LLM Markdown 后处理在成功时刷新统一模型心跳状态，在连续
请求失败达到阈值后通过心跳池标记 LLM 模型不可用。

失败状态只记录脱敏后的错误摘要，不写入提示词、OCR 文本、
Markdown 结果或凭证信息。

Refs: 模型心跳池
```

---

### Task 10: 查询接口与基础观测

**Files:**
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/health/interfaces/ModelHealthController.java`
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/health/interfaces/ModelHealthResponse.java`
- Test: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/ModelHealthApiContractTest.java`

- [ ] **Step 1: Write failing API contract test**

Test endpoint:

- `GET /api/v1/model-health`
- Response contains target type, model key, target id, status, last heartbeat time, last failure type, last error.
- Response does not contain credentials or raw request payload.

Run:

```bash
mvn -pl doclens-server -Dtest=ModelHealthApiContractTest test
```

Expected: FAIL because endpoint does not exist.

- [ ] **Step 2: Implement response DTO**

Fields:

- `target_type`
- `model_key`
- `target_id`
- `status`
- `consecutive_failures`
- `consecutive_successes`
- `last_heartbeat_at`
- `last_success_at`
- `last_failure_at`
- `last_failure_type`
- `last_error`
- `updated_at`

- [ ] **Step 3: Implement controller**

Controller:

- Injects `ModelHeartbeatPool`.
- Returns `pool.snapshot()` mapped to responses.
- Sorts by `target_type`, `model_key`, `target_id`.
- Does not expose secrets.

- [ ] **Step 4: Run focused test**

Run:

```bash
mvn -pl doclens-server -Dtest=ModelHealthApiContractTest test
```

Expected: PASS.

- [ ] **Step 5: Broader verification**

Run:

```bash
mvn -pl doclens-server test
```

Expected: PASS.

- [ ] **Step 6: code-review-spec gate**

Review controller for API shape, null safety, stable ordering, and sensitive data exposure.

- [ ] **Step 7: Commit**

```bash
git add doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/health/interfaces \
        doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/ModelHealthApiContractTest.java
git commit -F /tmp/model-health-api.commit
```

Commit message:

```text
feat(health): 新增模型健康查询接口

新增模型健康状态查询 API，返回 OCR 与 LLM 模型在运行时
心跳池中的状态、连续失败次数和最近错误摘要。

接口只暴露脱敏健康信息，不包含凭证、请求正文、OCR 文本或
LLM 提示词，便于后续 dashboard 和告警系统接入。

Refs: 模型心跳池
```

---

### Task 11: 配置文档与最终回归

**Files:**
- Modify: `README.md`
- Modify: `README_EN.md`
- Modify: `doclens-server/src/main/resources/application.yml`
- Modify: `docs/superpowers/plans/2026-06-10-model-heartbeat-pool.md`

- [ ] **Step 1: Update configuration examples**

Add example:

```yaml
doclens:
  model-heartbeat:
    enabled: true
    interval-seconds: 5
    retry-interval-seconds: 1
    attempts: 3
    db-sync-interval-seconds: 2
    stale-after-seconds: 20
  thread-pools:
    model-heartbeat-thread-pool:
      core-size: 2
      max-size: 8
      queue-capacity: 100
      keep-alive-seconds: 60
      thread-name-prefix: doclens-model-heartbeat-
```

- [ ] **Step 2: Document runtime behavior**

Document:

- Heartbeat uses isolated thread pool.
- Each target is probed serially.
- Success updates runtime pool immediately.
- Three heartbeat failures mark target DOWN.
- Three request failures mark target DOWN.
- DB sync happens asynchronously every 2 seconds.
- Stale restored UP state becomes UNKNOWN.

- [ ] **Step 3: Run full backend verification**

Run:

```bash
mvn test
```

Expected: PASS.

- [ ] **Step 4: Run frontend verification only if dashboard files changed**

If dashboard files are untouched, record as not applicable.

If dashboard files changed, run:

```bash
cd doclens-dashboard
npm test
npm run build
```

Expected: PASS.

- [ ] **Step 5: Final code-review-spec gate**

Review the full branch diff against all mandatory rules. Explicitly check:

- Thread pool isolation
- No loop DB or remote calls where batch APIs exist
- No sensitive data logging
- Method comments and class comments
- Branch coverage and else handling
- Null safety
- Parameter counts
- Magic constants
- Collection capacities
- Exception handling
- Commit atomicity

- [ ] **Step 6: Commit**

```bash
git add README.md README_EN.md doclens-server/src/main/resources/application.yml \
        docs/superpowers/plans/2026-06-10-model-heartbeat-pool.md
git commit -F /tmp/model-heartbeat-docs.commit
```

Commit message:

```text
docs(health): 补充模型心跳配置说明

补充模型心跳池的默认配置、线程池隔离方式、失败判定规则、
DB 同步周期和启动恢复策略说明。

文档用于指导部署方调整心跳探测频率、重试次数和线程池容量，
不改变运行时代码行为。

Refs: 模型心跳池
```

---

## Final Acceptance Criteria

- OCR 和 LLM 心跳使用独立线程池，不依赖业务请求线程池。
- 每个模型目标同一时间最多一个心跳 worker 在运行。
- 默认 5 秒心跳、失败后 1 秒重试、连续 3 次失败后标记 DOWN。
- 心跳成功立即更新内存心跳池。
- 请求链路连续失败 3 次后通过心跳池标记目标 DOWN。
- 健康状态持久化到 `doclens_model_health`。
- DB 与心跳池每 2 秒异步同步，且旧 DB 状态不能覆盖较新内存状态。
- 服务启动时从 DB 恢复状态，过期 UP 降级为 UNKNOWN。
- `DISABLED` 目标不参与心跳和路由。
- 日志和 API 响应不泄漏 API Key、图片内容、OCR 文本、LLM 提示词。
- Backend `mvn test` 通过。

## Self-Review

- Spec coverage: 覆盖线程池隔离、心跳池、单目标 worker、5s 可配置间隔、失败后 1s 三次重试、状态持久化、2s DB 同步、请求失败联动、启动恢复和观测接口。
- Placeholder scan: 本计划没有 `TBD`、`TODO`、`implement later` 或未说明的占位任务。
- Type consistency: 所有新增类型统一使用 `ModelHealth*` 前缀；OCR 使用 `OCR_NODE/modelKey/nodeId`，LLM 使用 `LLM_MARKDOWN/default/default`。
- Scope check: 计划聚焦后端心跳能力；dashboard 详细展示留到后续计划，避免把 UI 改造混入核心健康治理。
