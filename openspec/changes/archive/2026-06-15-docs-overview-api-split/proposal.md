## Why

The current README files try to serve as project overview, quick start,
integration guide, API reference, SDK guide, configuration catalog, and local
development guide at the same time. This makes the entry point long and makes
API details easy to miss as controllers and dashboard workflows grow.

The documentation needs a clearer reader path: README files should explain
what DocLens Java is and where to go next, while detailed reference material
should live in focused documents under `docs/`.

## What Changes

- Rewrite `README.md` and the English README as concise project overview pages.
- Add an HTTP API reference document that lists the major REST endpoint groups.
- Add focused SDK, configuration, and development documents for detailed usage.
- Link existing packaging and OpenWebUI integration documents from the README
  instead of duplicating their full content.
- Keep this as a documentation-only change with no product behavior changes.

## Capabilities

### New Capabilities

- `project-documentation`: Project documentation is split into overview,
  API reference, SDK usage, configuration, local development, packaging, and
  integration documents with clear cross-links.

### Modified Capabilities

- None.

## Impact

- Affects root README files and documentation files under `docs/`.
- Does not change Java, Vue, build, database, runtime, or HTTP behavior.
- Verification relies on markdown content review and link/path checks.
