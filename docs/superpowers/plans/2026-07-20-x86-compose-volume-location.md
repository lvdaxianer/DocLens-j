# x86 Compose Data Volume Location Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the x86 Compose file create named local bind volumes whose host root is configurable with `DOCLENS_DATA_ROOT`.

**Architecture:** Declare both persistent paths as service-level bind mounts with `create_host_path`. Default to Docker-host storage under `/var/lib/doclens-x86`, allow an absolute `DOCLENS_DATA_ROOT` override, and document migration from legacy named volumes.

**Tech Stack:** Docker Compose, Docker local volume driver, Bash verification script, Markdown

---

### Task 1: Configurable x86 data volume locations

**Files:**
- Modify: `scripts/verify-x86-runtime-delivery-layout.sh`
- Modify: `docker/x86/docker-compose.yml`
- Modify: `docker/x86/README.md`

- [x] **Step 1: Write the failing delivery-layout assertions**

Add literal assertions for `DOCLENS_DATA_ROOT`, `driver: local`, `o: bind`, and both
`${DOCLENS_DATA_ROOT}` subdirectories to `scripts/verify-x86-runtime-delivery-layout.sh`.

- [x] **Step 2: Run the focused check and verify RED**

Run: `bash scripts/verify-x86-runtime-delivery-layout.sh`

Expected: FAIL because `docker/x86/docker-compose.yml` does not yet declare configurable
local bind volume options.

- [x] **Step 3: Implement the Compose volume definitions and default directories**

Configure each service bind mount like this, with the matching `postgresql` or `storage` suffix:

```yaml
type: bind
source: "${DOCLENS_DATA_ROOT:-/var/lib/doclens-x86}/postgresql"
target: /var/lib/postgresql/data
bind:
  create_host_path: true
```

- [x] **Step 4: Document configuration and migration behavior**

Update `docker/x86/README.md` with the default location, custom `DOCLENS_DATA_ROOT`
example, required `mkdir -p`, Colima path-sharing note, and warning that existing named
volumes are not automatically migrated.

- [x] **Step 5: Run focused and broader verification**

Run: `bash scripts/verify-x86-runtime-delivery-layout.sh`

Expected: `x86 runtime delivery layout verified.`

Run: `docker-compose -f docker/x86/docker-compose.yml config`

Expected: both bind sources expand below `/var/lib/doclens-x86`.

Run the same config command from both the repository root and `docker/x86`.

Expected: both execution directories resolve to the same Docker-host paths.

Run with a temporary absolute `DOCLENS_DATA_ROOT` under a Docker-accessible shared path
and a temporary Compose project name, then inspect the created volumes.

Expected: both volume driver devices point to the selected temporary root.

- [x] **Step 6: Review and commit**

Review the complete diff against the approved design and canonical code-review rules,
then commit the configuration, verification, documentation, design, and plan atomically.

### Task 2: Working-directory-independent default path

**Files:**
- Modify: `scripts/verify-x86-runtime-delivery-layout.sh`
- Modify: `docker/x86/docker-compose.yml`
- Modify: `docker/x86/README.md`
- Modify: `docs/superpowers/specs/2026-07-20-x86-compose-volume-location-design.md`

- [x] **Step 1: Reproduce the duplicated path and verify RED**

Run Compose from `docker/x86` and confirm `${PWD}/docker/x86/data` expands to a
duplicated `docker/x86/docker/x86/data` path. Change the layout assertion to require
`${DOCLENS_DATA_ROOT:-./data}` and confirm it fails against the existing Compose file.

- [x] **Step 2: Use a Compose-relative default path**

Replace the `PWD`-based defaults with `${DOCLENS_DATA_ROOT:-./data}` for both volumes.
Document that Compose resolves `./data` relative to `docker/x86/docker-compose.yml`.

- [x] **Step 3: Verify all supported invocation paths**

Run the layout check and resolve Compose from both the repository root and `docker/x86`.
Create a temporary Compose project and verify both real volume devices point below
`docker/x86/data`, then remove all temporary Docker resources.

- [x] **Step 4: Review and commit the fix**

Review the complete fix diff against the approved design and canonical code-review rules,
run fresh verification, and create one atomic bug-fix commit.

### Task 3: PostgreSQL-compatible host bind mounts

**Files:**
- Modify: `scripts/verify-x86-runtime-delivery-layout.sh`
- Modify: `docker/x86/README.md`
- Modify: `docs/superpowers/specs/2026-07-20-x86-compose-volume-location-design.md`

- [x] **Step 1: Reproduce the initdb failure and verify RED**

Start the real Compose service and confirm `initdb` first rejects the tracked `.gitignore`,
then confirm a subdirectory still fails because the macOS shared filesystem rejects `chmod`.
Require service-level bind mounts under the Docker host and confirm the layout check fails.

- [x] **Step 2: Configure Docker-host bind directories**

Replace the local named volumes with service-level bind mounts using `create_host_path`,
default to `/var/lib/doclens-x86`, and document Linux and Colima path behavior.

- [x] **Step 3: Verify real startup and commit**

Recreate the empty local PostgreSQL directory, start the real Compose service, verify
PostgreSQL initialization and backend health, then review and commit the fix.
