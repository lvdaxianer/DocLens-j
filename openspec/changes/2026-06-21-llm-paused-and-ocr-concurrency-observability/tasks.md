## 1. LLM paused result state

- [x] 1.1 Add backend TDD coverage and implement LLM config-state classification so paused Markdown LLM configs return OCR passthrough with a paused warning and do not expose credential-resolution failures.
- [x] 1.2 Add failing dashboard tests for paused LLM result wording, then update result display rules to show `LLM 已暂停`.
- [ ] 1.3 Run focused backend/frontend tests and relevant broader slices for LLM result state.

## 2. OCR concurrency observability and same-node parallelism

- [ ] 2.1 Add failing backend tests for thread-pool capacity metrics and page-task worker runtime settings exposure.
- [ ] 2.2 Implement OCR metrics capacity fields and expose effective page-task worker settings.
- [ ] 2.3 Add a failing page-task execution test proving two queued pages can run concurrently on one OCR node with available slots.
- [ ] 2.4 Update dashboard metric types and resource cards to display active OCR work against configured capacity.
- [ ] 2.5 Run focused backend/frontend tests and relevant broader slices for OCR concurrency observability.
