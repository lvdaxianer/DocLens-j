## 1. Credential Enforcement

- [x] 1.1 Add failing tests that prove missing caller credentials are rejected and
  anonymous fallback no longer exists.
- [x] 1.2 Update caller credential resolution and request mapping so uploads and
  Dashboard/LLM endpoints require a matched configured caller.

## 2. Caller-Scoped Reads and Mutations

- [ ] 2.1 Add failing tests for caller-scoped Dashboard and OCR query results,
  including resource-not-found behavior for foreign batches/documents.
- [ ] 2.2 Update query and mutation entry points to pass caller identity through
  and reject cross-caller access.

## 3. Dashboard Client Headers

- [x] 3.1 Add failing tests for Dashboard API clients that verify the caller
  credential header is attached to all requests.
- [x] 3.2 Update the frontend request layer to read `X-DocLens-Credential`
  from `localStorage` and forward it as the caller credential header on every
  API call.

## 4. Verification

- [x] 4.1 Run the focused backend and frontend tests, the relevant broader
  verification commands, OpenSpec strict validation, and review the diff.
- [x] 4.2 Add a local dev runner default for `DOCLENS_LOCAL_CREDENTIAL_KEY`
  and verify the script tests cover the default while preserving external
  environment overrides.
