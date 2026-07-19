## MODIFIED Requirements

### Requirement: Docker image MUST support single-node all-in-one delivery

DocLens-j MUST provide a Dockerfile for a single-node image that contains the
frontend assets, backend runtime, and PostgreSQL runtime support.

#### Scenario: Operator mounts external runtime config

- **WHEN** an operator mounts an external Spring configuration directory and
  mounted runtime env file into the all-in-one image
- **THEN** the backend launch process reads the mounted runtime env file before
  starting Java
- **AND** mounted Spring profile and additional config location values reach the
  backend process
- **AND** packaged baseline config remains available alongside the mounted
  overrides

#### Scenario: Operator opens the documented dashboard entry

- **WHEN** an operator opens `/dashboard` or `/dashboard/` on the packaged
  single-node runtime
- **THEN** the server returns the bundled dashboard index page
- **AND** the dashboard assets continue to load from the packaged
  `static/dashboard` bundle
