#!/usr/bin/env bash
# 使用 Bash 执行入口脚本，保证局部变量和 wait -n 等语法可用。
# -E 继承 ERR 陷阱，-e 遇错退出，-u 禁止未定义变量，pipefail 让管道任一步失败都返回失败。
set -Eeuo pipefail

# 应用发行包的根目录，可通过容器环境变量覆盖。
export APP_HOME="${APP_HOME:-/opt/doclens}"
# 外挂运行时配置目录，默认对应 Dockerfile 声明的配置挂载点。
export DOCLENS_CONFIG_DIR="${DOCLENS_CONFIG_DIR:-/opt/doclens/config}"

# 优先加载挂载进来的启动配置文件，便于交付时通过文件而不是命令行参数改配置。
load_runtime_env() {
  # 保存 runtime.env 的完整路径，供当前 Shell 和后端启动脚本使用。
  local runtime_env_file

  runtime_env_file="${DOCLENS_CONFIG_DIR}/runtime.env"
  # 没有外挂配置时继续使用下面的默认值和镜像内配置。
  if [ ! -f "${runtime_env_file}" ]; then
    return
  fi

  # 自动 export runtime.env 中的变量，让 PostgreSQL 和 Java 子进程继承它们。
  set -a
  # shellcheck disable=SC1090
  # 执行外挂环境文件，把端口、数据库和其他启动参数加载到当前 Shell。
  . "${runtime_env_file}"
  # 恢复默认的非自动导出行为，避免影响后续脚本逻辑。
  set +a
}

# 在推导默认值和启动 PostgreSQL 之前读取外挂启动配置。
load_runtime_env

# PostgreSQL 数据库名称和用户；空值时使用本地单机默认值。
export POSTGRES_DB="${POSTGRES_DB:-doclens}"
# 将 PostgreSQL 连接用户传递给初始化和健康检查命令。
export POSTGRES_USER="${POSTGRES_USER:-doclens}"
# 优先使用显式 PostgreSQL 密码，其次使用 DocLens 数据库密码，最后使用开发默认密码。
export POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-${DOCLENS_DB_PASSWORD:-doclens}}"
# PostgreSQL 数据目录、主版本和监听端口。
# 指定 PostgreSQL 集群的数据目录。
export PGDATA="${PGDATA:-/var/lib/postgresql/data}"
# 指定 PostgreSQL 主版本，用于定位二进制目录。
export PG_MAJOR="${PG_MAJOR:-16}"
# 指定 PostgreSQL 对外监听的容器端口。
export POSTGRES_PORT="${POSTGRES_PORT:-${DOCLENS_POSTGRES_PORT:-5432}}"
# 后端监听端口；兼容 SERVER_PORT 和 DocLens 自有端口变量。
# 保留 DocLens 自有端口变量供入口和其他脚本使用。
export DOCLENS_SERVER_PORT="${DOCLENS_SERVER_PORT:-${SERVER_PORT:-10003}}"
# 同步设置 Spring Boot 识别的标准 SERVER_PORT 变量。
export SERVER_PORT="${SERVER_PORT:-${DOCLENS_SERVER_PORT}}"
# 使用 Spring 的 additional-location 追加配置目录，保留镜像内默认配置并允许外挂配置覆盖同名属性。
export SPRING_CONFIG_ADDITIONAL_LOCATION="${SPRING_CONFIG_ADDITIONAL_LOCATION:-optional:file:${APP_HOME}/conf/,optional:file:${DOCLENS_CONFIG_DIR}/}"

# 将 Shell 层数据库、存储和运行环境参数转换为 Spring 配置可读取的环境变量。
export DOCLENS_DB_URL="${DOCLENS_DB_URL:-jdbc:postgresql://127.0.0.1:${POSTGRES_PORT}/${POSTGRES_DB}}"
# 设置后端访问 PostgreSQL 的用户名。
export DOCLENS_DB_USERNAME="${DOCLENS_DB_USERNAME:-${POSTGRES_USER}}"
# 设置后端访问 PostgreSQL 的密码。
export DOCLENS_DB_PASSWORD="${DOCLENS_DB_PASSWORD:-${POSTGRES_PASSWORD}}"
# 设置 Spring JDBC 使用的 PostgreSQL 驱动类名。
export DOCLENS_DB_DRIVER="${DOCLENS_DB_DRIVER:-org.postgresql.Driver}"
# 设置文档文件持久化目录。
export DOCLENS_STORAGE_ROOT="${DOCLENS_STORAGE_ROOT:-/var/lib/doclens/storage}"
# 默认使用 dev Profile；生产 Compose 的 runtime.env 会显式设置为 prod。
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"
# 将 PostgreSQL 二进制目录放到 PATH，供 pg_ctl、pg_isready 和 postgres 使用。
# 确保 PostgreSQL 的命令优先从当前镜像版本中解析。
export PATH="/usr/lib/postgresql/${PG_MAJOR}/bin:${PATH}"

# 创建业务文件和 PostgreSQL 运行所需的目录，并统一交给 postgres 用户。
mkdir -p "${DOCLENS_STORAGE_ROOT}" "${PGDATA}" /var/run/postgresql
chown -R postgres:postgres "${DOCLENS_STORAGE_ROOT}" "${PGDATA}" /var/run/postgresql

# 生成 initdb 使用的密码文件，避免密码出现在进程参数中。
create_password_file() {
  # 创建临时密码文件，并写入初始化密码。
  password_file="$(mktemp)"
  printf '%s\n' "${POSTGRES_PASSWORD}" >"${password_file}"
  # initdb 由 postgres 用户执行，因此密码文件也必须归 postgres 所有。
  chown postgres:postgres "${password_file}"
  # 限制密码文件权限，避免其他用户读取数据库密码。
  chmod 600 "${password_file}"
  # 将文件路径返回给调用方，用于 initdb 的 --pwfile 参数。
  printf '%s\n' "${password_file}"
}

