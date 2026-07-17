## Why

DocLens-j already has a Spring Boot server, a Vite dashboard, PostgreSQL
runtime configuration, and Flyway database migrations. Operators still need a
repeatable release artifact, a Docker image, and Kubernetes deployment assets
that package those pieces without relying on ad hoc local build steps.

The requested delivery baseline is:

- use Maven Assembly to build a distributable server package;
- provide a Dockerfile that uses common public base images where possible;
- include frontend assets, backend runtime, and PostgreSQL support in the
  container delivery story;
- provide a Helm chart for Kubernetes deployment.

## What Changes

Add a Maven Assembly based distribution profile for `doclens-server` that
packages the executable server jar, configuration examples, and startup scripts.

Add a Dockerfile for a single-node all-in-one image that contains the built
server distribution and a PostgreSQL server process. The Dockerfile should use
well-known public images such as Maven, Node, Eclipse Temurin, and PostgreSQL
where possible instead of obscure custom base images.

Add a Helm chart that deploys DocLens-j on Kubernetes with a separate app
Deployment and PostgreSQL StatefulSet/Service, so Kubernetes deployments keep
the application and database lifecycles separable while still shipping both as
one chart.

Update packaging documentation with the new assembly, Docker, and Helm commands.

## Impact

- `doclens-server` Maven packaging configuration.
- Release scripts or assembly descriptors under the server module.
- Docker build files and entrypoint scripts at repository root or deployment
  paths.
- Helm chart files under a chart/deployment directory.
- Packaging documentation and OpenSpec packaging requirements.
