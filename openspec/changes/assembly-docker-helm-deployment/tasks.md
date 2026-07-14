## 1. Assembly distribution

- [x] 1.1 Add a Maven Assembly distribution for `doclens-server` that packages
  the executable server jar, config examples, and startup scripts.
- [ ] 1.2 Verify the assembly artifact is produced and contains the expected
  runtime files.

## 2. Docker all-in-one image

- [ ] 2.1 Add a Dockerfile and entrypoint that build frontend/backend artifacts
  and run DocLens-j with PostgreSQL in one image using common public base
  images where possible.
- [ ] 2.2 Verify the Dockerfile is syntactically usable and the image can be
  built or at least inspected with the available local toolchain.

## 3. Helm Kubernetes deployment

- [ ] 3.1 Add a Helm chart that deploys DocLens-j and PostgreSQL for Kubernetes,
  with external PostgreSQL override support.
- [ ] 3.2 Verify the Helm chart renders successfully with default values.

## 4. Documentation and final verification

- [ ] 4.1 Update packaging documentation with assembly, Docker, and Helm usage.
- [ ] 4.2 Run broad verification covering Maven packaging, Docker/Helm checks,
  OpenSpec validation, and repository diff checks.
