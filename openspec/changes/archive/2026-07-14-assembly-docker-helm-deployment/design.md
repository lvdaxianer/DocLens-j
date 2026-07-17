## Context

The frontend is built by Vite directly into
`doclens-server/src/main/resources/static/dashboard`, so a normal Spring Boot
server jar can serve the dashboard as static assets after the frontend build has
run. The backend uses Flyway migrations from
`doclens-spring-boot-starter/src/main/resources/db/migration`.

The repository already has local PostgreSQL Compose support. Kubernetes should
not run PostgreSQL inside the same application container because StatefulSet
storage and lifecycle controls are the natural Kubernetes primitive for the
database. A single-image all-in-one Dockerfile is still useful for single-node
trial or offline delivery.

## Decisions

- Add a `dist` Maven profile to `doclens-server` that runs
  `maven-assembly-plugin` and creates a `tar.gz` distribution.
- Keep the Spring Boot executable jar as the runtime artifact inside the
  distribution rather than inventing a custom launcher.
- Add startup scripts to normalize environment defaults and run the server with
  the production profile.
- Build the frontend in the Dockerfile with a common Node image, build the Java
  distribution with a common Maven image, and run the final image from a common
  PostgreSQL image plus JDK packages.
- In the all-in-one image, start PostgreSQL first, wait for readiness, then
  start DocLens-j against the local database. Flyway remains responsible for
  schema creation.
- In Helm, deploy:
  - one `Deployment` for DocLens-j;
  - one optional PostgreSQL `StatefulSet`;
  - Services, ConfigMaps, Secrets, PVC templates, probes, and environment
    variables needed by the app.
- Helm defaults use the bundled PostgreSQL StatefulSet. Operators can disable it
  and point `DOCLENS_DB_URL` at an external PostgreSQL instance.

## Risks / Trade-offs

- The all-in-one Docker image runs multiple processes. That is acceptable for
  single-node packaging, but the Helm chart keeps app and database separate for
  production-style Kubernetes deployments.
- Installing JDK packages into a PostgreSQL base image increases image size.
  This is the clearest way to honor the request for an image containing both
  backend and PostgreSQL while still using common public base images.
- Maven Assembly packaging should not replace the Spring Boot jar; it should
  wrap it with scripts and example configuration.
