## 1. Caller Attribution Storage

- [x] 1.1 Add the caller identity domain/API/application model, persist it on
  batches, expose it in upload responses and Dashboard batch read models, and
  cover the behavior with focused core tests plus a migration test.

## 2. Callback Attribution

- [x] 2.1 Add caller identity to completed-document callback payloads and cover
  the behavior with focused callback tests.

## 3. Native Upload Credentials

- [x] 3.1 Add Spring caller credential configuration and HTTP request
  resolution for `X-DocLens-Api-Key` and `Authorization: Bearer` uploads,
  rejecting unauthorized requests only when credentials are configured.

## 4. Dashboard Caller Visibility

- [ ] 4.1 Display caller identity in the Dashboard batch intake panel and cover
  the component behavior with focused frontend tests.

## 5. Final Verification

- [ ] 5.1 Run backend, frontend, documentation, OpenSpec validation, audit the
  acceptance criteria, and archive the completed change.
