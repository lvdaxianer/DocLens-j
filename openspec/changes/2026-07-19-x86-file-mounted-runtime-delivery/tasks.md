## Task Boundary And Agent Dispatch Plan

### Task 1: Add x86 file-mounted runtime delivery layout
- Module-oriented agent name: main-agent/x86-runtime-delivery
- Owned responsibility: replace the previous generic Compose example with an
  x86 delivery directory, mounted config files, Dockerfile visibility notes,
  packaging docs, and focused verification
- Allowed files or modules:
  - `Dockerfile`
  - `docker/entrypoint.sh`
  - `docker/x86/**`
  - `docs/packaging*.md`
  - focused verification scripts under `scripts/`
  - prior example files under `docker-compose.all-in-one.yml`,
    `docker/examples/all-in-one/**`
  - `openspec/changes/2026-07-19-x86-file-mounted-runtime-delivery/**`
- Out-of-scope work:
  - changing the Docker image build logic itself
  - changing Helm deployment behavior
  - splitting the frontend into a separate runtime process
- Dependencies:
  - current Maven assembly server distribution
  - current all-in-one Docker entrypoint and runtime image
  - packaging guides
- Focused verification commands:
  - `bash scripts/verify-x86-runtime-delivery-layout.sh`
- Broader verification commands:
  - `docker-compose -f docker/x86/docker-compose.yml config`
  - `git diff --check`
  - `openspec validate 2026-07-19-x86-file-mounted-runtime-delivery --strict`
- Handoff evidence: x86 operators can inspect one directory to understand how
  frontend and backend artifacts enter the image, how the container starts, and
  which mounted files control runtime behavior
- Direct-execution fallback reason: the change is localized to delivery assets,
  docs, and focused runtime scripts, so direct execution is safer than
  splitting tightly coupled context

## Tasks

- [x] 1. Add an x86 file-mounted runtime delivery layout and document the
  visible packaging and startup chain.
