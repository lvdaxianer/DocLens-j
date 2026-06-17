## Context

DocLens-j already has three relevant paths:

- Native upload API: `POST /api/v1/batches` maps multipart input through
  `CreateBatchRequestMapper` into `CreateBatchRequest` and then into
  `CreateBatchCommand`.
- Core persistence: `CreateBatchUseCase` creates a `Batch`, saves it through
  `BatchRepository`, creates document jobs, and emits initial events.
- Operations/read side: `DashboardQueryService` assembles batch rows and the
  Dashboard renders batch intake information.

The existing OpenWebUI integration has an internal token guard, but that is an
integration-specific trust boundary. Stage 2 needs a native caller credential
path for systems that call DocLens-j directly.

## Product Boundary

DocLens-j identifies calling systems, not people. Names and payloads must use
caller/client language. Credentials bind to `client_id`, `source_app`, and
optional `tenant_key`. Request metadata can help diagnosis, but it is not
trusted for authorization.

The MVP keeps first-run adoption intact: if no DocLens caller credentials are
configured, uploads remain accepted and are marked with an anonymous/system
caller identity. Once credentials are configured, `/api/v1/batches` requires a
matching API key or Bearer token.

## Data Model

Add caller identity as a small domain value object:

- `clientId`: required string after defaulting.
- `sourceApp`: optional display/source label after defaulting.
- `tenantKey`: optional tenant or workflow partition key.

Persist these fields on `ocr_batches` using a migration:

- `client_id VARCHAR(120) NOT NULL DEFAULT 'anonymous'`
- `source_app VARCHAR(160) NOT NULL DEFAULT 'unknown'`
- `tenant_key VARCHAR(160)`

Document jobs already carry batch ID and metadata. To keep the first P0 slice
small and avoid duplicating columns before a clear document-level query need,
documents and callback jobs inherit caller attribution through their batch.
Callback payloads include the caller object so downstream systems can consume
the attribution directly.

## Credential Resolution

Add Spring configuration under `doclens.clients`:

- `credentials`: list of configured callers.
- each item has `client-id`, `source-app`, optional `tenant-key`,
  `api-key`, and/or `bearer-token`.

Resolution order:

1. If no configured credentials exist, return the anonymous caller identity.
2. If `X-DocLens-Api-Key` matches a configured API key, return that caller.
3. If `Authorization: Bearer <token>` matches a configured bearer token, return
   that caller.
4. Otherwise throw a 401 upload error without logging secret values.

This keeps credentials out of request bodies and metadata. It also avoids
coupling DocLens-j to a personal login system.

## Component Map

- `CallerIdentity`: core domain value object for trusted caller attribution.
- `CreateBatchRequest` and `CreateBatchCommand`: carry caller identity through
  API and application layers.
- `Batch`: stores caller identity with batch intake fields.
- `MybatisPlusBatchRepository` and `BatchEntity`: persist and hydrate caller
  columns.
- `DocumentCompletedCallbackBody`: includes `caller` in callback payloads.
- `DocLensSpringProperties`: binds configured caller credentials.
- `CallerCredentialResolver` and `CallerCredentialException`: server-side HTTP
  credential resolution and rejection.
- `CreateBatchRequestMapper`: resolves caller identity from request headers.
- Dashboard batch row and `BatchIntakeInfoPanel`: expose and render caller
  identity.

## Error Handling

Invalid or missing credentials return HTTP 401 with a structured error detail.
Logs must not include API keys or bearer tokens. Configuration validation should
reject blank configured credentials during resolution rather than silently
creating unusable caller entries.

## Testing

Follow TDD per task:

- Core test proves `CreateBatchUseCase` persists caller identity and includes
  it in the upload response.
- Query test proves Dashboard batch rows expose caller identity.
- Callback test proves completed-document callback payload includes caller
  identity.
- Server contract test proves configured API key/Bearer token credentials allow
  upload and missing credentials are rejected.
- Dashboard component test proves caller identity is visible with stable empty
  placeholders.

Broader verification should include relevant Maven module tests, Dashboard
tests/build if frontend changes are touched, docs checks, and
`openspec validate --all --strict`.

## Risks / Trade-offs

- Making authentication always-on would break the Stage 1 local trial path, so
  enforcement is tied to configured credentials.
- Storing plain configured credentials is acceptable for the first local
  infrastructure slice because values come from Spring configuration, but docs
  and code should avoid logging them. Hashing, rotation, and disable/audit
  workflows remain P2.
- Document and callback job tables do not receive duplicate caller columns in
  this slice. Operational surfaces can resolve caller through batch ID and
  callback payloads carry the caller object for downstream consumers.
