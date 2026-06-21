## 1. Expose batch OCR runtime capacity

- [x] 1.1 Add failing backend tests proving batch detail returns model/node runtime concurrency capacity for the batch route.
- [x] 1.2 Implement batch detail OCR runtime capacity fields using existing OCR resources and node runtime snapshots.
- [x] 1.3 Verify focused backend tests and relevant broader backend slice.

## 2. Show running allocation separately from final allocation

- [x] 2.1 Add failing backend and frontend tests proving current-document running OCR allocation is rendered before final OCR completion.
- [x] 2.2 Add current-document running allocation data and update the batch OCR route panel labels/layout.
- [x] 2.3 Verify focused batch route panel tests and dashboard build/test slice.

## 3. Clarify LLM env credential failures

- [ ] 3.1 Add failing frontend tests for LLM credential environment variable guidance and friendly missing-env result copy.
- [ ] 3.2 Update LLM config drawer/result display wording without changing credential storage semantics.
- [ ] 3.3 Verify focused LLM UI tests and dashboard utility/component test slice.

## 4. Final verification and archive

- [ ] 4.1 Run full OpenSpec validation, final implementation audit, and archive this completed change.
