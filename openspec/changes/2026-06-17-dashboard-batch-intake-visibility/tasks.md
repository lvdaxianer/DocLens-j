## 1. Dashboard Detail API

- [ ] 1.1 Add a focused backend contract test proving Dashboard batch detail `batch` includes `callback_url`, `idempotency_key`, and `metadata`.
- [ ] 1.2 Extend the Dashboard batch row read model to expose the three intake fields without changing upload or callback semantics.

## 2. Dashboard Detail UI

- [ ] 2.1 Add a focused Vue component test for rendering callback URL, idempotency key, formatted metadata, and empty placeholders.
- [ ] 2.2 Add the batch intake information panel to the batch detail page and update Dashboard types.

## 3. Verification And Archive

- [ ] 3.1 Run focused backend/frontend tests plus broader verification for touched modules, then archive the OpenSpec change.
