## 1. Flyway Migration Stability

- [ ] 1.1 Add a failing test proving V1 still contains the original idempotency-key unique constraint while V16 removes it for final schema.
- [ ] 1.2 Restore V1 without changing V16 behavior and verify duplicate idempotency keys are accepted after all migrations.
- [ ] 1.3 Run focused migration tests, backend compile/start verification, OpenSpec validation, and final diff check.
