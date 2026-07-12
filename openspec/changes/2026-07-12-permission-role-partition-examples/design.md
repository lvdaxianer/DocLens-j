## Context

The current trusted gateway configuration has three concepts that are easy to
mix up:

- `principal`: the gateway-injected identity.
- `roles`: the operation level, currently enforced for admin routes.
- `allowed-partitions`: the data partition allowlist for `X-Doclens-Key`.

The code validates `allowed-partitions` before caller partition parsing and
rejects a principal that requests a partition outside its allowlist. Later data
queries use `X-Doclens-Key` as the caller partition, so examples should connect
that configuration value to actual request headers.

## Goals / Non-Goals

**Goals:**
- Add a concise concept table to both permission documents.
- Add concrete accepted/rejected request examples for `tenant-a`, `tenant-b`,
  and `tenant-c`.
- Explain that `roles` do not decide which tenant data can be read.
- Explain that `allowed-partitions` does not grant admin operations by itself.

**Non-Goals:**
- Do not change any Java implementation.
- Do not add new route policies or authentication behavior.
- Do not rename existing documentation files.

## Testing

- Run a focused RED/GREEN documentation check for the new role/partition
  examples.
- Run `openspec validate 2026-07-12-permission-role-partition-examples --strict`.
- Run `bash scripts/check-docs.sh`.
- Review the diff for consistency across English and Chinese documents.
