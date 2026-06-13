# LLM Chunking And Env Credentials Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add safe LLM Markdown post-processing for oversized OCR text, load-balance multiple LLM configurations, and remove raw API key entry/storage by switching LLM and online OCR credentials to environment variable references.

**Architecture:** LLM configuration owns a required `maxContextTokens` budget, an environment-variable credential reference, per-config concurrency limit, and per-config request interval. A focused chunking orchestrator estimates OCR text size, chooses single-call or ordered chunked post-processing, passes overlap as context-only input, and merges only the main-content outputs in order. Multiple healthy LLM configs are selected through round-robin load balancing and guarded by lightweight per-config rate gates. LLM and online OCR HTTP clients resolve secrets from server environment variables at call time, while persisted config and API responses only expose the environment variable name and configured status.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, H2, Vue 3, TypeScript, Naive UI, Vitest, Maven.

---

## Scope And Decisions

- LLM config must require `max_context_tokens`; old rows receive a safe migration default of `16000`.
- Runtime content budget is `floor(maxContextTokens * 0.8)`.
- Overlap target is `floor(contentBudget * 0.1)` on both previous and next sides.
- Chunk prompts must tell the model that previous/next overlap is context only and only `main_content` may be output.
- First implementation uses a conservative approximate token estimator, behind an interface so a real tokenizer can replace it later.
- First implementation falls back to original OCR text if any chunk fails; partial LLM output must not become final text.
- Programmatic ordered merge is the only merge step in v1; do not send all chunk outputs to LLM again.
- Frontend no longer accepts raw API keys for LLM configs or online OCR nodes.
- Persisted `credential_ref` is redefined as an environment variable name, not a raw secret.
- Saving validates environment variable name syntax only; testing and runtime calls validate that the environment variable exists and is non-blank.
- If no LLM config is configured or enabled, OCR processing continues unchanged and returns OCR text.
- Multiple healthy LLM configs for the same usage are load-balanced with round-robin; default config is only a fallback preference when the pool has a single config or when future routing needs a default marker.
- Each LLM config has `max_concurrency` and `request_interval_millis`; defaults are `1` and `1000`.
- LLM request interval is enforced per config, not globally, so one slow provider does not block unrelated providers.

## File Structure

### Backend Domain And Application

- Modify `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/LlmMarkdownConfig.java`
  - Carry `maxContextTokens` through configured/unconfigured/copy operations.
- Modify `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/LlmMarkdownConfigBuilder.java`
  - Add builder support for `maxContextTokens`.
- Modify `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/LlmMarkdownConfigState.java`
  - Store `maxContextTokens`.
- Modify `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/LlmMarkdownConfigAccessors.java`
  - Expose `maxContextTokens()`.
- Modify `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/LlmMarkdownConfigSettings.java`
  - Replace raw API key semantics with `credentialEnvVar`.
  - Add `maxContextTokens`.
  - Add `maxConcurrency` and `requestIntervalMillis`.
- Modify `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/LlmMarkdownConfigMutationFactory.java`
  - Validate URL/model/env-var/max-token/concurrency/interval fields.
  - Preserve old credential ref only if the user leaves env-var blank while editing an existing config.
- Modify `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/LlmConfigSelector.java`
  - Replace default-first selection with round-robin selection over healthy enabled configs for the requested usage.
- Create `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/LlmRateLimitPolicy.java`
  - Carry per-config concurrency and interval settings.
- Create `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/TokenEstimator.java`
  - Estimate token count for text.
- Create `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/ApproximateTokenEstimator.java`
  - Use conservative estimate: CJK chars count as 1 token; non-CJK text uses `ceil(nonWhitespaceChars / 4.0)` plus punctuation/line-break safety.
- Create `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownChunk.java`
  - Immutable chunk record with `chunkIndex`, `totalChunks`, `previousContext`, `mainContent`, `nextContext`, `estimatedTokens`.
- Create `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownChunkPlan.java`
  - Immutable plan record with `chunked`, `estimatedInputTokens`, `contentBudgetTokens`, `overlapTokens`, `chunks`.
- Create `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownChunker.java`
  - Split OCR text into one or more ordered chunks.
- Modify `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownPostProcessingRequest.java`
  - Add optional chunk fields or create a companion factory for chunk requests.
- Modify `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownPostProcessingResult.java`
  - Carry chunk metadata warnings without changing current callers.
- Modify `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentMarkdownPostProcessingService.java`
  - Use selected LLM config token budget through processor result metadata if available.
  - Preserve OCR fallback behavior.
- Modify `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentOcrResultBuilder.java`
  - Add raw output observability fields: `llm_chunked`, `llm_chunk_count`, `llm_max_context_tokens`, `llm_estimated_ocr_tokens`.

### Backend Infrastructure

- Create `doclens-spring-boot-starter/src/main/resources/db/migration/V11__doclens_llm_context_and_env_credentials.sql`
  - Add `max_context_tokens INTEGER NOT NULL DEFAULT 16000` to `doclens_llm_markdown_config`.
  - Add `max_concurrency INTEGER NOT NULL DEFAULT 1` to `doclens_llm_markdown_config`.
  - Add `request_interval_millis INTEGER NOT NULL DEFAULT 1000` to `doclens_llm_markdown_config`.
  - Add comments if supported by current database; keep H2 compatible.
- Modify `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/LlmMarkdownConfigEntity.java`
  - Add `maxContextTokens`.
- Modify `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusLlmMarkdownConfigRepository.java`
  - Map max token, concurrency, request interval, and credential env var.
- Create `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/shared/infrastructure/EnvironmentCredentialResolver.java`
  - Resolve environment variable references at runtime.
- Modify `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MarkdownPostProcessorFactory.java`
  - Resolve `credentialRef` through `EnvironmentCredentialResolver` before building HTTP processors.
- Modify `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ConfigurableMarkdownPostProcessor.java`
  - Wrap runtime processors with chunking orchestration.
- Create `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ChunkedMarkdownPostProcessor.java`
  - Choose single-call vs chunked execution using `MarkdownChunker`.
  - Execute chunks sequentially in order.
  - Merge only main chunk outputs.
  - Add business logs with `[LLM Markdown 分片]`.
