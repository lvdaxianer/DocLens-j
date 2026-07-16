# Packaging Guide

DocLens Java is packaged by delivery mode:

- SDK API jar: `doclens-api`
- SDK Core jar: `doclens-core`
- Embedded Spring Boot starter jar: `doclens-spring-boot-starter`
- Executable HTTP service jar: `doclens-server`

## Package SDK Artifacts

Package API only:

```bash
mvn -pl doclens-api -am clean package
```

Package Core:

```bash
mvn -pl doclens-core -am clean package
```

Package Spring Boot Starter:

```bash
mvn -pl doclens-spring-boot-starter -am clean package
```

Artifacts:

```text
doclens-api/target/doclens-api-0.1.0-SNAPSHOT.jar
doclens-core/target/doclens-core-0.1.0-SNAPSHOT.jar
doclens-spring-boot-starter/target/doclens-spring-boot-starter-0.1.0-SNAPSHOT.jar
```

These are regular thin jars, not Spring Boot executable fat jars. SDK modules are guarded by Maven Enforcer and must not include:

- `org.springframework.boot:spring-boot-starter-web`
- `org.springframework.boot:spring-boot-starter-tomcat`
- `org.springframework:spring-web`
- `org.springframework:spring-webmvc`

## Package HTTP Service

```bash
mvn -pl doclens-server -am clean package
```

Artifact:

```text
doclens-server/target/doclens-server-0.1.0-SNAPSHOT.jar
```

`doclens-server` is the only module that is allowed to depend on `spring-boot-starter-web`.

## Build Assembly Distribution

The distribution archive contains the executable server jar, startup script, and PostgreSQL production configuration examples:

```bash
mvn -pl doclens-server -am -Pdist -DskipTests package
```

Artifacts:

```text
doclens-server/target/doclens-server-0.1.0-SNAPSHOT-dist.tar.gz
doclens-server/target/doclens-server-0.1.0-SNAPSHOT-dist.zip
```

Main files after extraction:

```text
bin/doclens-server.sh
conf/application-prod.yml
conf/doclens.env.example
lib/doclens-server-0.1.0-SNAPSHOT.jar
```

Before running it, prepare the PostgreSQL connection, storage directory, OCR node, and trusted gateway secret environment variables from `conf/doclens.env.example`.

## Build Single-Node Docker Image

The Dockerfile builds the dashboard first, then the backend Assembly distribution, and finally uses a locally available Ubuntu image as the runtime base image. JDK21, Node22, and PostgreSQL are not downloaded during Docker build; prepare them under `docker/runtime/` first, then let the Dockerfile `COPY` and install them.

```bash
JDK_SOURCE=/Users/lvdaxianer/Downloads/jdk-21_linux-x64_bin.tar.gz \
DOCKER_PLATFORM=linux/amd64 \
  scripts/prepare-container-runtimes.sh
```

After preparation, the local build inputs should exist:

```text
docker/runtime/jdk-21_linux-x64_bin.tar.gz
docker/runtime/node-v22-linux-x64.tar.xz
docker/runtime/postgresql-16-ubuntu22.04-x64-debs.tar.gz
```

Build an x64 test image:

```bash
docker build \
  --platform linux/amd64 \
  --build-arg LOCAL_JDK_ARCHIVE=docker/runtime/jdk-21_linux-x64_bin.tar.gz \
  --build-arg LOCAL_NODE_ARCHIVE=docker/runtime/node-v22-linux-x64.tar.xz \
  --build-arg LOCAL_POSTGRES_DEB_ARCHIVE=docker/runtime/postgresql-16-ubuntu22.04-x64-debs.tar.gz \
  -t doclens-j:all-in-one .
```

For other CPU architectures, the JDK, Node, PostgreSQL Debian package bundle, and Ubuntu base image must use the same architecture. If the local `ubuntu:22.04` / `node:22-bookworm-slim` images are arm64 only, a fully local build should use an aarch64 JDK. If you use an x64 JDK, prepare amd64 Ubuntu/Node images first or allow the preparation step to pull the matching platform images. `scripts/prepare-container-runtimes.sh` supports environment variable overrides:

