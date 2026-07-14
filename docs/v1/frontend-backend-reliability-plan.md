# Frontend And Backend Reliability Implementation Plan

> This plan covers the v1 hardening work for backend caller partition safety and
> frontend interaction reliability. It is a planning artifact only; implementation
> should proceed task by task with focused tests and verification.

## Background

DocLens currently uses `X-Doclens-Key` as the caller partition key. The key is
intended to separate data spaces, rate-limit buckets, dashboard query scope, and
upload ownership. It is not intended to authenticate a user or prove that a
caller is allowed to access a partition.

The current backend and dashboard behavior creates two classes of risk:

- Backend reliability and authorization risk: any non-empty `X-Doclens-Key` can
  be accepted as a partition identity. Admin-like routes and callback retry
  routes do not yet have a separate authorization boundary.
- Frontend interaction risk: the dashboard depends on `sessionStorage` for the
  caller partition key but has no visible setup or recovery path. Some high-risk
  actions also rely mainly on button loading state and can be repeated during
  slow network conditions.

## Product Decisions

1. `X-Doclens-Key` is a data partition key only.
   It must not be documented or treated as an authentication token.
2. Authentication belongs to a trusted gateway or deployment boundary.
   Browser clients should not hold long-lived backend service secrets.
3. DocLens backend must validate both identity and partition authorization.
   A trusted principal can only access configured partitions.
4. Admin routes need stronger authorization than ordinary upload and query
   routes.
5. Frontend must show clear recovery states for missing partition, permission
   denial, rate limiting, and network failure.

## Target Architecture

```text
Browser / OpenWebUI
        |
        v
Trusted gateway / reverse proxy / internal SSO
        |
        v
DocLens backend
```

The gateway authenticates the real caller and injects trusted headers. DocLens
accepts those headers only when the request also proves it came through the
trusted gateway.

Example backend-facing request:

```http
X-Doclens-Gateway-Secret: <gateway-to-backend secret>
X-Doclens-Principal: alice
X-Doclens-Roles: user,admin
X-Doclens-Key: alice-workspace
```

Validation order:

1. Check the trusted gateway proof.
2. Read the trusted principal and roles.
3. Validate that the principal exists in DocLens authorization config.
4. Validate that `X-Doclens-Key` is allowed for the principal.
5. Match the route policy and required role.
6. Build the request caller partition identity for the rest of the application.

## Configuration Contract

Add production-oriented configuration under `doclens.gateway-auth`.

```yaml
doclens:
  gateway-auth:
    enabled: true
    trusted-gateway:
      header-name: X-Doclens-Gateway-Secret
      header-value: ${DOCLENS_GATEWAY_SECRET}
    principal-headers:
      principal: X-Doclens-Principal
      roles: X-Doclens-Roles
    principals:
      - principal: alice
        roles:
          - user
        allowed-partitions:
          - alice-workspace
          - demo-space
      - principal: open-webui
        roles:
          - user
        allowed-partitions:
          - open-webui
      - principal: admin
        roles:
          - user
          - admin
        allowed-partitions:
          - "*"
    route-policies:
      - pattern: /api/v1/ocr-governance-config/**
        required-role: admin
      - pattern: /api/v1/llm-markdown-config/**
        required-role: admin
      - pattern: /api/v1/ocr-models/**
        required-role: admin
      - pattern: /api/v1/ocr-nodes/**
        required-role: admin
      - pattern: /api/v1/dashboard/callback-jobs/*/retry
        required-role: admin
      - pattern: /api/v1/**
        required-role: user
```

Configuration rules:

- `enabled=false` is allowed only for local development and tests.
- Production startup must fail when `enabled=true` and
  `DOCLENS_GATEWAY_SECRET` is missing.
- Principal names are stable external identities provided by the gateway.
- Role names are simple strings for v1. Expected values are `user` and `admin`.
- `allowed-partitions: ["*"]` is admin-only and must be explicit.
- Route policies are evaluated from most-specific to least-specific.

## Backend Plan

### Task B1: Rename caller credential concepts

Goal: make the code vocabulary match product semantics.

Scope:

- Rename or wrap `CallerCredentialResolver` as `CallerPartitionResolver`.
- Keep a compatibility adapter only if existing call sites require a staged
  migration.
- Rename comments, messages, and docs that imply `X-Doclens-Key` is a
  credential.

Acceptance criteria:

- Public docs say `X-Doclens-Key` is a partition key, not a secret.
- Backend code no longer describes the partition key as a credential boundary.
- Existing upload, query, and dashboard flows still receive a `CallerIdentity`
  with the selected partition.