- Create `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/LlmConfigRateLimiter.java`
  - Enforce per-config `maxConcurrency` with semaphores.
  - Enforce per-config `requestIntervalMillis` with last-start timestamps.
  - Sleep outside synchronized sections.
- Modify `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ConfigurableMarkdownPostProcessor.java`
  - Select a config for each document/chunk call through round-robin selector.
  - Acquire/release per-config rate limiter permits around actual HTTP calls.
- Modify `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MarkdownPrompt.java`
  - Add chunk prompt that includes `previous_context`, `main_content`, `next_context`.
  - Explicitly forbid outputting overlap context and thinking process.
- Modify `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/HttpMarkdownPostProcessor.java`
  - Keep raw key out of logs.
  - Support chunk prompts.
- Modify `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/AnthropicMarkdownPostProcessor.java`
  - Keep raw key out of logs.
  - Support chunk prompts.
- Modify `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/DashScopeOnlineOcrClient.java`
  - Resolve online OCR credential env var at runtime.
  - Return clear error when env var is missing.
- Modify `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/PaddleOcrNodeImageExecutor.java`
  - Ensure only online OCR path resolves credentials; Paddle/Ollama remain keyless unless explicitly configured later.

### Server API

- Modify `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/processing/interfaces/LlmMarkdownConfigRequest.java`
  - Replace `api_key` with `credential_env_var`.
  - Add `max_context_tokens`.
  - Add `max_concurrency`.
  - Add `request_interval_millis`.
- Modify `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/processing/interfaces/LlmMarkdownConfigResponse.java`
  - Add `credential_env_var` and `max_context_tokens`.
  - Add `max_concurrency` and `request_interval_millis`.
  - Never expose raw key.
- Modify `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/processing/interfaces/DefaultLlmMarkdownConfigOperations.java`
  - Map new request/response fields.
- Modify `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrNodeRequest.java`
  - Replace online OCR `api_key` semantics with `credential_env_var`.
- Modify `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrNodeResponse.java`
  - Return credential env var and configured status only.

### Frontend

- Modify `doclens-dashboard/src/types/llmMarkdownConfig.ts`
  - Replace `api_key` payload field with `credential_env_var`.
  - Add `max_context_tokens`.
  - Add `max_concurrency`.
  - Add `request_interval_millis`.
- Modify `doclens-dashboard/src/api/llmMarkdownConfig.ts`
  - Send new fields.
- Modify `doclens-dashboard/src/utils/llmMarkdownConfigRules.ts`
  - Require valid env var name and valid max token integer.
- Modify `doclens-dashboard/src/components/ocr/LlmMarkdownConfigPanel.vue`
  - Remove raw API Key input.
  - Add `API Key 环境变量名`.
  - Add required `最大上下文 Token 数`.
  - Add `最大并发数`.
  - Add `请求间隔（毫秒）`.
  - Show credential status as env var/configured, never secret.
- Modify `doclens-dashboard/src/components/ocr/LlmMarkdownConfigTableColumns.ts`
  - Add optional max token column or compact display in existing config metadata.
- Modify `doclens-dashboard/src/types/ocrResources.ts`
  - Rename online OCR credential payload to `credential_env_var`.
- Modify `doclens-dashboard/src/api/ocrResources.ts`
  - Send env var reference for online OCR nodes.
- Modify `doclens-dashboard/src/utils/ocrNodeFormRules.ts`
  - Require env var name for online OCR nodes.
  - Do not require credentials for Paddle/Ollama local nodes.
- Modify `doclens-dashboard/src/components/ocr/OcrNodeFormDrawer.vue`
  - Remove raw API Key input for online OCR.
  - Add `API Key 环境变量名`.
  - Hide credential input for keyless local OCR types.
- Modify `doclens-dashboard/src/components/ocr/OcrNodeDetailDrawer.vue`
  - Display credential env var and configured status only.

### Documentation And Runtime

- Modify `README.md`
  - Document `maxContextTokens`.
  - Document LLM and online OCR env vars.
  - Show examples:
    - `export MINIMAX_API_KEY=真实密钥`
    - `export DASHSCOPE_API_KEY=真实密钥`
- Modify `README_EN.md`
  - Mirror English documentation.
- Modify `scripts/run-backend-dev.sh`
  - Add commented examples for env vars without hardcoding real secrets.
- Check docker/deployment files if present and add env var examples without real values.

---

## Task 1: Persist LLM Context Budget

**Files:**
- Create: `doclens-spring-boot-starter/src/main/resources/db/migration/V11__doclens_llm_context_and_env_credentials.sql`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/LlmMarkdownConfig.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/LlmMarkdownConfigBuilder.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/LlmMarkdownConfigState.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/LlmMarkdownConfigAccessors.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/LlmMarkdownConfigSettings.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/LlmMarkdownConfigMutationFactory.java`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/LlmMarkdownConfigServiceTest.java`

- [ ] **Step 1: Write failing domain validation tests**

Add focused tests to `LlmMarkdownConfigServiceTest`:

```java
@Test
void createsConfigWithRequiredMaxContextTokens() {
    LlmMarkdownConfigService service = new LlmMarkdownConfigService(new InMemoryConfigRepository());

    LlmMarkdownConfig saved = service.createConfig(settings("主配置", 10, true, true)
            .maxContextTokens(16000)
            .build());

    assertThat(saved.maxContextTokens()).isEqualTo(16000);
}

@Test
void rejectsInvalidMaxContextTokens() {
    LlmMarkdownConfigService service = new LlmMarkdownConfigService(new InMemoryConfigRepository());

    assertThatThrownBy(() -> service.createConfig(settings("主配置", 10, true, true)
            .maxContextTokens(999)
            .build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("llm markdown max context tokens must be at least 1000");
}
```

- [ ] **Step 2: Run RED**

Run:

```bash
mvn -pl doclens-core -Dtest=LlmMarkdownConfigServiceTest test
```

Expected: compilation fails because `maxContextTokens` does not exist.

- [ ] **Step 3: Implement max token field**

Add `maxContextTokens` to state, builder, accessors, settings, mutation normalization, and default/unconfigured creation.

Use constants:

```java
private static final int DEFAULT_MAX_CONTEXT_TOKENS = 16000;
private static final int MIN_MAX_CONTEXT_TOKENS = 1000;
```

