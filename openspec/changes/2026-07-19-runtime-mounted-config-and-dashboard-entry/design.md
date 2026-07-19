## Design

### Mounted runtime env propagation

The container entrypoint already loads `/opt/doclens/config/runtime.env`, but a
smoke test showed that the backend process still starts with the packaged
default Spring profile and config path. To make the mounted file authoritative
for the backend process as well, the packaged `doclens-server.sh` launcher will
load the same runtime env file when `DOCLENS_CONFIG_DIR` is available.

This is intentionally defensive: even if the parent shell environment is not
preserved as expected by the runtime chain, the mounted file still reaches the
final Java process.

### Dashboard entry routing

The dashboard assets are already bundled under `static/dashboard/`, and
`/dashboard/index.html` is reachable. A lightweight MVC controller will forward
`/dashboard` and `/dashboard/` to the bundled index page so the operator-facing
URL matches the packaging documentation.

### Verification

Focused verification will cover:

- the startup script contains the mounted runtime env hook
- `/dashboard/` forwards to the bundled dashboard entry page in a Spring MVC
  contract test
- the existing x86 delivery verification script checks that the packaged server
  launcher keeps the mounted runtime env hook
