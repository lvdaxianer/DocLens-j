#!/usr/bin/env bash
set -Eeuo pipefail

export POSTGRES_DB="${POSTGRES_DB:-doclens}"
export POSTGRES_USER="${POSTGRES_USER:-doclens}"
export POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-${DOCLENS_DB_PASSWORD:-doclens}}"
export PGDATA="${PGDATA:-/var/lib/postgresql/data}"
export PG_MAJOR="${PG_MAJOR:-16}"

export DOCLENS_DB_URL="${DOCLENS_DB_URL:-jdbc:postgresql://127.0.0.1:5432/${POSTGRES_DB}}"
export DOCLENS_DB_USERNAME="${DOCLENS_DB_USERNAME:-${POSTGRES_USER}}"
export DOCLENS_DB_PASSWORD="${DOCLENS_DB_PASSWORD:-${POSTGRES_PASSWORD}}"
export DOCLENS_DB_DRIVER="${DOCLENS_DB_DRIVER:-org.postgresql.Driver}"
export DOCLENS_STORAGE_ROOT="${DOCLENS_STORAGE_ROOT:-/var/lib/doclens/storage}"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"
export PATH="/usr/lib/postgresql/${PG_MAJOR}/bin:${PATH}"

mkdir -p "${DOCLENS_STORAGE_ROOT}" "${PGDATA}" /var/run/postgresql
chown -R postgres:postgres "${DOCLENS_STORAGE_ROOT}" "${PGDATA}" /var/run/postgresql

# 生成 initdb 使用的密码文件，避免密码出现在进程参数中。
create_password_file() {
  password_file="$(mktemp)"
  printf '%s\n' "${POSTGRES_PASSWORD}" >"${password_file}"
  chmod 600 "${password_file}"
  printf '%s\n' "${password_file}"
}

# 写入容器内 PostgreSQL 访问配置。
configure_postgres_access() {
  printf "listen_addresses = '*'\n" >>"${PGDATA}/postgresql.conf"
  printf "host all all 0.0.0.0/0 scram-sha-256\n" >>"${PGDATA}/pg_hba.conf"
}

# 判断目标业务数据库是否已经存在。
database_exists() {
  runuser -u postgres -- psql --username "${POSTGRES_USER}" --dbname postgres --tuples-only --no-align \
    --set "database_name=${POSTGRES_DB}" \
    --command "SELECT 1 FROM pg_database WHERE datname = :'database_name'" | grep -q 1
}

# 在初始化集群中创建业务数据库。
create_database_if_missing() {
  runuser -u postgres -- pg_ctl -D "${PGDATA}" -o "-c listen_addresses=''" -w start
  if ! database_exists; then
    runuser -u postgres -- createdb --username "${POSTGRES_USER}" "${POSTGRES_DB}"
  fi
  runuser -u postgres -- pg_ctl -D "${PGDATA}" -m fast -w stop
}

# 初始化首次启动需要的 PostgreSQL 数据目录。
initialize_postgres() {
  password_file="$(create_password_file)"

  runuser -u postgres -- initdb \
    --pgdata="${PGDATA}" \
    --username="${POSTGRES_USER}" \
    --pwfile="${password_file}" \
    --auth-host=scram-sha-256 \
    --auth-local=trust
  rm -f "${password_file}"

  configure_postgres_access
  create_database_if_missing
}

if [ ! -s "${PGDATA}/PG_VERSION" ]; then
  initialize_postgres
fi

runuser -u postgres -- postgres -D "${PGDATA}" &
postgres_pid=$!

until pg_isready -h 127.0.0.1 -p 5432 -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" >/dev/null 2>&1; do
  sleep 1
done

"${APP_HOME}/bin/doclens-server.sh" "$@" &
app_pid=$!

terminate() {
  kill -TERM "${app_pid}" "${postgres_pid}" 2>/dev/null || true
  wait "${app_pid}" "${postgres_pid}" 2>/dev/null || true
}

trap terminate INT TERM

set +e
wait -n "${app_pid}" "${postgres_pid}"
exit_code=$?
terminate
exit "${exit_code}"
