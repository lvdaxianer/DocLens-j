## Why

The permission configuration reference currently exists only in English. The
project needs separate Chinese and English files so operators and developers can
read production and development permission setup in their preferred language
without mixing both languages in a single document.

## What Changes

Keep `docs/permission-configuration.md` as the English permission reference and
add `docs/permission-configuration_zh.md` as the Chinese permission reference.
Update the configuration reference and documentation check script so both files
are discoverable and validated.

## Impact

This is documentation-only. It does not change runtime permission behavior,
configuration defaults, public APIs, database schema, or test behavior.
