## 1. Dashboard Session Caller Key

- [ ] 1.1 Add failing Dashboard API tests proving requests read
  `sessionStorage` key `X-DocLens-Credential-Key`, attach it as
  `X-DocLens-Api-Key` or `Authorization`, ignore the old
  `X-DocLens-Credential` key, and ignore the same new key in `localStorage`.
- [ ] 1.2 Update the Dashboard request helper to use `sessionStorage`, then run
  focused frontend tests, broader frontend verification, backend credential
  verification, OpenSpec strict validation, plan-implementation audit, and
  code-review-spec before marking the task complete.

## 2. OpenSpec Archive

- [ ] 2.1 Archive this completed OpenSpec change after implementation and final
  audit, then commit the archive/update separately.
