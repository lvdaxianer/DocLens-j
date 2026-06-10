# OCR Weighted Scheduling and Health Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade OCR node scheduling from fixed least-inflight selection to capacity-aware weighted dispatch with wait-queue semantics, while adding configurable node health governance including retry intervals, circuit-open windows, recovery thresholds, and manual reconnect flow.

**Architecture:** Keep the current `OcrRoutingService` as the synchronous OCR entrypoint, but move node selection and slot acquisition behind a new dispatch coordinator that uses runtime node state, weighted capacity scoring, and an in-memory pending queue. Extend node health management into an explicit state machine driven by configurable probe intervals, consecutive failure thresholds, circuit-open windows, and recovery success streaks, with a manual reconnect API that triggers fast recovery probes without bypassing the health model.

**Tech Stack:** Java 21, Spring Boot 3.5, Maven, MyBatis-Plus, Flyway, H2, Jackson, Java `HttpClient`, Vue 3, TypeScript, Pinia, Naive UI, Vite, JUnit 5, AssertJ.

---

## Scope and Product Decisions

### Scheduling Rules

- OCR image dispatch is **per-image and per-attempt**, never batch-preallocated.
- Candidate nodes are filtered by:
  - enabled
  - healthy enough to receive production traffic
  - route policy match
  - `inflight < maxConcurrency`
- Node scoring is:
  - idle capacity ratio is the primary factor
  - weight ratio is the secondary factor
  - first release default factors:
    - `idleFactor = 0.7`
    - `weightFactor = 0.3`
- The effective score formula is:

```text
idleRatio = (maxConcurrency - inflightImages) / maxConcurrency
weightRatio = weight / sum(weight of current candidate nodes)
score = idleRatio * idleFactor + weightRatio * weightFactor
```

- Selection is not `max(score)` directly. Instead:
  1. calculate score for every candidate
  2. find the highest score
  3. build a `top bucket` of nodes whose score is within `topBucketThreshold` of the highest score
  4. run smooth weighted round robin inside the top bucket
- This preserves:
  - idle-priority when capacity differs a lot
  - weight-based smoothing when nodes are similarly idle
- If all matching nodes are full, the image request enters a wait queue.
- When any node finishes one image, the dispatcher drains one eligible queued request and recalculates assignment using the then-current state.

### Health Governance Rules

- There are two different retry concepts and they must stay separate:
  - request retry: OCR call retry for a single image on an already selected node
  - health retry: periodic or manual probe attempts for a node lifecycle
- This plan adds or formalizes these health-governance defaults:
  - failure threshold: `3`
  - probe interval: `60s`
  - circuit-open duration: `86400s`
  - recovery success threshold: `3`
  - manual recovery attempts: `3`
- Manual reconnect does not force a node to `UP`.
- Manual reconnect triggers a short recovery-probe sequence and the node becomes usable only after the configured recovery success threshold is met.

### Node Defaults

- New default OCR node values:
  - `maxConcurrency = 10`
  - `weight = 50`
- Existing `doclens.extraction.ocr-concurrency` default should be raised from `1` to a value that allows single-document multi-page fan-out. First release default: `4`.

### Dashboard / UI Rules

- OCR node list and detail must show:
  - inflight images
  - queued images
  - weight
  - max concurrency
  - consecutive failures
  - recovery success streak
  - circuit-open remaining time
  - latest health error
- OCR node operations must distinguish:
  - `测试连接`: single probe, non-destructive
  - `手动连接`: start bounded recovery probing
- Batch detail should expose actual node hit distribution and queued image count where possible.

---

## Current Code Anchors

These files define the starting point and constrain the implementation shape:

- [doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingService.java](/Users/lvdaxianer/workspace/my/project/DocLens-j/doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingService.java)
  - current synchronous routing entrypoint
  - currently increments and decrements inflight around direct execution
- [doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/LeastInflightOcrNodeSelector.java](/Users/lvdaxianer/workspace/my/project/DocLens-j/doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/LeastInflightOcrNodeSelector.java)
  - current selection logic only compares inflight, latency, nodeId
- [doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrRuntimeNodePool.java](/Users/lvdaxianer/workspace/my/project/DocLens-j/doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrRuntimeNodePool.java)
  - currently keeps only runtime inflight counters
- [doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrRuntimeNode.java](/Users/lvdaxianer/workspace/my/project/DocLens-j/doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrRuntimeNode.java)
  - currently exposes no queued count, no circuit timestamps, no round-robin bookkeeping
- [doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthChecker.java](/Users/lvdaxianer/workspace/my/project/DocLens-j/doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthChecker.java)
  - currently uses only failure threshold and recovery threshold counters
- [doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthCheckScheduler.java](/Users/lvdaxianer/workspace/my/project/DocLens-j/doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthCheckScheduler.java)
  - currently runs one global fixed-delay health loop
- [doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrNodeMaintenanceController.java](/Users/lvdaxianer/workspace/my/project/DocLens-j/doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrNodeMaintenanceController.java)
  - currently exposes delete and single test only
- [doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringProperties.java](/Users/lvdaxianer/workspace/my/project/DocLens-j/doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringProperties.java)
  - current defaults still use `least-inflight`, `ocrConcurrency = 1`, node defaults `weight = 100`, `maxConcurrency = 4`
- [doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/query/infrastructure/OcrNodeMetricsAggregator.java](/Users/lvdaxianer/workspace/my/project/DocLens-j/doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/query/infrastructure/OcrNodeMetricsAggregator.java)
  - currently reports queued images as `0`

---

## File Structure

