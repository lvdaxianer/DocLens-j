## 1. Page-configured OCR concurrency

- [x] 1.1 Add a shared OCR concurrency resolver that prefers persisted dashboard nodes and falls back to bootstrap nodes.
- [ ] 1.2 Wire the OCR request executor and page-task worker defaults through the resolver, preserving explicit overrides.
- [ ] 1.3 Verify focused tests, broader starter tests/build, and runtime evidence.
