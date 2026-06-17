## 1. Credential Enforcement

- [ ] 1.1 Add failing tests that prove missing caller credentials are rejected and
  anonymous fallback no longer exists.
- [ ] 1.2 Update caller credential resolution and request mapping so uploads and
  Dashboard/LLM endpoints require a matched configured caller.

## 2. Caller-Scoped Reads and Mutations

- [ ] 2.1 Add failing tests for caller-scoped Dashboard and OCR query results,
  including resource-not-found behavior for foreign batches/documents.
- [ ] 2.2 Update query and mutation entry points to pass caller identity through
  and reject cross-caller access.

## 3. Dashboard Client Headers

- [ ] 3.1 Add failing tests for Dashboard API clients that verify the caller
  credential header is attached to all requests.
- [ ] 3.2 Update the frontend request layer to read caller credentials from the
  dashboard environment and include the correct header on every API call.

## 4. Verification

- [ ] 4.1 Run the focused backend and frontend tests, the relevant broader
  verification commands, OpenSpec strict validation, and review the diff.
