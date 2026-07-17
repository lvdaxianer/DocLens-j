## Context

The project already has an Assembly distribution and an all-in-one image. The
previous final image uses `postgres:16-bookworm`, which is convenient because
it includes PostgreSQL binaries and the upstream entrypoint. The new constraint
is to make Ubuntu the runtime base and treat JDK21, Node22, and PostgreSQL as
local artifacts copied into the image.

## Decisions

- Keep the existing frontend and Maven build stages on local Docker images by
  default, because the local image cache already contains Node22 and Maven with
  JDK21.
- Change the final runtime stage to default to `ubuntu:22.04`.
- Add build args for:
  - `LOCAL_JDK_ARCHIVE`
  - `LOCAL_NODE_ARCHIVE`
  - `LOCAL_POSTGRES_DEB_ARCHIVE`
- Install JDK and Node by extracting local tar archives.
- Install PostgreSQL by extracting a local archive that contains Debian package
  files and running `dpkg -i` inside the image. This keeps Docker build offline
  as long as the package archive was prepared in advance.
- Replace the dependency on the official PostgreSQL image entrypoint with a
  project-owned entrypoint that initializes `PGDATA`, starts PostgreSQL, waits
  for readiness, creates the configured database/user if needed, and then
  starts DocLens-j.
- Keep Flyway responsible for application schema initialization.

## Verification

The focused verification is a shell contract test that inspects Dockerfile and
entrypoint expectations. The broader verification renders Helm, validates
OpenSpec, checks diffs, and builds the image when all local artifacts are
present.

If Node or PostgreSQL archives are not present locally, the build verification
should report the missing artifact clearly rather than attempting a network
download in Docker.

## Risks / Trade-offs

- Installing PostgreSQL from a local Debian package bundle requires the bundle
  to include all package dependencies for the target Ubuntu version and CPU
  architecture.
- Node is not required at runtime for the Spring Boot server, but it is included
  because the requested image contract explicitly asks for Node22 to be handled
  like the JDK.
- A project-owned PostgreSQL entrypoint is a small amount of operational code,
  but it removes the runtime dependency on the official PostgreSQL image.
