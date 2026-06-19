# Upload Chunk Strategy Selection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let users choose a document-type chunking preset during upload so DocLens can apply different sliding-window defaults for general, news, technical, and academic articles.

**Architecture:** Keep chunking preset selection in the dashboard upload flow as a first-class form section next to the existing advanced upload options. Add a typed preset field to the multipart upload payload, map that preset to concrete chunking parameters on the server, and route those parameters into the Markdown chunker through a small configuration abstraction instead of hardcoding one global window size. Preserve the current default behavior by making `通用` the selected preset for new uploads. The first release uses fixed defaults of `GENERAL` = `400/80`, `NEWS` = `300/60`, `TECHNICAL` = `500/100`, and `ACADEMIC` = `800/150` for `chunk_size/overlap`.

**Tech Stack:** Vue 3, TypeScript, Naive UI, Pinia, Spring Boot, Java, JUnit, Vitest, Vue Test Utils

---

### Task 1: Add upload preset coverage in the dashboard form

**Files:**
- Create: `doclens-dashboard/src/components/upload/__tests__/UploadChunkStrategySelector.test.ts`
- Create: `doclens-dashboard/src/components/upload/UploadChunkStrategySelector.vue`
- Modify: `doclens-dashboard/src/components/dashboard/UploadDropzone.vue`
- Modify: `doclens-dashboard/src/types/upload.ts`
- Modify: `doclens-dashboard/src/utils/uploadFormRules.ts`

- [ ] **Step 1: Write the failing test**

```ts
import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'

import UploadChunkStrategySelector from '@/components/upload/UploadChunkStrategySelector.vue'

describe('UploadChunkStrategySelector', () => {
  it('shows the default preset and the four article presets', () => {
    const wrapper = mount(UploadChunkStrategySelector)

    expect(wrapper.text()).toContain('通用')
    expect(wrapper.text()).toContain('新闻类')
    expect(wrapper.text()).toContain('技术类')
    expect(wrapper.text()).toContain('论文类')
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd doclens-dashboard && npx vitest run src/components/upload/__tests__/UploadChunkStrategySelector.test.ts -v`
Expected: FAIL because the selector component does not exist yet.

- [ ] **Step 3: Write minimal implementation**

```vue
<script setup lang="ts">
import { NForm, NFormItem, NRadio, NRadioGroup } from 'naive-ui'

import type { UploadChunkStrategy, UploadChunkStrategyOptions } from '@/types/upload'

const model = defineModel<UploadChunkStrategyOptions>({ required: true })
const strategies: Array<{ label: string; value: UploadChunkStrategy; description: string }> = [
  { label: '通用', value: 'GENERAL', description: '默认滑动窗口，适合大多数文章' },
  { label: '新闻类', value: 'NEWS', description: '更小窗口，适合短段落和时效性内容' },
  { label: '技术类', value: 'TECHNICAL', description: '中等窗口，适合概念和步骤说明' },
  { label: '论文类', value: 'ACADEMIC', description: '更大窗口，适合长论证和公式说明' }
]
</script>

<template>
  <NForm class="upload-chunk-strategy" label-placement="top">
    <NFormItem label="文章分块策略">
      <NRadioGroup v-model:value="model.chunkStrategy">
        <NRadio v-for="item in strategies" :key="item.value" :value="item.value">
          {{ item.label }}
        </NRadio>
      </NRadioGroup>
    </NFormItem>
  </NForm>
</template>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd doclens-dashboard && npx vitest run src/components/upload/__tests__/UploadChunkStrategySelector.test.ts -v`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add doclens-dashboard/src/components/upload/UploadChunkStrategySelector.vue doclens-dashboard/src/components/upload/__tests__/UploadChunkStrategySelector.test.ts doclens-dashboard/src/components/dashboard/UploadDropzone.vue doclens-dashboard/src/types/upload.ts doclens-dashboard/src/utils/uploadFormRules.ts
git commit -m "✨ feat(upload): add chunk strategy preset selector"
```

### Task 2: Thread the chunk preset through upload payloads and request mapping

**Files:**
- Modify: `doclens-dashboard/src/components/dashboard/UploadDropzone.vue`
- Modify: `doclens-dashboard/src/types/upload.ts`
- Modify: `doclens-dashboard/src/api/upload.ts`
- Modify: `doclens-dashboard/src/stores/upload.ts`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/interfaces/CreateBatchRequestMapper.java`
- Modify: `doclens-api/src/main/java/io/github/lvdaxianer/doclens/j/api/CreateBatchRequest.java`

