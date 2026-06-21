## 1. Dashboard OCR metrics provider wiring

- [x] 1.1 Add a failing auto-configuration regression test proving Dashboard uses the real OCR metrics provider when OCR infrastructure beans exist.
- [x] 1.2 Fix Dashboard OCR metrics provider bean ordering/conditions so the real provider overrides the empty fallback.
- [x] 1.3 Verify focused tests, broader starter/query tests, and runtime Dashboard endpoint evidence.
