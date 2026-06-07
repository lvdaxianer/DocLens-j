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
