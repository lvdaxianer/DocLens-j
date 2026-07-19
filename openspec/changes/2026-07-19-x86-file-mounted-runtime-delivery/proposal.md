# Proposal

## Why

The previous all-in-one Compose example made runtime overrides possible, but it
did not make the packaging structure obvious enough. Operators still could not
easily see how the frontend assets enter the image, how the backend package is
copied into the image, what starts first inside the container, or which files
should be mounted for backend runtime configuration. The next delivery example
should optimize for visible structure, not only configurability.

## What Changes

- add a dedicated x86 runtime delivery layout under `docker/x86/`
- load startup and database settings from a mounted runtime env file instead of
  promoting many container env flags in the Compose example
- mount backend runtime settings from a dedicated `application.yml` file
- document the visible packaging chain from dashboard build to server assembly
  archive to Docker image to startup order
- remove the previous generic all-in-one Compose example that did not match the
  desired operator experience

## Impact

- x86 operators get one directory with the compose file, mounted config files,
  and a README that explains the delivery structure
- backend business settings can live in a mounted file instead of a long list
  of environment variable flags
- the Dockerfile and packaging docs become explicit about frontend inclusion,
  backend archive copying, and process startup order
