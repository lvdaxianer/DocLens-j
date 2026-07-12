## Context

DocLens-j currently uses Spring MVC interceptors rather than Spring Security to
protect API routes. The request path is registered in
`CallerPartitionWebMvcConfigurer`: global protection, trusted gateway
authentication, caller partition parsing, and caller traffic limiting all run
before controllers for `/api/v1/**`.

Production enables `doclens.gateway-auth.enabled=true` from
`application-prod.yml`, validates `X-Doclens-Gateway-Secret`, resolves
`X-Doclens-Principal`, checks that the requested `X-Doclens-Key` is in the
principal allowlist, and enforces the configured `admin` role for admin route
patterns. Local defaults do not enable gateway authentication, but requests
still need `X-Doclens-Key` so data does not fall into an anonymous shared
partition.

## Goals / Non-Goals

**Goals:**
- Explain the permission model as implemented today.
- Give production configuration examples with environment variables and curl.
- Give development examples for the default lightweight mode and for simulating
  production gateway authentication locally.
- Explain `X-Doclens-Key` data partitioning and how it differs from
  authentication.
- Document common HTTP failure statuses for missing headers, forbidden
  partitions, rate limits, and global protection.
- Call out known caveats, including old API key compatibility code and current
  `user` route-policy behavior.

**Non-Goals:**
- Do not change permission behavior or defaults.
- Do not add Spring Security, JWT, login sessions, or new authorization rules.
- Do not document every API endpoint; link to existing API/config references
  where appropriate.

## Decisions

- Create `docs/permission-configuration.md` as a standalone operator document.
- Keep `docs/configuration.md` as the broader property reference and add a
  short related-doc link to the new permission document.
- Use concrete but fake values in examples, such as `gateway-secret-local`,
  `dashboard-admin`, `tenant-a`, and `tenant-b`.
- Avoid recommending that callers treat `X-Doclens-Key` as a secret, because the
  code treats it as an opaque partition key rather than an authentication token.

## Testing

- Run `openspec validate 2026-07-12-permission-configuration-doc --strict`.
- Run a focused documentation RED/GREEN check using `test -f` and `grep` for
  required production and development sections.
- Inspect the final diff for placeholders, stale paths, and contradiction with
  the current implementation.