Validation message:

```java
"llm markdown max context tokens must be at least 1000"
```

- [ ] **Step 4: Add Flyway migration**

Create `V11__doclens_llm_context_and_env_credentials.sql`:

```sql
ALTER TABLE doclens_llm_markdown_config
    ADD COLUMN IF NOT EXISTS max_context_tokens INTEGER NOT NULL DEFAULT 16000;
```

- [ ] **Step 5: Run GREEN**

Run:

```bash
mvn -pl doclens-core -Dtest=LlmMarkdownConfigServiceTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/LlmMarkdownConfigSettings.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/LlmMarkdownConfigMutationFactory.java \
  doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/LlmMarkdownConfigServiceTest.java \
  doclens-spring-boot-starter/src/main/resources/db/migration/V11__doclens_llm_context_and_env_credentials.sql
git commit -m "feat: 增加LLM上下文token配置"
```

---

## Task 2: Expose Max Tokens Through Persistence And API

**Files:**
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/LlmMarkdownConfigEntity.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusLlmMarkdownConfigRepository.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/processing/interfaces/LlmMarkdownConfigRequest.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/processing/interfaces/LlmMarkdownConfigResponse.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/processing/interfaces/DefaultLlmMarkdownConfigOperations.java`
- Modify: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/LlmMarkdownConfigApiContractTest.java`
- Modify: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/LlmMarkdownConfigValidationApiContractTest.java`
- Modify: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/LlmMarkdownMultiConfigApiContractTest.java`

- [ ] **Step 1: Write failing API contract tests**

Update create/edit payloads to include:

```json
"max_context_tokens": 16000
```

Assert responses include:

```java
.andExpect(jsonPath("$.max_context_tokens").value(16000))
```

Add validation test:

```java
@Test
void rejectsTooSmallMaxContextTokens() throws Exception {
    mockMvc.perform(post("/api/v1/llm-markdown-config")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {
                      "name": "主配置",
                      "api_type": "openai",
                      "url": "https://llm.example.com/v1/chat/completions",
                      "model": "markdown-model",
                      "credential_env_var": "MINIMAX_API_KEY",
                      "max_context_tokens": 999,
                      "usage_type": "MARKDOWN_POST_PROCESSING",
                      "priority": 100,
                      "default_config": true,
                      "enabled": true
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("llm markdown max context tokens must be at least 1000"));
}
```

- [ ] **Step 2: Run RED**

Run:

```bash
mvn -pl doclens-server -Dtest=LlmMarkdownConfigApiContractTest,LlmMarkdownConfigValidationApiContractTest,LlmMarkdownMultiConfigApiContractTest test
```

Expected: FAIL because request/response field is missing.

- [ ] **Step 3: Implement API and persistence mapping**

Add `maxContextTokens` to entity, repository mapper conversion, request, response, and operations mapping.

- [ ] **Step 4: Run GREEN**

Run:

```bash
mvn -pl doclens-server -Dtest=LlmMarkdownConfigApiContractTest,LlmMarkdownConfigValidationApiContractTest,LlmMarkdownMultiConfigApiContractTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/LlmMarkdownConfigEntity.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusLlmMarkdownConfigRepository.java \
  doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/processing/interfaces \
  doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/LlmMarkdownConfigApiContractTest.java \
  doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/LlmMarkdownConfigValidationApiContractTest.java \
  doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/LlmMarkdownMultiConfigApiContractTest.java
git commit -m "feat: 暴露LLM上下文token接口"
```

---

## Task 3: Replace Raw LLM API Key With Env Var Reference

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/LlmMarkdownConfigSettings.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/LlmMarkdownConfigMutationFactory.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/processing/interfaces/LlmMarkdownConfigRequest.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/processing/interfaces/LlmMarkdownConfigResponse.java`
- Modify: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/LlmMarkdownConfigApiContractTest.java`
- Modify: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/LlmMarkdownConfigTestApiContractTest.java`
- Modify: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/LlmMarkdownConfigValidationApiContractTest.java`

- [ ] **Step 1: Write failing env var contract tests**

Change request payload from:

```json
"api_key": "sk-test"
```

to:

```json
"credential_env_var": "MINIMAX_API_KEY"
```

Assert:

```java
.andExpect(jsonPath("$.credential_env_var").value("MINIMAX_API_KEY"))
.andExpect(jsonPath("$.credential_configured").value(true))
.andExpect(jsonPath("$.api_key").doesNotExist())
```

Add invalid env var test:

```java
@Test
void rejectsInvalidCredentialEnvVarName() throws Exception {
    mockMvc.perform(post("/api/v1/llm-markdown-config")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {
                      "name": "主配置",
                      "api_type": "openai",
                      "url": "https://llm.example.com/v1/chat/completions",
                      "model": "markdown-model",
                      "credential_env_var": "bad-name",
                      "max_context_tokens": 16000,
                      "usage_type": "MARKDOWN_POST_PROCESSING",
                      "priority": 100,
                      "default_config": true,
                      "enabled": true
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("credential env var must match [A-Z_][A-Z0-9_]*"));
}
```

- [ ] **Step 2: Run RED**

Run:

```bash
mvn -pl doclens-server -Dtest=LlmMarkdownConfigApiContractTest,LlmMarkdownConfigTestApiContractTest,LlmMarkdownConfigValidationApiContractTest test
```

Expected: FAIL because `credential_env_var` is not supported.

- [ ] **Step 3: Implement LLM env var reference semantics**

Rename request-facing API field to `credential_env_var`. Internally keep `credentialRef` field name if a larger DB migration is not needed, but update Javadoc and variable names around API/application code to clarify it stores an environment variable reference.

Validation:

```java
private static final Pattern ENV_VAR_NAME = Pattern.compile("[A-Z_][A-Z0-9_]*");
```

Message:

```java
"credential env var must match [A-Z_][A-Z0-9_]*"
```

- [ ] **Step 4: Run GREEN**

Run:

