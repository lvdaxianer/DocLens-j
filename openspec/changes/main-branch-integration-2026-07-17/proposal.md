## Why

`main` currently lags behind the PostgreSQL unification and containerized
delivery work that already exists on the active feature branch chain. We need a
controlled integration path that brings those completed changes onto `main`
without disturbing unrelated uncommitted work in the current tree.

## What Changes

- Integrate the pending PostgreSQL runtime changes from the current feature
  branch chain into `main`.
- Integrate the container packaging, local runtime, and Kubernetes delivery
  assets that were completed on the same branch chain into `main`.
- Perform the integration in an isolated worktree so the existing local
  documentation edits remain untouched.
- Verify that `main` contains the effective code from every local non-`main`
  branch that is not already an ancestor of `main`.

## Capabilities

### New Capabilities
- `runtime-database`: Standardize DocLens runtime and local deployment on
  PostgreSQL-backed persistence.
- `deployment-packaging`: Provide container packaging and Kubernetes delivery
  assets for the PostgreSQL-based runtime.

### Modified Capabilities
- `project-documentation`: Refresh operational documentation references on
  `main` so the integrated PostgreSQL and packaging changes remain discoverable.

## Impact

- Affected git branches: `main`, `feature/main-统一PostgreSQL`,
  `feature/feature-main-统一PostgreSQL-容器化部署`, and
  `feat/feature/feature-main-统一PostgreSQL-容器化部署_Docker本地运行时`
- Affected assets may include Docker build/runtime files, Helm charts, compose
  assets, SQL initialization scripts, and related documentation
- No API contract changes are introduced beyond what already exists on the
  pending feature branches; this change integrates them into `main`
