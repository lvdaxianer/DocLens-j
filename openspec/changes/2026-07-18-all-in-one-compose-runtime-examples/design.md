# Design

## Summary

Keep the existing root `docker-compose.yml` dedicated to local development
PostgreSQL. Add a separate all-in-one delivery example that starts the packaged
DocLens image with three companion artifacts:

- a dedicated Compose file
- an env example for shell-level runtime values
- a mounted Spring `application.yml` example for larger `doclens.*` overrides

## File Layout

- `docker-compose.all-in-one.yml`
- `docker/examples/all-in-one/.env.example`
- `docker/examples/all-in-one/application.yml`

## Behavior

The Compose example will:

- run `doclens:amd64` by default, with image repository and tag overridable
- expose configurable application and PostgreSQL ports
- mount persistent PostgreSQL data and DocLens storage volumes
- mount the external Spring config example directory read-only
- set `DOCLENS_CONFIG_DIR` so the image entrypoint loads the mounted config

The env example will focus on:

- image repository and tag
- application and PostgreSQL ports
- PostgreSQL database/user/password
- JDBC URL and storage path defaults

The mounted `application.yml` example will focus on:

- shared `doclens.*` OCR, worker, extraction, render, callback, and traffic
  settings that are awkward to encode as many environment variables

## Verification

Focused verification will assert that:

- the dedicated Compose file exists and points at the all-in-one image
- the Compose file mounts the external config directory and uses
  `DOCLENS_CONFIG_DIR`
- the env example documents the expected runtime knobs
- the mounted `application.yml` example includes representative `doclens.*`
  runtime settings
- the packaging guides explain how to run the example

Broader verification will render the Compose file with `docker compose config`
and re-run existing packaging-related checks if needed.