Focused verification:

- Caller partition resolver tests.
- Existing caller partition API contract tests.

### Task B2: Add gateway authentication properties

Goal: bind trusted gateway, principal, partition, and route policy config.

Scope:

- Add immutable configuration records under `DocLensSpringProperties`.
- Validate required fields when `gateway-auth.enabled=true`.
- Add sensible test defaults for integration tests.

Acceptance criteria:

- Missing production gateway secret fails fast.
- Empty principal list fails fast when gateway auth is enabled.
- Local development can opt out explicitly.

Focused verification:

- Spring property binding tests.
- Production fail-fast startup test.

### Task B3: Add gateway authentication interceptor

Goal: reject requests that do not come through the trusted gateway.

Scope:

- Add a `GatewayAuthInterceptor` before caller partition and traffic
  interceptors.
- Compare the configured gateway secret using constant-time comparison.
- Reject missing or invalid principal headers with stable 401 responses.
- Store the authenticated principal and roles in request attributes.

Acceptance criteria:

- Missing gateway proof returns 401.
- Invalid gateway proof returns 401.
- Missing principal returns 401.
- Valid gateway proof and principal continues to partition authorization.

Focused verification:

- MockMvc tests for missing, invalid, and valid trusted gateway headers.
- Test that downstream controllers are not called when authentication fails.

### Task B4: Add partition authorization

Goal: allow `X-Doclens-Key` only when the authenticated principal can access it.

Scope:

- Validate `X-Doclens-Key` presence, length, and character set.
- Resolve the configured principal entry.
- Check `allowed-partitions`.
- Return 403 when the principal is authenticated but not allowed for the
  requested partition.

Acceptance criteria:

- `alice + alice-workspace` succeeds.
- `alice + bob-workspace` returns 403.
- Missing `X-Doclens-Key` returns 400 or 401 with a stable detail message.
- Overlong or malformed partition keys are rejected before reaching services.

Focused verification:

- Partition authorization unit tests.
- Contract test for upload, dashboard query, document result, delete, and retry
  under allowed and denied partitions.

### Task B5: Add route role authorization

Goal: protect admin-like routes separately from ordinary data operations.

Scope:

- Add route policy matcher for configured path patterns.
- Require `user` for ordinary `/api/v1/**` routes.
- Require `admin` for OCR governance, OCR resources, LLM config, callback
  retry, and future global operations.

Acceptance criteria:

- User role can upload, query, retry owned documents, and delete owned
  completed documents.
- User role cannot update OCR governance, OCR nodes, LLM config, or callback
  jobs.
- Admin role can access configured admin routes.

Focused verification:

- Route policy matcher tests.
- Contract tests for representative admin and user routes.

### Task B6: Fix callback retry ownership

Goal: prevent cross-partition callback retries.

Scope:

- Resolve callback job to its document or batch.
- Verify that the owning batch partition matches the current caller partition.
- Keep admin role requirement for manual callback retry.

Acceptance criteria:

- Admin in the correct partition can retry a failed callback job.
- Admin in another partition receives 403 or 404.
- Unknown callback job receives 404.
- Retry failure records remain observable.

Focused verification:

- Callback retry contract tests for same partition, different partition, and
  missing job.

### Task B7: Split runtime configuration by environment

Goal: prevent production from silently using local defaults.

Scope:

- Keep `application.yml` for safe shared defaults.
- Move the previously local-only database defaults, local storage root, local
  worker ID, and internal OCR endpoint defaults into `application-dev.yml`;
  the database baseline is PostgreSQL.
- Add `application-prod.yml` with environment-variable-only database, OCR
  endpoint, gateway auth, storage, and token settings.

Acceptance criteria:

- Production profile without required env vars fails at startup.
- Development profile remains easy to run locally.
- Test profile uses isolated PostgreSQL infrastructure.

Focused verification:

- Profile-specific Spring tests.
- Manual startup command for dev profile.
- Production missing-config startup test.

### Task B8: Stabilize error responses

Goal: let the frontend render useful recovery paths.

Scope:

- Return a stable error shape for authentication, authorization, partition,
  validation, rate limit, and internal errors.
- Include safe fields such as `code`, `detail`, `action`, and `trace_id`.
- Avoid exposing secrets, stack traces, database columns, or internal schema.

Example:

```json
{
  "code": "PARTITION_MISSING",
  "detail": "missing X-Doclens-Key partition header",
  "action": "select_data_partition"
}
```

