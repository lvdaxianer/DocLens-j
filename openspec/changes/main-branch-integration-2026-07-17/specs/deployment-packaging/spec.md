## ADDED Requirements

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
