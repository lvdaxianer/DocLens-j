## Task Boundary And Agent Dispatch Plan

### Task 1: Add container runtime override hooks
- Module-oriented agent name: main-agent/container-runtime-config
- Owned responsibility: update Docker runtime scripts, Helm chart values and
  templates, packaging docs, and focused verification for runtime overrides
- Allowed files or modules:
  - `docker/entrypoint.sh`
  - `Dockerfile`
  - `deploy/helm/doclens-j/**`
  - `doclens-server/src/assembly/conf/doclens.env.example`
  - `docs/packaging*.md`
  - focused verification scripts under `scripts/`
  - `openspec/changes/2026-07-18-container-runtime-config-overrides/**`
- Out-of-scope work:
  - changing unrelated permission documentation edits
  - redesigning Spring Boot property binding
  - changing OCR business logic
- Dependencies:
  - current all-in-one Docker entrypoint
  - current Helm chart templates and values
  - packaging guides
- Focused verification commands:
  - `bash scripts/verify-runtime-config-overrides.sh`
- Broader verification commands:
  - `bash scripts/verify-local-runtime-dockerfile.sh`
  - `helm lint deploy/helm/doclens-j`
  - `helm template doclens ./deploy/helm/doclens-j --set app.serverPort=18080`
  - `helm template doclens ./deploy/helm/doclens-j --set app.extraVolumes[0].name=extra-config --set app.extraVolumes[0].configMap.name=doclens-extra-config --set app.extraVolumeMounts[0].name=extra-config --set app.extraVolumeMounts[0].mountPath=/opt/doclens/config`
- Handoff evidence: Docker runtime exposes explicit env/config override hooks,
  Helm renders the same hooks, and packaging docs describe both paths
- Direct-execution fallback reason: the change is localized across tightly
  coupled delivery files, so direct execution is safer than splitting context

## Tasks

- [x] 1. Add Docker and Helm runtime override hooks for ports and external
  config, then verify the packaged configuration surface.
