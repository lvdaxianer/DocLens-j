# Proposal

## Why

DocLens packaging documentation already explains how to override container
runtime settings through environment variables and mounted Spring config, but
operators still need a concrete example they can run without assembling a long
`docker run` command by hand. The existing root `docker-compose.yml` is part of
the local development workflow and only starts PostgreSQL, so it should not be
repurposed for all-in-one runtime delivery.

## What Changes

- add a dedicated all-in-one Docker Compose example for the packaged runtime
- add an accompanying env example file with port, database, and image defaults
- add an external Spring `application.yml` example for larger `doclens.*`
  runtime overrides
- document the example workflow in the packaging guides

## Impact

- operators get a copyable Compose entry point for the all-in-one image
- complex `doclens.*` tuning can move into a mounted config file instead of a
  long environment variable list
- the existing development Compose workflow remains unchanged
