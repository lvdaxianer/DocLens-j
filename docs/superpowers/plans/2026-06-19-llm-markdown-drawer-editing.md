# LLM Markdown Drawer Editing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move LLM Markdown create/edit into a right-side drawer so the
configuration table and editor no longer compete for the same horizontal space.

**Architecture:** Keep the existing `useLlmMarkdownConfig` composable as the
single source of truth for the form state, row list, and save/test actions.
Replace the inline editor region in `LlmMarkdownConfigPanel` with a dedicated
drawer component that renders the same fields and footer actions. Wire the table
row edit action and the `新增配置` button to the same open/create drawer flow so
create and edit remain consistent.

**Tech Stack:** Vue 3, TypeScript, Naive UI, Vitest, Vue Test Utils

---

### Task 1: Add drawer coverage for create/edit entry points

**Files:**
- Create: `doclens-dashboard/src/components/ocr/__tests__/LlmMarkdownConfigPanel.drawer.test.ts`
- Modify: `doclens-dashboard/src/components/ocr/LlmMarkdownConfigPanel.vue`

- [ ] **Step 1: Write the failing test**

```ts
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h, ref } from 'vue'
import { mount } from '@vue/test-utils'

import LlmMarkdownConfigPanel from '@/components/ocr/LlmMarkdownConfigPanel.vue'

vi.mock('@/composables/useLlmMarkdownConfig', () => ({
  useLlmMarkdownConfig: () => ({
    form: ref({
      id: '',
      name: '',
      apiType: 'openai',
      url: '',
      model: '',
      credentialEnvVar: '',
      credentialConfigured: false,
      usageType: 'MARKDOWN_POST_PROCESSING',
      priority: 100,
      maxContextTokens: 16000,
      maxConcurrency: 1,
      requestIntervalMillis: 1000,
      defaultConfig: true,
      enabled: true,
      healthy: false,
      healthMessage: '',
      lastHealthAt: ''
    }),
    rows: ref([
      {
        id: 'llm-config-1',
        name: '默认 LLM 配置',
        apiType: 'openai',
        url: 'https://api.example.com/v1',
        model: 'gpt-4o-mini',
        credentialEnvVar: 'MODEL_API_KEY',
        credentialConfigured: true,
        usageType: 'MARKDOWN_POST_PROCESSING',
        priority: 100,
        maxContextTokens: 16000,
        maxConcurrency: 1,
        requestIntervalMillis: 1000,
        defaultConfig: true,
        enabled: true,
        healthy: true,
        healthMessage: '',
        lastHealthAt: ''
      }
    ]),
    isLoading: ref(false),
    isSaving: ref(false),
    isTesting: ref(false),
    actingId: ref(''),
    lastLoadedAt: ref(''),
    errorMessage: ref(''),
    editingTitle: ref('新增配置'),
    canSubmit: ref(true),
    hasConfigs: ref(true),
    emptyStatus: ref(''),
    capabilityHints: ref({ urlPlaceholder: '请输入完整接口地址', canTest: true }),
    loadConfig: vi.fn(),
    resetForm: vi.fn(),
    editConfig: vi.fn(),
    saveConfig: vi.fn(),
    toggleEnabled: vi.fn(),
    makeDefault: vi.fn(),
    removeConfig: vi.fn(),
    testConfig: vi.fn()
  })
}))

vi.mock('naive-ui', () => ({
  NAlert: passthrough('div', 'n-alert'),
  NButton: passthrough('button', 'n-button'),
  NDataTable: passthrough('div', 'n-data-table'),
  NDrawer: passthrough('aside', 'n-drawer'),
  NDrawerContent: passthrough('section', 'n-drawer-content'),
  NForm: passthrough('form', 'n-form'),
  NFormItem: passthrough('div', 'n-form-item'),
  NIcon: passthrough('span', 'n-icon'),
  NInput: passthrough('input', 'n-input'),
  NInputNumber: passthrough('input', 'n-input-number'),
  NSelect: passthrough('div', 'n-select'),
  NSwitch: passthrough('button', 'n-switch'),
  useMessage: () => ({ warning: vi.fn(), error: vi.fn(), success: vi.fn() })
}))

it('opens a drawer when creating a config and removes the inline editor from the panel body', async () => {
  const wrapper = mount(LlmMarkdownConfigPanel)

  await wrapper.findAll('button').find((button) => button.text() === '新增配置')?.trigger('click')

  expect(wrapper.find('.n-drawer').exists()).toBe(true)
  expect(wrapper.find('.llm-config-panel__editor').exists()).toBe(false)
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd doclens-dashboard && npx vitest run src/components/ocr/__tests__/LlmMarkdownConfigPanel.drawer.test.ts -v`
Expected: FAIL because the panel still renders the inline editor and does not expose a drawer.

- [ ] **Step 3: Write minimal implementation**

