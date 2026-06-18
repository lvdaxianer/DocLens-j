# Ollama Model Name Free Input Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 OCR 节点表单把 Ollama 视为一套模型家族标准，并允许用户在该家族节点上自由输入真实 provider model 名称。

**Architecture:** 这次改动只落在 dashboard 前端的 OCR 节点表单规则与表单组件上。规则层负责识别 Ollama 家族 key 并决定是否需要自由输入；表单层只负责根据规则展示输入框并把用户输入原样提交，不引入新的后端协议或数据结构。

**Tech Stack:** Vue 3, TypeScript, Naive UI, Vitest.

---

## File Structure

- Modify: `doclens-dashboard/src/utils/ocrNodeFormRules.ts`
  - Change Ollama family detection and keep payload generation aligned with the new rule.
- Modify: `doclens-dashboard/src/components/ocr/OcrNodeFormDrawer.vue`
  - Keep the model-name input visible for Ollama family nodes and preserve form behavior.
- Modify: `doclens-dashboard/src/utils/__tests__/ocrNodeFormRules.test.ts`
  - Add a regression test for a real Ollama family key such as `ollama_deepseek_ocr`.

## Task 1: Ollama family model name remains editable

**Files:**
- Modify: `doclens-dashboard/src/utils/ocrNodeFormRules.ts`
- Modify: `doclens-dashboard/src/components/ocr/OcrNodeFormDrawer.vue`
- Modify: `doclens-dashboard/src/utils/__tests__/ocrNodeFormRules.test.ts`

- [ ] **Step 1: Write the failing test**

Add a regression test proving that a real Ollama family model key like `ollama_deepseek_ocr` is treated as Ollama and keeps the provider model user-editable.

```ts
test('ollama family node keeps provider model editable for real family keys', () => {
  const form = createDefaultOcrNodeForm('ollama_deepseek_ocr')

  form.name = '内网 Ollama 节点'
  form.host = '10.100.30.215'
  form.port = 11434

  assert.equal(isOcrNodeFormSubmittable(form), false)

  form.providerModel = 'deepseek-ocr:latest'

  assert.equal(isOcrNodeFormSubmittable(form), true)
  assert.deepEqual(createOcrNodePayload(form).node, {
    deployment_type: 'OFFLINE',
    name: '内网 Ollama 节点',
    host: '10.100.30.215',
    port: 11434,
    channel_key: 'ollama',
    provider_model: 'deepseek-ocr:latest',
    enabled: true,
    participate_global: true,
    weight: 50,
    max_concurrency: 10
  })
})
```

- [ ] **Step 2: Run it to make sure it fails**

Run:

```bash
pnpm -C doclens-dashboard test -- src/utils/__tests__/ocrNodeFormRules.test.ts
```

Expected: FAIL because `ollama_deepseek_ocr` is not treated as an Ollama family key yet.

- [ ] **Step 3: Write minimal implementation**

Update the Ollama detection and form rule so the family check is based on the real key shape used by the repository.

```ts
export function isOllamaModel(modelKey: string): boolean {
  return modelKey.trim().startsWith('ollama')
}
```

Update the form component so the model-name input remains tied to the Ollama family rule and not to a single literal key.

```vue
<script setup lang="ts">
const hasOllamaProviderModel = computed(() =>
  form.deploymentType === 'OFFLINE' && isOllamaModel(form.modelKey)
)
</script>

<template>
  <NFormItem v-if="hasOllamaProviderModel" label="模型名称">
    <NInput v-model:value="form.providerModel" placeholder="例如 deepseek-ocr:latest" />
  </NFormItem>
</template>
```

- [ ] **Step 4: Run the test to make sure it passes**

Run:

```bash
pnpm -C doclens-dashboard test -- src/utils/__tests__/ocrNodeFormRules.test.ts
```

Expected: PASS.

- [ ] **Step 5: Run the broader focused test set**

Run:

```bash
pnpm -C doclens-dashboard test -- src/utils/__tests__/ocrNodeFormRules.test.ts src/utils/__tests__/ocrDisplayRules.test.ts src/utils/__tests__/formValidationRules.test.ts
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add \
  openspec/changes/2026-06-18-ollama-model-name-free-input \
  docs/superpowers/plans/2026-06-18-ollama-model-name-free-input.md
git commit -F /tmp/ollama-model-name-free-input-commit.txt
```

Use a Chinese Conventional Commit with a full body and footer, and reference the OpenSpec change in the footer.
