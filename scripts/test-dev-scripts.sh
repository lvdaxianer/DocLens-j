#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

assert_file() {
  local file_path="$1"
  if [[ ! -f "${ROOT_DIR}/${file_path}" ]]; then
    echo "missing file: ${file_path}" >&2
    exit 1
  fi
}

assert_help() {
  local file_path="$1"
  local expected="$2"
  local output
  output="$("${ROOT_DIR}/${file_path}" --help)"
  if [[ "${output}" != *"${expected}"* ]]; then
    echo "unexpected help output for ${file_path}" >&2
    exit 1
  fi
}

assert_file "scripts/dev-up.sh"
assert_file "scripts/dev-down.sh"
assert_file "scripts/dev-restart.sh"
assert_file "scripts/dev-status.sh"
assert_file "docker-compose.yml"
assert_help "scripts/dev-up.sh" "启动前后端开发服务"
assert_help "scripts/dev-down.sh" "停止前后端开发服务"
assert_help "scripts/dev-restart.sh" "强制重启前后端开发服务"
assert_help "scripts/dev-status.sh" "查看前后端开发服务状态"

if ! rg -n "postgresql:|postgres:16-alpine|DOCLENS_POSTGRES_PORT:-5432" "${ROOT_DIR}/docker-compose.yml" >/dev/null; then
  echo "docker compose missing local PostgreSQL service" >&2
  exit 1
fi

if ! rg -n "start_postgresql_service" "${ROOT_DIR}/scripts/dev-up.sh" >/dev/null; then
  echo "dev-up must start PostgreSQL before backend" >&2
  exit 1
fi

if ! rg -n "stop_postgresql_service" "${ROOT_DIR}/scripts/dev-down.sh" >/dev/null; then
  echo "dev-down must stop PostgreSQL compose service" >&2
  exit 1
fi

if ! rg -n "postgresql_state_line" "${ROOT_DIR}/scripts/dev-status.sh" >/dev/null; then
  echo "dev-status must report PostgreSQL service state" >&2
  exit 1
fi

backend_restart="${ROOT_DIR}/scripts/dev-restart.sh"
if ! rg -n "DOCLENS_LOCAL_CREDENTIAL_KEY=.*:-dlk_EUhMBpKkk6UGX3smR-8DkMmF-nlBtzAniXQLAGkutKc" \
  "${backend_restart}" >/dev/null; then
  echo "dev restart missing default local caller credential key" >&2
  exit 1
fi
backend_runner="${ROOT_DIR}/scripts/run-backend-dev.sh"
if ! rg -n -- "--no-stdin" "${backend_runner}" >/dev/null; then
  echo "backend runner must keep nodemon alive when started by nohup" >&2
  exit 1
fi
if ! rg -n "DOCLENS_DB_URL=.*jdbc:postgresql://localhost|DOCLENS_DB_DRIVER=.*org.postgresql.Driver" \
  "${backend_runner}" >/dev/null; then
  echo "backend runner missing PostgreSQL datasource defaults" >&2
  exit 1
fi

status_output="$("${ROOT_DIR}/scripts/dev-status.sh")"
if [[ "${status_output}" != *"frontend"* ]]; then
  echo "status output missing frontend section" >&2
  exit 1
fi

if ! rg -n "./scripts/dev-up.sh|./scripts/dev-down.sh|./scripts/dev-restart.sh|./scripts/dev-status.sh" \
  "${ROOT_DIR}/README.md" \
  "${ROOT_DIR}/doclens-dashboard/README.md" >/dev/null; then
  echo "documentation missing helper scripts" >&2
  exit 1
fi

"${ROOT_DIR}/scripts/check-docs.sh"
