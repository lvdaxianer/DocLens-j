## Why

The current `doclens:amd64` and `doclens:arm64` tags identify only the target
architecture, so operators cannot tell which public beta build is deployed or
roll back to a known release. The first public beta needs one version identity
across build artifacts and deployment assets.

## What Changes

- Set the public beta product version to `0.1.0-beta.1` across Maven,
  Dashboard, and Helm metadata.
- Build immutable architecture-specific image tags
  `doclens:0.1.0-beta.1-amd64` and `doclens:0.1.0-beta.1-arm64`.
- Keep `doclens:amd64` and `doclens:arm64` as compatibility aliases for local
  workflows, but stop using them as the default deployed image reference.
- Make the x86 Compose delivery default to the immutable beta tag while
  allowing operators to override it through `DOCLENS_IMAGE_TAG`.
- Make Helm default to the same public beta application version and image tag.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `deployment-packaging`: Require consistent public beta version metadata,
  immutable architecture image tags, and an overridable pinned Compose image.

## Impact

- Maven parent and child project versions.
- Dashboard package metadata and lockfile.
- Local runtime image build script and its shell contract checks.
- x86 Compose delivery, Helm chart metadata and values, and packaging docs.
- Existing `doclens:amd64` consumers remain compatible through an alias.
