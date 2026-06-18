#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/dev-common.sh"

if [[ "${1:-}" == "--help" ]]; then
  echo "强制重启前后端开发服务"
  exit 0
fi

require_commands bash curl lsof mvn node npm nodemon
export DOCLENS_LOCAL_CREDENTIAL_KEY="${DOCLENS_LOCAL_CREDENTIAL_KEY:-dlk_EUhMBpKkk6UGX3smR-8DkMmF-nlBtzAniXQLAGkutKc}"

echo "stopping old frontend and backend processes..."
stop_service "${BACKEND_PID_FILE}"
stop_service "${FRONTEND_PID_FILE}"
stop_service_by_port "${BACKEND_PORT}"
stop_service_by_port "${FRONTEND_PORT}"
cleanup_pid_file "${BACKEND_PID_FILE}"
cleanup_pid_file "${FRONTEND_PID_FILE}"

ensure_port_available "${FRONTEND_PORT}" "frontend"
ensure_port_available "${BACKEND_PORT}" "backend"

echo "starting frontend..."
start_frontend_service
if ! wait_for_http "${FRONTEND_URL}" 20 1; then
  stop_service "${FRONTEND_PID_FILE}"
  echo "frontend failed to start, check ${FRONTEND_LOG_FILE}" >&2
  exit 1
fi

echo "starting backend..."
start_backend_service
if ! wait_for_http "${BACKEND_HEALTH_URL}" 60 1; then
  stop_service "${BACKEND_PID_FILE}"
  stop_service "${FRONTEND_PID_FILE}"
  echo "backend failed to start, check ${BACKEND_LOG_FILE}" >&2
  exit 1
fi

echo "frontend: ${FRONTEND_URL}"
echo "backend: ${BACKEND_HEALTH_URL}"
echo "logs: ${RUN_DIR}"
