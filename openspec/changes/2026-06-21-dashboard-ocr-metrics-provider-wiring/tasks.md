## 1. Dashboard OCR metrics provider wiring

- [ ] 1.1 Add a failing auto-configuration regression test proving Dashboard uses the real OCR metrics provider when OCR infrastructure beans exist.
- [ ] 1.2 Fix Dashboard OCR metrics provider bean ordering/conditions so the real provider overrides the empty fallback.
- [ ] 1.3 Verify focused tests, broader starter/query tests, and runtime Dashboard endpoint evidence.