```bash
mvn -pl doclens-server -Dtest=LlmMarkdownConfigApiContractTest,LlmMarkdownConfigTestApiContractTest,LlmMarkdownConfigValidationApiContractTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/LlmMarkdownConfigSettings.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/LlmMarkdownConfigMutationFactory.java \
  doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/processing/interfaces \
  doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/LlmMarkdownConfigApiContractTest.java \
  doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/LlmMarkdownConfigTestApiContractTest.java \
  doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/LlmMarkdownConfigValidationApiContractTest.java
git commit -m "feat: 使用环境变量引用LLM凭证"
```

---

## Task 4: Resolve LLM Credentials At Runtime

**Files:**
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/shared/infrastructure/EnvironmentCredentialResolver.java`
- Create: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/shared/infrastructure/EnvironmentCredentialResolverTest.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MarkdownPostProcessorFactory.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/DefaultLlmMarkdownConfigTester.java`
- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/DefaultLlmMarkdownConfigTesterTest.java`
- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/HttpMarkdownPostProcessorTest.java`

- [ ] **Step 1: Write failing resolver tests**

```java
@Test
void resolvesConfiguredEnvironmentValue() {
    EnvironmentCredentialResolver resolver = new EnvironmentCredentialResolver(Map.of("MINIMAX_API_KEY", "sk-test"));

    assertThat(resolver.resolve("MINIMAX_API_KEY")).isEqualTo("sk-test");
}

@Test
void rejectsMissingEnvironmentValue() {
    EnvironmentCredentialResolver resolver = new EnvironmentCredentialResolver(Map.of());

    assertThatThrownBy(() -> resolver.resolve("MINIMAX_API_KEY"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("credential environment variable MINIMAX_API_KEY is not configured");
}
```

- [ ] **Step 2: Run RED**

Run:

```bash
mvn -pl doclens-spring-boot-starter -Dtest=EnvironmentCredentialResolverTest,DefaultLlmMarkdownConfigTesterTest,HttpMarkdownPostProcessorTest test
```

Expected: FAIL because resolver does not exist.

- [ ] **Step 3: Implement resolver and wire LLM factory**

`EnvironmentCredentialResolver` must have:

```java
public EnvironmentCredentialResolver() {
    this(System.getenv());
}
```

Factory behavior:

- `config.credentialValue()` is treated as env var name.
- Resolve the actual secret immediately before creating HTTP/Anthropic processors.
- Never log resolved value.

- [ ] **Step 4: Run GREEN**

Run:

```bash
mvn -pl doclens-spring-boot-starter -Dtest=EnvironmentCredentialResolverTest,DefaultLlmMarkdownConfigTesterTest,HttpMarkdownPostProcessorTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/shared/infrastructure/EnvironmentCredentialResolver.java \
  doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/shared/infrastructure/EnvironmentCredentialResolverTest.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MarkdownPostProcessorFactory.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/DefaultLlmMarkdownConfigTester.java \
  doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure
git commit -m "feat: 运行时解析LLM环境变量凭证"
```

---

## Task 5: Replace Online OCR API Key With Env Var Reference

**Files:**
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrNodeRequest.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces/OcrNodeResponse.java`
- Modify: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/OcrNodeApiContractSupport.java`
- Modify: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/OcrNodeCredentialApiContractTest.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/DashScopeOnlineOcrClient.java`
- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/DashScopeOnlineOcrClientTest.java`

- [ ] **Step 1: Write failing online OCR credential tests**

Update online OCR node JSON from:

```json
"api_key": "sk-test"
```

to:

```json
"credential_env_var": "DASHSCOPE_API_KEY"
```

Assert:

```java
.andExpect(jsonPath("$.credential_env_var").value("DASHSCOPE_API_KEY"))
.andExpect(jsonPath("$.credential_configured").value(true))
.andExpect(jsonPath("$.api_key").doesNotExist())
```

- [ ] **Step 2: Run RED**

Run:

```bash
mvn -pl doclens-server -Dtest=OcrNodeCredentialApiContractTest,OcrNodeApiContractTest test
```

Expected: FAIL because online OCR request field is not supported.

- [ ] **Step 3: Implement server request/response change**

Map `credential_env_var` into the existing node credential reference. Keep local Paddle/Ollama nodes credential-optional.

- [ ] **Step 4: Wire DashScope runtime env resolution**

Use `EnvironmentCredentialResolver` in DashScope client path. Missing env var should produce a clear health/call failure:

```text
credential environment variable DASHSCOPE_API_KEY is not configured
```

- [ ] **Step 5: Run GREEN**

Run:

```bash
mvn -pl doclens-server -Dtest=OcrNodeCredentialApiContractTest,OcrNodeApiContractTest test
mvn -pl doclens-spring-boot-starter -Dtest=DashScopeOnlineOcrClientTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/adapter/interfaces \
  doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/OcrNodeApiContractSupport.java \
  doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/OcrNodeCredentialApiContractTest.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/DashScopeOnlineOcrClient.java \
  doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/DashScopeOnlineOcrClientTest.java
git commit -m "feat: 使用环境变量引用在线OCR凭证"
```

---

## Task 6: Add Token Estimation And Chunk Planning

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/TokenEstimator.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/ApproximateTokenEstimator.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownChunk.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownChunkPlan.java`
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownChunker.java`
- Create: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/ApproximateTokenEstimatorTest.java`
- Create: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownChunkerTest.java`

- [ ] **Step 1: Write failing estimator tests**

```java
@Test
void estimatesCjkTextConservatively() {
    TokenEstimator estimator = new ApproximateTokenEstimator();

    assertThat(estimator.estimate("中文内容")).isGreaterThanOrEqualTo(4);
}

@Test
void estimatesBlankTextAsZero() {
    TokenEstimator estimator = new ApproximateTokenEstimator();

    assertThat(estimator.estimate("  \n ")).isZero();
}
```

- [ ] **Step 2: Write failing chunker tests**

