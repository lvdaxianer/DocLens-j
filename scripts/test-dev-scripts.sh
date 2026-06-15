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
assert_help "scripts/dev-up.sh" "启动前后端开发服务"
assert_help "scripts/dev-down.sh" "停止前后端开发服务"
assert_help "scripts/dev-restart.sh" "强制重启前后端开发服务"
assert_help "scripts/dev-status.sh" "查看前后端开发服务状态"

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
