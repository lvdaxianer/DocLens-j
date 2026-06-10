# LLM Protocol And Health Check Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Support OpenAI and Anthropic LLM Markdown protocols, and surface LLM/OCR heartbeat failures in the dashboard with 5 second polling.

**Architecture:** Extend the existing singleton LLM config with an `api_type` field. Route Markdown post-processing and config testing through protocol-specific HTTP processors. Add a small LLM health monitor using its own scheduler/executor, while OCR keeps the existing health scheduler/executor and online OCR uses a real provider access probe. Dashboard polling remains 5 seconds and displays both LLM and OCR unavailable states.

**Tech Stack:** Java 21, Spring Boot auto-configuration, H2/Flyway, Java HTTP client, Vue 3 Composition API, Pinia, Naive UI, Vitest, JUnit 5.

---

## Component Map

- `doclens-core/.../LlmMarkdownApiType.java`: enum for `OPENAI` and `ANTHROPIC` protocols.
- `doclens-core/.../LlmMarkdownConfig.java`: carries URL, model, API type, credential, and health fields.
- `doclens-core/.../LlmMarkdownConfigService.java`: validates URL ending rules and normalizes endpoints.
- `doclens-spring-boot-starter/.../AnthropicMarkdownPostProcessor.java`: sends Anthropic `/v1/messages` requests with `x-api-key`.
- `doclens-spring-boot-starter/.../MarkdownPostProcessorFactory.java`: creates the correct processor for a config.
- `doclens-spring-boot-starter/.../LlmMarkdownHealthChecker.java`: tests current LLM config and persists health fields.
- `doclens-spring-boot-starter/.../LlmMarkdownHealthCheckScheduler.java`: schedules LLM health checks on a dedicated scheduler and executor.
- `doclens-spring-boot-starter/.../DashScopeOnlineOcrClient.java`: exposes real online OCR permission probing for OCR health checks.
- `doclens-dashboard/src/components/ocr/LlmMarkdownConfigPanel.vue`: lets users choose OpenAI or Anthropic and shows correct endpoint hint.
- `doclens-dashboard/src/views/OcrHealthView.vue`: displays LLM health and OCR node health warnings.

## Task 1: Persist LLM API Type And Health Fields

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/LlmMarkdownApiType.java`
- Create: `doclens-spring-boot-starter/src/main/resources/db/migration/V8__doclens_llm_protocol_health.sql`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/LlmMarkdownConfig.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/LlmMarkdownConfigSettings.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/LlmMarkdownConfigService.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/LlmMarkdownConfigEntity.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusLlmMarkdownConfigRepository.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/processing/interfaces/LlmMarkdownConfigRequest.java`
- Modify: `doclens-server/src/main/java/io/github/lvdaxianer/doclens/j/processing/interfaces/LlmMarkdownConfigResponse.java`
- Test: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/LlmMarkdownConfigServiceTest.java`
- Test: `doclens-server/src/test/java/io/github/lvdaxianer/doclens/j/contract/LlmMarkdownConfigApiContractTest.java`

- [ ] **Step 1: Write RED tests for API type and URL normalization**
  - Add service tests:
    - OpenAI accepts a URL ending in `/v1` and normalizes to `/v1/chat/completions`.
    - Anthropic accepts a URL ending in `/anthropic` and normalizes to `/anthropic/v1/messages`.
    - Response keeps `api_type`.

- [ ] **Step 2: Run focused RED tests**
  - Run: `./mvnw -pl doclens-core -Dtest=LlmMarkdownConfigServiceTest test`
  - Expected: fails because `LlmMarkdownApiType` and settings fields do not exist.

- [ ] **Step 3: Implement domain, migration, repository, and interface mapping**
  - Add enum values `OPENAI` and `ANTHROPIC`.
  - Add Flyway columns:
    - `api_type VARCHAR(32) NOT NULL DEFAULT 'openai'`
    - `healthy BOOLEAN NOT NULL DEFAULT FALSE`
    - `health_message VARCHAR(2000) NOT NULL DEFAULT ''`
    - `last_health_at TIMESTAMP WITH TIME ZONE`
  - Preserve existing credentials during update when `api_key` is blank.

- [ ] **Step 4: Run focused GREEN tests**
  - Run: `./mvnw -pl doclens-core,doclens-server -Dtest=LlmMarkdownConfigServiceTest,LlmMarkdownConfigApiContractTest test`
  - Expected: pass.

## Task 2: Add Anthropic Markdown Processor

**Files:**
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/AnthropicMarkdownPostProcessor.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MarkdownPostProcessorFactory.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ConfigurableMarkdownPostProcessor.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/DefaultLlmMarkdownConfigTester.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/AnthropicMarkdownPostProcessorTest.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/ConfigurableMarkdownPostProcessorTest.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/DefaultLlmMarkdownConfigTesterTest.java`

- [ ] **Step 1: Write RED processor tests**
  - Assert Anthropic requests use:
    - `POST /anthropic/v1/messages`
    - `x-api-key`
    - `anthropic-version: 2023-06-01`
    - body fields `model`, `max_tokens`, `system`, `messages`.
  - Assert response text is read from `content[0].text`.

- [ ] **Step 2: Run focused RED tests**
  - Run: `./mvnw -pl doclens-spring-boot-starter -Dtest=AnthropicMarkdownPostProcessorTest test`
  - Expected: fails because the processor does not exist.