```java
@Test
void keepsSmallDocumentAsSingleChunk() {
    MarkdownChunker chunker = new MarkdownChunker(new ApproximateTokenEstimator());

    MarkdownChunkPlan plan = chunker.plan("短文档", 16000);

    assertThat(plan.chunked()).isFalse();
    assertThat(plan.chunks()).hasSize(1);
    assertThat(plan.chunks().getFirst().mainContent()).isEqualTo("短文档");
}

@Test
void splitsLargeDocumentWithContextOnlyOverlap() {
    MarkdownChunker chunker = new MarkdownChunker(new ApproximateTokenEstimator());
    String text = "段落内容\n\n".repeat(5000);

    MarkdownChunkPlan plan = chunker.plan(text, 2000);

    assertThat(plan.chunked()).isTrue();
    assertThat(plan.chunks()).hasSizeGreaterThan(1);
    assertThat(plan.chunks().get(1).previousContext()).isNotBlank();
    assertThat(plan.chunks().get(0).nextContext()).isNotBlank();
    assertThat(plan.chunks()).extracting(MarkdownChunk::chunkIndex)
            .containsExactlyElementsOf(IntStream.range(0, plan.chunks().size()).boxed().toList());
}
```

- [ ] **Step 3: Run RED**

Run:

```bash
mvn -pl doclens-core -Dtest=ApproximateTokenEstimatorTest,MarkdownChunkerTest test
```

Expected: FAIL because classes do not exist.

- [ ] **Step 4: Implement estimator and chunker**

Rules:

- `contentBudgetTokens = floor(maxContextTokens * 0.8)`.
- `overlapTokens = max(1, floor(contentBudgetTokens * 0.1))`.
- Prefer split boundaries in this order: page separator, Markdown heading, blank line, newline, hard character boundary.
- Never reorder chunks.
- Never include overlap in `mainContent`.

- [ ] **Step 5: Run GREEN**

Run:

```bash
mvn -pl doclens-core -Dtest=ApproximateTokenEstimatorTest,MarkdownChunkerTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/TokenEstimator.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/ApproximateTokenEstimator.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownChunk.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownChunkPlan.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownChunker.java \
  doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/ApproximateTokenEstimatorTest.java \
  doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownChunkerTest.java
git commit -m "feat: 增加LLM文本token估算和分片计划"
```

---

## Task 7: Execute Chunked LLM Markdown Post-Processing

**Files:**
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ChunkedMarkdownPostProcessor.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownPostProcessingRequest.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownPostProcessingResult.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ConfigurableMarkdownPostProcessor.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MarkdownPrompt.java`
- Create: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ChunkedMarkdownPostProcessorTest.java`
- Modify: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MarkdownPromptTest.java`

- [ ] **Step 1: Write failing chunked processor tests**

```java
@Test
void processesSmallDocumentOnce() {
    RecordingProcessor delegate = new RecordingProcessor(List.of(MarkdownPostProcessingResult.markdown("整理后")));
    ChunkedMarkdownPostProcessor processor = new ChunkedMarkdownPostProcessor(delegate,
            new MarkdownChunker(new ApproximateTokenEstimator()), 16000);

    MarkdownPostProcessingResult result = processor.process(request("短文档"));

    assertThat(result.markdown()).isEqualTo("整理后");
    assertThat(delegate.requests()).hasSize(1);
}

@Test
void processesLargeDocumentInOrderAndMergesOutputs() {
    RecordingProcessor delegate = new RecordingProcessor(List.of(
            MarkdownPostProcessingResult.markdown("第一段"),
            MarkdownPostProcessingResult.markdown("第二段")));
    ChunkedMarkdownPostProcessor processor = new ChunkedMarkdownPostProcessor(delegate,
            new MarkdownChunker(new ApproximateTokenEstimator()), 2000);

    MarkdownPostProcessingResult result = processor.process(request("段落内容\n\n".repeat(5000)));

    assertThat(result.markdown()).isEqualTo("第一段\n\n第二段");
    assertThat(delegate.requests()).hasSize(2);
    assertThat(delegate.requests().get(1).ocrText()).contains("previous_context");
}

@Test
void fallsBackToOriginalTextWhenAnyChunkFails() {
    RecordingProcessor delegate = new RecordingProcessor(List.of(
            MarkdownPostProcessingResult.markdown("第一段"),
            MarkdownPostProcessingResult.passthrough("原文", "LLM failed")));
    String original = "段落内容\n\n".repeat(5000);
    ChunkedMarkdownPostProcessor processor = new ChunkedMarkdownPostProcessor(delegate,
            new MarkdownChunker(new ApproximateTokenEstimator()), 2000);

    MarkdownPostProcessingResult result = processor.process(request(original));

    assertThat(result.markdown()).isEqualTo(original);
    assertThat(result.markdownApplied()).isFalse();
    assertThat(result.warnings()).anyMatch(warning -> warning.contains("chunk"));
}
```

- [ ] **Step 2: Run RED**

Run:

```bash
mvn -pl doclens-spring-boot-starter -Dtest=ChunkedMarkdownPostProcessorTest,MarkdownPromptTest test
```

Expected: FAIL because chunked processor does not exist.

- [ ] **Step 3: Implement chunk processor and prompt**

Prompt shape for chunk request:

```text
You are polishing OCR text into Markdown.
Use previous_context and next_context only to understand continuity.
Only output the Markdown result for main_content.
Do not repeat previous_context or next_context.
Do not output analysis, reasoning, or chain-of-thought.

previous_context:
...

main_content:
...

next_context:
...
```

- [ ] **Step 4: Wire runtime config max token**

`ConfigurableMarkdownPostProcessor` should create:

```java
MarkdownPostProcessor delegate = runtimeProcessor(config);
return new ChunkedMarkdownPostProcessor(delegate, new MarkdownChunker(new ApproximateTokenEstimator()),
        config.maxContextTokens()).process(request);
```

- [ ] **Step 5: Run GREEN**

Run:

```bash
mvn -pl doclens-spring-boot-starter -Dtest=ChunkedMarkdownPostProcessorTest,MarkdownPromptTest,ConfigurableMarkdownPostProcessorTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownPostProcessingRequest.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownPostProcessingResult.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ChunkedMarkdownPostProcessor.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ConfigurableMarkdownPostProcessor.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MarkdownPrompt.java \
  doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure
