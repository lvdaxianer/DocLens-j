## MODIFIED Requirements

### Requirement: Main branch ships PostgreSQL-based packaging assets
The `main` branch SHALL include packaging assets that run the frontend,
backend, and PostgreSQL together for local deployment and Kubernetes delivery.

#### Scenario: Local packaged runtime is available
- **WHEN** an operator prepares the local packaged runtime from `main`
- **THEN** they can build or run images and supporting assets that include the
  frontend, backend, and PostgreSQL components required by DocLens

#### Scenario: Kubernetes delivery assets are available
- **WHEN** an operator deploys DocLens from `main` to Kubernetes
- **THEN** the repository provides Helm chart assets that describe the
  PostgreSQL-based runtime topology

#### Scenario: Operator customizes application runtime surface

- **WHEN** an operator sets a custom application port or mounts extra config
  volumes through chart values
- **THEN** the rendered Deployment uses the configured container port
- **AND** the rendered Service routes to the same application port
- **AND** the rendered Deployment includes the requested extra volume and
  volumeMount entries for external runtime config

#### Scenario: Operator uses dedicated all-in-one Compose examples

- **WHEN** an operator wants a visible x86 single-node delivery layout
- **THEN** the repository provides a dedicated x86 runtime directory with a
  Compose file, mounted runtime env file, mounted backend `application.yml`,
  and operator README
- **AND** the entrypoint can load startup settings from the mounted runtime env
  file before PostgreSQL and backend startup
- **AND** the delivery documentation explains how frontend assets, backend
  archive contents, and startup order map into the image
