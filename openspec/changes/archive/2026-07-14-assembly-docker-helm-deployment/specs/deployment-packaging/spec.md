## ADDED Requirements

### Requirement: Assembly distribution MUST package the server runtime

DocLens-j MUST provide a Maven Assembly based distribution for the HTTP server.

#### Scenario: Maintainer builds the server distribution

- **WHEN** a maintainer runs the documented Maven distribution command
- **THEN** a compressed server distribution archive is produced
- **AND** the archive includes the executable server jar
- **AND** the archive includes startup scripts
- **AND** the archive includes configuration examples for PostgreSQL-backed
  runtime

### Requirement: Docker image MUST support single-node all-in-one delivery

DocLens-j MUST provide a Dockerfile for a single-node image that contains the
frontend assets, backend runtime, and PostgreSQL runtime support.

#### Scenario: Maintainer builds the all-in-one image

- **WHEN** a maintainer builds the Dockerfile from the repository root
- **THEN** the frontend dashboard assets are included in the backend runtime
- **AND** the server distribution is included in the final image
- **AND** PostgreSQL can be started inside the image for single-node use
- **AND** the Dockerfile uses common public base images where possible

### Requirement: Helm chart MUST support Kubernetes deployment

DocLens-j MUST provide a Helm chart for Kubernetes deployment.

#### Scenario: Operator deploys with bundled PostgreSQL

- **WHEN** an operator renders or installs the chart with default values
- **THEN** the chart includes a DocLens-j application Deployment
- **AND** the chart includes a PostgreSQL StatefulSet and Service
- **AND** the application receives PostgreSQL datasource environment variables
- **AND** persistent storage is configured for PostgreSQL and DocLens storage

#### Scenario: Operator deploys with external PostgreSQL

- **WHEN** an operator disables bundled PostgreSQL and provides an external
  datasource URL
- **THEN** the chart renders without the PostgreSQL StatefulSet
- **AND** the application still receives PostgreSQL datasource environment
  variables for the external database
