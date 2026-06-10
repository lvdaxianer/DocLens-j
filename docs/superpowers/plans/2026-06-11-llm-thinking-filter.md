# LLM Thinking Filter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 防止 LLM Markdown 后处理结果把 `<think>` 标签和模型思考过程保存到最终解析内容。

**Architecture:** 在提示词中明确禁止输出思考过程，同时在核心批次保存链路增加兜底清洗。清洗逻辑只作用于 LLM Markdown 后处理结果，不影响未启用 LLM 时的 OCR 原文直通。

**Tech Stack:** Java 21, JUnit 5, AssertJ, Spring Boot starter Markdown post processors.

---

### Task 1: 过滤 LLM 思考过程

**Files:**
- Create: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownThinkingSanitizer.java`
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCase.java`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseTest.java`
- Modify: `doclens-spring-boot-starter/src/main/java/io/github/lvdaxianer/doclens/j/processing/infrastructure/MarkdownPrompt.java`

- [x] **Step 1: Write failing tests**

Add tests proving:
- Paired `<think>...</think>` is removed before saving Markdown.
- Unclosed leading `<think>` content falls back to OCR text instead of saving thinking text.

- [x] **Step 2: Run RED**

Run:

```bash
mvn -pl doclens-core -Dtest=BatchProcessingUseCaseTest#processBatchRemovesThinkBlockFromLlmMarkdown+processBatchFallsBackToOcrTextWhenLlmReturnsOnlyThinking test
```

Expected: FAIL because thinking content is currently saved as final Markdown.

- [x] **Step 3: Implement sanitizer and apply before persistence**

Create a sanitizer that removes paired thinking blocks and detects unclosed leading thinking blocks. Apply it in `successPostProcessedText` before writing `OcrResult`.

- [x] **Step 4: Strengthen prompt**

Update `MarkdownPrompt.systemPrompt()` to explicitly forbid `<think>` tags, reasoning, analysis, explanations, and any non-Markdown wrapper text.

- [x] **Step 5: Verify**

Run:

```bash
mvn -pl doclens-core -Dtest=BatchProcessingUseCaseTest test
mvn -pl doclens-spring-boot-starter -DskipTests compile
git diff --check
```

- [x] **Step 6: Review and commit**

Apply `code-review-spec`, fix issues, then commit with Chinese Conventional Commit.

---

## Self-Review

Spec coverage:
- The issue in the screenshot is covered by paired and unclosed `<think>` tests.
- Prompt-level prevention and persistence-level fallback are both covered.

Placeholder scan:
- No placeholder implementation steps remain.

Type consistency:
- Sanitizer output is consumed by `BatchProcessingUseCase` before constructing `PostProcessedText`.

### Task 2: 兼容 LLM 思考标签变体

**Files:**
- Modify: `doclens-core/src/main/java/io/github/lvdaxianer/doclens/j/processing/application/MarkdownThinkingSanitizer.java`
- Modify: `doclens-core/src/test/java/io/github/lvdaxianer/doclens/j/processing/application/BatchProcessingUseCaseTest.java`

- [x] **Step 1: Write failing test**

Add a test proving uppercase and whitespace variants such as `<THINK >...</THINK>` are removed before persistence.

- [x] **Step 2: Run RED**

Run:

```bash
mvn -pl doclens-core -Dtest=BatchProcessingUseCaseTest#processBatchRemovesThinkTagVariantsFromLlmMarkdown test
```

Expected: FAIL because the current sanitizer only matches exact lowercase `<think>` tags.

- [x] **Step 3: Implement case-insensitive tag cleanup**

Replace exact string matching with case-insensitive tag detection that supports whitespace inside the tag name and attributes after `think`.

- [x] **Step 4: Verify and commit**

Run:

```bash
mvn -pl doclens-core -Dtest=BatchProcessingUseCaseTest test
mvn -pl doclens-spring-boot-starter -DskipTests compile
git diff --check
```

Apply `code-review-spec`, fix issues, then commit with Chinese Conventional Commit.
