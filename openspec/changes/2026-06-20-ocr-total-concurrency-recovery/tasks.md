## 1. Restore aggregate OCR dispatch capacity

- [x] 1.1 Add failing auto-configuration tests proving default page-task worker
  and OCR request executor concurrency use the sum of enabled participating OCR
  node capacities while explicit worker overrides still win.
- [x] 1.2 Implement aggregate node concurrency derivation and update server
  defaults so local page-task worker capacity can follow configured nodes.
- [x] 1.3 Run focused and broader verification for page-task worker and OCR
  thread-pool auto-configuration.

## 2. Fair queued page-task scheduling

- [ ] 2.1 Add a failing repository test proving queued page selection distributes
  a limited scan across multiple documents when one large document has many
  older queued pages.
- [ ] 2.2 Implement bounded fair selection for queued page tasks without changing
  page result aggregation order.
- [ ] 2.3 Run focused repository tests and the relevant page-task execution
  verification slice.
