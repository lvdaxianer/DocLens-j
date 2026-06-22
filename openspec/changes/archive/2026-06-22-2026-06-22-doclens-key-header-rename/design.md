## Context

The previous caller-key isolation work intentionally removed backend ownership
checks and made the incoming key an opaque partition key. The only incorrect
part is the public header name.

The corrected contract is exact and case-sensitive in documentation and source
constants: `X-Doclens-Key`.

## Goals / Non-Goals

**Goals:**
- Make Dashboard read `X-Doclens-Key` from `sessionStorage`.
- Make Dashboard send only `X-Doclens-Key` as the caller partition header.
- Make backend request resolution read only `X-Doclens-Key`.
- Update tests, docs, and OpenSpec assets so the canonical contract no longer
  names `X-Recall-Key`.
- Prove `X-Recall-Key` is ignored and does not create a compatibility alias.

**Non-Goals:**
- Do not change caller partition semantics.
- Do not add user, workspace, RBAC, or ownership validation concepts.
- Do not add header migration fallback logic.
- Do not change rate-limit bucket policy except for the header name used to
  resolve the partition.

## Decisions

- Use `X-Doclens-Key` as both the Dashboard session storage key and HTTP request
  header. This keeps the frontend model simple and mirrors the existing
  storage/header behavior.
- Do not accept `X-Recall-Key` on the backend. A caller using the old wrong
  header should behave exactly like a missing caller partition key.
- Do not emit `X-DocLens-Credential-Key`, `X-DocLens-Api-Key`, or
  `Authorization` from the Dashboard caller-key helper.
- Update the committed Dashboard static asset only if this repository treats the
  built dashboard bundle as a source-controlled runtime artifact for the server.

## Testing

- Add frontend tests that expect `X-Doclens-Key` from `sessionStorage` and
  verify `X-Recall-Key` in local/session storage is ignored.
- Add or update backend tests so request resolution succeeds with
  `X-Doclens-Key` and rejects/misses `X-Recall-Key`.
- Run the focused Dashboard API tests, focused backend caller contract tests,
  OpenSpec validation, and diff whitespace checks.