- [ ] **Step 3: Implement factory and Anthropic processor**
  - Keep OpenAI behavior unchanged.
  - Use `MarkdownPostProcessorFactory` from configurable processor and config tester.
  - Sanitize API key in errors and logs.

- [ ] **Step 4: Run focused GREEN tests**
  - Run: `./mvnw -pl doclens-spring-boot-starter -Dtest=AnthropicMarkdownPostProcessorTest,ConfigurableMarkdownPostProcessorTest,DefaultLlmMarkdownConfigTesterTest test`
  - Expected: pass.

## Task 3: Add LLM Heartbeat Scheduler

**Files:**
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/LlmMarkdownHealthChecker.java`
- Create: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/LlmMarkdownHealthCheckScheduler.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/domain/LlmMarkdownConfigRepository.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MybatisPlusLlmMarkdownConfigRepository.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensProcessingAutoConfiguration.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/LlmMarkdownHealthCheckerTest.java`

- [ ] **Step 1: Write RED health tests**
  - Healthy test persists `healthy=true`, clears message, and sets `last_health_at`.
  - Failed test persists `healthy=false`, stores a sanitized failure message, and sets `last_health_at`.

- [ ] **Step 2: Run focused RED tests**
  - Run: `./mvnw -pl doclens-spring-boot-starter -Dtest=LlmMarkdownHealthCheckerTest test`
  - Expected: fails because health classes and repository method do not exist.

- [ ] **Step 3: Implement scheduler and repository update**
  - Use a dedicated scheduler named `doclens-llm-health-scheduler-`.
  - Use a dedicated worker executor named `doclens-llm-health-`.
  - Default interval is 5 seconds.
  - Run checks through `LlmMarkdownConfigTester`.

- [ ] **Step 4: Run focused GREEN tests**
  - Run: `./mvnw -pl doclens-spring-boot-starter -Dtest=LlmMarkdownHealthCheckerTest test`
  - Expected: pass.

## Task 4: Make Online OCR Health Use Real Access Probe

**Files:**
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthChecker.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/DashScopeOnlineOcrClient.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/autoconfigure/DocLensOcrResourceAutoConfiguration.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/OcrHealthCheckerTest.java`
- Test: `doclens-spring-boot-starter/src/test/java/io/github/lvdaxianer/doclens/j/adapter/infrastructure/DashScopeOnlineOcrClientTest.java`

- [ ] **Step 1: Confirm existing RED/GREEN coverage**
  - Existing dirty changes already add `hasExecutionPermission` and test online access denial.
  - Keep those changes and wire `OcrHealthChecker` to call the real probe for online nodes.

- [ ] **Step 2: Run focused tests**
  - Run: `./mvnw -pl doclens-spring-boot-starter -Dtest=OcrHealthCheckerTest,DashScopeOnlineOcrClientTest test`
  - Expected: pass after wiring.

## Task 5: Surface Health In Dashboard And LLM Config UI

**Files:**
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/query/infrastructure/OcrDashboardMetricsProvider.java`
- Modify: `doclens-dashboard/src/types/llmMarkdownConfig.ts`
- Modify: `doclens-dashboard/src/types/dashboard.ts`
- Modify: `doclens-dashboard/src/utils/llmMarkdownConfigRules.ts`
- Modify: `doclens-dashboard/src/composables/useLlmMarkdownConfig.ts`
- Modify: `doclens-dashboard/src/components/ocr/LlmMarkdownConfigPanel.vue`
- Modify: `doclens-dashboard/src/views/OcrHealthView.vue`
- Test: `doclens-dashboard/src/utils/__tests__/llmMarkdownConfigRules.test.ts`

- [ ] **Step 1: Write RED frontend utility tests**
  - OpenAI form defaults to `/v1` endpoint guidance.
  - Anthropic form defaults to `/anthropic` endpoint guidance.
  - Payload includes `api_type`.

- [ ] **Step 2: Run RED tests**
  - Run: `cd doclens-dashboard && npm test -- llmMarkdownConfigRules`
  - Expected: fails because `apiType` is not supported.

- [ ] **Step 3: Implement UI and type updates**
  - Add protocol selector with `OpenAI` and `Anthropic`.
  - Show protocol-specific endpoint hint.
  - Show LLM health status, last health time, and failure message.
  - Show OCR down/recovering alerts on OCR health page.

- [ ] **Step 4: Run GREEN tests**
  - Run: `cd doclens-dashboard && npm test -- llmMarkdownConfigRules`
  - Expected: pass.

## Task 6: Verification, Review, And Commit

**Files:**
- All touched files in this plan.

- [ ] **Step 1: Run backend focused suites**
  - Run: `./mvnw -pl doclens-core,doclens-spring-boot-starter,doclens-server test`
  - Expected: pass.

- [ ] **Step 2: Run frontend tests and build**
  - Run: `cd doclens-dashboard && npm test && npm run build`
  - Expected: pass.

- [ ] **Step 3: Apply code-review-spec**
  - Review all diff items against required rules, especially secrets, thread pool isolation, null safety, parameter count, and loop remote calls.

- [ ] **Step 4: Commit atomically**
  - Use Chinese Conventional Commit with detailed body/footer.
