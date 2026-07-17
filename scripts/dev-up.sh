#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/dev-common.sh"

if [[ "${1:-}" == "--help" ]]; then
  echo "启动前后端开发服务"
  exit 0
fi

require_commands bash curl lsof mvn node npm nodemon docker
cleanup_pid_file "${FRONTEND_PID_FILE}"
cleanup_pid_file "${BACKEND_PID_FILE}"

if [[ -n "$(read_pid "${FRONTEND_PID_FILE}")" ]] || [[ -n "$(read_pid "${BACKEND_PID_FILE}")" ]]; then
  echo "existing dev services detected, run ./scripts/dev-down.sh first" >&2
  exit 1
fi

ensure_port_available "${FRONTEND_PORT}" "frontend"
ensure_port_available "${BACKEND_PORT}" "backend"

start_postgresql_service
start_frontend_service
if ! wait_for_http "${FRONTEND_URL}" 20 1; then
  stop_postgresql_service
  stop_service "${FRONTEND_PID_FILE}"
  echo "frontend failed to start, check ${FRONTEND_LOG_FILE}" >&2
  exit 1
fi

start_backend_service
if ! wait_for_http "${BACKEND_HEALTH_URL}" 40 1; then
  stop_service "${BACKEND_PID_FILE}"
  stop_service "${FRONTEND_PID_FILE}"
  stop_postgresql_service
  echo "backend failed to start, check ${BACKEND_LOG_FILE}" >&2
  exit 1
fi

echo "postgresql: localhost:${POSTGRESQL_PORT}/${POSTGRESQL_DB}"
echo "frontend: ${FRONTEND_URL}"
echo "backend: ${BACKEND_HEALTH_URL}"
echo "logs: ${RUN_DIR}"
