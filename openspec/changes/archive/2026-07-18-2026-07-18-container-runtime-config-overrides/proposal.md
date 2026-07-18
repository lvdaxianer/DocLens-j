# Proposal

## Why

DocLens container packaging already relies on Spring Boot configuration, but
the delivery surface does not make common runtime overrides obvious enough.
Operators need a first-class way to change ports and other runtime parameters
through environment variables, and they also need an external configuration
mount for larger overrides that are awkward to encode as many env vars.

## What Changes

- add explicit all-in-one container support for configurable application and
  PostgreSQL ports through environment variables with safe defaults
- add an external Spring configuration directory hook for the all-in-one
  container runtime
- expose the application port and external override hooks in the Helm chart
- document the recommended override paths in the packaging guide and env
  example file

## Impact

- Docker operators can change common runtime values without rebuilding images
- Kubernetes operators can align Service/container ports and mount extra config
  through chart values
- larger runtime changes can move to mounted config files instead of long env
  lists
