## 1. Skip Markdown post-processing for already-orchestrated uploads

- [x] 1.1 Add a failing backend test that proves `llmOrchestrated=true` bypasses Markdown post-processing and preserves the extracted OCR text.
- [ ] 1.2 Add a failing backend test that proves `llmOrchestrated=false` keeps the existing Markdown post-processing path.
- [ ] 1.3 Update the batch creation request, request mapper, document job, and post-processing entry point so the flag is threaded through and the skip decision happens before the LLM call.
- [ ] 1.4 Re-run the focused backend tests and the relevant broader processing test slice to confirm the skip behavior and default behavior both remain correct.