Acceptance criteria:

- Frontend can distinguish missing partition, permission denied, rate limited,
  validation failure, and server failure.
- Existing upload error detail remains compatible.

Focused verification:

- Global exception handler tests.
- Contract tests for 400, 401, 403, 429, and 500-safe error shapes.

## Frontend Plan

### Task F1: Add current identity and partition state

Goal: make the active data space visible and recoverable.

Scope:

- Add a small topbar identity area that displays current principal, roles, and
  partition when available.
- If gateway mode is enabled, display identity as read-only.
- If direct local mode remains supported, provide a local-only partition selector
  that writes `X-Doclens-Key` to `sessionStorage`.

Acceptance criteria:

- User can see which data partition the dashboard is showing.
- Missing partition produces a visible page-level recovery state.
- Gateway mode does not encourage users to edit trusted identity headers.

Focused verification:

- Component tests for identity present, missing partition, and local partition
  selector states.

### Task F2: Add unified API error parsing

Goal: show actionable backend errors across all dashboard API modules.

Scope:

- Create a shared API error parser for JSON `detail`, `code`, and `action`.
- Replace generic `请求失败：401 Unauthorized` style messages in dashboard,
  OCR resources, governance config, and LLM config API modules.
- Map common actions to user-facing copy:
  - `select_data_partition`
  - `request_permission`
  - `retry_later`
  - `fix_form`

Acceptance criteria:

- Missing partition shows a partition-specific recovery message.
- 403 shows permission-denied copy.
- 429 shows retry timing when available.
- Upload keeps its existing useful detail behavior.

Focused verification:

- API unit tests for 400, 401, 403, 429, network failure, and malformed JSON
  responses.

### Task F3: Lock upload inputs during submit

Goal: prevent user input loss during slow uploads.

Scope:

- Pass upload `loading` into file picker, file list, advanced options, chunk
  selector, and OCR routing selector.
- Disable file choosing, drag-and-drop, remove, clear, and route changes while
  upload is in flight.
- Keep the selected files visible with an uploading state until success or
  failure.

Acceptance criteria:

- User cannot select new files during an active upload.
- Failed upload preserves the selected files and form values.
- Successful upload resets the form only after navigation or success state is
  complete.

Focused verification:

- Upload component test for repeated click and slow upload.
- Upload component test that failed upload preserves form state.

### Task F4: Add OCR node operation state

Goal: prevent repeated node mutations and health operations.

Scope:

- Track node operation state by `nodeId + action`.
- Add loading/disabled state for test, reconnect, toggle enabled, edit during
  save, and delete.
- Prevent repeated calls from composable functions, not only from buttons.

Acceptance criteria:

- Slow `test` request cannot be fired twice for the same node.
- Slow `reconnect` request cannot be fired twice for the same node.
- Toggle enabled is locked until the backend responds.
- Delete shows loading after confirmation and cannot be repeated.

Focused verification:

- OcrNodeTable interaction tests.
- useOcrNodeActions tests for function-level reentry guard.

### Task F5: Add function-level reentry guards for config actions

Goal: make save and test actions safe even when UI state lags.

Scope:

- Guard LLM config save and test with `isSaving` and `isTesting`.
- Guard OCR governance save with `isSaving`.
- Guard LLM row actions with `actingId` before starting the remote call.

Acceptance criteria:

- Double-click save sends one request.
- Enter key plus button click sends one request.
- Row actions cannot overlap on the same row.

Focused verification:

- LLM config composable tests.
- OCR governance composable tests.

### Task F6: Improve destructive confirmations

Goal: make consequences clear before irreversible actions.

Scope:

- Update OCR node delete confirmation to mention routing and future OCR
  availability.
- Update LLM config delete confirmation to mention default config and future LLM
  Markdown processing impact.
- Keep document and batch delete copy consistent with the stronger wording.

Acceptance criteria:

- Destructive confirmations name the affected object and consequence.
- Confirmation copy is consistent across document, batch, OCR node, and LLM
  config deletes.

Focused verification:

- Snapshot or DOM tests for confirmation text.

### Task F7: Add stale-data feedback for auto-refresh

Goal: help users understand when dashboard data is not current.

Scope:

- Preserve last successful refresh time.
- Show a non-blocking warning when auto-refresh fails.
- Do not clear existing data on refresh failure.
- Provide a retry action near the failed refresh message.

Acceptance criteria:

