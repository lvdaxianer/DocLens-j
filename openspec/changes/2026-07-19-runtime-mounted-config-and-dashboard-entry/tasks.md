## Task Boundary And Agent Dispatch Plan

### Task 1: Repair mounted runtime config handoff and dashboard entry routing
- Module-oriented agent name: main-agent/runtime-delivery-fix
- Owned responsibility: make mounted runtime env values reach the backend
  process, make `/dashboard/` open the bundled dashboard entry, and extend
  focused delivery verification
- Allowed files or modules:
  - `doclens-server/src/assembly/bin/doclens-server.sh`
  - `doclens-server/src/main/java/**`
  - `doclens-server/src/test/java/**`
  - `scripts/verify-x86-runtime-delivery-layout.sh`
  - `openspec/changes/2026-07-19-runtime-mounted-config-and-dashboard-entry/**`
- Out-of-scope work:
  - changing PostgreSQL initialization behavior
  - changing frontend bundle contents or frontend router mode
  - changing Helm topology or Docker base image composition
- Dependencies:
  - current x86 file-mounted delivery layout
  - packaged dashboard static assets under `static/dashboard`
  - Spring MVC test support
- Focused verification commands:
  - `./mvnw -pl doclens-server -Dtest=DashboardEntryContractTest test`
  - `bash scripts/verify-x86-runtime-delivery-layout.sh`
- Broader verification commands:
  - `./mvnw -pl doclens-server test`
  - `openspec validate 2026-07-19-runtime-mounted-config-and-dashboard-entry --strict`
  - `git diff --check`
- Handoff evidence: mounted runtime settings are visible to the backend launch
  process, and the documented `/dashboard/` URL serves the bundled dashboard
  index page
- Direct-execution fallback reason: the change is narrow and spans one shell
  launcher, one routing entry, and one focused verifier, so splitting it across
  agents would add overhead without reducing risk

## Tasks

- [x] 1. Repair mounted runtime config handoff and make `/dashboard/` resolve to the bundled dashboard entry.
