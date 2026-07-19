# Design

## Summary

Replace the previous generic all-in-one Compose example with an x86-specific
delivery layout under `docker/x86/`. The x86 directory will become the visible
operator surface for single-node delivery:

- `docker/x86/docker-compose.yml`
- `docker/x86/README.md`
- `docker/x86/config/runtime.env`
- `docker/x86/config/application.yml`

## Runtime Layout

The Compose file will use `doclens:amd64` directly and mount the entire
`docker/x86/config/` directory into `/opt/doclens/config`. The entrypoint will
load `/opt/doclens/config/runtime.env` before it prepares PostgreSQL and before
it launches the backend. Backend business settings will remain in the mounted
`application.yml`.

This keeps the operator-facing structure visible:

- startup shell values and database settings in `runtime.env`
- backend business settings in `application.yml`
- volumes, ports, and image tag in `docker-compose.yml`

## Packaging Visibility

The Dockerfile will gain short comments that explain:

- frontend static assets are built into
  `doclens-server/src/main/resources/static/dashboard`
- the backend Spring Boot jar plus scripts and config examples are packaged
  into `docker/build/doclens-server-dist.tar.gz`
- the archive is extracted into `/opt/doclens`
- the entrypoint starts PostgreSQL first and then the backend, which serves the
  bundled dashboard at `/dashboard/`

The x86 README and packaging guides will repeat the same chain in operator
language.

## Verification

Focused verification will assert that:

- the x86 delivery directory exists with compose, README, and mounted config
  files
- the entrypoint loads the mounted `runtime.env`
- the x86 compose file mounts `/opt/doclens/config`
- the x86 README and packaging docs explain frontend packaging, backend archive
  copying, startup order, and config file mapping
- the old generic all-in-one Compose example has been removed

Broader verification will render the x86 Compose file through `docker-compose
config`, validate the OpenSpec change, and run `git diff --check`.
