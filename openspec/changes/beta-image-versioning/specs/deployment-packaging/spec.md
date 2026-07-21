## MODIFIED Requirements

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
- **AND** the immutable public beta tags are
  `doclens:0.1.0-beta.1-amd64` and `doclens:0.1.0-beta.1-arm64`
- **AND** successful builds refresh `doclens:amd64` and `doclens:arm64` as
  compatibility aliases
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

## ADDED Requirements

### Requirement: Public beta release metadata MUST be consistent

DocLens-j public beta packaging SHALL use `0.1.0-beta.1` consistently across
Maven artifacts, Dashboard package metadata, Docker image tags, and Helm chart
application metadata.

#### Scenario: Maintainer inspects public beta artifacts

- **WHEN** a maintainer inspects Maven, Dashboard, Docker, Compose, and Helm
  release metadata
- **THEN** every immutable release reference identifies `0.1.0-beta.1`
- **AND** architecture-specific Docker references include either `amd64` or
  `arm64`

### Requirement: Compose deployment MUST default to an immutable beta image

The x86 Compose delivery SHALL default to
`doclens:0.1.0-beta.1-amd64` and SHALL allow operators to override the image
tag without editing the Compose file.

#### Scenario: Operator starts the default x86 beta

- **WHEN** an operator starts the x86 Compose delivery without setting an image
  tag override
- **THEN** Compose selects `doclens:0.1.0-beta.1-amd64`

#### Scenario: Operator selects another immutable release

- **WHEN** an operator sets `DOCLENS_IMAGE_TAG` before starting Compose
- **THEN** Compose uses that tag from the `doclens` repository
