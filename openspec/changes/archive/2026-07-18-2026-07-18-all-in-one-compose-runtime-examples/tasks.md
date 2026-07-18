## Task Boundary And Agent Dispatch Plan

### Task 1: Add all-in-one Compose runtime examples
- Module-oriented agent name: main-agent/all-in-one-compose-example
- Owned responsibility: add dedicated all-in-one Compose example assets,
  packaging docs, and focused verification without changing the dev Compose
  workflow
- Allowed files or modules:
  - `docker-compose.all-in-one.yml`
  - `docker/examples/all-in-one/**`
  - `docs/packaging*.md`
  - focused verification scripts under `scripts/`
  - `openspec/changes/2026-07-18-all-in-one-compose-runtime-examples/**`
- Out-of-scope work:
  - changing the root `docker-compose.yml` development behavior
  - modifying Docker image build logic
  - changing Helm deployment behavior
- Dependencies:
  - current all-in-one Docker image runtime contract
  - packaging guide runtime override sections
  - existing dev-compose scripts that depend on root `docker-compose.yml`
- Focused verification commands:
  - `bash scripts/verify-all-in-one-compose-example.sh`
- Broader verification commands:
  - `docker compose --env-file docker/examples/all-in-one/.env.example -f docker-compose.all-in-one.yml config`
  - `git diff --check`
  - `openspec validate 2026-07-18-all-in-one-compose-runtime-examples --strict`
- Handoff evidence: operators can copy a dedicated Compose example, env
  example, and mounted Spring config example for all-in-one runtime delivery
- Direct-execution fallback reason: the change is localized to packaging docs
  and example delivery assets, so splitting agents would add overhead without
  reducing risk

## Tasks

- [x] 1. Add dedicated all-in-one Compose runtime examples and document how to
  use env overrides versus mounted Spring config overrides.
