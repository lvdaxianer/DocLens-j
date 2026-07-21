## Context

DocLens currently exposes architecture-only Docker tags while Maven,
Dashboard, and Helm metadata use different release forms. The image build is
local and produces separate amd64 and arm64 images, so there is no registry
multi-architecture manifest that can carry an architecture-neutral tag yet.

## Goals / Non-Goals

**Goals:**

- Use `0.1.0-beta.1` as the first public beta version everywhere operators
  inspect release metadata.
- Produce immutable architecture tags and retain architecture-only aliases for
  compatibility.
- Pin Compose and Helm defaults to an immutable beta image while preserving an
  explicit operator override.
- Keep the image build offline with respect to JDK, Node.js, PostgreSQL, Maven,
  and npm artifacts.

**Non-Goals:**

- Publishing images to a registry or creating a multi-architecture manifest.
- Rebuilding images or redeploying the 144 server in this local-only task.
- Changing the reusable runtime base image tag strategy.
- Defining the stable `1.0.0` release process.

## Decisions

1. The canonical public beta version is `0.1.0-beta.1`, following SemVer
   pre-release syntax. A date-only or mutable `beta` tag would not provide the
   same ordering and rollback identity.
2. Final images use `<version>-<architecture>` tags because local builds create
   separate platform images. The build script also refreshes `amd64` and
   `arm64` compatibility aliases after a successful versioned build.
3. The x86 Compose file uses
   `doclens:${DOCLENS_IMAGE_TAG:-0.1.0-beta.1-amd64}`. This pins the default
   while allowing an operator to select a later beta or rollback without
   editing the Compose file.
4. Helm metadata uses `appVersion: 0.1.0-beta.1`, and its default image points
   at `doclens:0.1.0-beta.1-amd64`. ARM clusters override `image.tag` until a
   registry multi-architecture manifest exists.
5. Maven modules and Dashboard package metadata use the same public beta
   version so the Assembly archive, server jar, and frontend metadata do not
   advertise conflicting versions.

## Risks / Trade-offs

- [Compatibility aliases remain mutable] -> Deployments use immutable tags by
  default; aliases exist only for existing local workflows.
- [Helm default is architecture-specific] -> Document `image.tag` override for
  arm64 and move to an architecture-neutral tag only after publishing a real
  multi-architecture manifest.
- [Changing Maven from SNAPSHOT affects artifact names] -> Existing wildcard
  discovery in the Assembly preparation script continues to locate the
  generated distribution, and verification covers the new version metadata.
- [Version values are repeated across ecosystem files] -> Contract scripts
  validate that all public release surfaces carry the same value.

## Migration Plan

1. Update metadata, build defaults, Compose, Helm, and documentation.
2. Run version contract checks, Maven tests/package checks, Dashboard tests and
   build, shell verification, and Helm lint.
3. In a later deployment task, build the versioned image, transfer it, set
   `DOCLENS_IMAGE_TAG`, and recreate the service.
4. Roll back by setting `DOCLENS_IMAGE_TAG` to the previous immutable tag and
   recreating the service.

## Open Questions

None for the first public beta.
