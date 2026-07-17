## Why

The current all-in-one Dockerfile builds the final image from the PostgreSQL
base image and only treats the JDK as a local archive. The requested packaging
model is stricter: prefer local Docker images, use Ubuntu as the final base
image, and copy all runtime installers or archives from the build context
instead of downloading them during `docker build`.

## What Changes

Update the single-node Docker build so the final runtime stage uses an Ubuntu
base image and installs JDK21, Node22, and PostgreSQL from local build-context
artifacts.

Add or update helper scripts and packaging documentation so operators can
prepare those local artifacts before building the image.

Add a lightweight verification script that checks the Dockerfile contract for
local runtime artifacts and prevents accidental reintroduction of network
downloads in the runtime stage.

## Impact

- `Dockerfile`
- Docker entrypoint and runtime environment assumptions
- Local runtime artifact preparation scripts
- Packaging documentation
- Deployment packaging OpenSpec requirements
