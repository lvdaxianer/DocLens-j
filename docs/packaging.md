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

Dockerfile 会先构建前端，再构建后端 Assembly，最终基于 PostgreSQL 镜像加入本地 JDK21 和 DocLens-j 运行包。JDK 不在 Docker build 内下载，先把已下载的 JDK21 Linux 包放进构建上下文：

```bash
cp /Users/lvdaxianer/Downloads/jdk-21_linux-aarch64_bin.tar.gz docker/jdk/jdk-21_linux-aarch64_bin.tar.gz
```

构建 aarch64 测试镜像：

```bash
docker build \
  --build-arg LOCAL_JDK_ARCHIVE=docker/jdk/jdk-21_linux-aarch64_bin.tar.gz \
  -t doclens-j:all-in-one .
```

如果使用 x64 JDK，可以把文件放到默认路径：

```text
docker/jdk/temurin-21-jdk-linux-x64.tar.gz
```

也可以用脚本下载到默认路径后再构建：

```bash
JDK_VERSION=21 JDK_ARCH=x64 scripts/download-container-jdk.sh
docker build -t doclens-j:all-in-one .
```

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
