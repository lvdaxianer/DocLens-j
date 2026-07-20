# x86 Compose 数据卷位置配置设计

## 目标

让 `docker/x86/docker-compose.yml` 显式创建 PostgreSQL 与 DocLens 文件存储命名卷，
同时允许部署者通过 `DOCLENS_DATA_ROOT` 指定数据在宿主机上的实际保存位置。

## 设计

两个数据目录直接在 Compose 服务中声明为 bind mount，并启用
`create_host_path`。数据库绑定到 `${DOCLENS_DATA_ROOT}/postgresql`，文件存储
绑定到 `${DOCLENS_DATA_ROOT}/storage`。

未设置 `DOCLENS_DATA_ROOT` 时，Compose 使用 Docker 主机内部的
`/var/lib/doclens-x86`。该绝对路径不依赖命令执行目录；在 Colima 环境中位于
Linux 虚拟机内部，具备 PostgreSQL 初始化所需的 Unix 权限语义。

## 运行约束

- `DOCLENS_DATA_ROOT` 必须是绝对路径。
- Compose 必须使用 `create_host_path` 自动创建两个绑定源目录。
- macOS 使用 Colima 时，PostgreSQL 不得放在 `/Users/...` 共享目录。
- 旧命名卷不会自动迁移到绑定目录；已有数据需要先备份或迁移。

## 验证

- 交付布局脚本必须检查 Compose 中的变量、两个 bind 目标和自动目录创建配置。
- `docker-compose config` 必须能展开默认根目录和自定义根目录。
- 使用临时 Compose 项目和临时根目录创建卷后，`docker volume inspect` 的
  `Mountpoint`/驱动选项必须指向指定目录。
