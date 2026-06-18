## 1. Ollama model name hint

- [x] 1.1 Add a failing component test that proves the Ollama family model-name
  field shows a free-input hint, and that PaddleOCR does not show the hint.
- [x] 1.2 Update `OcrNodeFormDrawer.vue` to render an Ollama-specific helper
  line under the model-name input without changing payload or validation rules.
- [x] 1.3 Re-run the focused component test and the broader dashboard build to
  confirm the UI hint does not regress the existing OCR form behavior.
- [x] 1.4 Mark the task complete and commit the change with a Chinese
  Conventional Commit message.