### Core Domain / Application

- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingService.java`
  - delegate slot acquisition and wait-queue behavior to a dispatch coordinator
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrDispatchCoordinator.java`
  - synchronous dispatch orchestration for image OCR requests
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrDispatchAcquireResult.java`
  - result object for slot acquisition vs queueing decisions
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrPendingRequestQueue.java`
  - queue abstraction for waiting OCR image requests
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/WeightedCapacityOcrNodeSelector.java`
  - idle-priority plus weight-secondary selection logic
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/SmoothWeightedRoundRobinState.java`
  - reusable SWRR bookkeeping for top-bucket selection
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrNodeScore.java`
  - typed score breakdown for selector tests and explainability
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrPendingRequest.java`
  - queued image OCR request snapshot
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRuntimeNodeView.java`
  - add `weight`, `queuedImages`, `availableSlots`, `circuitOpenUntil`, `consecutiveFailureCount`, `recoverySuccessCount`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrNodeSelector.java`
  - allow selector to return explanation-ready score context if needed
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingServiceProperties.java`
  - add scheduling factors and request retry backoff config
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrNode.java`
  - add circuit-open and health-governance fields
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrNodeMetrics.java`
  - extend queued and health lifecycle metrics
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrNodeStatus.java`
  - either add `CIRCUIT_OPEN` or formally encode circuit-open through separate timestamp while preserving status semantics
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrHealthGovernance.java`
  - immutable policy values for thresholds and intervals

### Spring Boot Starter / Infrastructure

- Modify: `doclens-spring-boot-starter/src/main/resources/db/migration/V4__doclens_ocr_node_deployment.sql`
  - if safe to extend, add node health governance fields in a new follow-up migration instead
- Create: `doclens-spring-boot-starter/src/main/resources/db/migration/V6__doclens_ocr_health_governance.sql`
  - add circuit-open, recovery, and health tracking columns
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrRuntimeNode.java`
  - runtime counters, queue counters, last assignment sequence
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrRuntimeNodePool.java`
  - atomic slot acquisition/release and queued counter APIs
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/InMemoryOcrPendingRequestQueue.java`
  - process-local wait queue implementation
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthChecker.java`
  - full probe state machine, circuit-open timing, recovery streak logic
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrManualRecoveryService.java`
  - bounded manual reconnect attempts
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensOcrResourceAutoConfiguration.java`
  - wire selector, queue, coordinator, manual recovery service
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringProperties.java`
  - add health-governance, scheduling-factor, manual recovery config and new defaults
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/query/infrastructure/OcrNodeMetricsAggregator.java`
  - pull real queued runtime values and health counters
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrNodeEntity.java`
  - map new health-governance fields
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/MybatisPlusOcrNodeRepository.java`
  - persist and read new fields

### Server Interfaces

- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrNodeMaintenanceController.java`
  - add manual reconnect endpoint
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrNodeReconnectResponse.java`
  - explicit reconnect result DTO
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrNodeResponse.java`
  - expose governance fields and default values without exposing secrets

### Dashboard

- Modify: `doclens-dashboard/src/types/ocrResources.ts`
  - include queued, health governance, reconnect action fields
- Modify: `doclens-dashboard/src/api/ocrResources.ts`
  - add manual reconnect API call and health-governance field mapping
- Modify: `doclens-dashboard/src/components/ocr/OcrNodeTable.vue`
  - show queued, circuit state, reconnect button
- Modify: `doclens-dashboard/src/components/ocr/OcrNodeDetailDrawer.vue`
  - surface health lifecycle details
- Modify: `doclens-dashboard/src/components/ocr/OcrNodeFormDrawer.vue`
  - default values for `weight = 50`, `max_concurrency = 10`
- Modify: `doclens-dashboard/src/utils/ocrNodeFormRules.ts`
  - lock new defaults and validation rules

---

## Implementation Tasks

### Task 1: Add Health Governance Domain and Config Defaults

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrHealthGovernance.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingServiceProperties.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringProperties.java`
- Modify: `doclens-server/src/main/resources/application.yml`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringPropertiesHealthGovernanceTest.java`

- [ ] **Step 1: Write the failing config test**

```java
package io.github.lvdaxianer.doclens.j.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DocLensSpringPropertiesHealthGovernanceTest {

    @Test
    void defaultsExposeWeightedSchedulingAndHealthGovernance() {
        DocLensSpringProperties properties = new DocLensSpringProperties(
                "./var/storage",
                true,
                "local-worker",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertThat(properties.ocr().loadBalanceStrategy()).isEqualTo("weighted-idle");
        assertThat(properties.ocr().idleFactor()).isEqualTo(0.7D);
        assertThat(properties.ocr().weightFactor()).isEqualTo(0.3D);
        assertThat(properties.ocr().topBucketThreshold()).isEqualTo(0.15D);
        assertThat(properties.ocr().failureThreshold()).isEqualTo(3);
        assertThat(properties.ocr().probeIntervalSeconds()).isEqualTo(60);
        assertThat(properties.ocr().circuitOpenSeconds()).isEqualTo(86400);
        assertThat(properties.ocr().recoverySuccessThreshold()).isEqualTo(3);
        assertThat(properties.ocr().manualRecoveryAttempts()).isEqualTo(3);
        assertThat(properties.extraction().ocrConcurrency()).isEqualTo(4);
        assertThat(DocLensSpringProperties.defaultPaddleNode().weight()).isEqualTo(50);
        assertThat(DocLensSpringProperties.defaultPaddleNode().maxConcurrency()).isEqualTo(10);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -pl doclens-spring-boot-starter -am -Dtest=DocLensSpringPropertiesHealthGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

```text
FAIL with cannot find methods idleFactor/weightFactor/topBucketThreshold/failureThreshold
```

- [ ] **Step 3: Add minimal domain/config structures**

```java
package io.github.lvdaxianer.doclens.j.adapter.domain;