# 写入容器内 PostgreSQL 访问配置。
configure_postgres_access() {
  # 允许 PostgreSQL 接收容器网络和宿主机端口映射的连接。
  printf "listen_addresses = '*'\n" >>"${PGDATA}/postgresql.conf"
  # 使用 SCRAM 对所有来源的数据库连接进行密码认证。
  printf "host all all 0.0.0.0/0 scram-sha-256\n" >>"${PGDATA}/pg_hba.conf"
}

# 判断目标业务数据库是否已经存在。
database_exists() {
  # 保存查询结果：1 表示数据库存在，空结果表示数据库不存在。
  local exists

  # 以 postgres 用户查询系统目录，并通过参数变量传入数据库名。
  if ! exists="$(runuser -u postgres -- psql --username "${POSTGRES_USER}" --dbname postgres --tuples-only --no-align \
    --set "database_name=${POSTGRES_DB}" <<'SQL'
SELECT 1 FROM pg_database WHERE datname = :'database_name';
SQL
  )"; then
    # 查询失败属于初始化错误，返回专用状态码交给上层处理。
    printf 'Failed to check PostgreSQL database existence: %s\n' "${POSTGRES_DB}" >&2
    return 2
  fi

  # 通过命令退出状态向调用方返回“存在”或“不存在”。
  [ "${exists}" = "1" ]
}

# 在初始化集群中创建业务数据库。
create_database_if_missing() {
  # 临时启动 PostgreSQL，供数据库存在性检查和创建操作使用。
  local exists_status

  runuser -u postgres -- pg_ctl -D "${PGDATA}" -o "-c listen_addresses=''" -w start
  # 允许 database_exists 返回 1，以便区分“不存在”和“查询失败”。
  set +e
  database_exists
  exists_status=$?
  set -e

  # 数据库已经存在时只关闭临时实例，不重复创建。
  if [ "${exists_status}" -eq 0 ]; then
    # 释放存在性检查临时启动的 PostgreSQL 进程。
    runuser -u postgres -- pg_ctl -D "${PGDATA}" -m fast -w stop
    return
  fi

  # 数据库不存在时创建业务数据库。
  if [ "${exists_status}" -eq 1 ]; then
    # 使用初始化用户创建缺失的业务数据库。
    runuser -u postgres -- createdb --username "${POSTGRES_USER}" "${POSTGRES_DB}"
  else
    # 查询失败时先关闭临时实例，再把原始错误码返回给调用方。
    runuser -u postgres -- pg_ctl -D "${PGDATA}" -m fast -w stop
    return "${exists_status}"
  fi
  # 创建完成后关闭临时实例，稍后由主流程启动正式实例。
  runuser -u postgres -- pg_ctl -D "${PGDATA}" -m fast -w stop
}

# 初始化首次启动需要的 PostgreSQL 数据目录。
initialize_postgres() {
  # 生成临时密码文件，并将路径传给 initdb。
  password_file="$(create_password_file)"

  # 初始化 PostgreSQL 集群、管理员用户和本地/远程认证方式。
  runuser -u postgres -- initdb \
    --pgdata="${PGDATA}" \
    --username="${POSTGRES_USER}" \
    --pwfile="${password_file}" \
    --auth-host=scram-sha-256 \
    --auth-local=trust
  # 初始化完成后立即删除明文密码文件。
  rm -f "${password_file}"

  # 配置访问规则并确保业务数据库已经创建。
  configure_postgres_access
  create_database_if_missing
}

# PG_VERSION 不存在时说明数据卷是首次使用，需要初始化 PostgreSQL。
if [ ! -s "${PGDATA}/PG_VERSION" ]; then
  initialize_postgres
fi

# 以 postgres 用户在后台启动正式 PostgreSQL 实例，并记录进程号供退出时回收。
# 后台运行 PostgreSQL，使入口脚本可以继续等待就绪并启动后端。
runuser -u postgres -- postgres -D "${PGDATA}" -p "${POSTGRES_PORT}" &
# 保存 PostgreSQL 后台进程号。
postgres_pid=$!

# 等待 PostgreSQL 接受业务用户连接，避免后端先于数据库启动。
until pg_isready -h 127.0.0.1 -p "${POSTGRES_PORT}" -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" >/dev/null 2>&1; do
  # 数据库尚未就绪时每秒轮询一次。
  sleep 1
done

# 启动后端；后端会读取 Spring 配置并通过 /dashboard/ 提供前端资源。
# 传递容器启动参数给后端启动脚本。
"${APP_HOME}/bin/doclens-server.sh" "$@" &
# 保存后端后台进程号。
app_pid=$!

# 统一停止后端和 PostgreSQL，避免容器退出时遗留子进程。
terminate() {
  # 忽略已经退出进程的 kill/wait 错误，保证清理流程可以继续完成。
  kill -TERM "${app_pid}" "${postgres_pid}" 2>/dev/null || true
  wait "${app_pid}" "${postgres_pid}" 2>/dev/null || true
}

# 接收容器停止信号并执行统一清理逻辑。
trap terminate INT TERM

# 任一核心进程退出后结束容器，并将退出码传给 Docker。
set +e
# 同时等待 PostgreSQL 或后端中的任意一个进程退出。
wait -n "${app_pid}" "${postgres_pid}"
# 保存第一个退出进程的状态码。
exit_code=$?
# 回收仍在运行的另一个核心进程，并使用首个退出码结束容器。
terminate
exit "${exit_code}"
