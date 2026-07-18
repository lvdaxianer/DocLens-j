# Design

## Scope

This change improves the delivery surface around configuration overrides. It
does not redesign Spring property binding or remove the existing shared
defaults from `application.yml`.

## Decisions

1. The all-in-one entrypoint will export explicit defaults for
   `DOCLENS_SERVER_PORT`, `SERVER_PORT`, and `POSTGRES_PORT`.
2. The all-in-one entrypoint will provide a default
   `SPRING_CONFIG_ADDITIONAL_LOCATION` that keeps packaged config available
   while allowing an external config directory to be mounted into the
   container.
3. The Helm chart will expose an application port value and generic extra
   volume / volumeMount hooks so external Spring config can be mounted without
   forking the chart.
4. Packaging docs and env examples will document two override paths:
   environment variables for common values and mounted config files for larger
   changes.

## Risks

- If the chart Service port and container port drift apart, probes and routing
  become confusing.
- If the external config location fully replaces packaged defaults, operators
  can accidentally drop required baseline settings.

## Verification

- focused test: verify runtime override hooks by script against entrypoint,
  Helm templates, and packaging docs
- broader verification: Helm lint plus focused template renders for custom
  server port and extra config mounts
