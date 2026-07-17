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

The single-node image uses a locally available Ubuntu image as the runtime base image. The dashboard assets, backend Maven Assembly distribution, JDK21, Node22, and PostgreSQL are not downloaded or built during Docker build; prepare them under `docker/build/` and `docker/runtime/` first, then let the Dockerfile `COPY` and install them.

Prepare the dashboard assets and backend Assembly distribution on the host:

```bash
scripts/prepare-docker-build-context.sh
```

After preparation, this local build input should exist:

```text
docker/build/doclens-server-dist.tar.gz
```

Prepare x86/amd64 build inputs:

```bash
DOCKER_PLATFORM=linux/amd64 \
JDK_SOURCE=/Users/lvdaxianer/Downloads/jdk-21_linux-x64_bin.tar.gz \
NODE_SOURCE=/Users/lvdaxianer/Downloads/node-v22.23.1-linux-x64.tar.xz \
POSTGRES_DEB_SOURCE_DIR=/Users/lvdaxianer/Downloads/postgresql-amd64-debs \
scripts/prepare-container-runtimes.sh
```

`POSTGRES_DEB_SOURCE_DIR` cannot contain only the `postgresql-16` and `postgresql-client-16` packages. It must contain the complete dependency closure needed to install PostgreSQL on Ubuntu 22.04, and only one `.deb` may exist for each package name. At minimum, include:

```text
postgresql-16
postgresql-client-16
postgresql-common
postgresql-client-common
libpq5
libicu70
libldap-2.5-0
libllvm15
libxml2
libxslt1.1
libreadline8
locales or locales-all
openssl
ssl-cert
tzdata
```

Generate that directory from a same-architecture Ubuntu 22.04 environment. For x86/amd64:

```bash
mkdir -p /Users/lvdaxianer/Downloads/postgresql-amd64-debs
docker run --rm --platform linux/amd64 \
  -v /Users/lvdaxianer/Downloads/postgresql-amd64-debs:/out \
  ubuntu:22.04 \
  bash -lc "set -euo pipefail
    export DEBIAN_FRONTEND=noninteractive
    apt-get update
    apt-get install -y --no-install-recommends ca-certificates curl gnupg
    install -d /usr/share/postgresql-common/pgdg
    curl -fsSL https://www.postgresql.org/media/keys/ACCC4CF8.asc \
      | gpg --dearmor -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.gpg
    printf 'deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.gpg] http://apt.postgresql.org/pub/repos/apt jammy-pgdg main\n' \
      >/etc/apt/sources.list.d/pgdg.list
    apt-get update
    apt-get install -y --download-only --no-install-recommends postgresql-16 postgresql-client-16
    apt-get install -y --download-only --reinstall libldap-2.5-0 libreadline8 openssl
    cp /var/cache/apt/archives/*.deb /out/"
```

For arm64, use the same command with `--platform linux/arm64` and `/Users/lvdaxianer/Downloads/postgresql-arm64-debs` as the output directory.

After preparation, the x86 local build inputs should exist:

```text
docker/runtime/jdk-21_linux-x64_bin.tar.gz
docker/runtime/node-v22-linux-x64.tar.gz
docker/runtime/postgresql-16-ubuntu22.04-x64-debs.tar.gz
```

Prepare arm64 build inputs with arm64/aarch64 JDK, Node, and PostgreSQL Debian packages:

```bash
DOCKER_PLATFORM=linux/arm64 \
JDK_SOURCE=/Users/lvdaxianer/Downloads/jdk-21_linux-aarch64_bin.tar.gz \
NODE_SOURCE=/Users/lvdaxianer/Downloads/node-v22.23.1-linux-arm64.tar.xz \
POSTGRES_DEB_SOURCE_DIR=/Users/lvdaxianer/Downloads/postgresql-arm64-debs \
scripts/prepare-container-runtimes.sh
```

After preparation, the arm64 local build inputs should exist:

```text
docker/runtime/jdk-21_linux-aarch64_bin.tar.gz
docker/runtime/node-v22-linux-arm64.tar.gz
docker/runtime/postgresql-16-ubuntu22.04-arm64-debs.tar.gz
```

Build the two image tags separately:

```bash
PLATFORMS='linux/amd64 linux/arm64' scripts/build-local-runtime-images.sh
```

You can also build only one platform:

```bash
PLATFORMS='linux/amd64' scripts/build-local-runtime-images.sh
PLATFORMS='linux/arm64' scripts/build-local-runtime-images.sh
```

The build script first creates reusable runtime base images `doclens:base-amd64` and `doclens:base-arm64`, containing Ubuntu, JDK21, Node22, and PostgreSQL. It then builds the final application images `doclens:amd64` and `doclens:arm64` from the matching base image. When only the application distribution changes, the base image layers can be reused instead of reinstalling JDK, Node, and PostgreSQL.

The JDK, Node, PostgreSQL Debian package bundle, and Ubuntu base image must use the same architecture; if one platform is missing an artifact, the build script reports the missing file directly.

For manual `docker build` usage, the corresponding build args are `LOCAL_JDK_ARCHIVE`, `LOCAL_NODE_ARCHIVE`, `LOCAL_POSTGRES_DEB_ARCHIVE`, and `LOCAL_SERVER_DIST_ARCHIVE`; in normal release work, prefer `scripts/build-local-runtime-images.sh` so both platform tags are generated consistently.

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
  doclens:amd64
```

For short smoke tests on small local Docker environments, you can add
`-e JAVA_OPTS='-Xms96m -Xmx256m -XX:MaxMetaspaceSize=256m'`. After startup,
the health endpoint still requires the caller partition header:

```bash
curl -fsS \
  -H 'X-Doclens-Key: local-dev' \
  http://127.0.0.1:10003/api/v1/health
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
  --set image.repository=doclens \
  --set image.tag=amd64 \
  --set postgresql.password='replace-with-strong-password' \
  --set app.gatewayAuth.secret='replace-with-gateway-secret'
```

The chart overrides the app container command by default and only starts `/opt/doclens/bin/doclens-server.sh`, so the application Pod does not start the image's embedded PostgreSQL process. PostgreSQL lifecycle is handled by the `StatefulSet`.

To use external PostgreSQL, disable the bundled PostgreSQL workload and provide the external JDBC URL:

```bash
helm upgrade --install doclens ./deploy/helm/doclens-j \
  --set image.repository=doclens \
  --set image.tag=amd64 \
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