git commit -m "feat: 支持LLM Markdown大文档分片处理"
```

---

## Task 8: Add LLM Round-Robin And Rate Gates

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/LlmMarkdownConfig.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/LlmMarkdownConfigBuilder.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/LlmMarkdownConfigState.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/LlmMarkdownConfigAccessors.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/LlmMarkdownConfigSettings.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/LlmMarkdownConfigMutationFactory.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/LlmConfigSelector.java`
- Create: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/LlmConfigSelectorRoundRobinTest.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/LlmConfigRateLimiter.java`
- Create: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/LlmConfigRateLimiterTest.java`
- Modify: `doclens-spring-boot-starter/src/main/resources/db/migration/V11__doclens_llm_context_and_env_credentials.sql`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/LlmMarkdownConfigEntity.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusLlmMarkdownConfigRepository.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ConfigurableMarkdownPostProcessor.java`

- [ ] **Step 1: Write failing round-robin selector tests**

```java
@Test
void selectsHealthyConfigsByRoundRobin() {
    InMemoryConfigRepository repository = new InMemoryConfigRepository();
    repository.save(config("llm-a", 10, false));
    repository.save(config("llm-b", 20, true));
    LlmConfigSelector selector = new LlmConfigSelector(repository);

    assertThat(selector.select(LlmUsageType.MARKDOWN_POST_PROCESSING)).get().extracting(LlmMarkdownConfig::id)
            .isEqualTo("llm-a");
    assertThat(selector.select(LlmUsageType.MARKDOWN_POST_PROCESSING)).get().extracting(LlmMarkdownConfig::id)
            .isEqualTo("llm-b");
    assertThat(selector.select(LlmUsageType.MARKDOWN_POST_PROCESSING)).get().extracting(LlmMarkdownConfig::id)
            .isEqualTo("llm-a");
}

@Test
void skipsUnhealthyConfigsDuringRoundRobin() {
    InMemoryConfigRepository repository = new InMemoryConfigRepository();
    repository.save(config("llm-a", 10, false).updateHealth(false, "down", OffsetDateTime.now()));
    repository.save(config("llm-b", 20, true));
    LlmConfigSelector selector = new LlmConfigSelector(repository);

    assertThat(selector.select(LlmUsageType.MARKDOWN_POST_PROCESSING)).get().extracting(LlmMarkdownConfig::id)
            .isEqualTo("llm-b");
    assertThat(selector.select(LlmUsageType.MARKDOWN_POST_PROCESSING)).get().extracting(LlmMarkdownConfig::id)
            .isEqualTo("llm-b");
}
```

- [ ] **Step 2: Write failing rate limiter tests**

```java
@Test
void limitsConcurrentRequestsPerConfig() throws Exception {
    LlmConfigRateLimiter limiter = new LlmConfigRateLimiter();
    LlmMarkdownConfig config = config("llm-a", 1, 0);

    LlmConfigRateLimiter.Permit first = limiter.acquire(config);
    Future<Boolean> blocked = Executors.newSingleThreadExecutor().submit(() -> {
        try (LlmConfigRateLimiter.Permit ignored = limiter.acquire(config)) {
            return true;
        }
    });

    assertThat(blocked.isDone()).isFalse();
    first.close();
    assertThat(blocked.get(1, TimeUnit.SECONDS)).isTrue();
}

@Test
void waitsBetweenRequestStartsForSameConfig() {
    LlmConfigRateLimiter limiter = new LlmConfigRateLimiter();
    LlmMarkdownConfig config = config("llm-a", 1, 1000);

    Instant start = Instant.now();
    try (LlmConfigRateLimiter.Permit ignored = limiter.acquire(config)) {
        // first request starts immediately
    }
    try (LlmConfigRateLimiter.Permit ignored = limiter.acquire(config)) {
        assertThat(Duration.between(start, Instant.now())).isGreaterThanOrEqualTo(Duration.ofMillis(900));
    }
}
```

- [ ] **Step 3: Run RED**

Run:

```bash
mvn -pl doclens-core -Dtest=LlmConfigSelectorRoundRobinTest test
mvn -pl doclens-spring-boot-starter -Dtest=LlmConfigRateLimiterTest test
```

Expected: FAIL because round-robin and rate limiter are not implemented.

- [ ] **Step 4: Implement config fields and migration columns**

Add fields:

- `maxConcurrency`
- `requestIntervalMillis`

Validation:

- `maxConcurrency >= 1`
- `requestIntervalMillis >= 0`
- recommended UI default is `1000`

Update `V11__doclens_llm_context_and_env_credentials.sql`:

```sql
ALTER TABLE doclens_llm_markdown_config
    ADD COLUMN IF NOT EXISTS max_concurrency INTEGER NOT NULL DEFAULT 1;

ALTER TABLE doclens_llm_markdown_config
    ADD COLUMN IF NOT EXISTS request_interval_millis INTEGER NOT NULL DEFAULT 1000;
```

- [ ] **Step 5: Implement round-robin selector**

`LlmConfigSelector` should:

- Build a sorted healthy enabled pool by `priority`, then `id`.
- Keep an `AtomicLong` cursor per `usageType`.
- Select `pool[cursor % pool.size()]`.
- Not always prefer default config when multiple configs are healthy.
- Return empty only when no healthy enabled config exists.

- [ ] **Step 6: Implement per-config rate limiter**

`LlmConfigRateLimiter` should:

- Use a `ConcurrentHashMap<String, Semaphore>` keyed by config id.
- Use a `ConcurrentHashMap<String, AtomicLong>` for last start timestamp.
- Acquire semaphore before sleeping.
- Enforce interval between request starts for the same config.
- Release semaphore in `Permit.close()`.

- [ ] **Step 7: Wire limiter into LLM processing**

Wrap each actual LLM HTTP call:

```java
try (LlmConfigRateLimiter.Permit ignored = rateLimiter.acquire(config)) {
    return processor.process(request);
}
```

For chunked processing, selection can happen per document in v1 so all chunks from one document use the same LLM config. This keeps style consistent inside one document while still load-balancing documents across configs.

- [ ] **Step 8: Run GREEN**

Run:

```bash
mvn -pl doclens-core -Dtest=LlmConfigSelectorRoundRobinTest,LlmMarkdownConfigServiceTest test
mvn -pl doclens-spring-boot-starter -Dtest=LlmConfigRateLimiterTest,ConfigurableMarkdownPostProcessorTest,ChunkedMarkdownPostProcessorTest test
```

Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/LlmMarkdownConfigSettings.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/LlmMarkdownConfigMutationFactory.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/LlmConfigSelector.java \
  doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/LlmConfigSelectorRoundRobinTest.java \
  doclens-spring-boot-starter/src/main/resources/db/migration/V11__doclens_llm_context_and_env_credentials.sql \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/LlmMarkdownConfigEntity.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusLlmMarkdownConfigRepository.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/LlmConfigRateLimiter.java \
  doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ConfigurableMarkdownPostProcessor.java \
  doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/LlmConfigRateLimiterTest.java
git commit -m "feat: 增加LLM轮询负载均衡和限流"
```

