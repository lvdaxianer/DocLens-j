## 1. Make upload default model-bound

- [ ] 1.1 Add a failing frontend test proving the OCR routing selector defaults to PaddleOCR model load balance when that model is available.
- [ ] 1.2 Implement the upload routing selector default so page-loaded model data wins over the config/global fallback.
- [ ] 1.3 Verify focused upload routing tests and the broader dashboard test/build slice.

## 2. Expose OCR capacity clearly

- [ ] 2.1 Add failing backend and frontend tests for model-level concurrency capacity and node max concurrency display.
- [ ] 2.2 Add aggregate capacity fields to the model list response and render model/node capacity in the OCR resources UI.
- [ ] 2.3 Verify focused capacity tests and the broader backend/frontend slices.

## 3. Clarify batch OCR route attribution

- [ ] 3.1 Add failing backend and frontend tests proving batch hit rows include and render OCR model names distinctly from node/provider details.
- [ ] 3.2 Resolve model names for batch hit rows and update the route panel display copy.
- [ ] 3.3 Verify focused batch route tests and the broader dashboard/backend slices.

## 4. Final verification and archive

- [ ] 4.1 Run full OpenSpec validation, final implementation audit, and archive this completed change.
