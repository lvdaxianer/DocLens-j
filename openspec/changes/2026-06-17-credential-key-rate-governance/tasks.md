## 1. Credential Key Naming

- [x] 1.1 Add failing Dashboard API tests proving requests read
  `localStorage` key `X-DocLens-Credential-Key`, attach it as
  `X-DocLens-Api-Key` or `Authorization`, and ignore the old
  `X-DocLens-Credential` key.
- [x] 1.2 Update the Dashboard request helper and API tests to use
  `X-DocLens-Credential-Key` consistently, then run focused frontend tests.

## 2. Traffic Configuration

- [x] 2.1 Add failing Spring property binding tests for
  `doclens.traffic.default-limits`, `doclens.traffic.global-protection`, and
  per credential `rate-limits`.
- [x] 2.2 Extend `DocLensSpringProperties` and `application.yml` with the
  traffic governance configuration and conservative local defaults.

## 3. Caller Rate Limiter Core

- [x] 3.1 Add failing unit tests for caller + interface-group isolated token
  buckets, including decimal QPS, burst, refill, and independent callers.
- [x] 3.2 Implement the minimal in-memory caller rate limiter and policy merge
  logic needed to pass the focused tests.

## 4. Server Enforcement

- [ ] 4.1 Add failing server contract tests proving `dashboard-read`,
  `detail-read`, `upload-write`, `ocr-mutation`, `config-mutation`, and
  `admin-health` can have different limits and return `429` independently.
- [ ] 4.2 Wire traffic group resolution and caller rate limiting into the web
  request chain after caller credential resolution, without changing resource
  ownership filtering semantics.

## 5. Error Semantics and Protection

- [ ] 5.1 Add failing tests for `401` missing/invalid credential, `404`
  cross-caller resource access, `429` caller/group over-limit, and `503`
  global protection.
- [ ] 5.2 Update exception mapping, response headers, and logging so errors are
  distinguishable while credentials remain redacted.

## 6. Verification and Planning Commit

- [ ] 6.1 Run focused backend and frontend tests, broader Maven/frontend
  verification, OpenSpec strict validation, and code-review-spec against the
  diff before marking implementation complete.