---

## Task 9: Add Chunk Observability To OCR Results

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentOcrResultBuilder.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentMarkdownPostProcessingService.java`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseTest.java`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentOcrResultBuilderTest.java`

- [ ] **Step 1: Write failing raw output test**

Assert final raw output includes:

```java
assertThat(rawOutput).containsEntry("llm_chunked", true);
assertThat(rawOutput).containsEntry("llm_chunk_count", 2);
assertThat(rawOutput).containsEntry("llm_max_context_tokens", 2000);
assertThat(rawOutput).containsKey("llm_estimated_ocr_tokens");
```

- [ ] **Step 2: Run RED**

Run:

```bash
mvn -pl doclens-core -Dtest=DocumentOcrResultBuilderTest,BatchProcessingUseCaseTest test
```

Expected: FAIL because raw output metadata does not exist.

- [ ] **Step 3: Implement metadata propagation**

Extend `MarkdownPostProcessingResult` metadata or add a small metadata record. Keep old behavior for processors that do not provide chunk metadata.

- [ ] **Step 4: Run GREEN**

Run:

```bash
mvn -pl doclens-core -Dtest=DocumentOcrResultBuilderTest,BatchProcessingUseCaseTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentOcrResultBuilder.java \
  doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/DocumentMarkdownPostProcessingService.java \
  doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application
git commit -m "feat: 增加LLM分片处理观测字段"
```

---

## Task 10: Update Frontend LLM Config Form

**Files:**
- Modify: `doclens-dashboard/src/types/llmMarkdownConfig.ts`
- Modify: `doclens-dashboard/src/api/llmMarkdownConfig.ts`
- Modify: `doclens-dashboard/src/utils/llmMarkdownConfigRules.ts`
- Modify: `doclens-dashboard/src/utils/__tests__/llmMarkdownConfigRules.test.ts`
- Modify: `doclens-dashboard/src/components/ocr/LlmMarkdownConfigPanel.vue`
- Modify: `doclens-dashboard/src/components/ocr/LlmMarkdownConfigTableColumns.ts`
- Modify: `doclens-dashboard/src/components/ocr/__tests__/LlmMarkdownConfigPanel.test.ts`

- [ ] **Step 1: Write failing frontend validation tests**

Add tests:

```ts
it('requires max context tokens', () => {
  expect(validateMaxContextTokens(undefined)).toBe('请输入最大上下文 Token 数')
})

it('rejects invalid credential env var names', () => {
  expect(validateCredentialEnvVar('bad-name')).toBe('环境变量名只能包含大写字母、数字和下划线，且不能以数字开头')
})

it('requires a positive max concurrency', () => {
  expect(validateMaxConcurrency(0)).toBe('最大并发数必须大于等于 1')
})

it('requires a non-negative request interval', () => {
  expect(validateRequestIntervalMillis(-1)).toBe('请求间隔不能小于 0')
})
```

- [ ] **Step 2: Run RED**

Run:

```bash
cd doclens-dashboard && npm run test -- llmMarkdownConfigRules
```

Expected: FAIL because validators/fields are missing.

- [ ] **Step 3: Implement frontend type and form changes**

UI labels:

- `API Key 环境变量名`
- `最大上下文 Token 数`
- `最大并发数`
- `请求间隔（毫秒）`

Placeholders:

- `例如：MINIMAX_API_KEY`
- `例如：16000`
- `例如：1`
- `例如：1000`

Remove raw secret placeholder:

- `可选，保存后不可回显`

- [ ] **Step 4: Run GREEN**

Run:

```bash
cd doclens-dashboard && npm run test -- llmMarkdownConfigRules LlmMarkdownConfigPanel
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add doclens-dashboard/src/types/llmMarkdownConfig.ts \
  doclens-dashboard/src/api/llmMarkdownConfig.ts \
  doclens-dashboard/src/utils/llmMarkdownConfigRules.ts \
  doclens-dashboard/src/utils/__tests__/llmMarkdownConfigRules.test.ts \
  doclens-dashboard/src/components/ocr/LlmMarkdownConfigPanel.vue \
  doclens-dashboard/src/components/ocr/LlmMarkdownConfigTableColumns.ts \
  doclens-dashboard/src/components/ocr/__tests__/LlmMarkdownConfigPanel.test.ts
git commit -m "feat: 前端LLM配置使用环境变量和token预算"
```

---

## Task 11: Update Frontend Online OCR Credential Form

**Files:**
- Modify: `doclens-dashboard/src/types/ocrResources.ts`
- Modify: `doclens-dashboard/src/api/ocrResources.ts`
- Modify: `doclens-dashboard/src/utils/ocrNodeFormRules.ts`
- Modify: `doclens-dashboard/src/utils/__tests__/ocrNodeFormRules.test.ts`
- Modify: `doclens-dashboard/src/components/ocr/OcrNodeFormDrawer.vue`
- Modify: `doclens-dashboard/src/components/ocr/OcrNodeDetailDrawer.vue`

- [ ] **Step 1: Write failing OCR node form tests**

Add tests:

```ts
it('requires credential env var for online OCR nodes', () => {
  const result = validateOcrNodeCredentialEnvVar('', 'online_ocr')
  expect(result).toBe('请输入 API Key 环境变量名')
})

