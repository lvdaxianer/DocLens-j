## Why

DocLens-j is positioned as document parsing infrastructure used by upstream
business systems and workflows. Stage 2 of the product roadmap requires every
new batch to be attributable to a calling system without introducing personal
login, user tables, RBAC, or end-user authorization.

Today uploads can carry metadata, callback URLs, and idempotency keys, but the
server does not resolve a trusted caller identity from request credentials.
That means multiple workflows sharing one DocLens-j deployment cannot reliably
separate responsibility for uploads, callbacks, or operational diagnosis.

## What Changes

Implement the P0 Stage 2 caller-governance slice:

- Add a caller identity model with `client_id`, `source_app`, and `tenant_key`.
- Persist caller identity on created batches and expose it through query and
  Dashboard batch read models.
- Include caller identity in completed-document callback payloads.
- Add a Spring HTTP credential resolver for native `/api/v1/batches` uploads.
  Configured API key or Bearer token credentials resolve to trusted caller
  identity; invalid or missing credentials are rejected when credentials are
  configured.
- Keep the local trial path usable by leaving upload authentication open when
  no DocLens caller credentials are configured.
- Show caller identity in the Dashboard batch intake panel.

## Impact

This change affects batch creation, persistence, read models, callback payloads,
upload API contract tests, and Dashboard intake display. It adds a database
migration for caller columns on `ocr_batches`.

It deliberately does not add personal login, end-user sessions, organization
tables, RBAC, quotas, credential rotation management, or caller-level rate
limits. Those remain later roadmap items.
