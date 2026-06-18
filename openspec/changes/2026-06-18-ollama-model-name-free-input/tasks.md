## 1. Ollama model-name free input

- [ ] 1.1 Add a failing test that proves a real Ollama family model key such as
  `ollama_deepseek_ocr` shows the editable model-name field and keeps the
  provider model user-editable.
- [ ] 1.2 Update the OCR node form rules so Ollama family detection matches the
  real model key shape used in the repository, not only the literal string
  `ollama`.
- [ ] 1.3 Re-run the focused OCR form-rule test and the broader OCR utility test
  set to confirm the new Ollama rule does not change PaddleOCR or online-node
  behavior.
- [ ] 1.4 Mark the task complete and commit the change with a Chinese
  Conventional Commit message.
