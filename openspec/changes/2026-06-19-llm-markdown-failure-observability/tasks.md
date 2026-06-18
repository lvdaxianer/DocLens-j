## 1. Preserve LLM failure reasons

- [x] 1.1 Add a failing test that proves Markdown post-processing fallback keeps the root cause message instead of a generic wrapper message.
- [x] 1.2 Update the Markdown post-processing failure path so the stored OCR result surfaces the concrete downstream failure reason.
- [x] 1.3 Add or adjust an HTTP post-processor test that proves wrapped IO failures still expose a useful cause string.
- [x] 1.4 Re-run the focused backend tests and the relevant broader test slice to confirm the failure message is preserved.
