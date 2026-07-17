## Context

The all-in-one image owns PostgreSQL initialization because the runtime stage is
Ubuntu-based rather than derived from the official PostgreSQL image. The
entrypoint must therefore initialize the cluster, start PostgreSQL, wait for
readiness, and start DocLens-j.

The failed startup log proves that the current password-file ownership is wrong:
`initdb` runs as `postgres`, but the temporary password file is root-owned and
mode `600`.

## Decisions

- Keep the existing `initdb --pwfile` approach so the password is not exposed in
  process arguments.
- After writing the password file, set owner to `postgres:postgres` and keep mode
  `600`.
- Keep the image repository/tag configurable, but change defaults to:
  - repository: `doclens`
  - amd64 tag: `amd64`
  - arm64 tag: `arm64`
- Keep explicit platform builds so local artifacts and image architecture remain
  matched.
- Remove frontend and Maven build stages from Dockerfile. The host prepares the
  dashboard static files and Maven Assembly distribution first, then stores the
  server distribution as `docker/build/doclens-server-dist.tar.gz`.
- Add `LOCAL_SERVER_DIST_ARCHIVE` so Docker build copies the prepared server
  distribution instead of running `npm ci` or `mvn package` inside Docker.
- Move Ubuntu + JDK21 + Node22 + PostgreSQL installation into
  `Dockerfile.runtime-base`, producing reusable `doclens:base-amd64` and
  `doclens:base-arm64` images.
- Make the final `Dockerfile` start from `DOCLENS_RUNTIME_BASE_IMAGE` and only
  unpack the prepared server distribution plus entrypoint.

## Verification

Focused verification:

- `scripts/verify-local-runtime-dockerfile.sh`
- `scripts/prepare-docker-build-context.sh`
- shell syntax checks for touched scripts
- `openspec validate docker-startup-smoke-tags --strict`

Runtime verification:

- run `scripts/prepare-docker-build-context.sh`
- rebuild `PLATFORMS='linux/amd64 linux/arm64' scripts/build-local-runtime-images.sh`,
  which creates both base tags and final app tags
- run `doclens:amd64` and `doclens:arm64` with temporary PostgreSQL/storage
  volumes
- poll `/api/v1/health` on each container until it returns successfully

## Risks / Trade-offs

- amd64 startup on an arm host may be slower due to emulation.
- Smoke containers use temporary named volumes so existing user data is not
  touched.
- Host-side npm/Maven preparation still needs network when dependencies are not
  already cached locally, but Docker build itself becomes artifact-only.
- Rebuilding a changed application distribution still rebuilds the final image,
  but reuses the stable runtime base image layers.
