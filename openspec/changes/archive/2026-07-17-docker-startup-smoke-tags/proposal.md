## Why

Runtime smoke testing showed that `doclens:amd64` could not finish first
startup. The all-in-one entrypoint creates the `initdb --pwfile` as root with
mode `600`, then executes `initdb` as the `postgres` user. PostgreSQL cannot
read the root-owned password file and exits before the application starts.

The user also requested the local image names to be `doclens:amd64` and
`doclens:arm64` instead of the previous `doclens-j:all-in-one-*` tags.

## What Changes

- Make the entrypoint password file readable by the `postgres` user while
  keeping restrictive permissions.
- Change the local runtime image build defaults to produce `doclens:amd64` and
  `doclens:arm64`.
- Move dashboard and Maven Assembly work out of Docker build so npm/Maven
  dependencies are resolved on the host before packaging, and Docker build only
  copies local artifacts.
- Split the local runtime image into reusable `doclens:base-amd64` and
  `doclens:base-arm64` base images, then build `doclens:amd64` and
  `doclens:arm64` from those base images.
- Update packaging docs and the Dockerfile contract verification script so this
  startup and tagging behavior is checked.
- Rebuild both architecture images and verify the all-in-one container reaches
  the health endpoint.

## Impact

- `docker/entrypoint.sh`
- `Dockerfile`
- `Dockerfile.runtime-base`
- `scripts/build-local-runtime-images.sh`
- `scripts/prepare-docker-build-context.sh`
- `scripts/verify-local-runtime-dockerfile.sh`
- Packaging documentation
- Deployment packaging OpenSpec requirements
