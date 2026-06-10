# Markdown Preview And Form Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make document Markdown render as preview with copy support, and add explicit UI validation for required dashboard form fields.

**Architecture:** Keep parsing and validation rules in small utility modules, then bind them into focused Vue components with Naive UI `NForm` validation. Preserve existing composables and API payload builders so behavior remains predictable.

**Tech Stack:** Vue 3, TypeScript, Naive UI, Vitest, node:test.

---

### Task 1: Markdown Result Preview

**Files:**
- Create: `doclens-dashboard/src/utils/markdownPreviewRules.ts`
- Test: `doclens-dashboard/src/utils/__tests__/markdownPreviewRules.test.ts`
- Modify: `doclens-dashboard/src/components/dashboard/DocumentResultDrawer.vue`
- Test: `doclens-dashboard/src/components/dashboard/__tests__/DocumentResultDrawer.test.ts`

- [ ] Step 1: Add failing tests for Markdown HTML rendering, escaping, links, and copy affordance.
- [ ] Step 2: Run focused tests and confirm RED.
- [ ] Step 3: Implement safe Markdown preview utility and drawer copy action.
- [ ] Step 4: Run focused and dashboard tests and confirm GREEN.

### Task 2: Required Field Validation

**Files:**
- Modify: `doclens-dashboard/src/utils/llmMarkdownConfigRules.ts`
- Modify: `doclens-dashboard/src/utils/ocrGovernanceConfigRules.ts`
- Modify: `doclens-dashboard/src/utils/ocrNodeFormRules.ts`
- Modify: `doclens-dashboard/src/utils/uploadFormRules.ts`
- Modify: `doclens-dashboard/src/components/ocr/LlmMarkdownConfigPanel.vue`
- Modify: `doclens-dashboard/src/components/ocr/OcrGovernanceConfigPanel.vue`
- Modify: `doclens-dashboard/src/components/ocr/OcrNodeFormDrawer.vue`
- Modify: `doclens-dashboard/src/components/upload/UploadAdvancedOptions.vue`
- Modify: `doclens-dashboard/src/components/upload/OcrRoutingSelector.vue`
- Test: existing `doclens-dashboard/src/utils/__tests__/*.test.ts` plus focused component tests where practical.

- [ ] Step 1: Add failing tests for validation rules and UI validation hooks.
- [ ] Step 2: Run focused tests and confirm RED.
- [ ] Step 3: Implement Naive UI rules, form refs, and validation-before-submit.
- [ ] Step 4: Run broader dashboard tests and build.
