## 1. LLM paused result state

- [x] 1.1 Add backend TDD coverage and implement LLM config-state classification so paused Markdown LLM configs return OCR passthrough with a paused warning and do not expose credential-resolution failures.
- [x] 1.2 Add failing dashboard tests for paused LLM result wording, then update result display rules to show `LLM 已暂停`.
- [x] 1.3 Run focused backend/frontend tests and relevant broader slices for LLM result state.

## 2. OCR concurrency observability and same-node parallelism

- [x] 2.1 Add backend TDD coverage and implement thread-pool capacity metrics plus page-task worker runtime settings exposure.
- [x] 2.2 Add page-task execution TDD coverage proving two queued pages can run concurrently on one OCR node with available slots.
- [x] 2.3 Update dashboard metric types and resource cards with TDD coverage to display active OCR work against configured capacity.
- [ ] 2.4 Run focused backend/frontend tests and relevant broader slices for OCR concurrency observability.
