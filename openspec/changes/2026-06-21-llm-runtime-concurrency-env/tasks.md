## 1. Runtime credential source

- [ ] 1.1 Add a failing backend test that proves production Markdown processor options resolve credentials from the process environment when no explicit map is supplied.
- [ ] 1.2 Implement the production environment fallback while keeping explicit test maps deterministic.
- [ ] 1.3 Run the focused credential/configurable Markdown tests.

## 2. LLM chunk concurrency limiter

- [ ] 2.1 Add failing limiter tests for per-slot request intervals and changed concurrency on the same config ID.
- [ ] 2.2 Implement per-slot request interval tracking and limiter state refresh on config limit changes.
- [ ] 2.3 Run focused limiter and chunk Markdown tests.

## 3. Dashboard failure wording and upload defaults

- [ ] 3.1 Add failing frontend tests for the credential failure copy and LLM concurrency wording.
- [ ] 3.2 Add a failing frontend test that proves the upload OCR route remains global load balance after models load.
- [ ] 3.3 Update the result drawer/config display copy so stale missing-env results are not described as a current process-env fact.
- [ ] 3.4 Update the upload OCR routing selector so model loading no longer overrides the global load-balance default.
- [ ] 3.5 Add a browser tab favicon and reference it from the Dashboard HTML entrypoint.
- [ ] 3.6 Run the focused Dashboard tests.

## 4. Verification and archive

- [ ] 4.1 Run the broader backend and Dashboard verification slices for touched areas.
- [ ] 4.2 Run plan-implementation consistency audit and code-review-spec.
- [ ] 4.3 Archive the completed OpenSpec change after all tasks pass.
