# Packaging Guide

DocLens Java 的打包按交付形态分开处理：

- SDK API 包：`doclens-api`
- SDK Core 包：`doclens-core`
- Spring Boot 嵌入式 Starter 包：`doclens-spring-boot-starter`
- HTTP 可运行服务包：`doclens-server`

### 打包 SDK

只打 SDK API：

```bash
mvn -pl doclens-api -am clean package
```

打 SDK Core：

```bash
mvn -pl doclens-core -am clean package
```

打 Spring Boot Starter：

```bash
mvn -pl doclens-spring-boot-starter -am clean package
```

产物位置：

```text
doclens-api/target/doclens-api-0.1.0-SNAPSHOT.jar
doclens-core/target/doclens-core-0.1.0-SNAPSHOT.jar
doclens-spring-boot-starter/target/doclens-spring-boot-starter-0.1.0-SNAPSHOT.jar
```

这些包都是普通 thin jar，不是 Spring Boot 可执行 fat jar。SDK 相关模块禁止携带以下 Web 依赖：

- `org.springframework.boot:spring-boot-starter-web`
- `org.springframework.boot:spring-boot-starter-tomcat`
- `org.springframework:spring-web`
- `org.springframework:spring-webmvc`

项目已在 `doclens-api`、`doclens-core`、`doclens-spring-boot-starter` 中配置 Maven Enforcer 规则；如果未来误引入 Web 依赖，构建会失败。

### 打包 HTTP 服务

HTTP 服务打包命令：

```bash
mvn -pl doclens-server -am clean package
```

产物位置：

```text
doclens-server/target/doclens-server-0.1.0-SNAPSHOT.jar
```

`doclens-server` 是唯一允许依赖 `spring-boot-starter-web` 的模块，它负责提供 REST API 和可运行服务入口。

### 构建 Assembly 发布包

发布包会把可执行服务 jar、启动脚本和 PostgreSQL 生产配置示例打进一个压缩包：

```bash
mvn -pl doclens-server -am -Pdist -DskipTests package
```

产物位置：

```text
doclens-server/target/doclens-server-0.1.0-SNAPSHOT-dist.tar.gz
doclens-server/target/doclens-server-0.1.0-SNAPSHOT-dist.zip
```

解压后主要目录：

```text
bin/doclens-server.sh
conf/application-prod.yml
conf/doclens.env.example
lib/doclens-server-0.1.0-SNAPSHOT.jar
```

运行前需要按 `conf/doclens.env.example` 准备 PostgreSQL 连接、对象存储目录、OCR 节点和可信网关密钥等环境变量。

### 构建单机 Docker 镜像

Dockerfile 会先构建前端，再构建后端 Assembly，最终使用本地已有的 Ubuntu 镜像作为 runtime 基础镜像。JDK21、Node22 和 PostgreSQL 都不在 Docker build 阶段下载，而是先准备到 `docker/runtime/`，再由 Dockerfile 直接 `COPY` 进镜像安装。

```bash
JDK_SOURCE=/Users/lvdaxianer/Downloads/jdk-21_linux-x64_bin.tar.gz \
DOCKER_PLATFORM=linux/amd64 \
  scripts/prepare-container-runtimes.sh
```

准备完成后应有以下本地构建输入：

```text
docker/runtime/jdk-21_linux-x64_bin.tar.gz
docker/runtime/node-v22-linux-x64.tar.xz
docker/runtime/postgresql-16-ubuntu22.04-x64-debs.tar.gz
```

构建 x64 测试镜像：

```bash
docker build \
  --platform linux/amd64 \
  --build-arg LOCAL_JDK_ARCHIVE=docker/runtime/jdk-21_linux-x64_bin.tar.gz \
  --build-arg LOCAL_NODE_ARCHIVE=docker/runtime/node-v22-linux-x64.tar.xz \
  --build-arg LOCAL_POSTGRES_DEB_ARCHIVE=docker/runtime/postgresql-16-ubuntu22.04-x64-debs.tar.gz \
  -t doclens-j:all-in-one .
```

如果要构建其他 CPU 架构，JDK、Node、PostgreSQL Debian 包和 Ubuntu 基础镜像必须使用同一架构。当前本机如果只有 arm64 的 `ubuntu:22.04` / `node:22-bookworm-slim`，完全本地构建时应使用 aarch64 JDK；如果使用 x64 JDK，则需要先准备 amd64 的 Ubuntu/Node 镜像，或允许准备阶段拉取对应平台镜像。`scripts/prepare-container-runtimes.sh` 支持用环境变量覆盖默认输入：

```bash
RUNTIME_DIR=docker/runtime \
DOCKER_PLATFORM=linux/arm64 \
JDK_SOURCE=/path/to/jdk-21_linux-aarch64_bin.tar.gz \
NODE_IMAGE=node:22-bookworm-slim \
POSTGRES_DOWNLOAD_IMAGE=ubuntu:22.04 \
POSTGRES_MAJOR=16 \
scripts/prepare-container-runtimes.sh
```

兼容入口 `scripts/download-container-jdk.sh` 仍保留，但它会转调 `scripts/prepare-container-runtimes.sh`，同时准备 JDK、Node 和 PostgreSQL artifacts。

PostgreSQL 初始化 SQL 不需要单独维护在 Dockerfile 中；镜像启动 DocLens-j 后仍由应用内 Flyway migration 初始化业务表结构。

单机运行时镜像内会先启动 PostgreSQL，再启动 DocLens-j：

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

单机镜像适合试运行、离线交付和小规模验证。生产 Kubernetes 部署应使用 Helm chart，把应用和 PostgreSQL 拆成独立工作负载。

### Kubernetes Helm 部署

Helm chart 位置：

```text
deploy/helm/doclens-j
```

默认渲染会创建 DocLens-j `Deployment`、应用 `Service`、配置 `ConfigMap`、密钥 `Secret`、应用存储 `PVC`，以及内置 PostgreSQL `StatefulSet` 和 `Service`：

```bash
helm template doclens ./deploy/helm/doclens-j
```

安装默认 chart：

```bash
helm install doclens ./deploy/helm/doclens-j \
  --set image.repository=doclens-j \
  --set image.tag=all-in-one \
  --set postgresql.password='replace-with-strong-password' \
  --set app.gatewayAuth.secret='replace-with-gateway-secret'
```

chart 默认会覆盖应用容器 command，只启动 `/opt/doclens/bin/doclens-server.sh`，避免 Kubernetes 中的应用 Pod 再启动镜像内置 PostgreSQL。PostgreSQL 生命周期由 `StatefulSet` 管理。

使用外部 PostgreSQL 时关闭内置 PostgreSQL，并提供外部 JDBC URL：

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

生产环境建议使用已有 Secret，而不是在命令行传递密码：

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

### 发布到本地 Maven 仓库

开发阶段可以先安装到本地仓库：

```bash
mvn clean install
```

如果只想安装 SDK 相关模块：

```bash
mvn -pl doclens-spring-boot-starter -am clean install
```

宿主项目引入 starter：

```xml
<dependency>
    <groupId>io.github.lvdaxianer</groupId>
    <artifactId>doclens-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```