- [ ] **Step 1: Write the failing test**

```ts
import { describe, expect, it } from 'vitest'
import { createUploadFormData } from '@/api/upload'

describe('createUploadFormData chunk strategy', () => {
  it('posts the selected chunk strategy preset', () => {
    const formData = createUploadFormData({
      files: [new File(['x'], 'a.txt')],
      metadata: '{}',
      callbackUrl: '',
      idempotencyKey: '',
      ocrRoutingMode: 'GLOBAL_LOAD_BALANCE',
      ocrModelKey: '',
      ocrNodeId: '',
      ocrLoadBalanceStrategy: 'weighted-idle',
      chunkStrategy: 'TECHNICAL'
    })

    expect(formData.get('chunkStrategy')).toBe('TECHNICAL')
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd doclens-dashboard && npx vitest run src/api/__tests__/upload.test.ts -v`
Expected: FAIL because the multipart payload does not include `chunkStrategy` yet.

- [ ] **Step 3: Write minimal implementation**

```ts
const CHUNK_STRATEGY_FIELD = 'chunkStrategy'

export function createUploadFormData(options: UploadBatchOptions): FormData {
  const formData = new FormData()
  formData.append('files', options.files[0])
  appendOptional(formData, 'metadata', options.metadata)
  appendOptional(formData, 'callback_url', options.callbackUrl)
  appendOptional(formData, 'idempotency_key', options.idempotencyKey)
  appendOptional(formData, 'ocrRoutingMode', options.ocrRoutingMode)
  appendOptional(formData, 'ocrModelKey', options.ocrModelKey)
  appendOptional(formData, 'ocrNodeId', options.ocrNodeId)
  appendOptional(formData, 'ocrLoadBalanceStrategy', options.ocrLoadBalanceStrategy)
  appendOptional(formData, CHUNK_STRATEGY_FIELD, options.chunkStrategy)
  return formData
}
```

```java
private static final String CHUNK_STRATEGY_PARAM = "chunkStrategy";

return new CreateBatchRequest(uploadFiles, jsonCodec.parseObject(form.metadata()), form.callbackUrl(),
        form.idempotencyKey(), form.adapterOverride(), form.pdfMode(), form.ocrRoutingMode(),
        form.ocrModelKey(), form.ocrNodeId(), form.ocrLoadBalanceStrategy(), form.chunkStrategy(),
        caller.clientId(), caller.sourceApp(), caller.tenantKey().orElse(""));
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd doclens-dashboard && npx vitest run src/api/__tests__/upload.test.ts -v`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add doclens-dashboard/src/components/dashboard/UploadDropzone.vue doclens-dashboard/src/types/upload.ts doclens-dashboard/src/api/upload.ts doclens-dashboard/src/stores/upload.ts doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/ingestion/interfaces/CreateBatchRequestMapper.java doclens-api/src/main/java/io/github/lvdaxianer/doclens/j/api/CreateBatchRequest.java
git commit -m "✨ feat(upload): carry chunk preset to backend"
```

### Task 3: Introduce backend chunking policy resolution and preset-aware defaults

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownChunker.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/ChunkPlanContext.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/ChunkStrategy.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/ChunkStrategyDefaults.java`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownChunkerTest.java`
- Create: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/ChunkStrategyDefaultsTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void newsPresetUsesSmallerWindowThanAcademicPreset() {
    assertThat(ChunkStrategyDefaults.forPreset(ChunkStrategy.NEWS).contentBudgetTokens()).isLessThan(
            ChunkStrategyDefaults.forPreset(ChunkStrategy.ACADEMIC).contentBudgetTokens());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd doclens-core && mvn -q -Dtest=ChunkStrategyDefaultsTest test`
Expected: FAIL because the preset resolution classes do not exist yet.

- [ ] **Step 3: Write minimal implementation**

```java
public enum ChunkStrategy {
    GENERAL,
    NEWS,
    TECHNICAL,
    ACADEMIC
}
```

