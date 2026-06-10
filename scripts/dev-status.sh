#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/dev-common.sh"

if [[ "${1:-}" == "--help" ]]; then
  echo "查看前后端开发服务状态"
  exit 0
fi

echo "$(service_state_line "frontend" "${FRONTEND_PID_FILE}" "${FRONTEND_PORT}")"
echo "frontend_url: ${FRONTEND_URL}"
echo "frontend_log: ${FRONTEND_LOG_FILE}"
echo "$(service_state_line "backend" "${BACKEND_PID_FILE}" "${BACKEND_PORT}")"
echo "backend_health: ${BACKEND_HEALTH_URL}"
echo "backend_log: ${BACKEND_LOG_FILE}"