it('does not require credential env var for ollama nodes', () => {
  const result = validateOcrNodeCredentialEnvVar('', 'ollama')
  expect(result).toBe('')
})
```

- [ ] **Step 2: Run RED**

Run:

```bash
cd doclens-dashboard && npm run test -- ocrNodeFormRules
```

Expected: FAIL because env-var validator is missing.

- [ ] **Step 3: Implement OCR form change**

For online OCR nodes:

- Show `API Key 环境变量名`.
- Send `credential_env_var`.

For Paddle/Ollama nodes:

- Hide credential input.
- Do not submit raw key or env var unless future provider requires it.

- [ ] **Step 4: Run GREEN**

Run:

```bash
cd doclens-dashboard && npm run test -- ocrNodeFormRules
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add doclens-dashboard/src/types/ocrResources.ts \
  doclens-dashboard/src/api/ocrResources.ts \
  doclens-dashboard/src/utils/ocrNodeFormRules.ts \
  doclens-dashboard/src/utils/__tests__/ocrNodeFormRules.test.ts \
  doclens-dashboard/src/components/ocr/OcrNodeFormDrawer.vue \
  doclens-dashboard/src/components/ocr/OcrNodeDetailDrawer.vue
git commit -m "feat: 前端在线OCR凭证改为环境变量"
```

---

## Task 12: Documentation And Runtime Examples

**Files:**
- Modify: `README.md`
- Modify: `README_EN.md`
- Modify: `scripts/run-backend-dev.sh`
- Check: `docker-compose.yml`
- Check: `docs/**/*.md`

- [ ] **Step 1: Update documentation**

Add Chinese docs:

```markdown
LLM Markdown 后处理配置不保存真实 API Key。页面只填写环境变量名，例如 `MINIMAX_API_KEY`。启动服务前设置：

```bash
export MINIMAX_API_KEY=真实密钥
export DASHSCOPE_API_KEY=真实密钥
```

`最大上下文 Token 数` 用于控制大文档分片。DocLens 会使用 80% 作为正文预算，超过预算时按顺序分片处理，并用前后 10% overlap 作为上下文。

多个健康可用的 LLM 配置会按轮询方式负载均衡。每个配置可设置 `最大并发数` 和 `请求间隔（毫秒）`，例如并发 `1`、间隔 `1000` 表示同一配置一次只处理一个请求，且请求启动间隔至少 1 秒。
```

Add English mirror text.

- [ ] **Step 2: Add script comments**

In `scripts/run-backend-dev.sh`, add commented examples only:

```bash
# export MINIMAX_API_KEY=replace-with-real-secret
# export DASHSCOPE_API_KEY=replace-with-real-secret
```

- [ ] **Step 3: Run docs grep**

Run:

```bash
rg -n "api_key|API Key|credential_env_var|max_context_tokens|max_concurrency|request_interval_millis|MINIMAX_API_KEY|DASHSCOPE_API_KEY" README.md README_EN.md scripts docs
```

Expected: docs mention env-var references and do not instruct users to paste raw secrets into UI.

- [ ] **Step 4: Commit**

```bash
git add README.md README_EN.md scripts/run-backend-dev.sh docs
git commit -m "docs: 说明LLM分片和环境变量凭证配置"
```

---

## Task 13: Broader Verification And Service Restart

**Files:**
- No code files expected.

- [ ] **Step 1: Run backend focused tests**

```bash
mvn -pl doclens-core -Dtest=LlmMarkdownConfigServiceTest,LlmConfigSelectorRoundRobinTest,ApproximateTokenEstimatorTest,MarkdownChunkerTest,DocumentOcrResultBuilderTest test
mvn -pl doclens-spring-boot-starter -Dtest=EnvironmentCredentialResolverTest,LlmConfigRateLimiterTest,ChunkedMarkdownPostProcessorTest,MarkdownPromptTest,DashScopeOnlineOcrClientTest test
mvn -pl doclens-server -Dtest=LlmMarkdownConfigApiContractTest,LlmMarkdownConfigValidationApiContractTest,LlmMarkdownConfigTestApiContractTest,OcrNodeCredentialApiContractTest,OcrNodeApiContractTest test
```

Expected: PASS.

- [ ] **Step 2: Run frontend tests**

```bash
cd doclens-dashboard && npm run test -- llmMarkdownConfigRules ocrNodeFormRules
```

Expected: PASS.

- [ ] **Step 3: Run package build**

```bash
mvn -pl doclens-server -am package -DskipTests
```

Expected: PASS.

- [ ] **Step 4: Restart services**

Stop old backend process on `10003`, then start:

```bash
DOCLENS_INTEGRATIONS_OPEN_WEBUI_INTERNAL_TOKEN=7d3079585812617132d4b29611eb5dbb524475b1462a93413c49fb08102899ea \
MINIMAX_API_KEY="${MINIMAX_API_KEY}" \
DASHSCOPE_API_KEY="${DASHSCOPE_API_KEY}" \
java -jar doclens-server/target/doclens-server-0.1.0-SNAPSHOT.jar
```

Ensure frontend is running on `10002`.

- [ ] **Step 5: Smoke verify**

```bash
curl -sS http://127.0.0.1:10003/actuator/health
curl -sS -o /dev/null -w '%{http_code}' http://127.0.0.1:10002/dashboard/
```

Expected:

- backend returns `{"status":"UP",...}`
- frontend returns `200`

- [ ] **Step 6: Commit final verification fixes if needed**

Only commit if verification required code/test/doc fixes:

```bash
git status --short
git add <verified-files>
git commit -m "chore: 修复LLM分片凭证改造验证问题"
```

---

## Self-Review Checklist

- [ ] Every requirement has at least one task.
- [ ] LLM max token input is required in backend and frontend.
- [ ] Large OCR text is chunked when estimated tokens exceed 80% of max context tokens.
- [ ] Chunk overlap is context-only and excluded from merge output.
- [ ] Chunk outputs merge in original order.
- [ ] Any chunk failure falls back to OCR original text.
- [ ] LLM raw API Key input is removed from UI and API.
- [ ] Online OCR raw API Key input is removed from UI and API.
- [ ] Runtime callers resolve secrets from environment variables.
- [ ] API responses never expose raw secrets.
- [ ] Documentation explains env var setup and token-budget behavior.
- [ ] Multiple healthy LLM configs are selected by round-robin instead of default-first behavior.
- [ ] Each LLM config enforces its own max concurrency.
- [ ] Each LLM config enforces its own request interval.
- [ ] All new Java files/classes/methods include code-review-spec-required Javadocs and `@author lvdaxianerplus`.
- [ ] Relevant tests and package build pass before service restart.
