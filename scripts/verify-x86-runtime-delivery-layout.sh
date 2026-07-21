#!/usr/bin/env bash
set -euo pipefail

x86_compose="${1:-docker/x86/docker-compose.yml}"
x86_readme="${2:-docker/x86/README.md}"
runtime_env="${3:-docker/x86/config/runtime.env}"
application_yml="${4:-docker/x86/config/application.yml}"
entrypoint="${5:-docker/entrypoint.sh}"
dockerfile="${6:-Dockerfile}"
packaging_doc="${7:-docs/packaging.md}"
packaging_doc_en="${8:-docs/packaging_en.md}"
legacy_compose="${9:-docker-compose.all-in-one.yml}"
legacy_examples_dir="${10:-docker/examples/all-in-one}"
server_launcher="${11:-doclens-server/src/assembly/bin/doclens-server.sh}"

failures=0

# 校验示例文件是否已经落库。
require_file() {
  local file="$1"
  local message="$2"

  if [ ! -f "${file}" ]; then
    printf 'FAIL: %s\n' "${message}" >&2
    failures=$((failures + 1))
  fi
}

# 校验字面字符串是否出现在目标文件中。
require_literal() {
  local file="$1"
  local text="$2"
  local message="$3"

  if [ ! -f "${file}" ] || ! grep -Fq -- "${text}" "${file}"; then
    printf 'FAIL: %s\n' "${message}" >&2
    failures=$((failures + 1))
  fi
}

# 校验目标文件不包含会注入具体 OCR 节点的配置。
require_absent_literal() {
  local file="$1"
  local text="$2"
  local message="$3"

  if [ -f "${file}" ] && grep -Fq -- "${text}" "${file}"; then
    printf 'FAIL: %s\n' "${message}" >&2
    failures=$((failures + 1))
  fi
}

# 校验旧示例已经从最终交付面移除。
require_missing() {
  local path="$1"
  local message="$2"

  if [ -e "${path}" ]; then
    printf 'FAIL: %s\n' "${message}" >&2
    failures=$((failures + 1))
  fi
}

require_file "${x86_compose}" \
  'x86 delivery compose file must exist.'
require_literal "${x86_compose}" 'doclens:${DOCLENS_IMAGE_TAG:-0.1.0-beta.1-amd64}' \
  'x86 delivery compose file must default to the immutable amd64 beta image.'
require_literal "${x86_compose}" './config:/opt/doclens/config:ro' \
  'x86 delivery compose file must mount the backend config directory.'
require_literal "${x86_compose}" 'DOCLENS_DATA_ROOT' \
  'x86 delivery compose file must expose a configurable data root.'
require_literal "${x86_compose}" 'type: bind' \
  'x86 delivery compose file must use explicit host bind mounts.'
require_literal "${x86_compose}" 'create_host_path: true' \
  'x86 delivery compose file must create missing host data directories.'
require_literal "${x86_compose}" '${DOCLENS_DATA_ROOT:-/var/lib/doclens-x86}/postgresql' \
  'PostgreSQL volume must use the configurable data root.'
require_literal "${x86_compose}" '${DOCLENS_DATA_ROOT:-/var/lib/doclens-x86}/storage' \
  'DocLens storage volume must use the configurable data root.'

require_file "${x86_readme}" \
  'x86 delivery README must exist.'
require_literal "${x86_readme}" 'doclens-server-dist.tar.gz' \
  'x86 delivery README must explain the backend archive copy.'
require_literal "${x86_readme}" 'static/dashboard' \
  'x86 delivery README must explain where frontend assets are packaged.'
require_literal "${x86_readme}" 'PostgreSQL' \
  'x86 delivery README must explain the startup order.'

require_file "${runtime_env}" \
  'Mounted runtime env file must exist.'
require_literal "${runtime_env}" 'POSTGRES_DB=' \
  'Mounted runtime env file must document PostgreSQL startup values.'
require_literal "${runtime_env}" 'DOCLENS_GATEWAY_SECRET=' \
  'Mounted runtime env file must document gateway startup values.'
require_absent_literal "${runtime_env}" 'DOCLENS_PADDLE_OCR_NODE_' \
  'Mounted runtime env file must not auto-register a concrete OCR node.'

require_file "${application_yml}" \
  'Mounted backend application.yml must exist.'
require_literal "${application_yml}" 'doclens:' \
  'Mounted backend application.yml must include doclens runtime settings.'

require_literal "${entrypoint}" 'runtime.env' \
  'Entrypoint must load mounted runtime.env before startup.'
require_literal "${server_launcher}" 'runtime.env' \
  'Packaged server launcher must preserve the mounted runtime.env hook.'
require_literal "${dockerfile}" 'static/dashboard' \
  'Dockerfile must explain where frontend assets come from.'
require_literal "${dockerfile}" 'doclens-server-dist.tar.gz' \
  'Dockerfile must explain that the backend archive is copied into the image.'

require_literal "${packaging_doc}" 'docker/x86/docker-compose.yml' \
  'Chinese packaging guide must mention the x86 delivery compose file.'
require_literal "${packaging_doc}" 'docker/x86/config/runtime.env' \
  'Chinese packaging guide must mention the mounted runtime env file.'
require_literal "${packaging_doc}" 'docker/x86/config/application.yml' \
  'Chinese packaging guide must mention the mounted backend application.yml.'
require_literal "${packaging_doc_en}" 'docker/x86/docker-compose.yml' \
  'English packaging guide must mention the x86 delivery compose file.'

require_missing "${legacy_compose}" \
  'Legacy generic all-in-one compose file must be removed.'
require_missing "${legacy_examples_dir}" \
  'Legacy generic all-in-one example directory must be removed.'

if [ "${failures}" -gt 0 ]; then
  exit 1
fi

printf 'x86 runtime delivery layout verified.\n'
