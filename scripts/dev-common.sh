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
FRONTEND_PORT=10002
BACKEND_PORT=10003
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
