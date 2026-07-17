## 1. Docker startup and tag contract

- [x] 1.1 Add focused verification for PostgreSQL password-file ownership and
  `doclens:amd64` / `doclens:arm64` tag defaults.
- [x] 1.2 Fix the entrypoint and build-script defaults, then update packaging
  documentation.
- [x] 1.3 Move dashboard/Maven Assembly work to a local build-context
  preparation script so Docker build copies prepared artifacts.
- [x] 1.4 Split reusable runtime base images from final application images to
  avoid reinstalling JDK, Node, and PostgreSQL for app-only rebuilds.

## 2. Runtime verification and archive

- [x] 2.1 Prepare local build artifacts, rebuild amd64/arm64 images, run both
  all-in-one containers, verify the health endpoint, then archive the OpenSpec
  change.
