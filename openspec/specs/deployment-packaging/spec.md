# deployment-packaging Specification

## Purpose
Define DocLens-j release packaging and deployment expectations for Maven
Assembly distributions, single-node Docker delivery, and Kubernetes Helm
deployments with PostgreSQL.
## Requirements
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
- **AND** the Dockerfile uses local Docker images where possible
- **AND** the final runtime stage uses an Ubuntu base image
- **AND** the final runtime stage installs JDK21, Node22, and PostgreSQL from
  local build-context artifacts rather than downloading them during Docker
  build
- **AND** the Docker build copies a local Maven Assembly server distribution
  instead of running npm or Maven dependency resolution inside Docker
- **AND** the image build can create reusable `doclens:base-amd64` and
  `doclens:base-arm64` runtime base images containing Ubuntu, JDK21, Node22,
  and PostgreSQL
- **AND** maintainers can build separate x86 and arm image tags from matching
  local runtime artifacts
- **AND** those default tags are `doclens:amd64` and `doclens:arm64`
- **AND** the local PostgreSQL Debian artifact preparation rejects duplicate
  packages and incomplete Debian dependency bundles before Docker build starts
- **AND** first startup initializes PostgreSQL without exposing the database
  password in process arguments or blocking the `postgres` user from reading
  the password file

#### Scenario: Operator overrides common runtime values

- **WHEN** an operator runs the all-in-one image with runtime environment
  variables
- **THEN** they can override the application port and PostgreSQL port without
  rebuilding the image
- **AND** packaged default values still exist when those environment variables
  are omitted

#### Scenario: Operator mounts external runtime config

- **WHEN** an operator mounts an external Spring configuration directory into
  the all-in-one image
- **THEN** the runtime can read that directory through a default
  `SPRING_CONFIG_ADDITIONAL_LOCATION` hook
- **AND** packaged baseline config remains available alongside the mounted
  overrides

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

