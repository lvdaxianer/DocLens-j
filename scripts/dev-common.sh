#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="${ROOT_DIR}/var/dev"
FRONTEND_PID_FILE="${RUN_DIR}/frontend.pid"
BACKEND_PID_FILE="${RUN_DIR}/backend.pid"
FRONTEND_LOG_FILE="${RUN_DIR}/frontend.log"
BACKEND_LOG_FILE="${RUN_DIR}/backend.log"
FRONTEND_RUNNER="${ROOT_DIR}/scripts/run-frontend-dev.sh"
BACKEND_RUNNER="${ROOT_DIR}/scripts/run-backend-dev.sh"
COMPOSE_FILE="${ROOT_DIR}/docker-compose.yml"
POSTGRESQL_SERVICE="postgresql"
FRONTEND_PORT=10002
BACKEND_PORT=10003
POSTGRESQL_PORT="${DOCLENS_POSTGRES_PORT:-5432}"
POSTGRESQL_DB="${DOCLENS_POSTGRES_DB:-doclens}"
POSTGRESQL_USER="${DOCLENS_POSTGRES_USER:-doclens}"
FRONTEND_URL="http://127.0.0.1:${FRONTEND_PORT}/dashboard/"
BACKEND_HEALTH_URL="http://127.0.0.1:${BACKEND_PORT}/actuator/health"
ensure_run_dir() {
  mkdir -p "${RUN_DIR}"
}

is_pid_running() {
  local pid="$1"
  kill -0 "${pid}" 2>/dev/null
}

read_pid() {
  local pid_file="$1"
  if [[ -f "${pid_file}" ]]; then
    cat "${pid_file}"
  fi
}

cleanup_pid_file() {
  local pid_file="$1"
  local pid
  pid="$(read_pid "${pid_file}")"
  if [[ -n "${pid}" ]] && ! is_pid_running "${pid}"; then
    rm -f "${pid_file}"
  fi
}

require_commands() {
  local missing=()
  local command_name
  for command_name in "$@"; do
    if ! command -v "${command_name}" >/dev/null 2>&1; then
      missing+=("${command_name}")
    fi
  done
  if [[ "${#missing[@]}" -gt 0 ]]; then
    printf 'missing command: %s\n' "${missing[*]}" >&2
    exit 1
  fi
}

# 判断当前环境是否可执行 Docker Compose。
has_docker_compose() {
  docker compose version >/dev/null 2>&1 || command -v docker-compose >/dev/null 2>&1
}

# 确认 Docker Compose 可用，否则终止本地开发启动流程。
require_docker_compose() {
  if ! has_docker_compose; then
    echo "missing command: docker compose or docker-compose" >&2
    exit 1
  fi
}

# 兼容 Docker Compose 插件和 standalone docker-compose 命令。
docker_compose() {
  if docker compose version >/dev/null 2>&1; then
    docker compose -f "${COMPOSE_FILE}" "$@"
    return 0
  fi
  if command -v docker-compose >/dev/null 2>&1; then
    docker-compose -f "${COMPOSE_FILE}" "$@"
    return 0
  fi
  echo "missing command: docker compose or docker-compose" >&2
  return 1
}

port_pid() {
  local port="$1"
  (lsof -tiTCP:"${port}" -sTCP:LISTEN 2>/dev/null || true) | head -n 1
}

ensure_port_available() {
  local port="$1"
  local service_name="$2"
  local pid
  pid="$(port_pid "${port}")"
  if [[ -n "${pid}" ]]; then
    echo "${service_name} port ${port} is already in use by PID ${pid}" >&2
    exit 1
  fi
}

stop_service() {
  local pid_file="$1"
  local pid
  pid="$(read_pid "${pid_file}")"
  if [[ -z "${pid}" ]]; then
    return 0
  fi
  if is_pid_running "${pid}"; then
    kill "${pid}" 2>/dev/null || true
    for _ in 1 2 3 4 5 6 7 8 9 10; do
      if ! is_pid_running "${pid}"; then
        break
      fi
      sleep 1
    done
    if is_pid_running "${pid}"; then
      kill -9 "${pid}" 2>/dev/null || true
    fi
  fi
  rm -f "${pid_file}"
}

stop_service_by_port() {
  local port="$1"
  local pid
  pid="$(port_pid "${port}")"
  if [[ -n "${pid}" ]] && is_pid_running "${pid}"; then
    kill "${pid}" 2>/dev/null || true
    sleep 1
    if is_pid_running "${pid}"; then
      kill -9 "${pid}" 2>/dev/null || true
    fi
  fi
}

wait_for_http() {
  local url="$1"
  local retries="$2"
  local delay_seconds="$3"
  local attempt
  for attempt in $(seq 1 "${retries}"); do
    if curl --silent --fail --max-time 3 "${url}" >/dev/null 2>&1; then
      return 0
    fi
    sleep "${delay_seconds}"
  done
  return 1
}

# 等待 PostgreSQL 容器进入可连接状态。
wait_for_postgresql_service() {
  local attempt
  for attempt in $(seq 1 40); do
    if docker_compose exec -T "${POSTGRESQL_SERVICE}" \
      pg_isready -U "${POSTGRESQL_USER}" -d "${POSTGRESQL_DB}" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  return 1
}

# 启动本地 PostgreSQL compose 服务。
start_postgresql_service() {
  require_docker_compose
  docker_compose up -d "${POSTGRESQL_SERVICE}" >/dev/null
  if ! wait_for_postgresql_service; then
    echo "postgresql failed to start from ${COMPOSE_FILE}" >&2
    exit 1
  fi
}

# 停止本地 PostgreSQL compose 服务，保留数据卷便于下次复用。
stop_postgresql_service() {
  if command -v docker >/dev/null 2>&1 && has_docker_compose; then
    docker_compose stop "${POSTGRESQL_SERVICE}" >/dev/null 2>&1 || true
  fi
}

# 输出 PostgreSQL compose 服务状态。
postgresql_state_line() {
  if ! command -v docker >/dev/null 2>&1 || ! has_docker_compose; then
    echo "postgresql: unavailable port=${POSTGRESQL_PORT} compose=${COMPOSE_FILE}"
    return 0
  fi
  if docker_compose ps "${POSTGRESQL_SERVICE}" 2>/dev/null | grep -q "Up"; then
    echo "postgresql: running port=${POSTGRESQL_PORT} database=${POSTGRESQL_DB}"
  else
    echo "postgresql: stopped port=${POSTGRESQL_PORT} database=${POSTGRESQL_DB}"
  fi
}

start_frontend_service() {
  ensure_run_dir
  nohup "${FRONTEND_RUNNER}" </dev/null >"${FRONTEND_LOG_FILE}" 2>&1 &
  echo "$!" >"${FRONTEND_PID_FILE}"
}

start_backend_service() {
  ensure_run_dir
  nohup "${BACKEND_RUNNER}" </dev/null >"${BACKEND_LOG_FILE}" 2>&1 &
  echo "$!" >"${BACKEND_PID_FILE}"
}

service_state_line() {
  local service_name="$1"
  local pid_file="$2"
  local port="$3"
  cleanup_pid_file "${pid_file}"
  local pid
  pid="$(read_pid "${pid_file}")"
  if [[ -n "${pid}" ]] && is_pid_running "${pid}"; then
    echo "${service_name}: running pid=${pid} port=${port}"
    return 0
  fi
  pid="$(port_pid "${port}")"
  if [[ -n "${pid}" ]]; then
    echo "${service_name}: running pid=${pid} port=${port} source=port-scan"
  else
    echo "${service_name}: stopped port=${port}"
  fi
}
