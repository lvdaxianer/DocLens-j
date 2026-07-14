#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/dev-common.sh"

if [[ "${1:-}" == "--help" ]]; then
  echo "停止前后端开发服务"
  exit 0
fi

stop_service "${BACKEND_PID_FILE}"
stop_service "${FRONTEND_PID_FILE}"
stop_service_by_port "${BACKEND_PORT}"
stop_service_by_port "${FRONTEND_PORT}"
stop_postgresql_service

echo "stopped frontend, backend and postgresql"
