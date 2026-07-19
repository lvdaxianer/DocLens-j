#!/usr/bin/env sh
set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
DOCLENS_CONFIG_DIR=${DOCLENS_CONFIG_DIR:-"$APP_HOME/config"}
JAR_FILE=$(find "$APP_HOME/lib" -maxdepth 1 -name 'doclens-server-*.jar' | head -n 1)

if [ -z "$JAR_FILE" ]; then
  echo "doclens-server jar was not found under $APP_HOME/lib" >&2
  exit 1
fi

load_runtime_env() {
  runtime_env_file="$DOCLENS_CONFIG_DIR/runtime.env"
  if [ ! -f "$runtime_env_file" ]; then
    return
  fi

  # 优先补充挂载进来的运行时参数，保证单机交付时通过文件改配置也能传给 Java 进程。
  set -a
  # shellcheck disable=SC1090
  . "$runtime_env_file"
  set +a
}

load_runtime_env

# 对允许留空的 runtime.env 字段重新补默认值，避免空串覆盖入口脚本已经推导出的配置。
POSTGRES_DB=${POSTGRES_DB:-doclens}
POSTGRES_USER=${POSTGRES_USER:-doclens}
POSTGRES_PASSWORD=${POSTGRES_PASSWORD:-${DOCLENS_DB_PASSWORD:-doclens}}
POSTGRES_PORT=${POSTGRES_PORT:-${DOCLENS_POSTGRES_PORT:-5432}}
DOCLENS_SERVER_PORT=${DOCLENS_SERVER_PORT:-${SERVER_PORT:-10003}}
SERVER_PORT=${SERVER_PORT:-${DOCLENS_SERVER_PORT}}
DOCLENS_DB_URL=${DOCLENS_DB_URL:-"jdbc:postgresql://127.0.0.1:${POSTGRES_PORT}/${POSTGRES_DB}"}
DOCLENS_DB_USERNAME=${DOCLENS_DB_USERNAME:-${POSTGRES_USER}}
DOCLENS_DB_PASSWORD=${DOCLENS_DB_PASSWORD:-${POSTGRES_PASSWORD}}
DOCLENS_DB_DRIVER=${DOCLENS_DB_DRIVER:-org.postgresql.Driver}
DOCLENS_STORAGE_ROOT=${DOCLENS_STORAGE_ROOT:-/var/lib/doclens/storage}

export DOCLENS_SERVER_PORT
export SERVER_PORT
export DOCLENS_DB_URL
export DOCLENS_DB_USERNAME
export DOCLENS_DB_PASSWORD
export DOCLENS_DB_DRIVER
export DOCLENS_STORAGE_ROOT
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
export SPRING_CONFIG_ADDITIONAL_LOCATION="${SPRING_CONFIG_ADDITIONAL_LOCATION:-optional:file:$APP_HOME/conf/,optional:file:${DOCLENS_CONFIG_DIR}/}"

exec java ${JAVA_OPTS:-} -jar "$JAR_FILE" "$@"