```vue
<script setup lang="ts">
const isDrawerVisible = ref(false)

function openCreateDrawer(): void {
  llmConfig.resetForm()
  isDrawerVisible.value = true
}
</script>

<template>
  <section class="panel llm-config-panel">
    <div class="panel__header llm-config-panel__header">
      <div>
        <h2 class="panel__title">LLM Markdown 后处理</h2>
        <span class="panel__hint">最后刷新：{{ formatDateTime(llmConfig.lastLoadedAt.value) }}</span>
      </div>
      <div class="llm-config-panel__actions">
        <NButton size="small" :loading="llmConfig.isLoading.value" @click="llmConfig.loadConfig">
          <template #icon><NIcon :component="RefreshCcw" /></template>
          刷新
        </NButton>
        <NButton size="small" type="primary" secondary @click="openCreateDrawer">
          <template #icon><NIcon :component="Plus" /></template>
          新增配置
        </NButton>
      </div>
    </div>
    <NDataTable
      class="llm-config-panel__table"
      :columns="columns"
      :data="llmConfig.rows.value"
      :loading="llmConfig.isLoading.value"
      :bordered="false"
      size="small"
    />
    <LlmMarkdownConfigDrawer
      :visible="isDrawerVisible"
      :form="llmConfig.form"
      :title="llmConfig.editingTitle.value"
      :loading="llmConfig.isSaving.value"
      :testing="llmConfig.isTesting.value"
      :can-submit="llmConfig.canSubmit.value"
      :capability-hints="llmConfig.capabilityHints.value"
      @close="isDrawerVisible = false"
      @save="llmConfig.saveConfig"
      @test="llmConfig.testConfig"
    />
  </section>
</template>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd doclens-dashboard && npx vitest run src/components/ocr/__tests__/LlmMarkdownConfigPanel.drawer.test.ts -v`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add doclens-dashboard/src/components/ocr/LlmMarkdownConfigPanel.vue doclens-dashboard/src/components/ocr/__tests__/LlmMarkdownConfigPanel.drawer.test.ts
git commit -m "✨ feat(ui): move llm config edit into drawer"
```

### Task 2: Rewire the shared LLM config editor state into the drawer

**Files:**
- Modify: `doclens-dashboard/src/composables/useLlmMarkdownConfig.ts`
- Modify: `doclens-dashboard/src/components/ocr/LlmMarkdownConfigPanel.vue`
- Create: `doclens-dashboard/src/components/ocr/LlmMarkdownConfigDrawer.vue`

- [ ] **Step 1: Write the failing test**

```ts
it('opens the same drawer for row edit and populates the selected config', async () => {
  const wrapper = mount(LlmMarkdownConfigPanel)
  await wrapper.findAll('button').find((button) => button.text() === '编辑')?.trigger('click')
  expect(wrapper.text()).toContain('编辑 LLM Markdown 配置')
  expect(wrapper.find('.n-drawer').exists()).toBe(true)
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd doclens-dashboard && npx vitest run src/components/ocr/__tests__/LlmMarkdownConfigPanel.drawer.test.ts -v`
Expected: FAIL because edit still targets inline state.

- [ ] **Step 3: Write minimal implementation**

```ts
const isDrawerVisible = shallowRef(false)

function openCreateDrawer(): void {
  resetForm()
  isDrawerVisible.value = true
}

function openEditDrawer(row: LlmMarkdownConfigRow): void {
  editConfig(row)
  isDrawerVisible.value = true
}

function closeDrawer(): void {
  isDrawerVisible.value = false
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd doclens-dashboard && npx vitest run src/components/ocr/__tests__/LlmMarkdownConfigPanel.drawer.test.ts -v`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add doclens-dashboard/src/components/ocr/LlmMarkdownConfigPanel.vue doclens-dashboard/src/components/ocr/LlmMarkdownConfigDrawer.vue doclens-dashboard/src/composables/useLlmMarkdownConfig.ts
git commit -m "✨ feat(ui): share llm config drawer state"
```

### Task 3: Verify the dashboard slice and finalize the change

**Files:**
- Modify: `doclens-dashboard/src/components/ocr/__tests__/LlmMarkdownConfigPanel.drawer.test.ts`
- Modify: `doclens-dashboard/src/components/ocr/LlmMarkdownConfigPanel.vue`
- Modify: `doclens-dashboard/src/components/ocr/LlmMarkdownConfigDrawer.vue`

- [ ] **Step 1: Add or adjust assertions for the drawer footer and field population**

```ts
expect(wrapper.text()).toContain('测试配置')
expect(wrapper.text()).toContain('保存')
```

- [ ] **Step 2: Run the focused UI tests and build**

Run: `cd doclens-dashboard && npm run test:ui && npm run build`
Expected: PASS.

- [ ] **Step 3: Run the plan consistency audit**

Check that the drawer now owns the editable LLM form, the table remains visible, and the row actions still function.

- [ ] **Step 4: Mark the task complete**

Update the OpenSpec checklist so exactly one checkbox is marked complete for this task.

- [ ] **Step 5: Commit**

```bash
git add doclens-dashboard/src/components/ocr/__tests__/LlmMarkdownConfigPanel.drawer.test.ts doclens-dashboard/src/components/ocr/LlmMarkdownConfigPanel.vue doclens-dashboard/src/components/ocr/LlmMarkdownConfigDrawer.vue
git commit -m "✅ test(ui): cover llm config drawer editing"
```
