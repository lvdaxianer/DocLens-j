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

- **WHEN** an operator wants a concrete all-in-one Docker Compose entry point
- **THEN** the repository provides a dedicated Compose example that is separate
  from the local development PostgreSQL compose file
- **AND** the example includes an env file template for image, port, and
  database defaults
- **AND** the example mounts an external Spring configuration directory for
  larger `doclens.*` runtime overrides
