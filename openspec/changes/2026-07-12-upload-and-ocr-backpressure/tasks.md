## 1. Server-side upload contract enforcement

- [x] 1.1 Add failing tests proving the upload mapper rejects more than 30 files
  and rejects non-HTTP callback URL schemes.
- [x] 1.2 Implement server-side upload count validation and strict callback URI
  scheme parsing without changing successful upload mapping behavior.
- [x] 1.3 Run focused server tests and broader backend verification for the
  upload boundary.

## 2. Bounded OCR dispatch waiting

- [ ] 2.1 Add failing tests proving the OCR pending queue rejects overflow and
  routed requests fail with a stable timeout when dispatch cannot complete.
- [ ] 2.2 Implement bounded pending queue capacity and dispatch wait timeout
  wiring through OCR routing configuration.
- [ ] 2.3 Run focused OCR routing/auto-configuration tests and broader backend
  verification for the touched modules.
