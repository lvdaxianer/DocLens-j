#!/usr/bin/env bash
set -Eeuo pipefail

export POSTGRES_DB="${POSTGRES_DB:-doclens}"
export POSTGRES_USER="${POSTGRES_USER:-doclens}"
export POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-${DOCLENS_DB_PASSWORD:-doclens}}"

export DOCLENS_DB_URL="${DOCLENS_DB_URL:-jdbc:postgresql://127.0.0.1:5432/${POSTGRES_DB}}"
export DOCLENS_DB_USERNAME="${DOCLENS_DB_USERNAME:-${POSTGRES_USER}}"
export DOCLENS_DB_PASSWORD="${DOCLENS_DB_PASSWORD:-${POSTGRES_PASSWORD}}"
export DOCLENS_DB_DRIVER="${DOCLENS_DB_DRIVER:-org.postgresql.Driver}"
export DOCLENS_STORAGE_ROOT="${DOCLENS_STORAGE_ROOT:-/var/lib/doclens/storage}"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"

mkdir -p "${DOCLENS_STORAGE_ROOT}"

/usr/local/bin/docker-entrypoint.sh postgres &
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