public record OcrHealthGovernance(
        int failureThreshold,
        int probeIntervalSeconds,
        int circuitOpenSeconds,
        int recoverySuccessThreshold,
        int manualRecoveryAttempts
) {
}
```

```java
public record OcrProperties(
        OcrRoutingMode defaultRoutingMode,
        String loadBalanceStrategy,
        double idleFactor,
        double weightFactor,
        double topBucketThreshold,
        int requestRetryTimes,
        int failureThreshold,
        int probeIntervalSeconds,
        int circuitOpenSeconds,
        int recoverySuccessThreshold,
        int manualRecoveryAttempts,
        boolean specificNodeFallbackEnabled
) {
}
```

```java
ocr = ocr == null ? new OcrProperties(
        OcrRoutingMode.GLOBAL_LOAD_BALANCE,
        "weighted-idle",
        0.7D,
        0.3D,
        0.15D,
        3,
        3,
        60,
        86400,
        3,
        3,
        false) : ocr;

extraction = extraction == null ? new ExtractionProperties(4) : extraction;
```

```java
private static PaddleOcrNodeProperties defaultPaddleNode() {
    return new PaddleOcrNodeProperties("paddle-215", "10.100.30.215", 8080, true, true, 50, 10);
}
```

```yaml
doclens:
  ocr:
    load-balance-strategy: weighted-idle
    idle-factor: 0.7
    weight-factor: 0.3
    top-bucket-threshold: 0.15
    request-retry-times: 3
    failure-threshold: 3
    probe-interval-seconds: 60
    circuit-open-seconds: 86400
    recovery-success-threshold: 3
    manual-recovery-attempts: 3
  extraction:
    ocr-concurrency: 4
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
mvn -pl doclens-spring-boot-starter -am -Dtest=DocLensSpringPropertiesHealthGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 5: Commit**

```bash
git add doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrHealthGovernance.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingServiceProperties.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringProperties.java \
  doclens-server/src/main/resources/application.yml \
  doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensSpringPropertiesHealthGovernanceTest.java
git commit -m "feat(ocr): 增加节点治理默认配置"
```

### Task 2: Persist Node Health Lifecycle Fields