```java
public record ChunkStrategyDefaults(int contentBudgetTokens, int overlapTokens) {
    public static ChunkStrategyDefaults forPreset(ChunkStrategy strategy) {
        return switch (strategy) {
            case NEWS -> new ChunkStrategyDefaults(240, 60);
            case TECHNICAL -> new ChunkStrategyDefaults(400, 100);
            case ACADEMIC -> new ChunkStrategyDefaults(640, 150);
            case GENERAL -> new ChunkStrategyDefaults(320, 80);
        };
    }
}
```

```java
public MarkdownChunkPlan plan(String text, int maxContextTokens, ChunkStrategy strategy) {
    ChunkStrategyDefaults strategyDefaults = ChunkStrategyDefaults.forPreset(strategy);
    return plan(text, maxContextTokens, strategyDefaults.contentBudgetTokens(),
            strategyDefaults.overlapTokens());
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd doclens-core && mvn -q -Dtest=ChunkStrategyDefaultsTest,MarkdownChunkerTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownChunker.java doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/ChunkPlanContext.java doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/ChunkStrategy.java doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/ChunkStrategyDefaults.java doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownChunkerTest.java doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/ChunkStrategyDefaultsTest.java
git commit -m "✨ feat(chunking): add preset-aware defaults"
```

### Task 4: Persist, verify, and document the new upload chunking behavior

**Files:**
- Create: `openspec/changes/2026-06-19-upload-chunk-strategy-selection/.openspec.yaml`
- Create: `openspec/changes/2026-06-19-upload-chunk-strategy-selection/proposal.md`
- Create: `openspec/changes/2026-06-19-upload-chunk-strategy-selection/design.md`
- Create: `openspec/changes/2026-06-19-upload-chunk-strategy-selection/tasks.md`
- Create: `openspec/changes/2026-06-19-upload-chunk-strategy-selection/specs/upload-chunk-strategy/spec.md`
- Create: `docs/superpowers/plans/2026-06-19-upload-chunk-strategy-selection.md`

- [ ] **Step 1: Add the plan/spec text and review for placeholders**

Write the OpenSpec proposal/design/tasks/spec so they state:
- upload page offers preset selection at file upload time
- preset list is exactly `GENERAL`, `NEWS`, `TECHNICAL`, `ACADEMIC`
- default preset remains `GENERAL`
- preset defaults are `GENERAL` = `400/80`, `NEWS` = `300/60`,
  `TECHNICAL` = `500/100`, and `ACADEMIC` = `800/150`
- backend maps preset to chunk window parameters before calling the chunker
- no custom numeric window editing is exposed in the first release
- existing uploads without the new field keep the default preset behavior

- [ ] **Step 2: Validate the OpenSpec change**

Run: `openspec validate 2026-06-19-upload-chunk-strategy-selection --strict`
Expected: PASS.

- [ ] **Step 3: Run focused and broader verification**

Run:
```bash
cd doclens-dashboard && npm run build
cd doclens-core && mvn test -Dtest=MarkdownChunkerTest,ChunkStrategyDefaultsTest
cd doclens-server && mvn test -Dtest=CreateBatchRequestMapperTest
```
Expected: PASS.

- [ ] **Step 4: Run plan-implementation consistency audit**

Check that:
- every goal in the proposal has a task
- every task has a concrete test command
- file paths match the repo layout
- the selected preset names are consistent across dashboard, API, and backend

- [ ] **Step 5: Apply code-review-spec to the final diff**

Review the changed files against `/Users/lvdaxianer/.claude/skills/code-review-spec/SKILL.md` and `/Users/lvdaxianer/.claude/skills/code-review-spec/spec.md`.
Pay special attention to:
- method comments for new methods and classes
- magic strings for preset names
- parameter count limits on upload payloads and helpers
- if/else coverage around default preset fallback

- [ ] **Step 6: Mark the task complete and commit the plan change**

After all checks pass, mark exactly one task complete in the OpenSpec checklist and create the planning commit for the spec and plan assets.

```bash
git add openspec/changes/2026-06-19-upload-chunk-strategy-selection docs/superpowers/plans/2026-06-19-upload-chunk-strategy-selection.md
git commit -m "📝 docs(plan): add upload chunk strategy plan"
```