- Users can distinguish stale data from empty data.
- Existing data stays visible when refresh fails.
- Manual refresh clears the warning on success.

Focused verification:

- Dashboard store tests for failed refresh.
- View tests for stale warning and retry action.

## Cross-Cutting Implementation Order

1. Backend configuration and contract.
2. Backend gateway auth and partition authorization.
3. Backend route role authorization.
4. Backend callback retry ownership.
5. Backend error response stabilization.
6. Frontend shared API error parser.
7. Frontend identity and partition state.
8. Frontend upload locking.
9. Frontend OCR node operation locking.
10. Frontend config action guards.
11. Frontend destructive confirmation copy.
12. Frontend stale refresh feedback.
13. End-to-end verification.

## Verification Matrix

| Area | Scenario | Expected result |
| --- | --- | --- |
| Gateway auth | Missing gateway secret header | 401 with stable auth error |
| Gateway auth | Invalid gateway secret header | 401 with stable auth error |
| Gateway auth | Valid gateway and principal | Request reaches partition authorization |
| Partition auth | Principal accesses allowed partition | 2xx for ordinary route |
| Partition auth | Principal accesses denied partition | 403 with stable partition error |
| Route role | User calls admin route | 403 with permission error |
| Route role | Admin calls admin route | Route succeeds when payload is valid |
| Callback retry | Retry job in same partition | Retry request accepted |
| Callback retry | Retry job in another partition | 403 or 404 |
| Config | Prod missing required env | Startup fails fast |
| Frontend error | Missing partition response | Recovery message shown |
| Frontend error | Permission denied response | Permission-denied message shown |
| Upload | Slow upload then repeated click | One submit request |
| Upload | Slow upload then file picker interaction | Picker is disabled |
| OCR node | Slow reconnect double click | One request |
| OCR node | Slow enable toggle repeat | One request and visible loading |
| LLM config | Save double click | One request |
| Refresh | Auto refresh failure | Existing data remains with stale warning |

## Rollout Plan

### Development rollout

1. Implement backend auth in disabled-by-default local mode.
2. Add tests for enabled auth mode.
3. Update dev scripts to either set local auth headers or explicitly disable
   gateway auth.
4. Implement frontend local partition recovery only for local mode.

### Staging rollout

1. Deploy behind a gateway that strips incoming trusted headers.
2. Gateway injects trusted principal and role headers.
3. Enable `doclens.gateway-auth.enabled=true`.
4. Verify ordinary user and admin route behavior.
5. Verify OpenWebUI partition behavior.

### Production rollout

1. Require production profile.
2. Require gateway auth enabled.
3. Require no committed production secrets.
4. Require production config fail-fast checks.
5. Monitor 401, 403, 429, and callback retry errors after rollout.

## Risks And Mitigations

| Risk | Mitigation |
| --- | --- |
| Existing local dashboard breaks because no gateway exists | Keep explicit local profile with gateway auth disabled or a local partition selector |
| Browser users spoof trusted headers | Gateway must strip incoming trusted headers and inject its own |
| Admin route policy misses a route | Add a route policy test that enumerates known admin controllers |
| Partition authorization blocks valid OpenWebUI requests | Add OpenWebUI-specific contract tests and document its configured partition |
| Frontend still shows generic errors | Shared API parser must be used by all API modules |
| Slow network causes duplicate mutation | Add composable-level guards in addition to button loading |

## Out Of Scope

- A full username/password user system.
- OAuth or OIDC integration implementation.
- Fine-grained per-document ACL beyond partition ownership.
- Multi-tenant admin UI for editing principal-to-partition mappings.
- Replacing backend idempotency with frontend debounce. Backend idempotency
  remains required for retried data-changing requests.

## Completion Checklist

- [ ] Backend config contract is implemented and documented.
- [ ] Gateway auth rejects missing and invalid trusted gateway requests.
- [ ] Principal-to-partition authorization is enforced.
- [ ] Admin route authorization is enforced.
- [ ] Callback retry cannot cross partitions.
- [ ] Production profile fails fast on missing required config.
- [ ] Frontend shows current identity and partition state.
- [ ] Frontend parses stable backend error details.
- [ ] Upload flow cannot lose user-selected files during slow network requests.
- [ ] OCR node actions cannot be repeated while in flight.
- [ ] Config save/test actions cannot be repeated while in flight.
- [ ] Destructive confirmations explain consequences consistently.
- [ ] Auto-refresh failure preserves data and shows stale state.
- [ ] Focused backend, frontend, and end-to-end verification commands pass.
