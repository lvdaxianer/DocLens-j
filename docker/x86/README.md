# DocLens x86 单机交付目录

这个目录专门给 x86 单机运行时使用，目标不是“参数尽量多”，而是让你一眼能
看清楚前端、后端、PostgreSQL 和配置文件在交付链路里的位置。

## 目录说明

- `docker-compose.yml`：x86 单机运行入口
- `config/runtime.env`：启动外壳参数，主要给容器入口脚本读取
- `config/application.yml`：后端业务配置，挂载后由 Spring Boot 读取
- `data/postgresql`：默认 PostgreSQL 数据目录
- `data/storage`：默认上传文件和处理结果目录

## 前端和后端是怎么进镜像的

### 1. 前端先在宿主机构建

`doclens-dashboard` 的构建产物会输出到：

```text
doclens-server/src/main/resources/static/dashboard
```

这一步在 [scripts/prepare-docker-build-context.sh](/Users/lvdaxianer/workspace/my/project/DocLens-j/scripts/prepare-docker-build-context.sh)
里完成。

### 2. 后端再打成 Assembly 分发包

同一个脚本会继续执行 Maven Assembly，把后端 jar、启动脚本、配置示例以及
已经进入 jar classpath 的前端静态资源一起打成：

```text
docker/build/doclens-server-dist.tar.gz
```

### 3. Dockerfile 把分发包复制进镜像

[Dockerfile](/Users/lvdaxianer/workspace/my/project/DocLens-j/Dockerfile) 会把
`docker/build/doclens-server-dist.tar.gz` 复制到镜像里，再解压到：

```text
/opt/doclens
```

所以从镜像角度看：

- `/opt/doclens/lib/doclens-server-*.jar` 是后端
- jar 里的 `static/dashboard` 是前端静态资源

## 容器里是怎么启动的

容器入口是 [docker/entrypoint.sh](/Users/lvdaxianer/workspace/my/project/DocLens-j/docker/entrypoint.sh)。
启动顺序是：

1. 先读取挂载进来的 `/opt/doclens/config/runtime.env`
2. 初始化并启动容器内 PostgreSQL
3. 等 PostgreSQL ready
4. 启动 `/opt/doclens/bin/doclens-server.sh`
5. 后端对外提供 API，同时把前端静态资源挂到 `/dashboard/`

也就是说，前端不是单独进程；它是后端静态托管的一部分。

## 怎么挂载配置文件

`docker-compose.yml` 会把本目录下的 `config/` 整体挂载到：

```text
/opt/doclens/config
```

其中：

- `runtime.env` 负责数据库、端口、网关密钥这类启动参数
- `application.yml` 负责 `doclens.*` 业务配置

## 怎么指定数据卷位置

`docker-compose.yml` 会显式创建两个使用 `local` 驱动的命名卷，并把它们绑定到
宿主机目录：

```text
${DOCLENS_DATA_ROOT}/postgresql -> /var/lib/postgresql/data
${DOCLENS_DATA_ROOT}/storage    -> /var/lib/doclens/storage
```

未设置 `DOCLENS_DATA_ROOT` 时，默认使用当前仓库下的：

```text
docker/x86/data/postgresql
docker/x86/data/storage
```

生产环境建议使用绝对路径。先创建目录，再把同一个变量传给 Compose：

```bash
sudo mkdir -p /srv/doclens/data/postgresql /srv/doclens/data/storage
DOCLENS_DATA_ROOT=/srv/doclens/data \
  docker-compose -f docker/x86/docker-compose.yml up -d
```

macOS 使用 Colima 时，自定义目录必须位于 Colima 可访问的共享路径中，例如
`/Users/<用户名>/doclens-data`。自定义目录也必须提前创建。

Docker 不会修改已经存在的命名卷驱动参数。如果这个 Compose 项目已经运行过，
需要先备份或迁移旧卷数据，再重新创建卷；不要在有业务数据时直接执行
`docker-compose down -v`。

## 怎么启动

下面这些命令默认都在仓库根目录执行。

先准备本地构建产物和 x86 镜像：

```bash
scripts/prepare-docker-build-context.sh
PLATFORMS='linux/amd64' scripts/build-local-runtime-images.sh
```

然后启动：

```bash
docker-compose -f docker/x86/docker-compose.yml up -d
```

## 修改配置时要注意

如果你改了 `config/runtime.env` 里的 `DOCLENS_SERVER_PORT` 或 `POSTGRES_PORT`，
记得同时修改 `docker-compose.yml` 里的端口映射；宿主机映射端口不是 Spring
配置能反向控制的。