```bash
RUNTIME_DIR=docker/runtime \
DOCKER_PLATFORM=linux/arm64 \
JDK_SOURCE=/path/to/jdk-21_linux-aarch64_bin.tar.gz \
NODE_IMAGE=node:22-bookworm-slim \
POSTGRES_DOWNLOAD_IMAGE=ubuntu:22.04 \
POSTGRES_MAJOR=16 \
scripts/prepare-container-runtimes.sh
```

The compatibility entrypoint `scripts/download-container-jdk.sh` is still available, but it delegates to `scripts/prepare-container-runtimes.sh` so JDK, Node, and PostgreSQL artifacts are prepared together.

No separate PostgreSQL initialization SQL is maintained in the Dockerfile. After the image starts DocLens-j, application Flyway migrations still initialize the business schema.

The single-node image starts PostgreSQL first, waits for readiness, and then starts DocLens-j:

```bash
docker run --rm \
  -p 10003:10003 \
  -p 15432:5432 \
  -e POSTGRES_DB=doclens \
  -e POSTGRES_USER=doclens \
  -e POSTGRES_PASSWORD=replace-with-strong-password \
  -e DOCLENS_GATEWAY_SECRET=replace-with-gateway-secret \
  -v doclens-postgresql:/var/lib/postgresql/data \
  -v doclens-storage:/var/lib/doclens/storage \
  doclens-j:all-in-one
```

Use the all-in-one image for trials, offline delivery, and small validation runs. For production-style Kubernetes deployments, use the Helm chart so the app and PostgreSQL run as separate workloads.

## Kubernetes Helm Deployment

Chart path:

```text
deploy/helm/doclens-j
```

The default render creates a DocLens-j `Deployment`, application `Service`, `ConfigMap`, `Secret`, app storage `PVC`, and bundled PostgreSQL `StatefulSet` and `Service`:

```bash
helm template doclens ./deploy/helm/doclens-j
```

Install the chart with bundled PostgreSQL:

```bash
helm install doclens ./deploy/helm/doclens-j \
  --set image.repository=doclens-j \
  --set image.tag=all-in-one \
  --set postgresql.password='replace-with-strong-password' \
  --set app.gatewayAuth.secret='replace-with-gateway-secret'
```

The chart overrides the app container command by default and only starts `/opt/doclens/bin/doclens-server.sh`, so the application Pod does not start the image's embedded PostgreSQL process. PostgreSQL lifecycle is handled by the `StatefulSet`.

To use external PostgreSQL, disable the bundled PostgreSQL workload and provide the external JDBC URL:

```bash
helm upgrade --install doclens ./deploy/helm/doclens-j \
  --set image.repository=doclens-j \
  --set image.tag=all-in-one \
  --set postgresql.enabled=false \
  --set app.database.url='jdbc:postgresql://postgresql.example.com:5432/doclens' \
  --set app.database.username='doclens' \
  --set app.database.password='replace-with-strong-password' \
  --set app.gatewayAuth.secret='replace-with-gateway-secret'
```

For production, prefer an existing Secret instead of passing passwords on the command line:

```bash
kubectl create secret generic doclens-database \
  --from-literal=DOCLENS_DB_PASSWORD='replace-with-strong-password' \
  --from-literal=DOCLENS_GATEWAY_SECRET='replace-with-gateway-secret'

helm upgrade --install doclens ./deploy/helm/doclens-j \
  --set postgresql.enabled=false \
  --set app.database.url='jdbc:postgresql://postgresql.example.com:5432/doclens' \
  --set app.database.username='doclens' \
  --set app.database.existingSecret=doclens-database
```

## Install Locally

Install all modules:

```bash
mvn clean install
```

Install SDK-related modules:

```bash
mvn -pl doclens-spring-boot-starter -am clean install
```
