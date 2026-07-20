# x86 Compose 数据卷位置配置设计

## 目标

让 `docker/x86/docker-compose.yml` 显式创建 PostgreSQL 与 DocLens 文件存储命名卷，
同时允许部署者通过 `DOCLENS_DATA_ROOT` 指定数据在宿主机上的实际保存位置。

## 设计

两个卷继续在 Compose 顶层 `volumes` 中声明，并使用 Docker `local` 驱动的 bind
选项。数据库卷绑定到 `${DOCLENS_DATA_ROOT}/postgresql`，文件存储卷绑定到
`${DOCLENS_DATA_ROOT}/storage`。

未设置 `DOCLENS_DATA_ROOT` 时，Compose 使用 `${PWD}/docker/x86/data`。仓库提供
默认子目录并忽略其中运行时数据，使首次启动具备有效的绑定源目录且不会误提交数据。

## 运行约束

- `DOCLENS_DATA_ROOT` 必须是绝对路径。
- 自定义根目录下的 `postgresql` 和 `storage` 子目录必须在启动前创建。
- macOS 使用 Colima 时，自定义目录必须位于 Colima 可共享的宿主机路径中。
- 已经创建的 Docker 卷不会因修改 Compose 驱动参数而原地迁移；已有数据需要先备份或迁移。

## 验证

- 交付布局脚本必须检查 Compose 中的变量、local 驱动和两个 bind 目标。
- `docker-compose config` 必须能展开默认根目录和自定义根目录。
- 使用临时 Compose 项目和临时根目录创建卷后，`docker volume inspect` 的
  `Mountpoint`/驱动选项必须指向指定目录。
