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

## Install Locally

Install all modules:

```bash
mvn clean install
```

Install SDK-related modules:

```bash
mvn -pl doclens-spring-boot-starter -am clean install
```
