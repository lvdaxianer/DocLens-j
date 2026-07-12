## Context

The current permission reference already explains trusted gateway
authentication, caller partition isolation, admin routes, traffic limits,
production configuration, development configuration, curl examples, and failure
responses. The user requested Chinese and English introductions in separate
files.

## Goals / Non-Goals

**Goals:**
- Preserve the existing English permission reference as
  `docs/permission-configuration.md`.
- Add a Chinese counterpart at `docs/permission-configuration_zh.md`.
- Link both files from `docs/configuration.md`.
- Validate both files through `scripts/check-docs.sh`.
- Update the project documentation spec to require bilingual permission
  references.

**Non-Goals:**
- Do not change permission behavior, request headers, route policies, or
  traffic limits.
- Do not merge Chinese and English text into one file.
- Do not rewrite unrelated documentation.

## Decisions

- Use `_zh` for the Chinese file because the current canonical file name is
  already English and the project has precedent for language-specific
  documentation files.
- Keep examples semantically equivalent across both files, including production
  env vars, local default mode, local gateway-auth simulation, and failure
  responses.
- Keep `docs/configuration.md` as the discovery point for both references.

## Testing

- Run a focused RED/GREEN documentation check for the Chinese file and its
  required sections.
- Run `openspec validate 2026-07-12-bilingual-permission-configuration-doc --strict`.
- Run `bash scripts/check-docs.sh`.
- Inspect the diff for scope, placeholders, stale links, and mismatched
  examples.