**Files:**
- Create: `doclens-spring-boot-starter/src/main/resources/db/migration/V6__doclens_ocr_health_governance.sql`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrNode.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrNodeEntity.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/MybatisPlusOcrNodeRepository.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/MybatisPlusOcrNodeRepositoryHealthGovernanceTest.java`

- [ ] **Step 1: Write the failing persistence test**

```java
@Test
void repositoryPersistsCircuitWindowAndRecoveryCounters() {
    OcrNode saved = repository.save(sampleNode().withHealthGovernance(
            2L,
            1L,
            Optional.of(OffsetDateTime.parse("2026-06-09T10:00:00+08:00")),
            Optional.of(OffsetDateTime.parse("2026-06-10T10:00:00+08:00")),
            Optional.of("timeout")));

    OcrNode reloaded = repository.findById(saved.id()).orElseThrow();

    assertThat(reloaded.failureCount()).isEqualTo(2L);
    assertThat(reloaded.successCount()).isEqualTo(1L);
    assertThat(reloaded.lastFailureAt()).isPresent();
    assertThat(reloaded.circuitOpenUntil()).isPresent();
    assertThat(reloaded.lastError()).contains("timeout");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -pl doclens-spring-boot-starter -am -Dtest=MybatisPlusOcrNodeRepositoryHealthGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

```text
FAIL because circuit_open_until / last_error columns and mappings do not exist
```

- [ ] **Step 3: Add migration and entity mappings**

```sql
ALTER TABLE ocr_node
    ADD COLUMN consecutive_failure_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN recovery_success_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN last_health_check_at TIMESTAMP WITH TIME ZONE NULL,
    ADD COLUMN circuit_open_until TIMESTAMP WITH TIME ZONE NULL,
    ADD COLUMN last_manual_recovery_at TIMESTAMP WITH TIME ZONE NULL;
```

```java
private Long consecutiveFailureCount;
private Long recoverySuccessCount;
private OffsetDateTime lastHealthCheckAt;
private OffsetDateTime circuitOpenUntil;
private OffsetDateTime lastManualRecoveryAt;
```

```java
entity.setConsecutiveFailureCount(node.failureCount());
entity.setRecoverySuccessCount(node.successCount());
entity.setLastHealthCheckAt(node.lastRequestAt().orElse(null));
entity.setCircuitOpenUntil(node.circuitOpenUntil().orElse(null));
entity.setLastError(node.lastError().orElse(null));
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
mvn -pl doclens-spring-boot-starter -am -Dtest=MybatisPlusOcrNodeRepositoryHealthGovernanceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 5: Commit**

```bash
git add doclens-spring-boot-starter/src/main/resources/db/migration/V6__doclens_ocr_health_governance.sql \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/domain/OcrNode.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrNodeEntity.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/MybatisPlusOcrNodeRepository.java \
  doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/MybatisPlusOcrNodeRepositoryHealthGovernanceTest.java
git commit -m "feat(ocr): 持久化节点健康治理字段"
```

### Task 3: Add Runtime Slot and Queue Counters

**Files:**
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrRuntimeNode.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrRuntimeNodePool.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRuntimeNodeView.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrRuntimeNodePoolSlotTest.java`

- [ ] **Step 1: Write the failing runtime slot test**

```java
@Test
void tryAcquireSlotStopsAtNodeConcurrencyLimitAndTracksQueue() {
    OcrRuntimeNodePool pool = new OcrRuntimeNodePool(repositoryWithSingleNode(2, 50));
    pool.initialize();

    assertThat(pool.tryAcquireSlot("node-1")).isPresent();
    assertThat(pool.tryAcquireSlot("node-1")).isPresent();
    assertThat(pool.tryAcquireSlot("node-1")).isEmpty();

    pool.incrementQueued("node-1");

    OcrRuntimeNodeView view = pool.snapshot().getFirst();
    assertThat(view.inflightImages()).isEqualTo(2);
    assertThat(view.queuedImages()).isEqualTo(1);
    assertThat(view.availableSlots()).isEqualTo(0);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -pl doclens-spring-boot-starter -am -Dtest=OcrRuntimeNodePoolSlotTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

```text
FAIL because tryAcquireSlot/incrementQueued/availableSlots do not exist
```

- [ ] **Step 3: Implement atomic slot and queue counters**

```java
public Optional<OcrRuntimeNodeView> tryAcquireSlot(String nodeId) {
    return find(nodeId)
            .filter(OcrRuntimeNode::hasAvailableSlot)
            .filter(node -> node.tryAcquireSlot())
            .map(OcrRuntimeNode::toView);
}

public Optional<OcrRuntimeNodeView> releaseSlot(String nodeId) {
    return find(nodeId).map(node -> {
        node.releaseSlot();
        return node.toView();
    });
}

public Optional<OcrRuntimeNodeView> incrementQueued(String nodeId) {
    return find(nodeId).map(node -> {
        node.incrementQueued();
        return node.toView();
    });
}
```

```java
public boolean tryAcquireSlot() {
    while (true) {
        int current = inflightImages.get();
        if (current >= node.maxConcurrency()) {
            return false;
        }
        if (inflightImages.compareAndSet(current, current + 1)) {
            return true;
        }
    }
}
```

```java
public OcrRuntimeNodeView toView() {
    int inflight = inflightImages.get();
    int queued = queuedImages.get();
    int maxConcurrency = node.maxConcurrency();
    return new OcrRuntimeNodeView(
            node.id(),
            node.modelKey(),
            node.enabled(),
            node.participateGlobal(),
            node.status(),
            node.weight(),
            maxConcurrency,
            inflight,
            queued,
            Math.max(0, maxConcurrency - inflight),
            node.avgLatencyMs(),
            node.circuitOpenUntil(),
            node.failureCount(),
            node.successCount());
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
mvn -pl doclens-spring-boot-starter -am -Dtest=OcrRuntimeNodePoolSlotTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 5: Commit**

```bash
git add doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrRuntimeNode.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrRuntimeNodePool.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRuntimeNodeView.java \
  doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrRuntimeNodePoolSlotTest.java
git commit -m "feat(ocr): 增加节点运行时占槽与排队计数"
```

### Task 4: Implement Weighted Capacity Selector

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrNodeScore.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/SmoothWeightedRoundRobinState.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/WeightedCapacityOcrNodeSelector.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/adapter/application/WeightedCapacityOcrNodeSelectorTest.java`

- [ ] **Step 1: Write the failing selector tests**

```java
@Test
void selectorPrefersFreerNodeEvenWhenItsWeightIsLower() {
    WeightedCapacityOcrNodeSelector selector = new WeightedCapacityOcrNodeSelector(0.7D, 0.3D, 0.15D);

    List<OcrRuntimeNodeView> nodes = List.of(
            node("A", 80, 10, 10),
            node("B", 60, 10, 8),
            node("C", 20, 10, 0));

    Optional<OcrRuntimeNodeView> selected = selector.select(OcrRoutePolicy.globalLoadBalance("weighted-idle"), nodes);

    assertThat(selected).isPresent();
    assertThat(selected.get().nodeId()).isEqualTo("C");
}

@Test
void selectorSmoothsAcrossSimilarlyIdleNodesByWeight() {
    WeightedCapacityOcrNodeSelector selector = new WeightedCapacityOcrNodeSelector(0.7D, 0.3D, 0.15D);

    List<OcrRuntimeNodeView> nodes = List.of(
            node("A", 80, 10, 0),
            node("B", 60, 10, 0),
            node("C", 20, 10, 0));

    List<String> picks = IntStream.range(0, 8)
            .mapToObj(index -> selector.select(OcrRoutePolicy.globalLoadBalance("weighted-idle"), nodes).orElseThrow().nodeId())
            .toList();

    assertThat(picks).containsExactly("A", "B", "A", "C", "B", "A", "B", "A");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -pl doclens-core -am -Dtest=WeightedCapacityOcrNodeSelectorTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

```text
FAIL because WeightedCapacityOcrNodeSelector and OcrNodeScore do not exist
```

- [ ] **Step 3: Implement score and bucket-based selection**

```java
public final class WeightedCapacityOcrNodeSelector implements OcrNodeSelector {

    private final double idleFactor;
    private final double weightFactor;
    private final double topBucketThreshold;
    private final SmoothWeightedRoundRobinState roundRobinState = new SmoothWeightedRoundRobinState();

    public WeightedCapacityOcrNodeSelector(double idleFactor, double weightFactor, double topBucketThreshold) {
        this.idleFactor = idleFactor;
        this.weightFactor = weightFactor;
        this.topBucketThreshold = topBucketThreshold;
    }

    @Override
    public Optional<OcrRuntimeNodeView> select(OcrRoutePolicy policy, List<OcrRuntimeNodeView> nodes) {
        List<OcrRuntimeNodeView> candidates = nodes.stream()
                .filter(node -> node.enabled())
                .filter(node -> node.status() == OcrNodeStatus.UP)
                .filter(node -> node.availableSlots() > 0)
                .filter(node -> matchesPolicy(policy, node))
                .toList();

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        long totalWeight = candidates.stream().mapToLong(OcrRuntimeNodeView::weight).sum();
        List<OcrNodeScore> scores = candidates.stream()
                .map(node -> score(node, totalWeight))
                .toList();

        double topScore = scores.stream().mapToDouble(OcrNodeScore::score).max().orElse(0D);
        List<OcrRuntimeNodeView> topBucket = scores.stream()
                .filter(score -> topScore - score.score() <= topBucketThreshold)
                .map(OcrNodeScore::node)
                .toList();

        return roundRobinState.next(topBucket);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
mvn -pl doclens-core -am -Dtest=WeightedCapacityOcrNodeSelectorTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 5: Commit**

```bash
git add doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrNodeScore.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/SmoothWeightedRoundRobinState.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/WeightedCapacityOcrNodeSelector.java \
  doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/adapter/application/WeightedCapacityOcrNodeSelectorTest.java
git commit -m "feat(ocr): 实现空闲优先加权调度选择器"
```

### Task 5: Add Pending Queue and Synchronous Dispatch Coordinator

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrPendingRequest.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrPendingRequestQueue.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrDispatchCoordinator.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrDispatchAcquireResult.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/InMemoryOcrPendingRequestQueue.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrDispatchCoordinatorTest.java`

- [ ] **Step 1: Write the failing wait-queue tests**

```java
@Test
void requestEntersWaitQueueWhenAllMatchingNodesAreFull() {
    FakePendingQueue queue = new FakePendingQueue();
    FakeRuntimeNodeProvider nodeProvider = fullNodes("node-a", "node-b");
    OcrDispatchCoordinator coordinator = coordinator(nodeProvider, queue);

    OcrDispatchAcquireResult result = coordinator.acquire(sampleRequest(), OcrRoutePolicy.globalLoadBalance("weighted-idle"));

    assertThat(result.queued()).isTrue();
    assertThat(queue.size()).isEqualTo(1);
}

@Test
void releaseTriggersDrainAndDispatchesQueuedRequest() {
    FakePendingQueue queue = new FakePendingQueue();
    FakeRuntimeNodeProvider nodeProvider = oneNodeAtCapacityThenFree("node-a", 1);
    OcrDispatchCoordinator coordinator = coordinator(nodeProvider, queue);

    coordinator.acquire(sampleRequest(), OcrRoutePolicy.globalLoadBalance("weighted-idle"));
    coordinator.acquire(sampleRequest("doc-2"), OcrRoutePolicy.globalLoadBalance("weighted-idle"));

    coordinator.release("node-a");

    assertThat(queue.size()).isEqualTo(0);
    assertThat(nodeProvider.snapshot().getFirst().inflightImages()).isEqualTo(1);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -pl doclens-core -am -Dtest=OcrDispatchCoordinatorTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

```text
FAIL because coordinator and queue abstractions do not exist
```

- [ ] **Step 3: Implement queue and coordinator**

```java
public interface OcrPendingRequestQueue {

    void enqueue(OcrPendingRequest request);

    Optional<OcrPendingRequest> poll(String queueKey);

    int size(String queueKey);
}
```

```java
public final class OcrDispatchCoordinator {

    public OcrDispatchAcquireResult acquire(ImageOcrRequest request, OcrRoutePolicy policy) {
        Optional<OcrRuntimeNodeView> selected = selector.select(policy, nodeProvider.snapshot());
        if (selected.isPresent()) {
            Optional<OcrRuntimeNodeView> acquired = nodeProvider.tryAcquireSlot(selected.get().nodeId());
            if (acquired.isPresent()) {
                return OcrDispatchAcquireResult.dispatched(acquired.get());
            }
        }
        queue.enqueue(OcrPendingRequest.from(request, policy));
        incrementQueuedForMatchingNodes(policy);
        return OcrDispatchAcquireResult.queued();
    }

    public void release(String nodeId) {
        nodeProvider.releaseSlot(nodeId);
        drainEligibleQueue(nodeId);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
mvn -pl doclens-core -am -Dtest=OcrDispatchCoordinatorTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 5: Commit**

```bash
git add doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrPendingRequest.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrPendingRequestQueue.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrDispatchCoordinator.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrDispatchAcquireResult.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/InMemoryOcrPendingRequestQueue.java \
  doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrDispatchCoordinatorTest.java
git commit -m "feat(ocr): 增加节点满载等待池协调器"
```

### Task 6: Refactor Routing Service to Use Dispatch Coordinator

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingService.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensOcrResourceAutoConfiguration.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingServiceTest.java`

- [ ] **Step 1: Write the failing routing-service integration test**

```java
@Test
void routingServiceAcquiresSlotThroughCoordinatorAndReleasesItAfterExecution() {
    RecordingDispatchCoordinator coordinator = new RecordingDispatchCoordinator("node-a");
    RecordingNodeExecutor executor = new RecordingNodeExecutor();
    OcrRoutingService service = service(coordinator, executor);

    OcrRouteExecutionResult result = service.recognize(sampleRequest(), OcrRoutePolicy.globalLoadBalance("weighted-idle"));

    assertThat(result.nodeId()).isEqualTo("node-a");
    assertThat(coordinator.acquireCount()).isEqualTo(1);
    assertThat(coordinator.releaseCount()).isEqualTo(1);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -pl doclens-core -am -Dtest=OcrRoutingServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

```text
FAIL because service still talks directly to nodeProvider and selector
```

- [ ] **Step 3: Replace direct select/inflight handling**

```java
public OcrRouteExecutionResult recognize(ImageOcrRequest request, OcrRoutePolicy requestedPolicy) {
    OffsetDateTime startedAt = OffsetDateTime.now();
    OcrRoutePolicy policy = effectivePolicy(requestedPolicy);
    OcrRouteAccumulator accumulator = new OcrRouteAccumulator(startedAt);
    OcrDispatchAcquireResult acquireResult = dispatchCoordinator.acquire(request, policy);
    if (acquireResult.queued()) {
        acquireResult.awaitDispatch();
    }
    OcrRuntimeNodeView node = acquireResult.node().orElseThrow();
    try {
        return executeWithCoordinator(request, policy, node, accumulator);
    } finally {
        dispatchCoordinator.release(node.nodeId());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
mvn -pl doclens-core -am -Dtest=OcrRoutingServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 5: Commit**

```bash
git add doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingService.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensOcrResourceAutoConfiguration.java \
  doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/adapter/application/OcrRoutingServiceTest.java
git commit -m "feat(ocr): 让路由服务接入等待池调度"
```

### Task 7: Implement Circuit Open and Recovery State Machine

**Files:**
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthChecker.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthCheckerLifecycleTest.java`

- [ ] **Step 1: Write the failing lifecycle tests**

```java
@Test
void repeatedFailuresOpenCircuitUntilConfiguredDeadline() {
    OcrHealthChecker checker = checker(false, governance(3, 60, 600, 3, 3));
    OcrNode node = sampleNode();

    checker.checkNode(node);
    checker.checkNode(repository.findById(node.id()).orElseThrow());
    checker.checkNode(repository.findById(node.id()).orElseThrow());

    OcrNode reloaded = repository.findById(node.id()).orElseThrow();
    assertThat(reloaded.status()).isEqualTo(OcrNodeStatus.DOWN);
    assertThat(reloaded.circuitOpenUntil()).isPresent();
}

@Test
void nodeNeedsThreeSuccessfulRecoveryProbesAfterCircuitWindowExpires() {
    OcrHealthChecker checker = checkerSequence(List.of(false, false, false, true, true, true), governance(3, 60, 600, 3, 3));
    OcrNode node = sampleNode();

    failToOpenCircuit(checker, node);
    advanceClockBy(Duration.ofSeconds(601));

    checker.checkNode(repository.findById(node.id()).orElseThrow());
    checker.checkNode(repository.findById(node.id()).orElseThrow());
    checker.checkNode(repository.findById(node.id()).orElseThrow());

    OcrNode reloaded = repository.findById(node.id()).orElseThrow();
    assertThat(reloaded.status()).isEqualTo(OcrNodeStatus.UP);
    assertThat(reloaded.circuitOpenUntil()).isEmpty();
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -pl doclens-spring-boot-starter -am -Dtest=OcrHealthCheckerLifecycleTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

```text
FAIL because circuit window and recovery probe state are not modeled
```

- [ ] **Step 3: Implement lifecycle rules**

```java
private OcrNode failedNode(OcrNode node, String errorMessage) {
    long consecutiveFailures = node.failureCount() + 1;
    OffsetDateTime now = clock.now();
    Optional<OffsetDateTime> circuitOpenUntil = consecutiveFailures >= properties.failureThreshold()
            ? Optional.of(now.plusSeconds(properties.circuitOpenSeconds()))
            : node.circuitOpenUntil();
    return replaceHealth(node, OcrNodeStatus.DOWN, consecutiveFailures, 0L, Optional.of(errorMessage), circuitOpenUntil, now);
}

private boolean shouldProbe(OcrNode node, OffsetDateTime now) {
    return node.circuitOpenUntil().isEmpty() || !node.circuitOpenUntil().get().isAfter(now);
}

private OcrNode successNode(OcrNode node) {
    if (node.status() == OcrNodeStatus.DOWN) {
        return replaceHealth(node, OcrNodeStatus.RECOVERING, 0L, 1L, Optional.empty(), Optional.empty(), clock.now());
    }
    if (node.status() == OcrNodeStatus.RECOVERING && reachedRecoveryThreshold(node)) {
        return replaceHealth(node, OcrNodeStatus.UP, 0L, 0L, Optional.empty(), Optional.empty(), clock.now());
    }
    return replaceHealth(node, node.status(), 0L, node.successCount() + 1, Optional.empty(), node.circuitOpenUntil(), clock.now());
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
mvn -pl doclens-spring-boot-starter -am -Dtest=OcrHealthCheckerLifecycleTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 5: Commit**

```bash
git add doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthChecker.java \
  doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthCheckerLifecycleTest.java
git commit -m "feat(ocr): 实现节点熔断与复活状态机"
```

### Task 8: Add Manual Reconnect Flow

**Files:**
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrManualRecoveryService.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrNodeMaintenanceController.java`
- Create: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrNodeReconnectResponse.java`
- Test: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/OcrNodeManualReconnectApiContractTest.java`

- [ ] **Step 1: Write the failing manual reconnect contract test**

```java
@Test
void manualReconnectRunsBoundedRecoveryAttemptsAndReturnsUpdatedState() throws Exception {
    mockMvc.perform(post("/api/v1/ocr-nodes/{nodeId}/reconnect", existingNodeId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.attempts").value(3))
            .andExpect(jsonPath("$.healthy").value(true))
            .andExpect(jsonPath("$.status").value("UP"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -pl doclens-server -am -Dtest=OcrNodeManualReconnectApiContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

```text
FAIL because /reconnect endpoint does not exist
```

- [ ] **Step 3: Implement reconnect service and endpoint**

```java
public OcrNodeReconnectResponse reconnectNode(String nodeId) {
    OcrNode node = managementService.requireNode(nodeId);
    ManualRecoveryResult result = manualRecoveryService.recover(node);
    nodePoolProvider.ifAvailable(OcrRuntimeNodePool::refresh);
    return new OcrNodeReconnectResponse(
            result.healthy(),
            result.attempts(),
            result.node().status().name(),
            result.node().circuitOpenUntil().map(OffsetDateTime::toString).orElse(""));
}
```

```java
@PostMapping("/{nodeId}/reconnect")
public OcrNodeReconnectResponse reconnectNode(@PathVariable String nodeId) {
    return reconnectService.reconnectNode(nodeId);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
mvn -pl doclens-server -am -Dtest=OcrNodeManualReconnectApiContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 5: Commit**

```bash
git add doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrManualRecoveryService.java \
  doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrNodeMaintenanceController.java \
  doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrNodeReconnectResponse.java \
  doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/OcrNodeManualReconnectApiContractTest.java
git commit -m "feat(ocr): 增加节点手动复活连接接口"
```

### Task 9: Surface Real Queue and Health Metrics to Dashboard APIs

**Files:**
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/query/infrastructure/OcrNodeMetricsAggregator.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrNodeResponse.java`
- Test: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/OcrNodeApiContractTest.java`

- [ ] **Step 1: Write the failing API assertions**

```java
.andExpect(jsonPath("$.items[0].queued_images").value(2))
.andExpect(jsonPath("$.items[0].weight").value(50))
.andExpect(jsonPath("$.items[0].max_concurrency").value(10))
.andExpect(jsonPath("$.items[0].failure_count").value(3))
.andExpect(jsonPath("$.items[0].circuit_open_until").isNotEmpty());
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -pl doclens-server -am -Dtest=OcrNodeApiContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

```text
FAIL because queued and governance fields are absent or zeroed
```

- [ ] **Step 3: Wire runtime queue metrics and DTO fields**

```java
return new OcrNodeMetrics(
        runtimeView.queuedImages(),
        runtimeView.queuedImages(),
        processedImagesToday,
        successImages,
        failedImages,
        avgLatencyMs,
        p95LatencyMs,
        lastRequestAt,
        lastError);
```

```java
public record OcrNodeResponse(
        String id,
        String name,
        int weight,
        @JsonProperty("max_concurrency") int maxConcurrency,
        @JsonProperty("inflight_images") int inflightImages,
        @JsonProperty("queued_images") int queuedImages,
        @JsonProperty("failure_count") long failureCount,
        @JsonProperty("circuit_open_until") String circuitOpenUntil
) {
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
mvn -pl doclens-server -am -Dtest=OcrNodeApiContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 5: Commit**

```bash
git add doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/query/infrastructure/OcrNodeMetricsAggregator.java \
  doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrNodeResponse.java \
  doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/OcrNodeApiContractTest.java
git commit -m "feat(dashboard): 暴露节点排队与熔断指标"
```

### Task 10: Update Dashboard OCR Node UI

**Files:**
- Modify: `doclens-dashboard/src/types/ocrResources.ts`
- Modify: `doclens-dashboard/src/api/ocrResources.ts`
- Modify: `doclens-dashboard/src/components/ocr/OcrNodeTable.vue`
- Modify: `doclens-dashboard/src/components/ocr/OcrNodeDetailDrawer.vue`
- Modify: `doclens-dashboard/src/components/ocr/OcrNodeFormDrawer.vue`
- Modify: `doclens-dashboard/src/utils/ocrNodeFormRules.ts`
- Test: `doclens-dashboard/src/utils/__tests__/ocrNodeFormRules.test.ts`
- Test: `doclens-dashboard/src/utils/__tests__/ocrDisplayRules.test.ts`

- [ ] **Step 1: Write the failing dashboard tests**

```ts
test('ocr node form defaults weight and max concurrency to product values', () => {
  const form = createDefaultOcrNodeForm()

  assert.equal(form.weight, 50)
  assert.equal(form.maxConcurrency, 10)
})

test('ocr node display exposes reconnect affordance and circuit window state', () => {
  const summary = summarizeOcrNode({
    status: 'DOWN',
    circuit_open_until: '2026-06-10T10:00:00+08:00',
    queued_images: 2
  })

  assert.equal(summary.canReconnect, true)
  assert.equal(summary.queueLabel, '排队 2 张')
})
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
node --experimental-strip-types --test \
  doclens-dashboard/src/utils/__tests__/ocrNodeFormRules.test.ts \
  doclens-dashboard/src/utils/__tests__/ocrDisplayRules.test.ts
```

Expected:

```text
FAIL because defaults and reconnect display fields do not exist
```

- [ ] **Step 3: Implement UI field mapping and actions**

```ts
export function createDefaultOcrNodeForm(): OcrNodeFormModel {
  return {
    name: '',
    weight: 50,
    maxConcurrency: 10,
    enabled: true,
    participateGlobal: true
  }
}
```

```ts
export async function reconnectOcrNode(nodeId: string): Promise<OcrNodeReconnectResponse> {
  return requestJson(`/api/v1/ocr-nodes/${nodeId}/reconnect`, {
    method: 'POST'
  })
}
```

```vue
<n-button
  v-if="row.status !== 'UP'"
  size="small"
  tertiary
  @click="handleReconnect(row.id)"
>
  手动连接
</n-button>
```

- [ ] **Step 4: Run tests and build**

Run:

```bash
node --experimental-strip-types --test \
  doclens-dashboard/src/utils/__tests__/ocrNodeFormRules.test.ts \
  doclens-dashboard/src/utils/__tests__/ocrDisplayRules.test.ts

cd doclens-dashboard && npm run build
```

Expected:

```text
All tests pass
vite build succeeds
```

- [ ] **Step 5: Commit**

```bash
git add doclens-dashboard/src/types/ocrResources.ts \
  doclens-dashboard/src/api/ocrResources.ts \
  doclens-dashboard/src/components/ocr/OcrNodeTable.vue \
  doclens-dashboard/src/components/ocr/OcrNodeDetailDrawer.vue \
  doclens-dashboard/src/components/ocr/OcrNodeFormDrawer.vue \
  doclens-dashboard/src/utils/ocrNodeFormRules.ts \
  doclens-dashboard/src/utils/__tests__/ocrNodeFormRules.test.ts \
  doclens-dashboard/src/utils/__tests__/ocrDisplayRules.test.ts
git commit -m "feat(dashboard): 展示节点治理状态并支持手动连接"
```

### Task 11: End-to-End Verification

**Files:**
- Modify: none unless fixes are required
- Test: existing backend and dashboard suites

- [ ] **Step 1: Run focused backend routing and lifecycle tests**

Run:

```bash
mvn -pl doclens-server -am \
  -Dtest=OcrNodeApiContractTest,OcrNodeManualReconnectApiContractTest,DocLensOcrApiContractTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 2: Run focused core and starter tests**

Run:

```bash
mvn -pl doclens-core -am \
  -Dtest=WeightedCapacityOcrNodeSelectorTest,OcrDispatchCoordinatorTest,OcrRoutingServiceTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

mvn -pl doclens-spring-boot-starter -am \
  -Dtest=DocLensSpringPropertiesHealthGovernanceTest,MybatisPlusOcrNodeRepositoryHealthGovernanceTest,OcrRuntimeNodePoolSlotTest,OcrHealthCheckerLifecycleTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 3: Run dashboard tests and production build**

Run:

```bash
node --experimental-strip-types --test \
  doclens-dashboard/src/utils/__tests__/ocrDisplayRules.test.ts \
  doclens-dashboard/src/utils/__tests__/ocrNodeFormRules.test.ts \
  doclens-dashboard/src/utils/__tests__/uploadFormRules.test.ts \
  doclens-dashboard/src/utils/__tests__/llmMarkdownConfigRules.test.ts \
  doclens-dashboard/src/utils/__tests__/llmResultDisplayRules.test.ts \
  doclens-dashboard/src/utils/__tests__/ocrNodeTableLayout.test.ts

cd doclens-dashboard && npm run build
```

Expected:

```text
All tests pass
Build succeeds
```

- [ ] **Step 4: Manual verification checklist**

Verify in a local run:

```text
1. Create three nodes with weight 80 / 60 / 20 and maxConcurrency 10.
2. Upload one 8-page Word or PDF document.
3. Confirm batch detail shows distribution across multiple nodes instead of a single fixed node.
4. Fill one node to its maxConcurrency and confirm new images prefer freer nodes.
5. Fill all nodes and confirm queued image count increases rather than immediate routing failure.
6. Force a node health failure three consecutive times and confirm circuit-open timestamp appears.
7. Wait or simulate circuit expiry, then confirm three successful recovery probes are required before the node returns to UP.
8. Click 手动连接 on a down node and confirm it runs bounded recovery attempts without exposing credentials.
```

- [ ] **Step 5: Final commit only if verification fixes were required**

```bash
git add <only files changed during verification fixes>
git commit -m "fix(ocr): 修正联调发现的问题"
```

---

## Risks and Guardrails

- Do not mix request retry timing with health probe timing.
- Do not let manual reconnect bypass the recovery success threshold.
- Do not expose API keys or credential refs in reconnect responses, logs, or UI.
- Do not count executor queue depth as node wait-queue depth; `queuedImages` must represent dispatch wait semantics.
- Preserve current `OcrRoutingService.recognize()` synchronous contract to avoid breaking `ImageDocumentExtractor` and `PdfImageDocumentExtractor`.
- Keep `DEFAULT` as backend compatibility only; do not surface it in dashboard behavior changes introduced by this plan.

## Self-Review

- Spec coverage:
  - weighted idle scheduling: covered in Tasks 1, 4, 5, 6
  - node defaults `weight=50`, `maxConcurrency=10`: covered in Tasks 1 and 10
  - failure threshold / probe interval / circuit open / recovery threshold / manual reconnect: covered in Tasks 1, 2, 7, 8
  - wait queue behavior: covered in Tasks 3, 5, 6, 9, 10
  - dashboard visibility: covered in Tasks 9 and 10
- Placeholder scan:
  - no `TBD` / `TODO`
  - every task has exact files, commands, and code examples
- Type consistency:
  - consistent use of `queuedImages`, `circuitOpenUntil`, `manualRecoveryAttempts`, `topBucketThreshold`

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-06-09-ocr-weighted-scheduling-and-health-governance.md`.

Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints
