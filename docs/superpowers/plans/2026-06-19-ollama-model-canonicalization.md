# Ollama Model Canonicalization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 Ollama OCR 使用 `ollama` 作为 canonical model key，并在后端、Dashboard 和测试中保持 `Ollama` 的清晰展示，同时兼容旧的 `ollama_deepseek_ocr` 配置。

**Architecture:** 这次改动分成后端契约收敛和前端展示联动两层。后端负责把模型定义、API 响应和兼容别名统一起来；前端只消费新的 canonical 语义，继续保留 Ollama family 的 provider model 自由输入行为，不新增新的表单字段或协议。

**Tech Stack:** Java 21, Spring Boot 3, Jackson, Vue 3, TypeScript, Naive UI, Vitest, Maven.

---

## File Structure

- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensOcrNodeManagementAutoConfiguration.java`
  - Rename the Ollama OCR model definition to the canonical `ollama` key and `Ollama` display name, while preserving legacy alias handling where needed.
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrModelResponse.java`
  - Explicitly serialize `name` so the API returns a readable model name for the dashboard.
- Modify: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/OcrNodeApiContractTest.java`
  - Assert the model list returns canonical `ollama` plus the readable `name`.
- Modify: `doclens-dashboard/src/components/ocr/OcrNodeFormDrawer.vue`
  - Keep the Ollama provider model input free-form while the selector reflects the new display name.
- Modify: `doclens-dashboard/src/utils/ocrNodeFormRules.ts`
  - Preserve Ollama family behavior against the canonical key and legacy alias.
- Modify: `doclens-dashboard/src/utils/ocrDisplayRules.ts`
  - Ensure fallback display logic continues to render the new canonical name cleanly.
- Modify: `doclens-dashboard/src/utils/__tests__/ocrNodeFormRules.test.ts`
  - Add or update regression coverage for `ollama` and `ollama_deepseek_ocr`.
- Modify: `doclens-dashboard/src/components/ocr/__tests__/OcrNodeFormDrawer.test.ts`
  - Assert the component shows `Ollama` in the selector text and still exposes the free-input hint.
- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OllamaOcrClientTest.java`
  - Update fixture data to the canonical key.
- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OllamaOcrHealthClientTest.java`
  - Update fixture data to the canonical key.
- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrNodeImageExecutorTest.java`
  - Update Ollama-related test fixtures to the canonical key where they model Ollama family behavior.

## Task 1: Canonicalize Ollama model identity in backend contracts

**Files:**
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensOcrNodeManagementAutoConfiguration.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrModelResponse.java`
- Modify: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/OcrNodeApiContractTest.java`
- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OllamaOcrClientTest.java`
- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OllamaOcrHealthClientTest.java`
- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrNodeImageExecutorTest.java`

- [ ] **Step 1: Write the failing backend contract test**

Add a contract test assertion that the model list contains a canonical `ollama` entry and that the response exposes a readable `name` field.

```java
@Test
void listSupportedModelsReturnsCanonicalOllama() throws Exception {
    String response = mockMvc.perform(authenticatedGet("/api/v1/ocr-models"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

    JsonNode ollamaModel = findModelByKey(response, "ollama");

    assertThat(ollamaModel.get("name").asText()).isEqualTo("Ollama");
    assertThat(ollamaModel.get("provider_model").asText()).isEqualTo("deepseek-ocr:latest");
}
```

- [ ] **Step 2: Run it to make sure it fails**

Run:

```bash
mvn -pl doclens-server -Dtest=OcrNodeApiContractTest test
```

Expected: FAIL because the API still exposes the legacy key or omits `name`.

- [ ] **Step 3: Write minimal implementation**

Update the backend model definition and response DTO:

```java
private static final String OLLAMA_MODEL_KEY = "ollama";
private static final String OLLAMA_MODEL_NAME = "Ollama";

@JsonProperty("name")
public String name() {
    return definition.name();
}
```

If a legacy alias is required for node parsing, keep it in the compatibility path
only and do not expose it as the canonical model list entry.

- [ ] **Step 4: Run the backend contract test again**

Run:

```bash
mvn -pl doclens-server -Dtest=OcrNodeApiContractTest test
```

Expected: PASS.

- [ ] **Step 5: Re-run related spring boot starter tests**

Run:

```bash
mvn -pl doclens-spring-boot-starter -Dtest=OllamaOcrClientTest,OllamaOcrHealthClientTest,PaddleOcrNodeImageExecutorTest test
```

Expected: PASS after the test fixtures are updated to the canonical key.

## Task 2: Update dashboard display and Ollama family form behavior

**Files:**
- Modify: `doclens-dashboard/src/components/ocr/OcrNodeFormDrawer.vue`
- Modify: `doclens-dashboard/src/utils/ocrNodeFormRules.ts`
- Modify: `doclens-dashboard/src/utils/ocrDisplayRules.ts`
- Modify: `doclens-dashboard/src/utils/__tests__/ocrNodeFormRules.test.ts`
- Modify: `doclens-dashboard/src/components/ocr/__tests__/OcrNodeFormDrawer.test.ts`

- [ ] **Step 1: Write the failing UI regression test**

Add a dashboard component test that mounts the OCR form with a canonical Ollama model and asserts the selector text includes `Ollama` while the provider model hint remains visible.

```ts
it('shows Ollama by display name and keeps provider model free input', () => {
  const wrapper = mount(OcrNodeFormDrawer, {
    props: {
      visible: true,
      selectedModelKey: 'ollama',
      models: [model('ollama', 'Ollama'), model('paddle_ocr', 'PaddleOCR')],
      loading: false
    }
  })

  expect(wrapper.text()).toContain('Ollama · ollama')
  expect(wrapper.text()).toContain('Ollama family 节点可自由填写真实 provider model')
})
```

- [ ] **Step 2: Run it to make sure it fails**

Run:

```bash
pnpm -C doclens-dashboard test -- src/components/ocr/__tests__/OcrNodeFormDrawer.test.ts
```

Expected: FAIL because the selector text and model fixtures still use the legacy key/name.

- [ ] **Step 3: Write minimal implementation**

Update the dashboard model display helper and form rules:

```ts
export function displayModelName(model: OcrModelDisplaySource): string {
  const name = (model.name ?? '').trim()
  if (name) {
    return name
  } else {
    return model.modelKey?.trim() || '-'
  }
}

export function isOllamaModel(modelKey: string): boolean {
  return modelKey.trim() === 'ollama' || modelKey.trim().startsWith('ollama_')
}
```

Update the selector label to render `Ollama` from the response `name`, while keeping the provider model field and hint unchanged.

- [ ] **Step 4: Run the UI test again**

Run:

```bash
pnpm -C doclens-dashboard test -- src/components/ocr/__tests__/OcrNodeFormDrawer.test.ts
```

Expected: PASS.

- [ ] **Step 5: Run the broader dashboard OCR rule tests**

Run:

```bash
pnpm -C doclens-dashboard test -- src/utils/__tests__/ocrNodeFormRules.test.ts src/components/ocr/__tests__/OcrNodeFormDrawer.test.ts
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add \
  openspec/changes/2026-06-19-ollama-model-canonicalization \
  docs/superpowers/plans/2026-06-19-ollama-model-canonicalization.md
git commit -F /tmp/ollama-model-canonicalization-commit.txt
```

Use a Chinese Conventional Commit with a full body and footer, and reference the OpenSpec change in the footer.

