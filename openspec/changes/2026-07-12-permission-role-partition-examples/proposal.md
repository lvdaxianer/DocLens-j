## Why

The bilingual permission configuration documents list `roles` and
`allowed-partitions`, but readers may not understand the difference between
identity, admin role authorization, and data partition authorization. Operators
need concrete examples that explain why `tenant-a` and `tenant-b` appear under
`allowed-partitions` and how those values relate to `X-Doclens-Key`.

## What Changes

Add practical role and partition examples to both English and Chinese permission
configuration documents. The examples will explain that `principal` means who is
calling, `roles` control admin route access, and `allowed-partitions` controls
which `X-Doclens-Key` values the principal may use.

## Impact

This is documentation-only. It does not change runtime authorization behavior,
configuration defaults, public APIs, database schema, or tests.
