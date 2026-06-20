# LLM Markdown Chunk Observability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make chunked Markdown processing observable in the dashboard by showing chunk count in batch detail and showing the chunk executor load in OCR resource metrics.

**Architecture:** Reuse the chunk metadata already emitted by the Markdown pipeline and project it into the dashboard query layer so batch detail rows can show chunk counts without re-running chunk planning. Reuse the existing OCR dashboard metrics pattern to expose the dedicated LLM chunk executor in the resource cards, keeping the chunking algorithm and request flow unchanged.

**Tech Stack:** Java 21, Spring Boot, JUnit 5, AssertJ, Vue 3, TypeScript, Naive UI, Vitest, Vue Test Utils

---

### Task 1: Add chunk count to batch detail document rows

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/query/application/DashboardRowAssembler.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/query/application/OcrQueryService.java`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/query/application/DashboardQueryServiceTest.java`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/query/application/DashboardQueryServiceFixtures.java`
- Modify: `doclens-dashboard/src/types/dashboard.ts`
- Modify: `doclens-dashboard/src/components/dashboard/DocumentTrackCards.vue`
- Modify: `doclens-dashboard/src/components/dashboard/batchDocumentTableColumns.ts`
- Modify: `doclens-dashboard/src/views/__tests__/BatchDetailView.result.test.ts`
- Modify: `doclens-dashboard/src/components/dashboard/__tests__/BatchDetailView.callback.test.ts`
- Modify: `doclens-dashboard/src/components/dashboard/__tests__/BatchDetailView.delete.test.ts`

- [ ] **Step 1: Write the failing backend test**

```java
@Test
void batchDetailDocumentRowsExposeChunkCount() {
    DashboardQueryService service = serviceWithChunkedDocument();

    Map<String, Object> batchDetail = service.batchDetail("batch-test");

    assertThat(batchDetail.get("documents")).asInstanceOf(LIST)
            .first()
            .satisfies(document -> assertThat(((Map<?, ?>) document).get("llm_chunk_count")).isEqualTo(4));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd /Users/lvdaxianer/workspace/my/project/DocLens-j && mvn -pl doclens-core -Dtest=DashboardQueryServiceTest test
```

Expected: FAIL because document rows do not yet expose `llm_chunk_count`.

- [ ] **Step 3: Write minimal implementation**

Project the chunk metadata into the dashboard row assembler and surface it in the Vue types and rendering:

```java
private Map<String, Object> documentRow(DocumentJob document, List<Map<String, Object>> finalHitNodes) {
    Map<String, Object> payload = new LinkedHashMap<>(15);
    payload.put("llm_chunk_count", documentResultChunkCount(document));
    ...
    return payload;
}

private int documentResultChunkCount(DocumentJob document) {
    return resultRepository.findByDocumentId(document.documentId())
            .map(result -> result.rawVendorOutput().get("llm_chunk_count"))
            .map(value -> value instanceof Number number ? number.intValue() : 0)
            .orElse(0);
}
```

```ts
export interface DocumentRow {
  ...
  llm_chunk_count: number
}
```

```vue
<span v-if="document.llm_chunk_count > 1">分块：{{ formatNumber(document.llm_chunk_count) }} 个 chunk</span>
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
cd /Users/lvdaxianer/workspace/my/project/DocLens-j && mvn -pl doclens-core -Dtest=DashboardQueryServiceTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git -C /Users/lvdaxianer/workspace/my/project/DocLens-j add \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/query/application/DashboardRowAssembler.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/query/application/OcrQueryService.java \
  doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/query/application/DashboardQueryServiceTest.java \
  doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/query/application/DashboardQueryServiceFixtures.java \
  doclens-dashboard/src/types/dashboard.ts \
  doclens-dashboard/src/components/dashboard/DocumentTrackCards.vue \
  doclens-dashboard/src/components/dashboard/batchDocumentTableColumns.ts \
  doclens-dashboard/src/views/__tests__/BatchDetailView.result.test.ts \
  doclens-dashboard/src/components/dashboard/__tests__/BatchDetailView.callback.test.ts \
  doclens-dashboard/src/components/dashboard/__tests__/BatchDetailView.delete.test.ts
```

### Task 2: Expose LLM Markdown chunk executor activity in OCR metrics

**Files:**
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensDashboardMetricsAutoConfiguration.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/query/infrastructure/OcrDashboardMetricsProvider.java`
- Modify: `doclens-dashboard/src/components/dashboard/OcrResourceMetricCards.vue`
- Modify: `doclens-dashboard/src/types/dashboard.ts`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/query/application/DashboardOcrMetricsTestFixtures.java`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/query/application/DashboardQueryServiceTest.java`
- Modify: `doclens-dashboard/src/components/dashboard/__tests__/OverviewView.delete.test.ts`
- Modify: `doclens-dashboard/src/views/OverviewView.vue`

- [ ] **Step 1: Write the failing backend metrics test**

```java
@Test
void summaryExposesLlmMarkdownChunkExecutorMetrics() {
    DashboardQueryService service = dashboardServiceWithOcrMetrics();

    Map<String, Object> summary = service.summary();

    assertThat(summary.get("ocr_resources")).asInstanceOf(MAP)
            .extractingByKey("thread_pools")
            .asInstanceOf(MAP)
            .extractingByKey("llm_markdown_chunk")
            .asInstanceOf(MAP)
            .containsEntry("active_count", 2)
            .containsEntry("queue_size", 3);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd /Users/lvdaxianer/workspace/my/project/DocLens-j && mvn -pl doclens-core -Dtest=DashboardQueryServiceTest test
```

Expected: FAIL because the LLM chunk executor is not yet part of the metrics payload.

- [ ] **Step 3: Write minimal implementation**

Thread the chunk executor into dashboard metrics and render a dedicated resource card:

```java
@Bean
@ConditionalOnMissingBean
DashboardThreadPools dashboardThreadPools(
        @Qualifier("doclensDocumentProcessingExecutor") ExecutorService documentProcessingExecutor,
        @Qualifier("doclensOcrRequestExecutor") ExecutorService ocrRequestExecutor,
        @Qualifier("doclensOcrHealthExecutor") ExecutorService ocrHealthExecutor,
        @Qualifier("doclensCallbackExecutor") ExecutorService callbackExecutor,
        @Qualifier("doclensLlmMarkdownChunkExecutor") ExecutorService llmMarkdownChunkExecutor
) {
    return new DashboardThreadPools(documentProcessingExecutor, ocrRequestExecutor, ocrHealthExecutor,
            callbackExecutor, llmMarkdownChunkExecutor);
}
```

```ts
const cards = computed(() => [
  ...,
  {
    key: 'llm-chunk-queue',
    label: 'LLM Markdown chunk 队列',
    value: formatNumber(props.metrics?.thread_pools.llm_markdown_chunk?.queue_size),
    note: `${formatNumber(props.metrics?.thread_pools.llm_markdown_chunk?.active_count)} 个活跃 chunk 任务`,
    icon: Activity,
    tone: 'latency'
  }
])
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
cd /Users/lvdaxianer/workspace/my/project/DocLens-j && mvn -pl doclens-core -Dtest=DashboardQueryServiceTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git -C /Users/lvdaxianer/workspace/my/project/DocLens-j add \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensDashboardMetricsAutoConfiguration.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/query/infrastructure/OcrDashboardMetricsProvider.java \
  doclens-dashboard/src/components/dashboard/OcrResourceMetricCards.vue \
  doclens-dashboard/src/types/dashboard.ts \
  doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/query/application/DashboardOcrMetricsTestFixtures.java \
  doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/query/application/DashboardQueryServiceTest.java \
  doclens-dashboard/src/components/dashboard/__tests__/OverviewView.delete.test.ts \
  doclens-dashboard/src/views/OverviewView.vue
```
